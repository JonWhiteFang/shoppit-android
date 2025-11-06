package com.shoppit.analysis.baseline

import com.shoppit.analysis.models.Finding
import java.io.File

/**
 * Manages baseline files for tracking changes in findings over time.
 */
interface BaselineManager {
    /**
     * Saves findings to a baseline file.
     * 
     * @param findings The findings to save
     * @param baselineFile The file where the baseline will be saved
     */
    fun saveBaseline(findings: List<Finding>, baselineFile: File)
    
    /**
     * Loads findings from a baseline file.
     * 
     * @param baselineFile The file to load from
     * @return List of findings, or empty list if file doesn't exist
     */
    fun loadBaseline(baselineFile: File): List<Finding>
    
    /**
     * Compares current findings with baseline to identify new and fixed issues.
     * 
     * @param currentFindings Current analysis findings
     * @param baselineFindings Baseline findings
     * @return Pair of (new findings, fixed findings)
     */
    fun compareWithBaseline(
        currentFindings: List<Finding>,
        baselineFindings: List<Finding>
    ): Pair<List<Finding>, List<Finding>>
}
