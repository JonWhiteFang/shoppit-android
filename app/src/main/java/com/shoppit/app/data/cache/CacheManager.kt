package com.shoppit.app.data.cache

/**
 * Generic interface for managing in-memory cache.
 * 
 * Provides basic cache operations including get, put, invalidate, and clear.
 * Implementations should handle cache eviction policies and size limits.
 * 
 * @param K The type of cache keys
 * @param V The type of cached values
 */
interface CacheManager<K, V> {
    /**
     * Retrieves a value from the cache.
     * 
     * @param key The cache key
     * @return The cached value, or null if not found or expired
     */
    fun get(key: K): V?
    
    /**
     * Stores a value in the cache.
     * 
     * @param key The cache key
     * @param value The value to cache
     */
    fun put(key: K, value: V)
    
    /**
     * Invalidates a specific cache entry.
     * 
     * @param key The cache key to invalidate
     */
    fun invalidate(key: K)
    
    /**
     * Invalidates all cache entries.
     */
    fun invalidateAll()
    
    /**
     * Returns the current number of entries in the cache.
     * 
     * @return The cache size
     */
    fun size(): Int
    
    /**
     * Clears all entries from the cache.
     * This is an alias for invalidateAll() for consistency with standard collections.
     */
    fun clear() = invalidateAll()
}
