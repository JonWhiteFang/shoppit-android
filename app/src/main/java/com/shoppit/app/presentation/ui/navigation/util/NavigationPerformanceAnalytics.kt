package com.shoppit.app.presentation.ui.navigation.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance rating enum.
 */
enum class PerformanceRating {
    EXCELLENT,  // 90-100
    GOOD,       // 75-89
    FAIR,       // 60-74
    POOR,       // 40-59
    CRITICAL,   // 0-39
    UNKNOWN
}

/**
 * Performance score data class.
 */
data class PerformanceScore(
    val overallScore: Int = 0,
    val timingScore: Int = 0,
    val frameRateScore: Int = 0,
    val memoryScore: Int = 0,
    val rating: PerformanceRating = PerformanceRating.UNKNOWN
)

/**
 * Issue severity enum.
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Issue type enum.
 */
enum class IssueType {
    SLOW_TRANSITION,
    HIGH_MEMORY_USAGE,
    FRAME_DROPS,
    MEMORY_LEAK,
    GENERAL_PERFORMANCE
}

/**
 * Performance issue data class.
 */
data class PerformanceIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val route: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Utility object for monitoring navigation performance.
 * Tracks navigation timing, memory usage, and performance metrics.
 *
 * Requirements:
 * - 9.3: Collect navigation performance metrics
 * - 9.4: Monitor navigation timing and latency
 * - 9.5: Track memory usage during navigation
 */
object NavigationPerformanceAnalytics {
    
    private val navigationStartTimes = ConcurrentHashMap<String, Long>()
    private val navigationMetrics = ConcurrentHashMap<String, NavigationMetrics>()
    private val performanceIssues = mutableListOf<PerformanceIssue>()
    
    private val _performanceScore = MutableStateFlow(PerformanceScore())
    val performanceScore: StateFlow<PerformanceScore> = _performanceScore.asStateFlow()
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    init {
        updatePerformanceScore()
    }
    
    /**
     * Data class to hold navigation performance metrics.
     */
    data class NavigationMetrics(
        val route: String,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val memoryUsedBytes: Long? = null,
        val success: Boolean = true
    )
    
    /**
     * Starts monitoring navigation performance for a route.
     *
     * @param route The route being navigated to
     */
    fun startMonitoring(route: String) {
        val startTime = System.currentTimeMillis()
        navigationStartTimes[route] = startTime
        
        Timber.d("Started performance monitoring for route: ${sanitize(route)}")
    }
    
    /**
     * Stops monitoring and records navigation performance metrics.
     *
     * @param route The route that was navigated to
     * @param success Whether the navigation was successful
     */
    fun stopMonitoring(route: String, success: Boolean = true) {
        val startTime = navigationStartTimes.remove(route)
        if (startTime == null) {
            Timber.w("No start time found for route: ${sanitize(route)}")
            return
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Get memory usage (optional)
        val memoryUsed = try {
            val runtime = Runtime.getRuntime()
            runtime.totalMemory() - runtime.freeMemory()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get memory usage")
            null
        }
        
        val metrics = NavigationMetrics(
            route = route,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            memoryUsedBytes = memoryUsed,
            success = success
        )
        
        navigationMetrics[route] = metrics
        
        Timber.d("Navigation performance for '${sanitize(route)}': ${duration}ms, success=$success")
        
        // Log warning if navigation took too long
        if (duration > 1000) {
            Timber.w("Slow navigation detected: ${sanitize(route)} took ${duration}ms")
            recordIssue(
                PerformanceIssue(
                    type = IssueType.SLOW_TRANSITION,
                    severity = if (duration > 2000) IssueSeverity.HIGH else IssueSeverity.MEDIUM,
                    route = route,
                    description = "Navigation took ${duration}ms (threshold: 1000ms)"
                )
            )
        }
        
        updatePerformanceScore()
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("navigation_performance") {
        //     param("route", route)
        //     param("duration_ms", duration)
        //     param("success", success.toString())
        //     memoryUsed?.let { param("memory_bytes", it) }
        // }
    }
    
    /**
     * Gets the performance metrics for a specific route.
     *
     * @param route The route to get metrics for
     * @return The navigation metrics, or null if not found
     */
    fun getMetrics(route: String): NavigationMetrics? {
        return navigationMetrics[route]
    }
    
    /**
     * Gets all recorded navigation metrics.
     *
     * @return Map of route to navigation metrics
     */
    fun getAllMetrics(): Map<String, NavigationMetrics> {
        return navigationMetrics.toMap()
    }
    
    /**
     * Clears all recorded metrics.
     */
    fun clearMetrics() {
        navigationStartTimes.clear()
        navigationMetrics.clear()
        Timber.d("Cleared all navigation performance metrics")
        updatePerformanceScore()
    }
    
    /**
     * Clears all data including metrics and issues.
     */
    fun clearAllData() {
        clearMetrics()
        synchronized(performanceIssues) {
            performanceIssues.clear()
        }
        updatePerformanceScore()
        Timber.d("Cleared all performance analytics data")
    }
    
    /**
     * Gets the average navigation duration across all routes.
     *
     * @return Average duration in milliseconds, or null if no metrics
     */
    fun getAverageDuration(): Long? {
        val metrics = navigationMetrics.values
        if (metrics.isEmpty()) return null
        
        return metrics.map { it.duration }.average().toLong()
    }
    
    /**
     * Gets the slowest navigation route.
     *
     * @return The slowest navigation metrics, or null if no metrics
     */
    fun getSlowestNavigation(): NavigationMetrics? {
        return navigationMetrics.values.maxByOrNull { it.duration }
    }
    
    /**
     * Gets the success rate of navigations.
     *
     * @return Success rate as a percentage (0-100), or null if no metrics
     */
    fun getSuccessRate(): Double? {
        val metrics = navigationMetrics.values
        if (metrics.isEmpty()) return null
        
        val successCount = metrics.count { it.success }
        return (successCount.toDouble() / metrics.size) * 100
    }
    
    /**
     * Logs a summary of navigation performance.
     */
    fun logPerformanceSummary() {
        val metrics = navigationMetrics.values
        if (metrics.isEmpty()) {
            Timber.i("No navigation performance metrics available")
            return
        }
        
        val avgDuration = getAverageDuration()
        val slowest = getSlowestNavigation()
        val successRate = getSuccessRate()
        
        Timber.i("""
            Navigation Performance Summary:
            - Total navigations: ${metrics.size}
            - Average duration: ${avgDuration}ms
            - Slowest navigation: ${slowest?.route?.let { sanitize(it) }} (${slowest?.duration}ms)
            - Success rate: ${"%.2f".format(successRate)}%
        """.trimIndent())
    }
    
    /**
     * Records a performance issue.
     */
    private fun recordIssue(issue: PerformanceIssue) {
        synchronized(performanceIssues) {
            performanceIssues.add(issue)
            // Keep only last 100 issues
            if (performanceIssues.size > 100) {
                performanceIssues.removeAt(0)
            }
        }
    }
    
    /**
     * Gets recent performance issues.
     *
     * @param limit Maximum number of issues to return
     * @return List of recent performance issues
     */
    fun getRecentIssues(limit: Int = 20): List<PerformanceIssue> {
        return synchronized(performanceIssues) {
            performanceIssues.takeLast(limit).reversed()
        }
    }
    
    /**
     * Updates the performance score based on current metrics.
     */
    private fun updatePerformanceScore() {
        val metrics = navigationMetrics.values
        if (metrics.isEmpty()) {
            _performanceScore.value = PerformanceScore(
                overallScore = 100,
                timingScore = 100,
                frameRateScore = 100,
                memoryScore = 100,
                rating = PerformanceRating.EXCELLENT
            )
            return
        }
        
        // Calculate timing score (based on average duration)
        val avgDuration = getAverageDuration() ?: 0L
        val timingScore = when {
            avgDuration <= 300 -> 100
            avgDuration <= 500 -> 85
            avgDuration <= 800 -> 70
            avgDuration <= 1000 -> 55
            avgDuration <= 1500 -> 40
            else -> 25
        }
        
        // Calculate frame rate score (placeholder - would need actual frame data)
        val frameRateScore = 90 // Default good score
        
        // Calculate memory score (placeholder - would need actual memory data)
        val memoryScore = 85 // Default good score
        
        // Calculate overall score
        val overallScore = (timingScore + frameRateScore + memoryScore) / 3
        
        // Determine rating
        val rating = when {
            overallScore >= 90 -> PerformanceRating.EXCELLENT
            overallScore >= 75 -> PerformanceRating.GOOD
            overallScore >= 60 -> PerformanceRating.FAIR
            overallScore >= 40 -> PerformanceRating.POOR
            else -> PerformanceRating.CRITICAL
        }
        
        _performanceScore.value = PerformanceScore(
            overallScore = overallScore,
            timingScore = timingScore,
            frameRateScore = frameRateScore,
            memoryScore = memoryScore,
            rating = rating
        )
    }
}
