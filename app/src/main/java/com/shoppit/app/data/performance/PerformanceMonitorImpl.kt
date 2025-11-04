package com.shoppit.app.data.performance

import com.shoppit.app.data.memory.MemoryMetrics
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
        private const val SLOW_NAVIGATION_THRESHOLD = 100L // milliseconds (Requirement 9.1)
        private const val TARGET_NAVIGATION_TIME = 100L // milliseconds (Requirement 9.1)
        private const val TARGET_FRAME_TIME = 16.67 // milliseconds (60 FPS target, Requirement 2.5)
        private const val MAX_FRAME_HISTORY = 1000 // Keep last 1000 frames
    }
    
    // Query tracking
    private val queryMetrics = ConcurrentHashMap<String, MutableQueryMetrics>()
    private val transactionMetrics = ConcurrentHashMap<String, MutableQueryMetrics>()
    
    // Navigation tracking (Requirement 9.4, 10.2)
    private val navigationMetrics = ConcurrentHashMap<String, MutableNavigationMetrics>()
    
    // Frame tracking (Requirement 2.5, 10.1, 10.4)
    private val frameMetrics = ConcurrentHashMap<String, MutableFrameMetrics>()
    private val frameHistory = mutableListOf<FrameMetrics>()
    private val frameHistoryLock = Any()
    
    // Cache tracking
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    
    // Memory tracking
    private var latestMemoryMetrics: MemoryMetrics? = null
    private val memoryPressureEvents = AtomicInteger(0)
    private val memoryUsageThresholdMB = 150L // 150MB threshold from requirements
    
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
        navigationMetrics.clear()
        frameMetrics.clear()
        synchronized(frameHistoryLock) {
            frameHistory.clear()
        }
        cacheHits.set(0)
        cacheMisses.set(0)
        latestMemoryMetrics = null
        memoryPressureEvents.set(0)
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
    
    override fun trackMemoryUsage(usedMemory: Long, availableMemory: Long) {
        val maxMemory = Runtime.getRuntime().maxMemory()
        
        latestMemoryMetrics = MemoryMetrics(
            currentUsage = usedMemory,
            maxUsage = maxMemory,
            availableMemory = availableMemory,
            pressureEvents = memoryPressureEvents.get()
        )
        
        // Log if memory usage exceeds threshold (150MB)
        val usedMB = usedMemory / (1024 * 1024)
        if (usedMB > memoryUsageThresholdMB) {
            Timber.w("Memory usage exceeds threshold: ${usedMB}MB (threshold: ${memoryUsageThresholdMB}MB)")
            val metrics = latestMemoryMetrics?.toMegabytes()
            Timber.w("Memory metrics: $metrics")
        }
    }
    
    override fun getMemoryMetrics(): MemoryMetrics? {
        return latestMemoryMetrics
    }
    
    override fun trackMemoryPressureEvent() {
        val count = memoryPressureEvents.incrementAndGet()
        Timber.d("Memory pressure event tracked (total: $count)")
        
        // Update latest metrics if available
        latestMemoryMetrics?.let { metrics ->
            latestMemoryMetrics = metrics.copy(pressureEvents = count)
        }
    }
    
    override fun trackNavigation(from: String, to: String, duration: Long) {
        val key = "$from->$to"
        val metrics = navigationMetrics.getOrPut(key) { MutableNavigationMetrics(from, to) }
        metrics.addTransition(duration)
        
        // Log slow navigations (Requirement 9.1)
        if (duration > TARGET_NAVIGATION_TIME) {
            Timber.w("Slow navigation detected: $from -> $to (${duration}ms, target: ${TARGET_NAVIGATION_TIME}ms)")
        } else {
            Timber.d("Navigation completed: $from -> $to (${duration}ms)")
        }
    }
    
    override fun getNavigationMetrics(): List<com.shoppit.app.data.performance.NavigationMetrics> {
        return navigationMetrics.values
            .map { it.toNavigationMetrics() }
            .sortedByDescending { it.transitionCount }
    }
    
    override fun getSlowNavigations(threshold: Long): List<com.shoppit.app.data.performance.NavigationMetrics> {
        return navigationMetrics.values
            .filter { it.avgDuration >= threshold }
            .map { it.toNavigationMetrics() }
            .sortedByDescending { it.avgDuration }
    }
    
    override fun trackFrameTime(screenName: String, frameTime: Long) {
        val metrics = frameMetrics.getOrPut(screenName) { MutableFrameMetrics(screenName) }
        metrics.addFrame(frameTime)
        
        val isDropped = frameTime > TARGET_FRAME_TIME
        
        // Add to frame history
        synchronized(frameHistoryLock) {
            frameHistory.add(
                FrameMetrics(
                    screenName = screenName,
                    frameTime = frameTime,
                    timestamp = System.currentTimeMillis(),
                    isDropped = isDropped
                )
            )
            
            // Keep only last MAX_FRAME_HISTORY frames
            if (frameHistory.size > MAX_FRAME_HISTORY) {
                frameHistory.removeAt(0)
            }
        }
        
        // Log slow frames (Requirement 2.5)
        if (isDropped) {
            Timber.w("Slow frame detected on $screenName: ${frameTime}ms (target: ${TARGET_FRAME_TIME}ms)")
        }
    }
    
    override fun getFrameDropStats(screenName: String?): FrameDropStats {
        val relevantMetrics = if (screenName != null) {
            frameMetrics[screenName]?.let { listOf(it) } ?: emptyList()
        } else {
            frameMetrics.values.toList()
        }
        
        if (relevantMetrics.isEmpty()) {
            return FrameDropStats(
                totalFrames = 0,
                droppedFrames = 0,
                avgFrameTime = 0.0,
                maxFrameTime = 0,
                frameDropRate = 0.0
            )
        }
        
        val totalFrames = relevantMetrics.sumOf { it.frameCount }
        val droppedFrames = relevantMetrics.sumOf { it.droppedFrameCount }
        val avgFrameTime = relevantMetrics.map { it.avgFrameTime }.average()
        val maxFrameTime = relevantMetrics.maxOf { it.maxFrameTime }
        val frameDropRate = if (totalFrames > 0) droppedFrames.toDouble() / totalFrames else 0.0
        
        return FrameDropStats(
            totalFrames = totalFrames,
            droppedFrames = droppedFrames,
            avgFrameTime = avgFrameTime,
            maxFrameTime = maxFrameTime,
            frameDropRate = frameDropRate
        )
    }
    
    override fun getSlowFrames(threshold: Double): List<FrameMetrics> {
        return synchronized(frameHistoryLock) {
            frameHistory.filter { it.frameTime > threshold }.toList()
        }
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
    
    /**
     * Mutable metrics for tracking navigation performance.
     * Thread-safe using atomic operations.
     * 
     * Requirements: 9.1, 9.4, 10.2
     */
    private class MutableNavigationMetrics(val from: String, val to: String) {
        private val totalDuration = AtomicLong(0)
        private val _transitionCount = AtomicInteger(0)
        private val _lastTransition = AtomicLong(0)
        private val _minDuration = AtomicLong(Long.MAX_VALUE)
        private val _maxDuration = AtomicLong(0)
        
        val transitionCount: Int get() = _transitionCount.get()
        val avgDuration: Long get() {
            val count = transitionCount
            return if (count > 0) totalDuration.get() / count else 0
        }
        
        fun addTransition(duration: Long) {
            totalDuration.addAndGet(duration)
            _transitionCount.incrementAndGet()
            _lastTransition.set(System.currentTimeMillis())
            
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
        
        fun toNavigationMetrics(): com.shoppit.app.data.performance.NavigationMetrics {
            return com.shoppit.app.data.performance.NavigationMetrics(
                from = from,
                to = to,
                avgDuration = avgDuration,
                transitionCount = transitionCount,
                lastTransition = _lastTransition.get(),
                minDuration = _minDuration.get().let { if (it == Long.MAX_VALUE) 0 else it },
                maxDuration = _maxDuration.get()
            )
        }
    }
    
    /**
     * Mutable metrics for tracking frame rendering performance.
     * Thread-safe using atomic operations.
     * 
     * Requirements: 2.5, 10.1, 10.4
     */
    private class MutableFrameMetrics(val screenName: String) {
        private val totalFrameTime = AtomicLong(0)
        private val _frameCount = AtomicInteger(0)
        private val _droppedFrameCount = AtomicInteger(0)
        private val _maxFrameTime = AtomicLong(0)
        
        val frameCount: Int get() = _frameCount.get()
        val droppedFrameCount: Int get() = _droppedFrameCount.get()
        val avgFrameTime: Double get() {
            val count = frameCount
            return if (count > 0) totalFrameTime.get().toDouble() / count else 0.0
        }
        val maxFrameTime: Long get() = _maxFrameTime.get()
        
        fun addFrame(frameTime: Long) {
            totalFrameTime.addAndGet(frameTime)
            _frameCount.incrementAndGet()
            
            // Track dropped frames (exceeding 16.67ms for 60 FPS)
            if (frameTime > TARGET_FRAME_TIME) {
                _droppedFrameCount.incrementAndGet()
            }
            
            // Update max
            var currentMax = _maxFrameTime.get()
            while (frameTime > currentMax) {
                if (_maxFrameTime.compareAndSet(currentMax, frameTime)) break
                currentMax = _maxFrameTime.get()
            }
        }
    }
}
