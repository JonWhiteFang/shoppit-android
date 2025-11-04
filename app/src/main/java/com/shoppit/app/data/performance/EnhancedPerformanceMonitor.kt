package com.shoppit.app.data.performance

/**
 * Enhanced performance monitor interface that extends base PerformanceMonitor
 * with additional metrics for UI rendering, navigation, and memory monitoring.
 * 
 * This interface provides comprehensive performance tracking including:
 * - Frame time tracking for UI rendering performance
 * - Navigation transition monitoring
 * - Memory usage tracking
 * - Database query performance
 * - Cache effectiveness metrics
 * 
 * Requirements: 10.1, 10.2, 10.3
 */
interface EnhancedPerformanceMonitor : PerformanceMonitor {
    
    /**
     * Gets a comprehensive performance report including all metrics.
     * 
     * @return Enhanced performance summary with all tracked metrics
     * 
     * Requirements: 10.4, 10.5
     */
    fun getEnhancedSummary(): EnhancedPerformanceSummary
    
    /**
     * Exports performance metrics in a structured format.
     * 
     * @return Map of metric categories to their values
     * 
     * Requirements: 10.5
     */
    fun exportMetrics(): Map<String, Any>
}

/**
 * Enhanced performance summary including all tracked metrics.
 *
 * @property baseSummary Base performance summary (queries, transactions, cache)
 * @property frameStats Frame drop statistics
 * @property navigationStats Navigation performance statistics
 * @property memoryStats Memory usage statistics
 * @property slowFrameCount Number of frames exceeding 16.67ms
 * @property slowNavigationCount Number of navigations exceeding 100ms
 * 
 * Requirements: 10.4, 10.5
 */
data class EnhancedPerformanceSummary(
    val baseSummary: PerformanceSummary,
    val frameStats: FrameDropStats?,
    val navigationStats: NavigationStats,
    val memoryStats: MemoryStats?,
    val slowFrameCount: Int,
    val slowNavigationCount: Int
)

/**
 * Navigation performance statistics.
 *
 * @property totalNavigations Total number of navigation transitions
 * @property avgNavigationTime Average navigation time in milliseconds
 * @property slowNavigationCount Number of navigations exceeding threshold
 * 
 * Requirements: 9.1, 9.4, 10.2
 */
data class NavigationStats(
    val totalNavigations: Int,
    val avgNavigationTime: Long,
    val slowNavigationCount: Int
)

/**
 * Memory usage statistics.
 *
 * @property currentUsageMB Current memory usage in megabytes
 * @property maxUsageMB Maximum memory available in megabytes
 * @property availableMemoryMB Available memory in megabytes
 * @property pressureEvents Number of memory pressure events
 * @property usagePercentage Memory usage as percentage (0.0 to 1.0)
 * 
 * Requirements: 4.5, 10.3
 */
data class MemoryStats(
    val currentUsageMB: Long,
    val maxUsageMB: Long,
    val availableMemoryMB: Long,
    val pressureEvents: Int,
    val usagePercentage: Double
)
