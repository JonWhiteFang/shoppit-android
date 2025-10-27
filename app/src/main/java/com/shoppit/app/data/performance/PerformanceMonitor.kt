package com.shoppit.app.data.performance

/**
 * Interface for monitoring database and cache performance.
 * Tracks query execution times, transaction durations, and cache hit rates.
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
