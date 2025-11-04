package com.shoppit.app.data.cache

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * LRU (Least Recently Used) cache implementation with TTL and access frequency tracking.
 * 
 * This cache manager implements an LRU + TTL eviction policy with configurable size.
 * Eviction prioritizes entries based on:
 * 1. Expired entries (TTL exceeded)
 * 2. Least recently used entries
 * 3. Least frequently accessed entries
 * 
 * Thread-safe implementation using ConcurrentHashMap.
 * 
 * @param K The type of cache keys
 * @param V The type of cached values
 * @property maxSize Maximum number of entries in the cache
 * @property defaultTtl Default time-to-live for cache entries in milliseconds
 * @property onEviction Optional callback invoked when an entry is evicted
 */
class LruCacheManager<K, V>(
    private val maxSize: Int,
    private val defaultTtl: Long,
    private val onEviction: ((K, V) -> Unit)? = null,
    private val metricsTracker: CacheMetricsTracker? = null,
    private val cacheName: String = "unknown"
) : CacheManager<K, V> {
    
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val accessOrder = LinkedHashMap<K, Long>(maxSize, 0.75f, true)
    
    init {
        require(maxSize > 0) { "Cache max size must be positive" }
        require(defaultTtl > 0) { "Cache TTL must be positive" }
        Timber.d("LruCacheManager initialized: maxSize=$maxSize, defaultTtl=${defaultTtl}ms")
    }
    
    override fun get(key: K): V? {
        val entry = cache[key] ?: return null.also {
            Timber.v("Cache miss: $key")
            metricsTracker?.recordMiss("$cacheName:$key")
        }
        
        // Check if entry is expired
        if (entry.isExpired()) {
            Timber.v("Cache entry expired: $key")
            invalidate(key)
            metricsTracker?.recordMiss("$cacheName:$key")
            return null
        }
        
        // Update access order
        synchronized(accessOrder) {
            accessOrder[key] = System.currentTimeMillis()
        }
        
        // Update access count
        cache[key] = entry.incrementAccessCount()
        
        Timber.v("Cache hit: $key (accessCount=${entry.accessCount + 1})")
        metricsTracker?.recordHit("$cacheName:$key")
        return entry.data
    }
    
    override fun put(key: K, value: V) {
        // Check if we need to evict entries
        if (cache.size >= maxSize && !cache.containsKey(key)) {
            evictLeastRecentlyUsed()
        }
        
        val entry = CacheEntry(
            data = value,
            timestamp = System.currentTimeMillis(),
            ttl = defaultTtl,
            accessCount = 0
        )
        
        cache[key] = entry
        synchronized(accessOrder) {
            accessOrder[key] = System.currentTimeMillis()
        }
        
        metricsTracker?.recordWrite("$cacheName:$key")
        Timber.v("Cache put: $key (size=${cache.size})")
    }
    
    override fun invalidate(key: K) {
        val entry = cache.remove(key)
        synchronized(accessOrder) {
            accessOrder.remove(key)
        }
        
        if (entry != null) {
            onEviction?.invoke(key, entry.data)
            Timber.v("Cache invalidated: $key")
        }
    }
    
    override fun invalidateAll() {
        val keys = cache.keys.toList()
        keys.forEach { key ->
            val entry = cache.remove(key)
            if (entry != null) {
                onEviction?.invoke(key, entry.data)
            }
        }
        
        synchronized(accessOrder) {
            accessOrder.clear()
        }
        
        Timber.d("Cache cleared: ${keys.size} entries removed")
    }
    
    override fun size(): Int = cache.size
    
    /**
     * Evicts the least recently used entry from the cache.
     * Prioritizes entries based on:
     * 1. Expired entries (TTL exceeded)
     * 2. Least frequently accessed entries
     * 3. Least recently used entries
     */
    private fun evictLeastRecentlyUsed() {
        // First, try to evict expired entries
        val expiredKey = cache.entries
            .firstOrNull { it.value.isExpired() }
            ?.key
        
        if (expiredKey != null) {
            val entry = cache.remove(expiredKey)
            synchronized(accessOrder) {
                accessOrder.remove(expiredKey)
            }
            
            if (entry != null) {
                onEviction?.invoke(expiredKey, entry.data)
                metricsTracker?.recordEviction("$cacheName:$expiredKey", "TTL")
                Timber.v("Cache evicted (expired): $expiredKey")
            }
            return
        }
        
        // If no expired entries, evict based on access frequency and recency
        val candidateKey = synchronized(accessOrder) {
            // Get entries sorted by access order (oldest first)
            val lruCandidates = accessOrder.keys.take(5)
            
            // Among LRU candidates, find the one with lowest access count
            lruCandidates.minByOrNull { key ->
                cache[key]?.accessCount ?: Int.MAX_VALUE
            }
        }
        
        if (candidateKey != null) {
            val entry = cache.remove(candidateKey)
            synchronized(accessOrder) {
                accessOrder.remove(candidateKey)
            }
            
            if (entry != null) {
                onEviction?.invoke(candidateKey, entry.data)
                metricsTracker?.recordEviction("$cacheName:$candidateKey", "LRU+frequency")
                Timber.v("Cache evicted (LRU+frequency): $candidateKey (accessCount=${entry.accessCount})")
            }
        }
    }
    
    /**
     * Removes all expired entries from the cache.
     * 
     * @return The number of expired entries removed
     */
    fun cleanupExpired(): Int {
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { invalidate(it) }
        
        if (expiredKeys.isNotEmpty()) {
            Timber.d("Cleaned up ${expiredKeys.size} expired cache entries")
        }
        
        return expiredKeys.size
    }
    
    /**
     * Returns cache statistics for monitoring.
     * 
     * @return CacheMetrics containing hit rate and other statistics
     */
    fun getMetrics(): CacheMetrics {
        val totalAccesses = cache.values.sumOf { it.accessCount }
        return CacheMetrics(
            size = cache.size,
            maxSize = maxSize,
            totalAccesses = totalAccesses
        )
    }
    
    /**
     * Evicts entries under memory pressure.
     * Removes a percentage of cache entries, prioritizing:
     * 1. Expired entries
     * 2. Least frequently accessed entries
     * 3. Least recently used entries
     * 
     * @param percentage Percentage of cache to evict (0.0 to 1.0)
     * @return Number of entries evicted
     */
    fun evictUnderMemoryPressure(percentage: Double = 0.3): Int {
        require(percentage in 0.0..1.0) { "Percentage must be between 0.0 and 1.0" }
        
        val targetEvictions = (cache.size * percentage).toInt().coerceAtLeast(1)
        var evicted = 0
        
        // First, remove all expired entries
        val expiredKeys = cache.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredKeys.forEach { key ->
            invalidate(key)
            evicted++
        }
        
        // If we haven't reached target, evict based on access frequency
        if (evicted < targetEvictions) {
            val remainingToEvict = targetEvictions - evicted
            
            // Get entries sorted by access count (ascending) and last access time
            val candidates = synchronized(accessOrder) {
                cache.entries
                    .sortedWith(
                        compareBy<Map.Entry<K, CacheEntry<V>>> { it.value.accessCount }
                            .thenBy { accessOrder[it.key] ?: Long.MAX_VALUE }
                    )
                    .take(remainingToEvict)
                    .map { it.key }
            }
            
            candidates.forEach { key ->
                invalidate(key)
                evicted++
            }
        }
        
        Timber.d("Evicted $evicted entries under memory pressure (target: $targetEvictions)")
        return evicted
    }
    
    /**
     * Checks if a cache entry is stale based on a custom TTL.
     * 
     * @param key The cache key
     * @param staleTtl The TTL threshold for staleness
     * @return True if the entry is stale, false otherwise
     */
    fun isStale(key: K, staleTtl: Long): Boolean {
        val entry = cache[key] ?: return false
        return entry.isStale(staleTtl)
    }
    
    /**
     * Gets all stale cache keys based on a custom TTL.
     * 
     * @param staleTtl The TTL threshold for staleness
     * @return List of stale cache keys
     */
    fun getStaleKeys(staleTtl: Long): List<K> {
        return cache.entries
            .filter { it.value.isStale(staleTtl) }
            .map { it.key }
    }
    
    /**
     * Gets the timestamp of a cache entry.
     * 
     * @param key The cache key
     * @return Timestamp in milliseconds, or null if entry doesn't exist
     */
    fun getEntryTimestamp(key: K): Long? {
        return cache[key]?.timestamp
    }
}

/**
 * Cache metrics for monitoring and analysis.
 * 
 * @property size Current number of entries in the cache
 * @property maxSize Maximum cache capacity
 * @property totalAccesses Total number of cache accesses
 */
data class CacheMetrics(
    val size: Int,
    val maxSize: Int,
    val totalAccesses: Int
) {
    /**
     * Calculates the cache utilization percentage.
     */
    val utilizationPercent: Double
        get() = if (maxSize > 0) (size.toDouble() / maxSize) * 100 else 0.0
}
