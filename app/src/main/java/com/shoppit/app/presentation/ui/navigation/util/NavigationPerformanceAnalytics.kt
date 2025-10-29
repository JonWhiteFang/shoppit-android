package com.shoppit.app.presentation.ui.navigation.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Aggregates all navigation performance metrics and provides comprehensive analytics.
 * Combines timing, frame rate, and memory metrics for complete performance monitoring.
 *
 * Requirements:
 * - 7.1: Complete navigation transitions within 300ms
 * - 7.2: Display smooth animations without frame drops
 * - 7.5: Monitor navigation performance metrics
 */
object NavigationPerformanceAnalytics {
    
    private val _performanceScore = MutableStateFlow<PerformanceScore>(PerformanceScore())
    val performanceScore: StateFlow<PerformanceScore> = _performanceScore.asStateFlow()
    
    private val performanceIssues = mutableListOf<PerformanceIssue>()
    private const val MAX_ISSUES_HISTORY = 50
    
    /**
     * Starts comprehensive performance monitoring for a navigation transition.
     * @param route The destination route being navigated to
     */
    fun startMonitoring(route: String) {
        NavigationPerformanceMonitor.startTransition(route)
        NavigationFrameRateMonitor.startMonitoring()
        NavigationMemoryMonitor.startMonitoring()
        
        Timber.d("Comprehensive performance monitoring started for: $route")
    }
    
    /**
     * Stops monitoring and analyzes all metrics.
     * @param route The destination route that was navigated to
     */
    fun stopMonitoring(route: String) {
        NavigationPerformanceMonitor.endTransition(route)
        NavigationFrameRateMonitor.stopMonitoring()
        NavigationMemoryMonitor.stopMonitoring()
        
        analyzePerformance(route)
        
        Timber.d("Comprehensive performance monitoring stopped for: $route")
    }
    
    /**
     * Analyzes all performance metrics and calculates a performance score.
     */
    private fun analyzePerformance(route: String) {
        val timingMetrics = NavigationPerformanceMonitor.metrics.value
        val frameMetrics = NavigationFrameRateMonitor.metrics.value
        val memoryMetrics = NavigationMemoryMonitor.metrics.value
        
        // Calculate individual scores (0-100)
        val timingScore = calculateTimingScore(timingMetrics.averageTransitionTimeMs)
        val frameRateScore = calculateFrameRateScore(frameMetrics.averageFps, frameMetrics.frameDropPercentage)
        val memoryScore = calculateMemoryScore(memoryMetrics.memoryUsagePercentage)
        
        // Overall score is weighted average
        val overallScore = (timingScore * 0.4 + frameRateScore * 0.4 + memoryScore * 0.2).toInt()
        
        _performanceScore.value = PerformanceScore(
            overallScore = overallScore,
            timingScore = timingScore,
            frameRateScore = frameRateScore,
            memoryScore = memoryScore,
            rating = getPerformanceRating(overallScore)
        )
        
        // Detect and record performance issues
        detectPerformanceIssues(route, timingMetrics, frameMetrics, memoryMetrics)
    }
    
    /**
     * Calculates timing score based on average transition time.
     */
    private fun calculateTimingScore(avgTimeMs: Double): Int {
        return when {
            avgTimeMs <= 200 -> 100
            avgTimeMs <= 300 -> 90
            avgTimeMs <= 400 -> 75
            avgTimeMs <= 500 -> 60
            avgTimeMs <= 700 -> 40
            else -> 20
        }
    }
    
    /**
     * Calculates frame rate score based on FPS and drop rate.
     */
    private fun calculateFrameRateScore(avgFps: Double, dropRate: Float): Int {
        val fpsScore = when {
            avgFps >= 58 -> 100
            avgFps >= 55 -> 90
            avgFps >= 50 -> 75
            avgFps >= 45 -> 60
            avgFps >= 40 -> 40
            else -> 20
        }
        
        val dropPenalty = (dropRate * 2).toInt() // 2 points per 1% drop rate
        
        return (fpsScore - dropPenalty).coerceIn(0, 100)
    }
    
    /**
     * Calculates memory score based on usage percentage.
     */
    private fun calculateMemoryScore(usagePercentage: Float): Int {
        return when {
            usagePercentage <= 50 -> 100
            usagePercentage <= 60 -> 90
            usagePercentage <= 70 -> 75
            usagePercentage <= 80 -> 60
            usagePercentage <= 90 -> 40
            else -> 20
        }
    }
    
    /**
     * Gets performance rating based on overall score.
     */
    private fun getPerformanceRating(score: Int): PerformanceRating {
        return when {
            score >= 90 -> PerformanceRating.EXCELLENT
            score >= 75 -> PerformanceRating.GOOD
            score >= 60 -> PerformanceRating.FAIR
            score >= 40 -> PerformanceRating.POOR
            else -> PerformanceRating.CRITICAL
        }
    }
    
    /**
     * Detects and records performance issues.
     */
    private fun detectPerformanceIssues(
        route: String,
        timingMetrics: NavigationMetrics,
        frameMetrics: FrameRateMetrics,
        memoryMetrics: MemoryMetrics
    ) {
        val issues = mutableListOf<PerformanceIssue>()
        
        // Check timing issues
        if (timingMetrics.averageTransitionTimeMs > 300) {
            issues.add(
                PerformanceIssue(
                    route = route,
                    type = IssueType.SLOW_TRANSITION,
                    severity = if (timingMetrics.averageTransitionTimeMs > 500) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "Slow transition: ${"%.0f".format(timingMetrics.averageTransitionTimeMs)}ms (target: 300ms)",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check frame rate issues
        if (frameMetrics.averageFps < 55) {
            issues.add(
                PerformanceIssue(
                    route = route,
                    type = IssueType.LOW_FRAME_RATE,
                    severity = if (frameMetrics.averageFps < 45) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "Low frame rate: ${"%.1f".format(frameMetrics.averageFps)} fps (target: 60 fps)",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        if (frameMetrics.frameDropPercentage > 5.0f) {
            issues.add(
                PerformanceIssue(
                    route = route,
                    type = IssueType.FRAME_DROPS,
                    severity = if (frameMetrics.frameDropPercentage > 10.0f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "High frame drop rate: ${"%.1f".format(frameMetrics.frameDropPercentage)}%",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Check memory issues
        if (memoryMetrics.memoryUsagePercentage > 80f) {
            issues.add(
                PerformanceIssue(
                    route = route,
                    type = IssueType.HIGH_MEMORY_USAGE,
                    severity = if (memoryMetrics.memoryUsagePercentage > 90f) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "High memory usage: ${"%.1f".format(memoryMetrics.memoryUsagePercentage)}%",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        val memoryDeltaMB = memoryMetrics.memoryDeltaBytes / (1024.0 * 1024.0)
        if (memoryDeltaMB > 50) {
            issues.add(
                PerformanceIssue(
                    route = route,
                    type = IssueType.MEMORY_LEAK,
                    severity = if (memoryDeltaMB > 100) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    description = "Large memory increase: ${"%.2f".format(memoryDeltaMB)} MB",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        
        // Record issues
        performanceIssues.addAll(issues)
        
        // Keep history size manageable
        while (performanceIssues.size > MAX_ISSUES_HISTORY) {
            performanceIssues.removeAt(0)
        }
        
        // Log issues
        issues.forEach { issue ->
            when (issue.severity) {
                IssueSeverity.HIGH -> Timber.e("Performance Issue [${issue.type}]: ${issue.description}")
                IssueSeverity.MEDIUM -> Timber.w("Performance Issue [${issue.type}]: ${issue.description}")
                IssueSeverity.LOW -> Timber.i("Performance Issue [${issue.type}]: ${issue.description}")
            }
        }
    }
    
    /**
     * Gets recent performance issues.
     */
    fun getRecentIssues(count: Int = 10): List<PerformanceIssue> {
        return performanceIssues.takeLast(count)
    }
    
    /**
     * Gets issues for a specific route.
     */
    fun getIssuesForRoute(route: String): List<PerformanceIssue> {
        return performanceIssues.filter { it.route == route }
    }
    
    /**
     * Gets comprehensive performance report.
     */
    fun getComprehensiveReport(context: Context? = null): String {
        val score = _performanceScore.value
        
        return buildString {
            appendLine("=================================")
            appendLine("Navigation Performance Analytics")
            appendLine("=================================")
            appendLine()
            appendLine("Overall Performance Score: ${score.overallScore}/100 (${score.rating})")
            appendLine("  - Timing Score: ${score.timingScore}/100")
            appendLine("  - Frame Rate Score: ${score.frameRateScore}/100")
            appendLine("  - Memory Score: ${score.memoryScore}/100")
            appendLine()
            appendLine(NavigationPerformanceMonitor.getPerformanceReport())
            appendLine()
            appendLine(NavigationFrameRateMonitor.getFrameRateReport())
            appendLine()
            appendLine(NavigationMemoryMonitor.getMemoryReport(context))
            appendLine()
            appendLine(NavigationPreloader.getStatistics())
            appendLine()
            
            if (performanceIssues.isNotEmpty()) {
                appendLine("Recent Performance Issues:")
                appendLine("==========================")
                getRecentIssues(5).forEach { issue ->
                    appendLine("[${issue.severity}] ${issue.type}: ${issue.description}")
                }
            } else {
                appendLine("No performance issues detected")
            }
        }
    }
    
    /**
     * Clears all performance data.
     */
    fun clearAllData() {
        NavigationPerformanceMonitor.clearMetrics()
        NavigationFrameRateMonitor.clearMetrics()
        NavigationMemoryMonitor.clearMetrics()
        NavigationPreloader.clearData()
        performanceIssues.clear()
        _performanceScore.value = PerformanceScore()
        
        Timber.d("All performance analytics data cleared")
    }
}

/**
 * Overall performance score with breakdown.
 */
data class PerformanceScore(
    val overallScore: Int = 0,
    val timingScore: Int = 0,
    val frameRateScore: Int = 0,
    val memoryScore: Int = 0,
    val rating: PerformanceRating = PerformanceRating.UNKNOWN
)

/**
 * Performance rating categories.
 */
enum class PerformanceRating {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    CRITICAL,
    UNKNOWN
}

/**
 * Represents a detected performance issue.
 */
data class PerformanceIssue(
    val route: String,
    val type: IssueType,
    val severity: IssueSeverity,
    val description: String,
    val timestamp: Long
)

/**
 * Types of performance issues.
 */
enum class IssueType {
    SLOW_TRANSITION,
    LOW_FRAME_RATE,
    FRAME_DROPS,
    HIGH_MEMORY_USAGE,
    MEMORY_LEAK
}

/**
 * Severity levels for performance issues.
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH
}
