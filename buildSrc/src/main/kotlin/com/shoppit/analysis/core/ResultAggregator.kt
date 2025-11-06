package com.shoppit.analysis.core

import com.shoppit.analysis.models.AnalysisMetrics
import com.shoppit.analysis.models.Finding

/**
 * Interface for aggregating analysis results.
 */
interface ResultAggregator {
    /**
     * Aggregates findings from multiple analyzers.
     * 
     * @param findings All findings from all analyzers
     * @return Aggregated and deduplicated findings
     */
    fun aggregate(findings: List<Finding>): List<Finding>
    
    /**
     * Calculates metrics from findings.
     * 
     * @param findings All findings
     * @param totalFiles Total number of files analyzed
     * @param totalLines Total lines of code analyzed
     * @param analysisTimeMs Time taken for analysis in milliseconds
     * @return Calculated metrics
     */
    fun calculateMetrics(
        findings: List<Finding>,
        totalFiles: Int,
        totalLines: Int,
        analysisTimeMs: Long
    ): AnalysisMetrics
}
