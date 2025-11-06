package com.shoppit.analysis.reporting

import com.shoppit.analysis.core.AnalysisResult
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Implementation of ReportGenerator that creates Markdown reports.
 */
class ReportGeneratorImpl : ReportGenerator {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    override fun generateReport(result: AnalysisResult, outputFile: File) {
        val report = buildString {
            appendHeader(result)
            appendSummary(result)
            appendNewAndFixedFindings(result)
            appendFindingsByPriority(result)
            appendFindingsByCategory(result)
            appendDetailedFindings(result)
            appendFooter(result)
        }
        
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(report)
        
        println("Report generated: ${outputFile.absolutePath}")
    }
    
    private fun StringBuilder.appendHeader(result: AnalysisResult) {
        appendLine("# Code Quality Analysis Report")
        appendLine()
        appendLine("**Generated:** ${LocalDateTime.now().format(dateFormatter)}")
        appendLine("**Analysis Time:** ${result.metrics.analysisTimeMs}ms")
        appendLine()
        appendLine("---")
        appendLine()
    }
    
    private fun StringBuilder.appendSummary(result: AnalysisResult) {
        val metrics = result.metrics
        
        appendLine("## Summary")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|--------|-------|")
        appendLine("| Total Files | ${metrics.totalFiles} |")
        appendLine("| Kotlin Files | ${metrics.kotlinFiles} |")
        appendLine("| Total Lines | ${metrics.totalLines} |")
        appendLine("| Total Findings | ${metrics.totalFindings} |")
        appendLine("| Critical | ${metrics.criticalCount} |")
        appendLine("| High | ${metrics.highCount} |")
        appendLine("| Medium | ${metrics.mediumCount} |")
        appendLine("| Low | ${metrics.lowCount} |")
        appendLine("| Info | ${metrics.infoCount} |")
        appendLine()
    }
    
    private fun StringBuilder.appendNewAndFixedFindings(result: AnalysisResult) {
        if (result.newFindings.isNotEmpty() || result.fixedFindings.isNotEmpty()) {
            appendLine("## Changes Since Baseline")
            appendLine()
            
            if (result.newFindings.isNotEmpty()) {
                appendLine("### 🆕 New Findings: ${result.newFindings.size}")
                appendLine()
                result.newFindings.take(10).forEach { finding ->
                    appendLine("- **${finding.priority}** - ${finding.title} (${finding.file}:${finding.line ?: "?"})")
                }
                if (result.newFindings.size > 10) {
                    appendLine("- ... and ${result.newFindings.size - 10} more")
                }
                appendLine()
            }
            
            if (result.fixedFindings.isNotEmpty()) {
                appendLine("### ✅ Fixed Findings: ${result.fixedFindings.size}")
                appendLine()
                result.fixedFindings.take(10).forEach { finding ->
                    appendLine("- **${finding.priority}** - ${finding.title} (${finding.file}:${finding.line ?: "?"})")
                }
                if (result.fixedFindings.size > 10) {
                    appendLine("- ... and ${result.fixedFindings.size - 10} more")
                }
                appendLine()
            }
        }
    }
    
    private fun StringBuilder.appendFindingsByPriority(result: AnalysisResult) {
        appendLine("## Findings by Priority")
        appendLine()
        
        Priority.values().forEach { priority ->
            val count = result.metrics.findingsByPriority[priority] ?: 0
            if (count > 0) {
                val icon = when (priority) {
                    Priority.CRITICAL -> "🔴"
                    Priority.HIGH -> "🟠"
                    Priority.MEDIUM -> "🟡"
                    Priority.LOW -> "🔵"
                    Priority.INFO -> "⚪"
                }
                appendLine("### $icon $priority: $count")
                appendLine()
                
                val findings = result.findings.filter { it.priority == priority }.take(5)
                findings.forEach { finding ->
                    appendLine("- **${finding.category}** - ${finding.title}")
                    appendLine("  - File: `${finding.file}:${finding.line ?: "?"}`")
                    appendLine("  - ${finding.description}")
                    appendLine()
                }
                
                if (count > 5) {
                    appendLine("_... and ${count - 5} more ${priority.name.lowercase()} priority findings_")
                    appendLine()
                }
            }
        }
    }
    
    private fun StringBuilder.appendFindingsByCategory(result: AnalysisResult) {
        appendLine("## Findings by Category")
        appendLine()
        
        result.metrics.findingsByCategory.entries
            .sortedByDescending { it.value }
            .forEach { (category, count) ->
                appendLine("### ${category.name.replace("_", " ")}: $count")
                appendLine()
                
                val findings = result.findings.filter { it.category == category }.take(3)
                findings.forEach { finding ->
                    appendLine("- **${finding.priority}** - ${finding.title} (`${finding.file}:${finding.line ?: "?"}`)")
                }
                
                if (count > 3) {
                    appendLine("- _... and ${count - 3} more_")
                }
                appendLine()
            }
    }
    
    private fun StringBuilder.appendDetailedFindings(result: AnalysisResult) {
        if (result.findings.isEmpty()) {
            appendLine("## Detailed Findings")
            appendLine()
            appendLine("✅ No issues found!")
            appendLine()
            return
        }
        
        appendLine("## Detailed Findings")
        appendLine()
        
        result.findings.forEach { finding ->
            appendLine("### ${finding.id}")
            appendLine()
            appendLine("**Priority:** ${finding.priority}")
            appendLine("**Category:** ${finding.category}")
            appendLine("**File:** `${finding.file}:${finding.line ?: "?"}`")
            appendLine()
            appendLine("**Title:** ${finding.title}")
            appendLine()
            appendLine("**Description:**")
            appendLine(finding.description)
            appendLine()
            
            finding.suggestion?.let {
                appendLine("**Suggestion:**")
                appendLine(it)
                appendLine()
            }
            
            finding.codeSnippet?.let {
                appendLine("**Code:**")
                appendLine("```kotlin")
                appendLine(it)
                appendLine("```")
                appendLine()
            }
            
            if (finding.metadata.isNotEmpty()) {
                appendLine("**Metadata:**")
                finding.metadata.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
                appendLine()
            }
            
            appendLine("**Effort:** ${finding.effort}")
            appendLine()
            appendLine("---")
            appendLine()
        }
    }
    
    private fun StringBuilder.appendFooter(result: AnalysisResult) {
        appendLine("## Analysis Complete")
        appendLine()
        appendLine("Generated by Shoppit Code Quality Analysis Plugin")
        appendLine()
    }
}
