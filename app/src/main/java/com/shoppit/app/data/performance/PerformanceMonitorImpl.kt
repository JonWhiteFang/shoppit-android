package com.shoppit.app.data.performance

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PerformanceMonitor that tracks database and cache performance metrics.
 * Thread-safe implementation using concurrent data structures.
 */
@Singleton
class PerformanceMonitorImpl @Inject constructor() : PerformanceMonitor {
    
    companion object {
        private const val SLOW_QUERY_THRESHOLD = 100L // milliseconds
        private const val WARNING_QUERY_THRESHOLD = 50L // milliseconds
    }
    
    // Query tracking
    private val queryMetrics = ConcurrentHashMap<String, MutableQueryMetrics>()
    private val transactionMetrics = ConcurrentHashMap<String, MutableQueryMetrics>()
    
    // Cache tracking
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    override fun trackQuery(query: String, duration: Long) {
        val metrics = queryMetrics.getOrPut(query) { MutableQueryMetrics(query) }
        metrics.addExecution(duration)
        
        // Log slow queries
        when {
            duration >= SLOW_QUERY_THRESHOLD -> {
                Timber.w("Slow query detected: $query (${duration}ms)")
            }
            duration >= WARNING_QUERY_THRESHOLD -> {
                Timber.d("Query took ${duration}ms: $query")
            }
        }
    }
    
    override fun trackTransaction(operation: String, duration: Long) {
        val metrics = transactionMetrics.getOrPut(operation) { MutableQueryMetrics(operation) }
        metrics.addExecution(duration)
        
        // Log slow transactions
        if (duration >= SLOW_QUERY_THRESHOLD) {
            Timber.w("Slow transaction detected: $operation (${duration}ms)")
        }
    }
    
    override fun getSlowQueries(threshold: Long): List<QueryMetrics> {
        return queryMetrics.values
            .filter { it.avgDuration >= threshold }
            .map { it.toQueryMetrics() }
            .sortedByDescending { it.avgDuration }
    }
    
    override fun getCacheHitRate(): Double {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val total = hits + misses
        
        return if (total > 0) {
            hits.toDouble() / total.toDouble()
        } else {
            0.0
        }
    }
    
    override fun trackCacheHit() {
        cacheHits.incrementAndGet()
    }
    
    override fun trackCacheMiss() {
        cacheMisses.incrementAndGet()
    }
    
    override fun reset() {
        queryMetrics.clear()
        transactionMetrics.clear()
        cacheHits.set(0)
        cacheMisses.set(0)
        Timber.d("Performance metrics reset")
    }
    
    override fun getSummary(): PerformanceSummary {
        val totalQueries = queryMetrics.values.sumOf { it.executionCount }
        val totalTransactions = transactionMetrics.values.sumOf { it.executionCount }
        
        val avgQueryDuration = if (queryMetrics.isNotEmpty()) {
            queryMetrics.values.map { it.avgDuration }.average().toLong()
        } else {
            0L
        }
        
        val avgTransactionDuration = if (transactionMetrics.isNotEmpty()) {
            transactionMetrics.values.map { it.avgDuration }.average().toLong()
        } else {
            0L
        }
        
        val slowQueryCount = queryMetrics.values.count { it.avgDuration >= SLOW_QUERY_THRESHOLD }
        
        return PerformanceSummary(
            totalQueries = totalQueries,
            totalTransactions = totalTransactions,
            avgQueryDuration = avgQueryDuration,
            avgTransactionDuration = avgTransactionDuration,
            cacheHitRate = getCacheHitRate(),
            slowQueryCount = slowQueryCount
        )
    }
    
    /**
     * Mutable metrics for tracking query/transaction performance.
     * Thread-safe using atomic operations.
     */
    private class MutableQueryMetrics(val query: String) {
        private val totalDuration = AtomicLong(0)
        private val _executionCount = AtomicInteger(0)
        private val _lastExecuted = AtomicLong(0)
        private val _minDuration = AtomicLong(Long.MAX_VALUE)
        private val _maxDuration = AtomicLong(0)
        
        val executionCount: Int get() = _executionCount.get()
        val avgDuration: Long get() {
            val count = executionCount
            return if (count > 0) totalDuration.get() / count else 0
        }
        
        fun addExecution(duration: Long) {
            totalDuration.addAndGet(duration)
            _executionCount.incrementAndGet()
            _lastExecuted.set(System.currentTimeMillis())
            
            // Update min/max
            var currentMin = _minDuration.get()
            while (duration < currentMin) {
                if (_minDuration.compareAndSet(currentMin, duration)) break
                currentMin = _minDuration.get()
            }
            
            var currentMax = _maxDuration.get()
            while (duration > currentMax) {
                if (_maxDuration.compareAndSet(currentMax, duration)) break
                currentMax = _maxDuration.get()
            }
        }
        
        fun toQueryMetrics(): QueryMetrics {
            return QueryMetrics(
                query = query,
                avgDuration = avgDuration,
                executionCount = executionCount,
                lastExecuted = _lastExecuted.get(),
                minDuration = _minDuration.get().let { if (it == Long.MAX_VALUE) 0 else it },
                maxDuration = _maxDuration.get()
            )
        }
    }
}
