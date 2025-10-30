package com.shoppit.app.presentation.ui.navigation.util

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

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
        
        Timber.d("Started performance monitoring for route: $route")
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
            Timber.w("No start time found for route: $route")
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
        
        Timber.d("Navigation performance for '$route': ${duration}ms, success=$success")
        
        // Log warning if navigation took too long
        if (duration > 1000) {
            Timber.w("Slow navigation detected: $route took ${duration}ms")
        }
        
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
            - Slowest navigation: ${slowest?.route} (${slowest?.duration}ms)
            - Success rate: ${"%.2f".format(successRate)}%
        """.trimIndent())
    }
}
