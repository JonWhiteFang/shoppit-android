package com.shoppit.app.data.performance

import com.shoppit.app.data.memory.MemoryMetrics

/**
 * Interface for monitoring database and cache performance.
 * Tracks query execution times, transaction durations, and cache hit rates.
 * 
 * Requirements: 4.5, 10.3
 */
interface PerformanceMonitor {
    /**
     * Tracks a database query execution.
     *
     * @param query The SQL query or operation name
     * @param duration The execution time in milliseconds
     */
    fun trackQuery(query: String, duration: Long)
    
    /**
     * Tracks a database transaction execution.
     *
     * @param operation The transaction operation name
     * @param duration The execution time in milliseconds
     */
    fun trackTransaction(operation: String, duration: Long)
    
    /**
     * Retrieves queries that exceed the specified threshold.
     *
     * @param threshold The minimum duration in milliseconds (default: 100ms)
     * @return List of slow query metrics
     */
    fun getSlowQueries(threshold: Long = 100): List<QueryMetrics>
    
    /**
     * Calculates the cache hit rate.
     *
     * @return Cache hit rate as a percentage (0.0 to 1.0)
     */
    fun getCacheHitRate(): Double
    
    /**
     * Tracks a cache hit event.
     */
    fun trackCacheHit()
    
    /**
     * Tracks a cache miss event.
     */
    fun trackCacheMiss()
    
    /**
     * Resets all performance metrics.
     */
    fun reset()
    
    /**
     * Gets a summary of all performance metrics.
     *
     * @return Performance summary
     */
    fun getSummary(): PerformanceSummary
    
    /**
     * Tracks memory usage snapshot.
     * 
     * @param usedMemory Current memory usage in bytes
     * @param availableMemory Available memory in bytes
     * 
     * Requirements: 4.5, 10.3
     */
    fun trackMemoryUsage(usedMemory: Long, availableMemory: Long)
    
    /**
     * Gets the latest memory metrics.
     * 
     * @return Latest memory metrics, or null if no metrics have been tracked
     * 
     * Requirements: 4.5, 10.3
     */
    fun getMemoryMetrics(): MemoryMetrics?
    
    /**
     * Tracks a memory pressure event.
     * 
     * Requirements: 4.5
     */
    fun trackMemoryPressureEvent()
    
    /**
     * Tracks a navigation transition.
     * 
     * @param from The source route
     * @param to The destination route
     * @param duration The transition duration in milliseconds
     * 
     * Requirements: 9.1, 9.4, 10.2
     */
    fun trackNavigation(from: String, to: String, duration: Long)
    
    /**
     * Gets navigation metrics.
     * 
     * @return List of navigation metrics
     * 
     * Requirements: 9.4, 10.2
     */
    fun getNavigationMetrics(): List<NavigationMetrics>
    
    /**
     * Gets slow navigation transitions exceeding threshold.
     * 
     * @param threshold The minimum duration in milliseconds (default: 100ms)
     * @return List of slow navigation metrics
     * 
     * Requirements: 9.1, 10.2
     */
    fun getSlowNavigations(threshold: Long = 100): List<NavigationMetrics>
    
    /**
     * Tracks UI frame rendering time.
     * 
     * @param screenName The name of the screen being rendered
     * @param frameTime The frame rendering duration in milliseconds
     * 
     * Requirements: 2.5, 10.1, 10.4
     */
    fun trackFrameTime(screenName: String, frameTime: Long)
    
    /**
     * Gets frame drop statistics for a specific screen.
     * 
     * @param screenName The name of the screen (null for all screens)
     * @return Frame drop statistics
     * 
     * Requirements: 2.5, 10.4
     */
    fun getFrameDropStats(screenName: String? = null): FrameDropStats
    
    /**
     * Gets slow frames exceeding threshold.
     * 
     * @param threshold The minimum frame time in milliseconds (default: 16.67ms for 60 FPS)
     * @return List of slow frame metrics
     * 
     * Requirements: 2.5, 10.4
     */
    fun getSlowFrames(threshold: Double = 16.67): List<FrameMetrics>
}

/**
 * Metrics for a specific query or operation.
 *
 * @property query The SQL query or operation name
 * @property avgDuration Average execution time in milliseconds
 * @property executionCount Number of times the query was executed
 * @property lastExecuted Timestamp of last execution
 * @property minDuration Minimum execution time in milliseconds
 * @property maxDuration Maximum execution time in milliseconds
 */
data class QueryMetrics(
    val query: String,
    val avgDuration: Long,
    val executionCount: Int,
    val lastExecuted: Long,
    val minDuration: Long,
    val maxDuration: Long
)

/**
 * Summary of all performance metrics.
 *
 * @property totalQueries Total number of queries executed
 * @property totalTransactions Total number of transactions executed
 * @property avgQueryDuration Average query execution time in milliseconds
 * @property avgTransactionDuration Average transaction execution time in milliseconds
 * @property cacheHitRate Cache hit rate as a percentage (0.0 to 1.0)
 * @property slowQueryCount Number of slow queries (> 100ms)
 */
data class PerformanceSummary(
    val totalQueries: Int,
    val totalTransactions: Int,
    val avgQueryDuration: Long,
    val avgTransactionDuration: Long,
    val cacheHitRate: Double,
    val slowQueryCount: Int
)

/**
 * Metrics for navigation transitions.
 *
 * @property from Source route
 * @property to Destination route
 * @property avgDuration Average transition time in milliseconds
 * @property transitionCount Number of times this transition occurred
 * @property lastTransition Timestamp of last transition
 * @property minDuration Minimum transition time in milliseconds
 * @property maxDuration Maximum transition time in milliseconds
 * 
 * Requirements: 9.1, 9.4, 10.2
 */
data class NavigationMetrics(
    val from: String,
    val to: String,
    val avgDuration: Long,
    val transitionCount: Int,
    val lastTransition: Long,
    val minDuration: Long,
    val maxDuration: Long
)

/**
 * Statistics for frame drops and rendering performance.
 *
 * @property totalFrames Total number of frames rendered
 * @property droppedFrames Number of frames that exceeded 16.67ms (60 FPS target)
 * @property avgFrameTime Average frame rendering time in milliseconds
 * @property maxFrameTime Maximum frame rendering time in milliseconds
 * @property frameDropRate Percentage of dropped frames (0.0 to 1.0)
 * 
 * Requirements: 2.5, 10.4
 */
data class FrameDropStats(
    val totalFrames: Int,
    val droppedFrames: Int,
    val avgFrameTime: Double,
    val maxFrameTime: Long,
    val frameDropRate: Double
)

/**
 * Metrics for individual frame rendering.
 *
 * @property screenName Name of the screen being rendered
 * @property frameTime Frame rendering duration in milliseconds
 * @property timestamp When the frame was rendered
 * @property isDropped Whether this frame exceeded the 16.67ms threshold
 * 
 * Requirements: 2.5, 10.4
 */
data class FrameMetrics(
    val screenName: String,
    val frameTime: Long,
    val timestamp: Long,
    val isDropped: Boolean
)
