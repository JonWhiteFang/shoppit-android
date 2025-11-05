package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority

/**
 * Interface for aggregating and processing analysis results.
 */
interface ResultAggregator {
    /**
     * Aggregates findings from multiple analyzers.
     *
     * @param findings List of all findings from analyzers
     * @return Aggregated result with organized findings and metrics
     */
    fun aggregate(findings: List<Finding>): AggregatedResult
    
    /**
     * Deduplicates similar findings.
     *
     * @param findings List of findings that may contain duplicates
     * @return List of unique findings
     */
    fun deduplicate(findings: List<Finding>): List<Finding>
    
    /**
     * Calculates overall metrics from findings.
     *
     * @param findings List of findings to analyze
     * @return Calculated metrics
     */
    fun calculateMetrics(findings: List<Finding>): AnalysisMetrics
}

/**
 * Aggregated analysis results with organized findings and metrics.
 *
 * @property findings All findings after deduplication
 * @property metrics Overall analysis metrics
 * @property byCategory Findings grouped by analysis category
 * @property byPriority Findings grouped by priority level
 * @property byFile Findings grouped by file path
 */
data class AggregatedResult(
    val findings: List<Finding>,
    val metrics: AnalysisMetrics,
    val byCategory: Map<AnalysisCategory, List<Finding>>,
    val byPriority: Map<Priority, List<Finding>>,
    val byFile: Map<String, List<Finding>>
)
