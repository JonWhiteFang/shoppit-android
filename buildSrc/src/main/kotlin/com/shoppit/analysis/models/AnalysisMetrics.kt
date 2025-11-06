package com.shoppit.analysis.models

/**
 * Metrics collected during analysis.
 */
data class AnalysisMetrics(
    val totalFiles: Int = 0,
    val totalLines: Int = 0,
    val kotlinFiles: Int = 0,
    val testFiles: Int = 0,
    val totalFindings: Int = 0,
    val findingsByPriority: Map<Priority, Int> = emptyMap(),
    val findingsByCategory: Map<AnalysisCategory, Int> = emptyMap(),
    val analysisTimeMs: Long = 0
) {
    val criticalCount: Int get() = findingsByPriority[Priority.CRITICAL] ?: 0
    val highCount: Int get() = findingsByPriority[Priority.HIGH] ?: 0
    val mediumCount: Int get() = findingsByPriority[Priority.MEDIUM] ?: 0
    val lowCount: Int get() = findingsByPriority[Priority.LOW] ?: 0
    val infoCount: Int get() = findingsByPriority[Priority.INFO] ?: 0
}
