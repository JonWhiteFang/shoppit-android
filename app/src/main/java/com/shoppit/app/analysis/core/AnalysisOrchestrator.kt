package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.AnalysisMetrics
import kotlin.time.Duration

/**
 * Interface for orchestrating the entire code quality analysis process.
 */
interface AnalysisOrchestrator {
    /**
     * Runs complete analysis on the entire codebase.
     *
     * @return Analysis result with all findings and metrics
     */
    suspend fun analyzeAll(): AnalysisResult
    
    /**
     * Runs analysis on specific files or directories.
     *
     * @param paths List of file or directory paths to analyze
     * @return Analysis result for the specified paths
     */
    suspend fun analyzeIncremental(paths: List<String>): AnalysisResult
    
    /**
     * Runs specific analyzers only.
     *
     * @param paths Optional list of paths to analyze (null for all files)
     * @param analyzers List of analyzer types to run
     * @return Analysis result from the specified analyzers
     */
    suspend fun analyzeWithFilters(
        paths: List<String>? = null,
        analyzers: List<String>
    ): AnalysisResult
}

/**
 * Result of a code quality analysis run.
 *
 * @property findings All findings discovered during analysis
 * @property metrics Overall analysis metrics
 * @property executionTime Time taken to complete the analysis
 * @property filesAnalyzed Number of files analyzed
 */
data class AnalysisResult(
    val findings: List<Finding>,
    val metrics: AnalysisMetrics,
    val executionTime: Duration,
    val filesAnalyzed: Int
)
