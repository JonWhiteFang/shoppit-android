package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.FileProcessListener
import io.gitlab.arturbosch.detekt.cli.CliArgs
import io.gitlab.arturbosch.detekt.cli.loadConfiguration
import io.gitlab.arturbosch.detekt.core.DetektFacade
import io.gitlab.arturbosch.detekt.core.ProcessingSettings
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Integrates Detekt static analysis tool into the code quality analysis system.
 * 
 * Detekt is a comprehensive static analysis tool for Kotlin that checks for:
 * - Code smells
 * - Complexity issues
 * - Formatting problems
 * - Potential bugs
 * - Performance issues
 * - Security vulnerabilities
 * 
 * This integration:
 * 1. Runs Detekt programmatically on specified files
 * 2. Parses Detekt findings
 * 3. Converts them to our Finding model
 * 4. Maps Detekt severity to our Priority system
 * 5. Categorizes findings appropriately
 */
class DetektIntegration(
    private val configPath: String = "app/detekt-config.yml",
    private val baselinePath: String? = null
) : CodeAnalyzer {
    
    override val name: String = "Detekt Integration"
    override val description: String = "Comprehensive static analysis using Detekt"
    
    override fun appliesTo(fileInfo: FileInfo): Boolean {
        // Detekt applies to all Kotlin files
        return fileInfo.path.endsWith(".kt") || fileInfo.path.endsWith(".kts")
    }
    
    override fun analyze(file: KtFile, fileInfo: FileInfo): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        try {
            // Run Detekt on the file
            val detektFindings = runDetektOnFile(fileInfo.path)
            
            // Convert Detekt findings to our Finding model
            detektFindings.forEach { detektFinding ->
                findings.add(convertDetektFinding(detektFinding, fileInfo))
            }
            
        } catch (e: Exception) {
            // Log error but don't fail the entire analysis
            println("[WARN] Detekt analysis failed for ${fileInfo.path}: ${e.message}")
        }
        
        return findings
    }
    
    /**
     * Runs Detekt on multiple paths and returns findings.
     * This is the main entry point for the orchestrator.
     */
    fun runDetekt(paths: List<String>): Result<List<Finding>> {
        return try {
            val allFindings = mutableListOf<Finding>()
            
            paths.forEach { path ->
                val file = File(path)
                if (file.isDirectory) {
                    // Scan directory for Kotlin files
                    file.walkTopDown()
                        .filter { it.extension == "kt" || it.extension == "kts" }
                        .forEach { ktFile ->
                            val detektFindings = runDetektOnFile(ktFile.absolutePath)
                            detektFindings.forEach { detektFinding ->
                                val fileInfo = FileInfo(
                                    path = ktFile.absolutePath,
                                    relativePath = ktFile.relativeTo(File(".")).path,
                                    layer = detectLayer(ktFile.absolutePath)
                                )
                                allFindings.add(convertDetektFinding(detektFinding, fileInfo))
                            }
                        }
                } else if (file.extension == "kt" || file.extension == "kts") {
                    // Single file
                    val detektFindings = runDetektOnFile(file.absolutePath)
                    detektFindings.forEach { detektFinding ->
                        val fileInfo = FileInfo(
                            path = file.absolutePath,
                            relativePath = file.relativeTo(File(".")).path,
                            layer = detectLayer(file.absolutePath)
                        )
                        allFindings.add(convertDetektFinding(detektFinding, fileInfo))
                    }
                }
            }
            
            Result.success(allFindings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Detects the layer of a file based on its path.
     */
    private fun detectLayer(path: String): String {
        return when {
            path.contains("/data/") -> "Data"
            path.contains("/domain/") -> "Domain"
            path.contains("/ui/") -> "UI"
            else -> "Unknown"
        }
    }
    
    /**
     * Runs Detekt on a single file and returns the findings.
     */
    private fun runDetektOnFile(filePath: String): List<DetektFindingData> {
        val findings = mutableListOf<DetektFindingData>()
        
        try {
            // Load Detekt configuration
            val configFile = File(configPath)
            if (!configFile.exists()) {
                println("[WARN] Detekt config not found at $configPath, using default config")
                return emptyList()
            }
            
            val config = loadConfiguration(Paths.get(configPath))
            
            // Set up processing settings
            val inputPath = Paths.get(filePath)
            val settings = ProcessingSettings(
                inputPaths = listOf(inputPath),
                config = config,
                spec = null,
                baseline = baselinePath?.let { Paths.get(it) },
                excludePatterns = emptyList(),
                pluginPaths = emptyList(),
                classpath = emptyList(),
                languageVersion = null,
                jvmTarget = null,
                executorService = null
            )
            
            // Create Detekt facade
            val detekt = DetektFacade.create(settings)
            
            // Run analysis
            val result = detekt.run()
            
            // Extract findings
            result.findings.forEach { (ruleSetId, ruleSetFindings) ->
                ruleSetFindings.forEach { finding ->
                    findings.add(
                        DetektFindingData(
                            ruleSetId = ruleSetId,
                            ruleId = finding.id,
                            message = finding.message,
                            severity = finding.severity.name,
                            filePath = finding.entity.location.file,
                            line = finding.entity.location.source.line,
                            column = finding.entity.location.source.column,
                            codeSnippet = finding.entity.signature
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            println("[ERROR] Failed to run Detekt: ${e.message}")
            e.printStackTrace()
        }
        
        return findings
    }
    
    /**
     * Converts a Detekt finding to our Finding model.
     */
    private fun convertDetektFinding(
        detektFinding: DetektFindingData,
        fileInfo: FileInfo
    ): Finding {
        val priority = mapDetektSeverityToPriority(detektFinding.severity, detektFinding.ruleSetId)
        val category = mapDetektRuleSetToCategory(detektFinding.ruleSetId)
        val effort = estimateEffort(detektFinding.ruleSetId, detektFinding.ruleId)
        
        return Finding(
            file = fileInfo.path,
            line = detektFinding.line,
            column = detektFinding.column,
            category = category,
            priority = priority,
            effort = effort,
            title = "${detektFinding.ruleSetId}: ${detektFinding.ruleId}",
            description = detektFinding.message,
            codeSnippet = detektFinding.codeSnippet,
            recommendation = getRecommendation(detektFinding.ruleSetId, detektFinding.ruleId),
            source = "Detekt"
        )
    }
    
    /**
     * Maps Detekt severity to our Priority system.
     */
    private fun mapDetektSeverityToPriority(severity: String, ruleSetId: String): Priority {
        return when {
            // Security issues are always critical
            ruleSetId == "security" -> Priority.CRITICAL
            
            // Map Detekt severity
            severity == "error" -> Priority.HIGH
            severity == "warning" -> Priority.MEDIUM
            severity == "info" -> Priority.LOW
            
            // Default
            else -> Priority.MEDIUM
        }
    }
    
    /**
     * Maps Detekt rule set to our AnalysisCategory.
     */
    private fun mapDetektRuleSetToCategory(ruleSetId: String): AnalysisCategory {
        return when (ruleSetId) {
            "complexity" -> AnalysisCategory.CODE_SMELLS
            "style" -> AnalysisCategory.NAMING_CONVENTIONS
            "comments" -> AnalysisCategory.DOCUMENTATION
            "coroutines" -> AnalysisCategory.STATE_MANAGEMENT
            "empty-blocks" -> AnalysisCategory.CODE_SMELLS
            "exceptions" -> AnalysisCategory.ERROR_HANDLING
            "naming" -> AnalysisCategory.NAMING_CONVENTIONS
            "performance" -> AnalysisCategory.PERFORMANCE
            "potential-bugs" -> AnalysisCategory.ARCHITECTURE
            "formatting" -> AnalysisCategory.NAMING_CONVENTIONS
            else -> AnalysisCategory.CODE_SMELLS
        }
    }
    
    /**
     * Estimates effort required to fix the issue.
     */
    private fun estimateEffort(ruleSetId: String, ruleId: String): Effort {
        return when {
            // Formatting and simple style issues are trivial
            ruleSetId == "formatting" -> Effort.TRIVIAL
            ruleSetId == "style" && ruleId in listOf(
                "MagicNumber",
                "UnusedImports",
                "WildcardImport"
            ) -> Effort.TRIVIAL
            
            // Complexity issues often require refactoring
            ruleSetId == "complexity" && ruleId in listOf(
                "LongMethod",
                "LargeClass",
                "CyclomaticComplexMethod"
            ) -> Effort.LARGE
            
            // Most other issues are small to medium
            ruleSetId in listOf("naming", "comments") -> Effort.SMALL
            
            // Default
            else -> Effort.MEDIUM
        }
    }
    
    /**
     * Provides a recommendation for fixing the issue.
     */
    private fun getRecommendation(ruleSetId: String, ruleId: String): String {
        return when (ruleSetId) {
            "complexity" -> when (ruleId) {
                "LongMethod" -> "Break this function into smaller, focused functions. Each function should do one thing well."
                "LargeClass" -> "Split this class into smaller classes following the Single Responsibility Principle."
                "CyclomaticComplexMethod" -> "Reduce complexity by extracting conditional logic into separate functions or using early returns."
                "NestedBlockDepth" -> "Reduce nesting by using early returns, guard clauses, or extracting nested logic into functions."
                "LongParameterList" -> "Consider using a parameter object or builder pattern to reduce the number of parameters."
                else -> "Simplify the code structure to improve readability and maintainability."
            }
            
            "style" -> when (ruleId) {
                "MagicNumber" -> "Extract magic numbers into named constants to improve code readability."
                "UnusedImports" -> "Remove unused imports to keep the code clean."
                "WildcardImport" -> "Use explicit imports instead of wildcard imports for better clarity."
                else -> "Follow Kotlin coding conventions for better code consistency."
            }
            
            "naming" -> "Follow Kotlin naming conventions: classes in PascalCase, functions/variables in camelCase, constants in UPPER_SNAKE_CASE."
            
            "comments" -> "Add KDoc comments to public APIs to improve code documentation and maintainability."
            
            "coroutines" -> "Follow coroutine best practices: inject dispatchers, avoid GlobalScope, use structured concurrency."
            
            "exceptions" -> "Implement proper error handling: catch specific exceptions, don't swallow errors, use Result type for failable operations."
            
            "performance" -> "Optimize performance by avoiding unnecessary allocations, using sequences for large collections, and caching expensive computations."
            
            "potential-bugs" -> "Fix potential bugs to prevent runtime errors and unexpected behavior."
            
            else -> "Review and fix this issue according to Detekt recommendations."
        }
    }
    
    /**
     * Data class to hold Detekt finding information.
     */
    private data class DetektFindingData(
        val ruleSetId: String,
        val ruleId: String,
        val message: String,
        val severity: String,
        val filePath: String,
        val line: Int,
        val column: Int,
        val codeSnippet: String
    )
}
