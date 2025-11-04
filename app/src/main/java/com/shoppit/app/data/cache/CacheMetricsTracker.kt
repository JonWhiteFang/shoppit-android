package com.shoppit.app.data.cache

import com.shoppit.app.data.performance.PerformanceMonitor
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks cache effectiveness metrics including hit rate, miss rate, and eviction statistics.
 * 
 * Integrates with PerformanceMonitor to provide comprehensive cache performance data.
 * Thread-safe implementation using atomic counters.
 * 
 * Requirements: 8.3, 8.4
 */
@Singleton
class CacheMetricsTracker @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) {
    
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    private val cacheEvictions = AtomicLong(0)
    private val cacheWrites = AtomicLong(0)
    
    /**
     * Records a cache hit event.
     * 
     * @param cacheKey The cache key that was hit
     */
    fun recordHit(cacheKey: String) {
        cacheHits.incrementAndGet()
        performanceMonitor.trackCacheHit()
        Timber.v("Cache hit: $cacheKey (total hits: ${cacheHits.get()})")
    }
    
    /**
     * Records a cache miss event.
     * 
     * @param cacheKey The cache key that was missed
     */
    fun recordMiss(cacheKey: String) {
        cacheMisses.incrementAndGet()
        performanceMonitor.trackCacheMiss()
        Timber.v("Cache miss: $cacheKey (total misses: ${cacheMisses.get()})")
    }
    
    /**
     * Records a cache eviction event.
     * 
     * @param cacheKey The cache key that was evicted
     * @param reason The reason for eviction (e.g., "LRU", "TTL", "memory_pressure")
     */
    fun recordEviction(cacheKey: String, reason: String = "unknown") {
        cacheEvictions.incrementAndGet()
        Timber.v("Cache eviction: $cacheKey (reason: $reason, total evictions: ${cacheEvictions.get()})")
    }
    
    /**
     * Records a cache write event.
     * 
     * @param cacheKey The cache key that was written
     */
    fun recordWrite(cacheKey: String) {
        cacheWrites.incrementAndGet()
        Timber.v("Cache write: $cacheKey (total writes: ${cacheWrites.get()})")
    }
    
    /**
     * Calculates the current cache hit rate.
     * 
     * @return Cache hit rate as a percentage (0.0 to 1.0)
     */
    fun getHitRate(): Double {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        
        return if (total > 0) {
            hits.toDouble() / total
        } else {
            0.0
        }
    }
    
    /**
     * Gets comprehensive cache effectiveness metrics.
     * 
     * @return CacheEffectivenessReport with all metrics
     */
    fun getEffectivenessReport(): CacheEffectivenessReport {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val evictions = cacheEvictions.get()
        val writes = cacheWrites.get()
        val total = hits + misses
        
        val hitRate = if (total > 0) hits.toDouble() / total else 0.0
        val missRate = if (total > 0) misses.toDouble() / total else 0.0
        
        return CacheEffectivenessReport(
            totalHits = hits,
            totalMisses = misses,
            totalEvictions = evictions,
            totalWrites = writes,
            hitRate = hitRate,
            missRate = missRate,
            totalAccesses = total
        )
    }
    
    /**
     * Logs a summary of cache effectiveness metrics.
     */
    fun logSummary() {
        val report = getEffectivenessReport()
        Timber.i("""
            Cache Effectiveness Summary:
            - Total Accesses: ${report.totalAccesses}
            - Hits: ${report.totalHits} (${String.format("%.2f%%", report.hitRate * 100)})
            - Misses: ${report.totalMisses} (${String.format("%.2f%%", report.missRate * 100)})
            - Evictions: ${report.totalEvictions}
            - Writes: ${report.totalWrites}
        """.trimIndent())
    }
    
    /**
     * Resets all cache metrics counters.
     */
    fun reset() {
        cacheHits.set(0)
        cacheMisses.set(0)
        cacheEvictions.set(0)
        cacheWrites.set(0)
        Timber.d("Cache metrics reset")
    }
}

/**
 * Comprehensive report of cache effectiveness metrics.
 * 
 * @property totalHits Total number of cache hits
 * @property totalMisses Total number of cache misses
 * @property totalEvictions Total number of cache evictions
 * @property totalWrites Total number of cache writes
 * @property hitRate Cache hit rate (0.0 to 1.0)
 * @property missRate Cache miss rate (0.0 to 1.0)
 * @property totalAccesses Total number of cache accesses (hits + misses)
 */
data class CacheEffectivenessReport(
    val totalHits: Long,
    val totalMisses: Long,
    val totalEvictions: Long,
    val totalWrites: Long,
    val hitRate: Double,
    val missRate: Double,
    val totalAccesses: Long
) {
    /**
     * Checks if cache hit rate meets the target threshold.
     * 
     * @param threshold Target hit rate (default: 0.8 for 80%)
     * @return True if hit rate meets or exceeds threshold
     */
    fun meetsTargetHitRate(threshold: Double = 0.8): Boolean {
        return hitRate >= threshold
    }
    
    /**
     * Gets a human-readable summary of the report.
     */
    fun getSummary(): String {
        return """
            Cache Effectiveness:
            - Hit Rate: ${String.format("%.2f%%", hitRate * 100)}
            - Miss Rate: ${String.format("%.2f%%", missRate * 100)}
            - Total Accesses: $totalAccesses
            - Evictions: $totalEvictions
            - Target Met: ${if (meetsTargetHitRate()) "Yes" else "No"}
        """.trimIndent()
    }
}
