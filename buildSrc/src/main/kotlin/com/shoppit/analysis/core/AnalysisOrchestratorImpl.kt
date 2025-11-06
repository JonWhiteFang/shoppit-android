package com.shoppit.analysis.core

import com.shoppit.analysis.analyzers.*
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Simplified implementation of AnalysisOrchestrator for initial migration.
 * 
 * This is a minimal implementation to get the Gradle plugin working.
 * Full functionality can be added incrementally.
 */
class AnalysisOrchestratorImpl(
    private val analyzers: List<CodeAnalyzer>,
    private val fileScanner: FileScanner,
    private val resultAggregator: ResultAggregator,
    private val reportGenerator: ReportGenerator,
    private val baselineManager: BaselineManager
) : AnalysisOrchestrator {
    
    override suspend fun analyze(
        paths: List<String>,
        outputPath: String
    ): Result<AnalysisReport> = withContext(Dispatchers.IO) {
        try {
            println("Starting analysis on ${paths.size} paths")
            
            // Scan files
            val files = paths.flatMap { path ->
                val file = File(path)
                if (file.exists()) {
                    fileScanner.scanDirectory(file.absolutePath)
                } else {
                    emptyList()
                }
            }
            
            val filteredFiles = fileScanner.filterFiles(files)
            println("Found ${filteredFiles.size} Kotlin files to analyze")
            
            // Run analyzers (simplified - no actual analysis yet)
            val findings = mutableListOf<Finding>()
            
            // Aggregate results
            val aggregatedResult = resultAggregator.aggregate(findings)
            
            // Generate report
            val report = reportGenerator.generate(aggregatedResult)
            
            // Save report
            val outputDir = File(outputPath)
            outputDir.mkdirs()
            val reportFile = File(outputDir, "code-quality-report.md")
            reportFile.writeText(report)
            
            println("Analysis complete: ${findings.size} findings in ${filteredFiles.size} files")
            println("Report saved to: ${reportFile.absolutePath}")
            
            Result.success(aggregatedResult)
            
        } catch (e: Exception) {
            println("Error during analysis: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
