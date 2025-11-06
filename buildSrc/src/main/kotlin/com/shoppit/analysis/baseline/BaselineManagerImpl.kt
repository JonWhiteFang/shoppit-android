package com.shoppit.analysis.baseline

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shoppit.analysis.core.AggregatedResult
import com.shoppit.analysis.core.Baseline
import com.shoppit.analysis.core.BaselineManager
import com.shoppit.analysis.core.Comparison
import com.shoppit.analysis.models.AnalysisMetrics
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Implementation of BaselineManager that stores baselines and history as JSON files.
 *
 * @property outputPath Base directory for storing baseline and history files
 */
class BaselineManagerImpl(
    private val outputPath: String = ".kiro/specs/code-quality-analysis"
) : BaselineManager {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private val baselineFile: File
        get() = File(outputPath, "baseline.json")
    
    private val historyDir: File
        get() = File(outputPath, "history")
    
    init {
        // Ensure directories exist
        File(outputPath).mkdirs()
        historyDir.mkdirs()
    }
    
    override fun loadBaseline(): Baseline? {
        return try {
            if (!baselineFile.exists()) {
                return null
            }
            
            val json = baselineFile.readText()
            gson.fromJson(json, Baseline::class.java)
        } catch (e: IOException) {
            System.err.println("Failed to load baseline: ${e.message}")
            null
        } catch (e: Exception) {
            System.err.println("Failed to parse baseline: ${e.message}")
            null
        }
    }
    
    override fun saveBaseline(metrics: AnalysisMetrics, findings: List<Finding>) {
        try {
            val baseline = Baseline(
                timestamp = System.currentTimeMillis(),
                metrics = metrics,
                findingIds = findings.map { it.id }.toSet()
            )
            
            val json = gson.toJson(baseline)
            baselineFile.writeText(json)
            
            println("Baseline saved to: ${baselineFile.absolutePath}")
        } catch (e: IOException) {
            System.err.println("Failed to save baseline: ${e.message}")
            throw e
        }
    }
    
    override fun compare(current: AnalysisMetrics, baseline: Baseline): Comparison {
        val improved = mutableMapOf<String, Double>()
        val regressed = mutableMapOf<String, Double>()
        
        // Compare findings by priority
        Priority.values().forEach { priority ->
            val currentCount = current.findingsByPriority[priority] ?: 0
            val baselineCount = baseline.metrics.findingsByPriority[priority] ?: 0
            
            if (currentCount < baselineCount) {
                val improvement = ((baselineCount - currentCount).toDouble() / baselineCount) * 100
                improved["${priority.name.lowercase()}_issues"] = improvement
            } else if (currentCount > baselineCount) {
                val regression = ((currentCount - baselineCount).toDouble() / 
                    (baselineCount.takeIf { it > 0 } ?: 1)) * 100
                regressed["${priority.name.lowercase()}_issues"] = regression
            }
        }
        
        // Compare complexity
        if (current.averageComplexity < baseline.metrics.averageComplexity) {
            val improvement = ((baseline.metrics.averageComplexity - current.averageComplexity) / 
                baseline.metrics.averageComplexity) * 100
            improved["average_complexity"] = improvement
        } else if (current.averageComplexity > baseline.metrics.averageComplexity) {
            val regression = ((current.averageComplexity - baseline.metrics.averageComplexity) / 
                baseline.metrics.averageComplexity) * 100
            regressed["average_complexity"] = regression
        }
        
        // Compare function length
        if (current.averageFunctionLength < baseline.metrics.averageFunctionLength) {
            val improvement = ((baseline.metrics.averageFunctionLength - current.averageFunctionLength) / 
                baseline.metrics.averageFunctionLength) * 100
            improved["average_function_length"] = improvement
        } else if (current.averageFunctionLength > baseline.metrics.averageFunctionLength) {
            val regression = ((current.averageFunctionLength - baseline.metrics.averageFunctionLength) / 
                baseline.metrics.averageFunctionLength) * 100
            regressed["average_function_length"] = regression
        }
        
        // Compare class length
        if (current.averageClassLength < baseline.metrics.averageClassLength) {
            val improvement = ((baseline.metrics.averageClassLength - current.averageClassLength) / 
                baseline.metrics.averageClassLength) * 100
            improved["average_class_length"] = improvement
        } else if (current.averageClassLength > baseline.metrics.averageClassLength) {
            val regression = ((current.averageClassLength - baseline.metrics.averageClassLength) / 
                baseline.metrics.averageClassLength) * 100
            regressed["average_class_length"] = regression
        }
        
        // Compare test coverage
        if (current.testCoveragePercentage > baseline.metrics.testCoveragePercentage) {
            val improvement = current.testCoveragePercentage - baseline.metrics.testCoveragePercentage
            improved["test_coverage"] = improvement
        } else if (current.testCoveragePercentage < baseline.metrics.testCoveragePercentage) {
            val regression = baseline.metrics.testCoveragePercentage - current.testCoveragePercentage
            regressed["test_coverage"] = regression
        }
        
        // Compare documentation coverage
        if (current.documentationCoveragePercentage > baseline.metrics.documentationCoveragePercentage) {
            val improvement = current.documentationCoveragePercentage - 
                baseline.metrics.documentationCoveragePercentage
            improved["documentation_coverage"] = improvement
        } else if (current.documentationCoveragePercentage < baseline.metrics.documentationCoveragePercentage) {
            val regression = baseline.metrics.documentationCoveragePercentage - 
                current.documentationCoveragePercentage
            regressed["documentation_coverage"] = regression
        }
        
        // Note: resolved and newIssues will be populated by comparing finding IDs
        // This requires the current findings list, which will be handled in saveToHistory
        return Comparison(
            improved = improved,
            regressed = regressed,
            resolved = emptyList(), // Will be populated when comparing findings
            newIssues = emptyList() // Will be populated when comparing findings
        )
    }
    
    override fun saveToHistory(result: AggregatedResult) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val timestamp = dateFormat.format(Date())
            val historyFile = File(historyDir, "analysis_$timestamp.json")
            
            // Create history entry with comparison to baseline if available
            val baseline = loadBaseline()
            val historyEntry = HistoryEntry(
                timestamp = System.currentTimeMillis(),
                metrics = result.metrics,
                findingIds = result.findings.map { it.id }.toSet(),
                comparison = baseline?.let { compare(result.metrics, it) }
            )
            
            val json = gson.toJson(historyEntry)
            historyFile.writeText(json)
            
            println("Analysis saved to history: ${historyFile.absolutePath}")
        } catch (e: IOException) {
            System.err.println("Failed to save analysis to history: ${e.message}")
            // Don't throw - history is optional
        }
    }
    
    /**
     * Compares current findings with baseline to identify resolved and new issues.
     *
     * @param currentFindings Current analysis findings
     * @param baseline Baseline to compare against
     * @return Comparison with resolved and new issue IDs
     */
    fun compareFindings(currentFindings: List<Finding>, baseline: Baseline): Comparison {
        val currentIds = currentFindings.map { it.id }.toSet()
        val baselineIds = baseline.findingIds
        
        val resolved = baselineIds.filter { it !in currentIds }
        val newIssues = currentIds.filter { it !in baselineIds }
        
        // Get metric comparison
        val metrics = currentFindings.firstOrNull()?.let { 
            // This is a simplified approach - in real usage, metrics would be passed separately
            AnalysisMetrics(
                totalFiles = 0,
                totalFindings = currentFindings.size,
                findingsByPriority = currentFindings.groupBy { it.priority }
                    .mapValues { it.value.size },
                findingsByCategory = currentFindings.groupBy { it.category }
                    .mapValues { it.value.size },
                averageComplexity = 0.0,
                averageFunctionLength = 0.0,
                averageClassLength = 0.0,
                testCoveragePercentage = 0.0,
                documentationCoveragePercentage = 0.0
            )
        }
        
        val metricComparison = metrics?.let { compare(it, baseline) } ?: Comparison(
            improved = emptyMap(),
            regressed = emptyMap(),
            resolved = emptyList(),
            newIssues = emptyList()
        )
        
        return metricComparison.copy(
            resolved = resolved,
            newIssues = newIssues
        )
    }
    
    /**
     * Loads all historical analysis entries.
     *
     * @return List of historical entries sorted by timestamp (newest first)
     */
    fun loadHistory(): List<HistoryEntry> {
        return try {
            if (!historyDir.exists()) {
                return emptyList()
            }
            
            historyDir.listFiles { file -> file.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val json = file.readText()
                        gson.fromJson(json, HistoryEntry::class.java)
                    } catch (e: Exception) {
                        System.err.println("Failed to parse history file ${file.name}: ${e.message}")
                        null
                    }
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            System.err.println("Failed to load history: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Clears the baseline file.
     */
    fun clearBaseline() {
        if (baselineFile.exists()) {
            baselineFile.delete()
            println("Baseline cleared")
        }
    }
    
    /**
     * Clears all history files.
     */
    fun clearHistory() {
        if (historyDir.exists()) {
            historyDir.listFiles()?.forEach { it.delete() }
            println("History cleared")
        }
    }
}

/**
 * Historical analysis entry stored in the history directory.
 *
 * @property timestamp When this analysis was performed
 * @property metrics Metrics from this analysis
 * @property findingIds Set of finding IDs from this analysis
 * @property comparison Comparison with baseline (if available)
 */
data class HistoryEntry(
    val timestamp: Long,
    val metrics: AnalysisMetrics,
    val findingIds: Set<String>,
    val comparison: Comparison?
)
