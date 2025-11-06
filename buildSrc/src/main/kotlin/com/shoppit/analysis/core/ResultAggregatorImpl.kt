package com.shoppit.analysis.core

import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.AnalysisMetrics
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority

/**
 * Implementation of ResultAggregator that combines and deduplicates findings.
 */
class ResultAggregatorImpl : ResultAggregator {
    
    override fun aggregate(findings: List<Finding>): List<Finding> {
        // Deduplicate findings by fingerprint
        val uniqueFindings = findings
            .groupBy { it.fingerprint }
            .map { (_, duplicates) -> duplicates.first() }
            .sortedWith(
                compareBy<Finding> { it.priority }
                    .thenBy { it.category }
                    .thenBy { it.file }
                    .thenBy { it.line ?: Int.MAX_VALUE }
            )
        
        println("Aggregated ${findings.size} findings into ${uniqueFindings.size} unique findings")
        
        return uniqueFindings
    }
    
    override fun calculateMetrics(
        findings: List<Finding>,
        totalFiles: Int,
        totalLines: Int,
        analysisTimeMs: Long
    ): AnalysisMetrics {
        val findingsByPriority = findings.groupingBy { it.priority }.eachCount()
        val findingsByCategory = findings.groupingBy { it.category }.eachCount()
        
        val kotlinFiles = totalFiles // Assuming all scanned files are Kotlin
        val testFiles = 0 // Will be calculated by FileScanner
        
        return AnalysisMetrics(
            totalFiles = totalFiles,
            totalLines = totalLines,
            kotlinFiles = kotlinFiles,
            testFiles = testFiles,
            totalFindings = findings.size,
            findingsByPriority = findingsByPriority,
            findingsByCategory = findingsByCategory,
            analysisTimeMs = analysisTimeMs
        )
    }
}
