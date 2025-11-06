package com.shoppit.analysis.reporting

import com.shoppit.analysis.core.AggregatedResult
import com.shoppit.analysis.core.Baseline
import com.shoppit.analysis.core.ReportGenerator
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.AnalysisMetrics
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of ReportGenerator that creates markdown-formatted analysis reports.
 */
class ReportGeneratorImpl : ReportGenerator {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    
    override fun generate(result: AggregatedResult, baseline: Baseline?): String {
        return buildString {
            appendLine("# Code Quality Analysis Report")
            appendLine()
            appendLine("**Generated:** ${dateFormat.format(Date())}")
            appendLine("**Files Analyzed:** ${result.metrics.totalFiles}")
            appendLine("**Total Findings:** ${result.metrics.totalFindings}")
            appendLine()
            
            // Executive Summary
            append(generateSummary(result.metrics, baseline))
            appendLine()
            
            // Findings by Priority and Category
            appendLine("## Findings by Category")
            appendLine()
            appendFindingsByPriority(result)
            appendLine()
            
            // Detailed Findings
            appendLine("## Detailed Findings")
            appendLine()
            append(generateFindings(result.findings))
            appendLine()
            
            // Improvement Recommendations
            appendLine("## Improvement Recommendations")
            appendLine()
            appendImprovementRecommendations(result.findings)
            appendLine()
            
            // Baseline Comparison (if available)
            if (baseline != null) {
                appendLine("## Baseline Comparison")
                appendLine()
                appendBaselineComparison(result, baseline)
                appendLine()
            }
            
            // Next Steps
            appendLine("## Next Steps")
            appendLine()
            appendNextSteps(result)
        }
    }
    
    override fun generateSummary(metrics: AnalysisMetrics, baseline: Baseline?): String {
        return buildString {
            appendLine("## Executive Summary")
            appendLine()
            
            // Key Metrics
            appendLine("### Key Metrics")
            appendLine()
            
            Priority.values().forEach { priority ->
                val count = metrics.findingsByPriority[priority] ?: 0
                val trend = if (baseline != null) {
                    val baselineCount = baseline.metrics.findingsByPriority[priority] ?: 0
                    val diff = count - baselineCount
                    when {
                        diff > 0 -> " (↑ +$diff from baseline)"
                        diff < 0 -> " (↓ $diff from baseline)"
                        else -> " (→ no change)"
                    }
                } else ""
                
                appendLine("- **${priority.name.capitalize()} Issues:** $count$trend")
            }
            appendLine()
            
            // Code Metrics
            appendLine("### Code Metrics")
            appendLine()
            appendLine("- **Average Cyclomatic Complexity:** ${"%.2f".format(metrics.averageComplexity)}")
            appendLine("- **Average Function Length:** ${"%.1f".format(metrics.averageFunctionLength)} lines")
            appendLine("- **Average Class Length:** ${"%.1f".format(metrics.averageClassLength)} lines")
            appendLine("- **Test Coverage:** ${"%.1f".format(metrics.testCoveragePercentage)}%")
            appendLine("- **Documentation Coverage:** ${"%.1f".format(metrics.documentationCoveragePercentage)}%")
            appendLine()
            
            // Top Issues
            appendLine("### Top Issues by Category")
            appendLine()
            val topCategories = metrics.findingsByCategory
                .entries
                .sortedByDescending { it.value }
                .take(5)
            
            if (topCategories.isNotEmpty()) {
                topCategories.forEachIndexed { index, (category, count) ->
                    appendLine("${index + 1}. **${category.name.replace('_', ' ').capitalize()}:** $count issues")
                }
            } else {
                appendLine("No issues found.")
            }
        }
    }
    
    override fun generateFindings(findings: List<Finding>): String {
        if (findings.isEmpty()) {
            return "No findings to report.\n"
        }
        
        return buildString {
            // Group by priority, then by category
            val groupedFindings = findings
                .groupBy { it.priority }
                .toSortedMap(compareBy { it.ordinal })
            
            groupedFindings.forEach { (priority, priorityFindings) ->
                appendLine("### ${priority.name.capitalize()} Priority")
                appendLine()
                
                val byCategory = priorityFindings.groupBy { it.category }
                byCategory.forEach { (category, categoryFindings) ->
                    appendLine("#### ${category.name.replace('_', ' ').capitalize()}")
                    appendLine()
                    
                    categoryFindings.forEach { finding ->
                        appendFinding(finding)
                        appendLine()
                    }
                }
            }
        }
    }
    
    private fun StringBuilder.appendFinding(finding: Finding) {
        appendLine("##### ${finding.title}")
        appendLine()
        appendLine("**File:** `${finding.file}:${finding.lineNumber}`")
        appendLine()
        appendLine("**Description:** ${finding.description}")
        appendLine()
        
        // Code Snippet
        if (finding.codeSnippet.isNotBlank()) {
            appendLine("**Current Code:**")
            appendLine("```kotlin")
            appendLine(finding.codeSnippet.trim())
            appendLine("```")
            appendLine()
        }
        
        // Recommendation
        appendLine("**Recommendation:** ${finding.recommendation}")
        appendLine()
        
        // Before/After Examples
        if (finding.beforeExample != null && finding.afterExample != null) {
            appendLine("**Before:**")
            appendLine("```kotlin")
            appendLine(finding.beforeExample.trim())
            appendLine("```")
            appendLine()
            
            appendLine("**After:**")
            appendLine("```kotlin")
            appendLine(finding.afterExample.trim())
            appendLine("```")
            appendLine()
        }
        
        // Effort and Auto-fix
        appendLine("**Effort:** ${finding.effort.name.capitalize()}")
        if (finding.autoFixable) {
            appendLine("**Auto-fixable:** Yes")
        }
        appendLine()
        
        // References
        if (finding.references.isNotEmpty()) {
            appendLine("**References:**")
            finding.references.forEach { ref ->
                appendLine("- $ref")
            }
            appendLine()
        }
        
        appendLine("---")
    }
    
    private fun StringBuilder.appendFindingsByPriority(result: AggregatedResult) {
        Priority.values().forEach { priority ->
            val findings = result.byPriority[priority] ?: emptyList()
            if (findings.isNotEmpty()) {
                appendLine("### ${priority.name.capitalize()} Priority (${findings.size} issues)")
                appendLine()
                
                val byCategory = findings.groupBy { it.category }
                byCategory.forEach { (category, categoryFindings) ->
                    appendLine("- **${category.name.replace('_', ' ').capitalize()}:** ${categoryFindings.size} issues")
                }
                appendLine()
            }
        }
    }
    
    private fun StringBuilder.appendImprovementRecommendations(findings: List<Finding>) {
        val byEffort = findings.groupBy { it.effort }
        
        // Quick Wins (Trivial)
        val trivial = byEffort[Effort.TRIVIAL] ?: emptyList()
        if (trivial.isNotEmpty()) {
            appendLine("### Quick Wins (< 5 minutes each)")
            appendLine()
            trivial.take(10).forEach { finding ->
                appendLine("- ${finding.title} (`${finding.file}:${finding.lineNumber}`)")
            }
            if (trivial.size > 10) {
                appendLine("- ... and ${trivial.size - 10} more")
            }
            appendLine()
        }
        
        // Short Term (Small)
        val small = byEffort[Effort.SMALL] ?: emptyList()
        if (small.isNotEmpty()) {
            appendLine("### Short Term (5-30 minutes each)")
            appendLine()
            small.take(10).forEach { finding ->
                appendLine("- ${finding.title} (`${finding.file}:${finding.lineNumber}`)")
            }
            if (small.size > 10) {
                appendLine("- ... and ${small.size - 10} more")
            }
            appendLine()
        }
        
        // Medium Term (Medium)
        val medium = byEffort[Effort.MEDIUM] ?: emptyList()
        if (medium.isNotEmpty()) {
            appendLine("### Medium Term (30 minutes - 2 hours each)")
            appendLine()
            medium.take(10).forEach { finding ->
                appendLine("- ${finding.title} (`${finding.file}:${finding.lineNumber}`)")
            }
            if (medium.size > 10) {
                appendLine("- ... and ${medium.size - 10} more")
            }
            appendLine()
        }
        
        // Long Term (Large)
        val large = byEffort[Effort.LARGE] ?: emptyList()
        if (large.isNotEmpty()) {
            appendLine("### Long Term (> 2 hours each)")
            appendLine()
            large.take(10).forEach { finding ->
                appendLine("- ${finding.title} (`${finding.file}:${finding.lineNumber}`)")
            }
            if (large.size > 10) {
                appendLine("- ... and ${large.size - 10} more")
            }
            appendLine()
        }
    }
    
    private fun StringBuilder.appendBaselineComparison(result: AggregatedResult, baseline: Baseline) {
        val currentMetrics = result.metrics
        val baselineMetrics = baseline.metrics
        
        // Improvements
        appendLine("### Improvements")
        appendLine()
        
        val improvements = mutableListOf<String>()
        
        // Check each priority level
        Priority.values().forEach { priority ->
            val current = currentMetrics.findingsByPriority[priority] ?: 0
            val base = baselineMetrics.findingsByPriority[priority] ?: 0
            if (current < base) {
                improvements.add("- **${priority.name.capitalize()}:** ${base - current} issues resolved")
            }
        }
        
        // Check metrics
        if (currentMetrics.averageComplexity < baselineMetrics.averageComplexity) {
            val improvement = ((baselineMetrics.averageComplexity - currentMetrics.averageComplexity) / 
                baselineMetrics.averageComplexity * 100)
            improvements.add("- **Complexity:** Improved by ${"%.1f".format(improvement)}%")
        }
        
        if (currentMetrics.testCoveragePercentage > baselineMetrics.testCoveragePercentage) {
            val improvement = currentMetrics.testCoveragePercentage - baselineMetrics.testCoveragePercentage
            improvements.add("- **Test Coverage:** Increased by ${"%.1f".format(improvement)}%")
        }
        
        if (improvements.isNotEmpty()) {
            improvements.forEach { appendLine(it) }
        } else {
            appendLine("No improvements detected.")
        }
        appendLine()
        
        // Regressions
        appendLine("### Regressions")
        appendLine()
        
        val regressions = mutableListOf<String>()
        
        // Check each priority level
        Priority.values().forEach { priority ->
            val current = currentMetrics.findingsByPriority[priority] ?: 0
            val base = baselineMetrics.findingsByPriority[priority] ?: 0
            if (current > base) {
                regressions.add("- **${priority.name.capitalize()}:** ${current - base} new issues")
            }
        }
        
        // Check metrics
        if (currentMetrics.averageComplexity > baselineMetrics.averageComplexity) {
            val regression = ((currentMetrics.averageComplexity - baselineMetrics.averageComplexity) / 
                baselineMetrics.averageComplexity * 100)
            regressions.add("- **Complexity:** Degraded by ${"%.1f".format(regression)}%")
        }
        
        if (currentMetrics.testCoveragePercentage < baselineMetrics.testCoveragePercentage) {
            val regression = baselineMetrics.testCoveragePercentage - currentMetrics.testCoveragePercentage
            regressions.add("- **Test Coverage:** Decreased by ${"%.1f".format(regression)}%")
        }
        
        if (regressions.isNotEmpty()) {
            regressions.forEach { appendLine(it) }
        } else {
            appendLine("No regressions detected.")
        }
    }
    
    private fun StringBuilder.appendNextSteps(result: AggregatedResult) {
        val criticalCount = result.byPriority[Priority.CRITICAL]?.size ?: 0
        val highCount = result.byPriority[Priority.HIGH]?.size ?: 0
        val mediumCount = result.byPriority[Priority.MEDIUM]?.size ?: 0
        val lowCount = result.byPriority[Priority.LOW]?.size ?: 0
        
        if (criticalCount > 0) {
            appendLine("1. **Address all Critical priority issues immediately** ($criticalCount issues)")
        }
        
        if (highCount > 0) {
            appendLine("${if (criticalCount > 0) "2" else "1"}. **Create tasks for High priority issues** ($highCount issues)")
        }
        
        if (mediumCount > 0) {
            val step = if (criticalCount > 0 && highCount > 0) "3" 
                      else if (criticalCount > 0 || highCount > 0) "2" 
                      else "1"
            appendLine("$step. **Schedule refactoring for Medium priority issues** ($mediumCount issues)")
        }
        
        if (lowCount > 0) {
            val step = listOf(criticalCount, highCount, mediumCount).count { it > 0 } + 1
            appendLine("$step. **Consider Low priority issues for future improvements** ($lowCount issues)")
        }
        
        if (criticalCount == 0 && highCount == 0 && mediumCount == 0 && lowCount == 0) {
            appendLine("No issues found. Great job maintaining code quality!")
        }
    }
    
    private fun String.capitalize(): String {
        return this.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() 
        }
    }
}
