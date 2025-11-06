package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority

/**
 * Implementation of ResultAggregator that collects, deduplicates, and processes findings.
 */
class ResultAggregatorImpl : ResultAggregator {
    
    override fun aggregate(findings: List<Finding>): AggregatedResult {
        // Assign priorities to findings based on category
        val prioritizedFindings = findings.map { assignPriority(it) }
        
        // Deduplicate findings
        val uniqueFindings = deduplicate(prioritizedFindings)
        
        // Calculate metrics
        val metrics = calculateMetrics(uniqueFindings)
        
        // Group findings by category
        val byCategory = uniqueFindings.groupBy { it.category }
        
        // Group findings by priority
        val byPriority = uniqueFindings.groupBy { it.priority }
        
        // Group findings by file
        val byFile = uniqueFindings.groupBy { it.file }
        
        return AggregatedResult(
            findings = uniqueFindings,
            metrics = metrics,
            byCategory = byCategory,
            byPriority = byPriority,
            byFile = byFile
        )
    }
    
    override fun deduplicate(findings: List<Finding>): List<Finding> {
        // Group findings by file, line, and category to identify duplicates
        val grouped = findings.groupBy { finding ->
            DeduplicationKey(
                file = finding.file,
                lineNumber = finding.lineNumber,
                category = finding.category,
                title = finding.title
            )
        }
        
        // For each group, keep only the finding with highest priority
        return grouped.values.map { duplicates ->
            duplicates.maxByOrNull { finding ->
                // Higher priority = lower ordinal value
                -finding.priority.ordinal
            } ?: duplicates.first()
        }
    }
    
    /**
     * Key for identifying duplicate findings.
     * Findings are considered duplicates if they have the same file, line, category, and title.
     */
    private data class DeduplicationKey(
        val file: String,
        val lineNumber: Int,
        val category: AnalysisCategory,
        val title: String
    )
    
    /**
     * Assigns priority to a finding based on its category.
     * Security issues are CRITICAL, architecture violations are HIGH,
     * performance issues are MEDIUM, and style issues are LOW.
     *
     * @param finding The finding to assign priority to
     * @return Finding with assigned priority (or original priority if already set appropriately)
     */
    private fun assignPriority(finding: Finding): Finding {
        // If finding already has an explicit priority that makes sense, keep it
        // Otherwise, assign based on category
        val suggestedPriority = when (finding.category) {
            AnalysisCategory.SECURITY -> Priority.CRITICAL
            AnalysisCategory.ARCHITECTURE,
            AnalysisCategory.ERROR_HANDLING -> Priority.HIGH
            AnalysisCategory.PERFORMANCE,
            AnalysisCategory.CODE_SMELL,
            AnalysisCategory.STATE_MANAGEMENT,
            AnalysisCategory.COMPOSE,
            AnalysisCategory.DATABASE,
            AnalysisCategory.DEPENDENCY_INJECTION -> Priority.MEDIUM
            AnalysisCategory.NAMING,
            AnalysisCategory.DOCUMENTATION,
            AnalysisCategory.TEST_COVERAGE -> Priority.LOW
        }
        
        // If the finding's priority is already more severe than suggested, keep it
        // Otherwise, use the suggested priority
        return if (finding.priority.ordinal < suggestedPriority.ordinal) {
            finding
        } else {
            finding.copy(priority = suggestedPriority)
        }
    }
    
    override fun calculateMetrics(findings: List<Finding>): AnalysisMetrics {
        // Count unique files
        val totalFiles = findings.map { it.file }.distinct().size
        
        // Count findings by priority
        val findingsByPriority = Priority.values().associateWith { priority ->
            findings.count { it.priority == priority }
        }
        
        // Count findings by category
        val findingsByCategory = AnalysisCategory.values().associateWith { category ->
            findings.count { it.category == category }
        }
        
        // Calculate average complexity from code smell findings
        val complexityFindings = findings.filter { finding ->
            finding.category == AnalysisCategory.CODE_SMELL &&
            finding.title.contains("complexity", ignoreCase = true)
        }
        val averageComplexity = if (complexityFindings.isNotEmpty()) {
            complexityFindings.mapNotNull { finding ->
                // Extract complexity value from description or title
                extractComplexityValue(finding)
            }.average()
        } else {
            0.0
        }
        
        // Calculate average function length from code smell findings
        val functionLengthFindings = findings.filter { finding ->
            finding.category == AnalysisCategory.CODE_SMELL &&
            finding.title.contains("function", ignoreCase = true) &&
            finding.title.contains("long", ignoreCase = true)
        }
        val averageFunctionLength = if (functionLengthFindings.isNotEmpty()) {
            functionLengthFindings.mapNotNull { finding ->
                extractLengthValue(finding)
            }.average()
        } else {
            0.0
        }
        
        // Calculate average class length from code smell findings
        val classLengthFindings = findings.filter { finding ->
            finding.category == AnalysisCategory.CODE_SMELL &&
            finding.title.contains("class", ignoreCase = true) &&
            finding.title.contains("large", ignoreCase = true)
        }
        val averageClassLength = if (classLengthFindings.isNotEmpty()) {
            classLengthFindings.mapNotNull { finding ->
                extractLengthValue(finding)
            }.average()
        } else {
            0.0
        }
        
        // Calculate test coverage percentage
        val testCoverageFindings = findings.filter { it.category == AnalysisCategory.TEST_COVERAGE }
        val totalComponents = findings.filter { finding ->
            finding.category == AnalysisCategory.TEST_COVERAGE ||
            finding.file.contains("ViewModel") ||
            finding.file.contains("UseCase") ||
            finding.file.contains("Repository")
        }.map { it.file }.distinct().size
        
        val testedComponents = totalComponents - testCoverageFindings.size
        val testCoveragePercentage = if (totalComponents > 0) {
            (testedComponents.toDouble() / totalComponents) * 100
        } else {
            100.0
        }
        
        // Calculate documentation coverage percentage
        val documentationFindings = findings.filter { it.category == AnalysisCategory.DOCUMENTATION }
        val totalPublicApis = findings.filter { finding ->
            finding.category == AnalysisCategory.DOCUMENTATION ||
            finding.description.contains("public", ignoreCase = true)
        }.map { "${it.file}:${it.lineNumber}" }.distinct().size
        
        val documentedApis = totalPublicApis - documentationFindings.size
        val documentationCoveragePercentage = if (totalPublicApis > 0) {
            (documentedApis.toDouble() / totalPublicApis) * 100
        } else {
            100.0
        }
        
        return AnalysisMetrics(
            totalFiles = totalFiles,
            totalFindings = findings.size,
            findingsByPriority = findingsByPriority,
            findingsByCategory = findingsByCategory,
            averageComplexity = averageComplexity,
            averageFunctionLength = averageFunctionLength,
            averageClassLength = averageClassLength,
            testCoveragePercentage = testCoveragePercentage,
            documentationCoveragePercentage = documentationCoveragePercentage
        )
    }
    
    /**
     * Extracts complexity value from a finding's description or title.
     * Looks for patterns like "complexity: 20" or "complexity of 20".
     */
    private fun extractComplexityValue(finding: Finding): Double? {
        val text = "${finding.title} ${finding.description}"
        val regex = """complexity[:\s]+of[\s]+(\d+)|complexity[:\s]+(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.firstOrNull { it.toDoubleOrNull() != null }?.toDoubleOrNull()
    }
    
    /**
     * Extracts length value (lines of code) from a finding's description or title.
     * Looks for patterns like "50 lines" or "length: 50".
     */
    private fun extractLengthValue(finding: Finding): Double? {
        val text = "${finding.title} ${finding.description}"
        val regex = """(\d+)[\s]+lines|length[:\s]+(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.firstOrNull { it.toDoubleOrNull() != null }?.toDoubleOrNull()
    }
}
