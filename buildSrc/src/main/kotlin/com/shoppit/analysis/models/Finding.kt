package com.shoppit.analysis.models

/**
 * Represents a single code quality issue or recommendation.
 *
 * @property id Unique identifier for this finding
 * @property analyzer ID of the analyzer that found this issue
 * @property category Category of analysis
 * @property priority Priority level of the issue
 * @property title Short title describing the issue
 * @property description Detailed description of the issue
 * @property file File path where the issue was found
 * @property lineNumber Line number where the issue occurs
 * @property columnNumber Column number where the issue occurs (optional)
 * @property codeSnippet Code snippet showing the issue
 * @property recommendation Specific recommendation for fixing the issue
 * @property beforeExample Example of problematic code (optional)
 * @property afterExample Example of corrected code (optional)
 * @property autoFixable Whether this issue can be automatically fixed
 * @property autoFix Automatic fix code (optional)
 * @property effort Estimated effort to fix the issue
 * @property references Links to documentation or resources
 * @property relatedFindings IDs of related findings
 */
data class Finding(
    val id: String,
    val analyzer: String,
    val category: AnalysisCategory,
    val priority: Priority,
    val title: String,
    val description: String,
    val file: String,
    val lineNumber: Int,
    val columnNumber: Int? = null,
    val codeSnippet: String,
    val recommendation: String,
    val beforeExample: String? = null,
    val afterExample: String? = null,
    val autoFixable: Boolean = false,
    val autoFix: String? = null,
    val effort: Effort,
    val references: List<String> = emptyList(),
    val relatedFindings: List<String> = emptyList()
)
