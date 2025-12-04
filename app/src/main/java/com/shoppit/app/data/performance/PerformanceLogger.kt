package com.shoppit.app.data.performance

import com.shoppit.app.BuildConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structured logger for performance metrics.
 * Provides tagged logging with debug-only detailed logging.
 * 
 * Requirements: 10.3
 */
@Singleton
class PerformanceLogger @Inject constructor() {
    
    companion object {
        private const val TAG_PERFORMANCE = "Performance"
        private const val TAG_QUERY = "Performance:Query"
        private const val TAG_TRANSACTION = "Performance:Transaction"
        private const val TAG_CACHE = "Performance:Cache"
        private const val TAG_MEMORY = "Performance:Memory"
        private const val TAG_NAVIGATION = "Performance:Navigation"
        private const val TAG_FRAME = "Performance:Frame"
        private const val TAG_SUMMARY = "Performance:Summary"
    }
    
    private fun sanitize(value: Any?): String = value.toString()
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\u001B", "")  // Remove ANSI escape sequences
        .replace("\u0000", "")  // Remove null bytes
        .filter { it.code >= 32 || it in "\n\r\t" }  // Remove control characters except safe ones
    
    /**
     * Logs a query execution with structured data.
     */
    fun logQuery(query: String, duration: Long, threshold: Long = 100) {
        val sanitizedQuery = sanitize(query)
        if (duration >= threshold) {
            Timber.tag(TAG_QUERY).w("Slow query: $sanitizedQuery | Duration: ${duration}ms | Threshold: ${threshold}ms")
        } else if (BuildConfig.DEBUG) {
            Timber.tag(TAG_QUERY).d("Query: $sanitizedQuery | Duration: ${duration}ms")
        }
    }
    
    /**
     * Logs a transaction execution with structured data.
     */
    fun logTransaction(operation: String, duration: Long, threshold: Long = 100) {
        val sanitizedOperation = sanitize(operation)
        if (duration >= threshold) {
            Timber.tag(TAG_TRANSACTION).w("Slow transaction: $sanitizedOperation | Duration: ${duration}ms | Threshold: ${threshold}ms")
        } else if (BuildConfig.DEBUG) {
            Timber.tag(TAG_TRANSACTION).d("Transaction: $sanitizedOperation | Duration: ${duration}ms")
        }
    }
    
    /**
     * Logs cache hit/miss events.
     */
    fun logCacheHit(key: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG_CACHE).d("Cache hit: ${sanitize(key)}")
        }
    }
    
    fun logCacheMiss(key: String) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG_CACHE).d("Cache miss: ${sanitize(key)}")
        }
    }
    
    /**
     * Logs cache statistics.
     */
    fun logCacheStats(hitRate: Double, hits: Int, misses: Int) {
        val hitRatePercent = (hitRate * 100).toInt()
        Timber.tag(TAG_CACHE).i("Cache stats | Hit rate: $hitRatePercent% | Hits: $hits | Misses: $misses")
    }
    
    /**
     * Logs memory usage with structured data.
     */
    fun logMemoryUsage(usedMB: Long, maxMB: Long, availableMB: Long, threshold: Long = 150) {
        if (usedMB > threshold) {
            Timber.tag(TAG_MEMORY).w("Memory usage exceeds threshold | Used: ${usedMB}MB | Max: ${maxMB}MB | Available: ${availableMB}MB | Threshold: ${threshold}MB")
        } else if (BuildConfig.DEBUG) {
            Timber.tag(TAG_MEMORY).d("Memory usage | Used: ${usedMB}MB | Max: ${maxMB}MB | Available: ${availableMB}MB")
        }
    }
    
    /**
     * Logs memory pressure events.
     */
    fun logMemoryPressure(level: String, usedMB: Long, availableMB: Long) {
        Timber.tag(TAG_MEMORY).w("Memory pressure: ${sanitize(level)} | Used: ${usedMB}MB | Available: ${availableMB}MB")
    }
    
    /**
     * Logs navigation transitions with structured data.
     */
    fun logNavigation(from: String, to: String, duration: Long, threshold: Long = 100) {
        val sanitizedFrom = sanitize(from)
        val sanitizedTo = sanitize(to)
        if (duration > threshold) {
            Timber.tag(TAG_NAVIGATION).w("Slow navigation: $sanitizedFrom -> $sanitizedTo | Duration: ${duration}ms | Threshold: ${threshold}ms")
        } else if (BuildConfig.DEBUG) {
            Timber.tag(TAG_NAVIGATION).d("Navigation: $sanitizedFrom -> $sanitizedTo | Duration: ${duration}ms")
        }
    }
    
    /**
     * Logs frame rendering with structured data.
     */
    fun logFrame(screenName: String, frameTime: Long, threshold: Double = 16.67) {
        val sanitizedScreenName = sanitize(screenName)
        if (frameTime > threshold) {
            Timber.tag(TAG_FRAME).w("Slow frame: $sanitizedScreenName | Frame time: ${frameTime}ms | Threshold: ${threshold}ms | Target: 60 FPS")
        } else if (BuildConfig.DEBUG) {
            Timber.tag(TAG_FRAME).v("Frame: $sanitizedScreenName | Frame time: ${frameTime}ms")
        }
    }
    
    /**
     * Logs frame drop statistics.
     */
    fun logFrameDropStats(stats: FrameDropStats) {
        val dropRatePercent = (stats.frameDropRate * 100).toInt()
        val avgFrameTimeFormatted = "%.2f".format(stats.avgFrameTime)
        Timber.tag(TAG_FRAME).i(
            "Frame stats | Total: ${stats.totalFrames} | Dropped: ${stats.droppedFrames} | " +
            "Drop rate: $dropRatePercent% | Avg: ${avgFrameTimeFormatted}ms | " +
            "Max: ${stats.maxFrameTime}ms"
        )
    }
    
    /**
     * Logs a comprehensive performance summary.
     */
    fun logSummary(summary: EnhancedPerformanceSummary) {
        Timber.tag(TAG_SUMMARY).i("=== Performance Summary ===")
        
        // Database metrics
        Timber.tag(TAG_SUMMARY).i(
            "Database | Queries: ${summary.baseSummary.totalQueries} | " +
            "Avg: ${summary.baseSummary.avgQueryDuration}ms | " +
            "Slow: ${summary.baseSummary.slowQueryCount}"
        )
        
        Timber.tag(TAG_SUMMARY).i(
            "Transactions | Total: ${summary.baseSummary.totalTransactions} | " +
            "Avg: ${summary.baseSummary.avgTransactionDuration}ms"
        )
        
        // Cache metrics
        val cacheHitPercent = (summary.baseSummary.cacheHitRate * 100).toInt()
        Timber.tag(TAG_SUMMARY).i("Cache | Hit rate: $cacheHitPercent%")
        
        // Frame metrics
        summary.frameStats?.let { frameStats ->
            val dropRatePercent = (frameStats.frameDropRate * 100).toInt()
            val avgFrameTimeFormatted = "%.2f".format(frameStats.avgFrameTime)
            Timber.tag(TAG_SUMMARY).i(
                "Frames | Total: ${frameStats.totalFrames} | Dropped: ${frameStats.droppedFrames} | " +
                "Drop rate: $dropRatePercent% | Avg: ${avgFrameTimeFormatted}ms"
            )
        }
        
        // Navigation metrics
        Timber.tag(TAG_SUMMARY).i(
            "Navigation | Total: ${summary.navigationStats.totalNavigations} | " +
            "Avg: ${summary.navigationStats.avgNavigationTime}ms | " +
            "Slow: ${summary.navigationStats.slowNavigationCount}"
        )
        
        // Memory metrics
        summary.memoryStats?.let { memoryStats ->
            val usagePercent = (memoryStats.usagePercentage * 100).toInt()
            Timber.tag(TAG_SUMMARY).i(
                "Memory | Used: ${memoryStats.currentUsageMB}MB | " +
                "Max: ${memoryStats.maxUsageMB}MB | " +
                "Usage: $usagePercent% | " +
                "Pressure events: ${memoryStats.pressureEvents}"
            )
        }
        
        Timber.tag(TAG_SUMMARY).i("=========================")
    }
    
    /**
     * Logs slow queries with details.
     */
    fun logSlowQueries(queries: List<QueryMetrics>, threshold: Long = 100) {
        if (queries.isEmpty()) {
            Timber.tag(TAG_QUERY).i("No slow queries detected (threshold: ${threshold}ms)")
            return
        }
        
        Timber.tag(TAG_QUERY).w("=== Slow Queries (${queries.size}) ===")
        queries.take(10).forEach { query ->
            Timber.tag(TAG_QUERY).w(
                "Query: ${sanitize(query.query)} | Avg: ${query.avgDuration}ms | " +
                "Count: ${query.executionCount} | Max: ${query.maxDuration}ms"
            )
        }
    }
    
    /**
     * Logs slow navigations with details.
     */
    fun logSlowNavigations(navigations: List<NavigationMetrics>, threshold: Long = 100) {
        if (navigations.isEmpty()) {
            Timber.tag(TAG_NAVIGATION).i("No slow navigations detected (threshold: ${threshold}ms)")
            return
        }
        
        Timber.tag(TAG_NAVIGATION).w("=== Slow Navigations (${navigations.size}) ===")
        navigations.forEach { nav ->
            Timber.tag(TAG_NAVIGATION).w(
                "Navigation: ${sanitize(nav.from)} -> ${sanitize(nav.to)} | Avg: ${nav.avgDuration}ms | " +
                "Count: ${nav.transitionCount} | Max: ${nav.maxDuration}ms"
            )
        }
    }
    
    /**
     * Logs slow frames with details.
     */
    fun logSlowFrames(frames: List<FrameMetrics>, threshold: Double = 16.67) {
        if (frames.isEmpty()) {
            Timber.tag(TAG_FRAME).i("No slow frames detected (threshold: ${threshold}ms)")
            return
        }
        
        Timber.tag(TAG_FRAME).w("=== Slow Frames (${frames.size}) ===")
        frames.take(20).forEach { frame ->
            Timber.tag(TAG_FRAME).w(
                "Frame: ${sanitize(frame.screenName)} | Time: ${frame.frameTime}ms | " +
                "Timestamp: ${frame.timestamp}"
            )
        }
    }
    
    /**
     * Logs performance metrics export.
     */
    fun logMetricsExport(metrics: Map<String, Any>) {
        if (BuildConfig.DEBUG) {
            Timber.tag(TAG_PERFORMANCE).d("=== Exported Metrics ===")
            metrics.forEach { (category, data) ->
                Timber.tag(TAG_PERFORMANCE).d("${sanitize(category)}: ${sanitize(data)}")
            }
            Timber.tag(TAG_PERFORMANCE).d("=======================")
        }
    }
}
