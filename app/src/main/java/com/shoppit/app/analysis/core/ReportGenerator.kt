package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Finding

/**
 * Interface for generating analysis reports.
 */
interface ReportGenerator {
    /**
     * Generates a complete analysis report in markdown format.
     *
     * @param result Aggregated analysis results
     * @param baseline Optional baseline for comparison
     * @return Markdown-formatted report
     */
    fun generate(result: AggregatedResult, baseline: Baseline?): String
    
    /**
     * Generates an executive summary section.
     *
     * @param metrics Analysis metrics
     * @param baseline Optional baseline for comparison
     * @return Markdown-formatted executive summary
     */
    fun generateSummary(metrics: AnalysisMetrics, baseline: Baseline?): String
    
    /**
     * Generates the findings section of the report.
     *
     * @param findings List of findings to include
     * @return Markdown-formatted findings section
     */
    fun generateFindings(findings: List<Finding>): String
}

/**
 * Baseline snapshot for tracking improvements over time.
 *
 * @property timestamp When the baseline was created
 * @property metrics Metrics at baseline time
 * @property findingIds Set of finding IDs present in the baseline
 */
data class Baseline(
    val timestamp: Long,
    val metrics: AnalysisMetrics,
    val findingIds: Set<String>
)
