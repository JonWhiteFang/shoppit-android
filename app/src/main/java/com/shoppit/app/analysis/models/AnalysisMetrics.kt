package com.shoppit.app.analysis.models

/**
 * Overall metrics from code quality analysis.
 *
 * @property totalFiles Total number of files analyzed
 * @property totalFindings Total number of findings across all categories
 * @property findingsByPriority Count of findings grouped by priority level
 * @property findingsByCategory Count of findings grouped by analysis category
 * @property averageComplexity Average cyclomatic complexity across all functions
 * @property averageFunctionLength Average function length in lines of code
 * @property averageClassLength Average class length in lines of code
 * @property testCoveragePercentage Percentage of components with test coverage
 * @property documentationCoveragePercentage Percentage of public APIs with documentation
 */
data class AnalysisMetrics(
    val totalFiles: Int,
    val totalFindings: Int,
    val findingsByPriority: Map<Priority, Int>,
    val findingsByCategory: Map<AnalysisCategory, Int>,
    val averageComplexity: Double,
    val averageFunctionLength: Double,
    val averageClassLength: Double,
    val testCoveragePercentage: Double,
    val documentationCoveragePercentage: Double
)
