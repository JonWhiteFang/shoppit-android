package com.shoppit.app.data.cache

import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * LRU (Least Recently Used) cache implementation.
 * 
 * This cache manager implements an LRU eviction policy with configurable size and TTL.
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
    private val onEviction: ((K, V) -> Unit)? = null
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
        }
        
        // Check if entry is expired
        if (entry.isExpired()) {
            Timber.v("Cache entry expired: $key")
            invalidate(key)
            return null
        }
        
        // Update access order
        synchronized(accessOrder) {
            accessOrder[key] = System.currentTimeMillis()
        }
        
        // Update access count
        cache[key] = entry.incrementAccessCount()
        
        Timber.v("Cache hit: $key (accessCount=${entry.accessCount + 1})")
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
     */
    private fun evictLeastRecentlyUsed() {
        val lruKey = synchronized(accessOrder) {
            accessOrder.keys.firstOrNull()
        }
        
        if (lruKey != null) {
            val entry = cache.remove(lruKey)
            synchronized(accessOrder) {
                accessOrder.remove(lruKey)
            }
            
            if (entry != null) {
                onEviction?.invoke(lruKey, entry.data)
                Timber.v("Cache evicted (LRU): $lruKey")
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
