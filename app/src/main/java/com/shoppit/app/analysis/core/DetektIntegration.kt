package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Integration with Detekt static analysis tool.
 * 
 * Runs Detekt programmatically and converts findings to our Finding model.
 */
interface DetektIntegration {
    /**
     * Runs Detekt analysis on the specified paths
     * 
     * @param paths List of file or directory paths to analyze
     * @param configPath Path to Detekt configuration file
     * @return List of findings from Detekt
     */
    suspend fun runDetekt(
        paths: List<String>,
        configPath: String = "app/detekt-config.yml"
    ): Result<List<Finding>>
    
    /**
     * Converts Detekt Issue to our Finding model
     */
    fun convertIssue(issue: Issue, file: String, line: Int): Finding
}

/**
 * Implementation of DetektIntegration
 */
class DetektIntegrationImpl(
    private val projectRoot: String
) : DetektIntegration {
    
    override suspend fun runDetekt(
        paths: List<String>,
        configPath: String
    ): Result<List<Finding>> = withContext(Dispatchers.IO) {
        try {
            Timber.d("Running Detekt analysis on ${paths.size} paths")
            
            // Validate config file exists
            val configFile = File(projectRoot, configPath)
            if (!configFile.exists()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Detekt config file not found: ${configFile.absolutePath}")
                )
            }
            
            // Validate paths exist
            val validPaths = paths.mapNotNull { path ->
                val file = File(projectRoot, path)
                if (file.exists()) {
                    file.toPath()
                } else {
                    Timber.w("Path not found: ${file.absolutePath}")
                    null
                }
            }
            
            if (validPaths.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            // Create Detekt settings
            val settings = ProcessingSettings(
                inputPaths = validPaths,
                config = loadConfiguration(configFile.toPath()),
                outPrinter = System.out,
                errorPrinter = System.err
            )
            
            // Run Detekt
            val detektFacade = DetektFacade.create(settings)
            val detektion = detektFacade.run()
            
            // Convert findings
            val findings = mutableListOf<Finding>()
            detektion.findings.forEach { (ruleSetId, issueList) ->
                issueList.forEach { issue ->
                    val finding = convertDetektIssue(
                        issue = issue,
                        ruleSetId = ruleSetId
                    )
                    findings.add(finding)
                }
            }
            
            Timber.d("Detekt found ${findings.size} issues")
            Result.success(findings)
            
        } catch (e: Exception) {
            Timber.e(e, "Error running Detekt")
            Result.failure(e)
        }
    }
    
    override fun convertIssue(issue: Issue, file: String, line: Int): Finding {
        return Finding(
            id = "detekt-${issue.id}-$file-$line",
            analyzer = "detekt",
            category = mapRuleSetToCategory(issue.id),
            priority = mapSeverityToPriority(issue.severity),
            title = issue.id,
            description = issue.description,
            file = file,
            lineNumber = line,
            columnNumber = null,
            codeSnippet = "",
            recommendation = "Fix this ${issue.id} issue. ${issue.description}",
            beforeExample = null,
            afterExample = null,
            autoFixable = false,
            autoFix = null,
            effort = mapDebtToEffort(issue.debt),
            references = listOf("https://detekt.dev/docs/rules/${issue.id.lowercase()}"),
            relatedFindings = emptyList()
        )
    }
    
    /**
     * Converts Detekt Issue to our Finding model
     */
    private fun convertDetektIssue(
        issue: io.gitlab.arturbosch.detekt.api.Finding,
        ruleSetId: String
    ): Finding {
        val file = issue.location.file
        val line = issue.location.source.line
        
        return Finding(
            id = "detekt-${issue.id}-${file.replace("/", "-")}-$line",
            analyzer = "detekt",
            category = mapRuleSetToCategory(ruleSetId),
            priority = mapSeverityToPriority(issue.severity),
            title = "${issue.id} (Detekt)",
            description = issue.messageOrDescription(),
            file = file,
            lineNumber = line,
            columnNumber = issue.location.source.column,
            codeSnippet = issue.entity.signature ?: "",
            recommendation = buildRecommendation(issue),
            beforeExample = null,
            afterExample = null,
            autoFixable = false,
            autoFix = null,
            effort = mapDebtToEffort(issue.issue.debt),
            references = listOf(
                "https://detekt.dev/docs/rules/${ruleSetId.lowercase()}#${issue.id.lowercase()}"
            ),
            relatedFindings = emptyList()
        )
    }
    
    /**
     * Maps Detekt rule set to our analysis category
     */
    private fun mapRuleSetToCategory(ruleSetId: String): AnalysisCategory {
        return when {
            ruleSetId.contains("complexity", ignoreCase = true) -> AnalysisCategory.CODE_SMELL
            ruleSetId.contains("coroutines", ignoreCase = true) -> AnalysisCategory.STATE_MANAGEMENT
            ruleSetId.contains("empty-blocks", ignoreCase = true) -> AnalysisCategory.CODE_SMELL
            ruleSetId.contains("exceptions", ignoreCase = true) -> AnalysisCategory.ERROR_HANDLING
            ruleSetId.contains("naming", ignoreCase = true) -> AnalysisCategory.NAMING
            ruleSetId.contains("performance", ignoreCase = true) -> AnalysisCategory.PERFORMANCE
            ruleSetId.contains("potential-bugs", ignoreCase = true) -> AnalysisCategory.CODE_SMELL
            ruleSetId.contains("style", ignoreCase = true) -> AnalysisCategory.NAMING
            ruleSetId.contains("compose", ignoreCase = true) -> AnalysisCategory.COMPOSE
            ruleSetId.contains("comments", ignoreCase = true) -> AnalysisCategory.DOCUMENTATION
            else -> AnalysisCategory.CODE_SMELL
        }
    }
    
    /**
     * Maps Detekt severity to our priority
     */
    private fun mapSeverityToPriority(severity: Severity): Priority {
        return when (severity) {
            Severity.CodeSmell -> Priority.LOW
            Severity.Style -> Priority.LOW
            Severity.Warning -> Priority.MEDIUM
            Severity.Defect -> Priority.HIGH
            Severity.Maintainability -> Priority.MEDIUM
            Severity.Security -> Priority.CRITICAL
            Severity.Performance -> Priority.MEDIUM
        }
    }
    
    /**
     * Maps Detekt debt to our effort estimate
     */
    private fun mapDebtToEffort(debt: Debt): Effort {
        val minutes = debt.mins
        return when {
            minutes <= 5 -> Effort.TRIVIAL
            minutes <= 30 -> Effort.SMALL
            minutes <= 120 -> Effort.MEDIUM
            else -> Effort.LARGE
        }
    }
    
    /**
     * Builds a recommendation message from Detekt issue
     */
    private fun buildRecommendation(issue: io.gitlab.arturbosch.detekt.api.Finding): String {
        val message = issue.messageOrDescription()
        val ruleName = issue.id
        
        return buildString {
            append("Detekt rule '$ruleName' was violated. ")
            append(message)
            append("\n\n")
            append("Consider refactoring this code to comply with the rule. ")
            append("See the Detekt documentation for more details and examples.")
        }
    }
}
