package com.shoppit.app.analysis

import com.shoppit.app.analysis.core.*
import com.shoppit.app.analysis.reporting.ReportGeneratorImpl
import com.shoppit.app.analysis.baseline.BaselineManagerImpl
import com.shoppit.app.analysis.integration.DetektIntegrationImpl
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Main entry point for running code quality analysis from Gradle tasks or CLI.
 * 
 * This runner:
 * - Instantiates all required dependencies
 * - Configures the analysis orchestrator
 * - Executes the analysis
 * - Reports results to console
 * 
 * Usage:
 * ```
 * val runner = AnalysisRunner(
 *     projectRoot = "app/src/main/java",
 *     outputPath = ".kiro/specs/code-quality-analysis"
 * )
 * runner.runAnalysis()
 * ```
 */
class AnalysisRunner(
    private val projectRoot: String = "app/src/main/java",
    private val outputPath: String = ".kiro/specs/code-quality-analysis",
    private val analyzers: List<String>? = null,
    private val generateBaseline: Boolean = false,
    private val enableDetekt: Boolean = true
) {
    
    /**
     * Runs the complete analysis workflow.
     * 
     * @return Analysis result with findings and metrics
     */
    fun runAnalysis(): AnalysisResult {
        println("\n[INFO] Initializing analysis components...")
        
        // Validate project root exists
        val projectRootFile = File(projectRoot)
        if (!projectRootFile.exists()) {
            throw IllegalArgumentException("Project root does not exist: $projectRoot")
        }
        
        // Create output directory if it doesn't exist
        val outputDir = File(outputPath)
        if (!outputDir.exists()) {
            outputDir.mkdirs()
            println("[INFO] Created output directory: $outputPath")
        }
        
        // Instantiate dependencies
        val fileScanner = FileScannerImpl()
        val resultAggregator = ResultAggregatorImpl()
        val reportGenerator = ReportGeneratorImpl()
        val baselineManager = BaselineManagerImpl(outputPath)
        val detektIntegration = DetektIntegrationImpl(projectRoot)
        
        // Create orchestrator
        val orchestrator = AnalysisOrchestratorImpl(
            fileScanner = fileScanner,
            resultAggregator = resultAggregator,
            reportGenerator = reportGenerator,
            baselineManager = baselineManager,
            detektIntegration = detektIntegration,
            projectRoot = projectRoot,
            enableDetekt = enableDetekt
        )
        
        println("[INFO] Components initialized successfully")
        println("[INFO] Starting analysis...")
        println()
        
        // Run analysis
        val result = runBlocking {
            if (analyzers != null && analyzers.isNotEmpty()) {
                // Run with specific analyzers
                println("[INFO] Running filtered analysis with analyzers: ${analyzers.joinToString(", ")}")
                orchestrator.analyzeWithFilters(null, analyzers)
            } else {
                // Run complete analysis
                println("[INFO] Running complete analysis on all files")
                orchestrator.analyzeAll()
            }
        }
        
        // Report results
        println()
        println("=".repeat(80))
        println("Analysis Complete")
        println("=".repeat(80))
        println()
        println("Summary:")
        println("  Files Analyzed:    ${result.filesAnalyzed}")
        println("  Total Findings:    ${result.findings.size}")
        println("  Execution Time:    ${result.executionTime}")
        println()
        
        // Breakdown by priority
        val byPriority = result.findings.groupBy { it.priority }
        println("Findings by Priority:")
        println("  Critical:          ${byPriority[com.shoppit.app.analysis.models.Priority.CRITICAL]?.size ?: 0}")
        println("  High:              ${byPriority[com.shoppit.app.analysis.models.Priority.HIGH]?.size ?: 0}")
        println("  Medium:            ${byPriority[com.shoppit.app.analysis.models.Priority.MEDIUM]?.size ?: 0}")
        println("  Low:               ${byPriority[com.shoppit.app.analysis.models.Priority.LOW]?.size ?: 0}")
        println()
        
        // Breakdown by category
        val byCategory = result.findings.groupBy { it.category }
        println("Findings by Category:")
        byCategory.entries.sortedByDescending { it.value.size }.forEach { (category, findings) ->
            println("  ${category.name.padEnd(20)}: ${findings.size}")
        }
        println()
        
        println("Report saved to: $outputPath/analysis-report.md")
        
        if (generateBaseline) {
            println("Baseline saved to: $outputPath/baseline.json")
        }
        
        println()
        println("=".repeat(80))
        
        return result
    }
    
    companion object {
        /**
         * Main entry point for command-line execution.
         * 
         * Usage:
         * ```
         * java -cp ... com.shoppit.app.analysis.AnalysisRunner [options]
         * ```
         */
        @JvmStatic
        fun main(args: Array<String>) {
            // Parse command-line arguments
            val projectRoot = args.getOrNull(0) ?: "app/src/main/java"
            val outputPath = args.getOrNull(1) ?: ".kiro/specs/code-quality-analysis"
            val analyzersArg = args.getOrNull(2)
            val analyzers = analyzersArg?.split(",")?.map { it.trim() }
            val generateBaseline = args.getOrNull(3)?.toBoolean() ?: false
            val enableDetekt = args.getOrNull(4)?.toBoolean() ?: true
            
            try {
                val runner = AnalysisRunner(
                    projectRoot = projectRoot,
                    outputPath = outputPath,
                    analyzers = analyzers,
                    generateBaseline = generateBaseline,
                    enableDetekt = enableDetekt
                )
                
                runner.runAnalysis()
            } catch (e: Exception) {
                System.err.println("Error running analysis: ${e.message}")
                e.printStackTrace()
                System.exit(1)
            }
        }
    }
}
