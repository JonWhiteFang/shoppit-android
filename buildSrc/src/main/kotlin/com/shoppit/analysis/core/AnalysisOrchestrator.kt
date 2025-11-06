package com.shoppit.analysis.core

import com.shoppit.analysis.models.AnalysisMetrics
import com.shoppit.analysis.models.Finding
import java.io.File

/**
 * Result of a complete analysis run.
 */
data class AnalysisResult(
    val findings: List<Finding>,
    val metrics: AnalysisMetrics,
    val newFindings: List<Finding> = emptyList(),
    val fixedFindings: List<Finding> = emptyList()
)

/**
 * Orchestrates the entire analysis process.
 * 
 * Coordinates file scanning, analyzer execution, result aggregation,
 * baseline comparison, and report generation.
 */
interface AnalysisOrchestrator {
    /**
     * Runs a complete analysis.
     * 
     * @param sourceDir The source directory to analyze
     * @param outputDir The directory where reports will be generated
     * @param baselinePath Optional path to baseline file for comparison
     * @return Analysis result with findings and metrics
     */
    suspend fun runAnalysis(
        sourceDir: File,
        outputDir: File,
        baselinePath: String? = null
    ): AnalysisResult
}
