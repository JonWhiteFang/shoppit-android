package com.shoppit.analysis.models

/**
 * Represents a code quality finding.
 */
data class Finding(
    val id: String,
    val category: AnalysisCategory,
    val priority: Priority,
    val title: String,
    val description: String,
    val file: String,
    val line: Int? = null,
    val column: Int? = null,
    val effort: Effort = Effort.MEDIUM,
    val suggestion: String? = null,
    val codeSnippet: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Unique identifier for baseline comparison.
     */
    val fingerprint: String
        get() = "$category:$file:$line:$title".hashCode().toString()
}
