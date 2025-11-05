package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Finding

/**
 * Interface for managing baseline snapshots and tracking improvements.
 */
interface BaselineManager {
    /**
     * Loads the existing baseline from storage.
     *
     * @return Baseline if it exists, null otherwise
     */
    fun loadBaseline(): Baseline?
    
    /**
     * Saves the current analysis as the baseline.
     *
     * @param metrics Current analysis metrics
     * @param findings Current findings
     */
    fun saveBaseline(metrics: AnalysisMetrics, findings: List<Finding>)
    
    /**
     * Compares current analysis with the baseline.
     *
     * @param current Current analysis metrics
     * @param baseline Baseline to compare against
     * @return Comparison results showing improvements and regressions
     */
    fun compare(current: AnalysisMetrics, baseline: Baseline): Comparison
    
    /**
     * Saves the current analysis to history.
     *
     * @param result Aggregated analysis result to save
     */
    fun saveToHistory(result: AggregatedResult)
}

/**
 * Comparison between current analysis and baseline.
 *
 * @property improved Metrics that have improved (metric name -> improvement percentage)
 * @property regressed Metrics that have regressed (metric name -> regression percentage)
 * @property resolved Finding IDs that were resolved since baseline
 * @property newIssues Finding IDs that are new since baseline
 */
data class Comparison(
    val improved: Map<String, Double>,
    val regressed: Map<String, Double>,
    val resolved: List<String>,
    val newIssues: List<String>
)
