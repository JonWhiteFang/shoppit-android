package com.shoppit.app.data.cache

/**
 * Represents a cached entry with metadata for TTL and access tracking.
 * 
 * @param T The type of the cached data
 * @property data The actual cached data
 * @property timestamp The time when this entry was created (in milliseconds)
 * @property ttl Time-to-live in milliseconds
 * @property accessCount Number of times this entry has been accessed
 */
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long,
    val ttl: Long,
    val accessCount: Int = 0
) {
    /**
     * Checks if this cache entry has expired based on its TTL.
     * 
     * @return true if the entry is expired, false otherwise
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > ttl
    }
    
    /**
     * Checks if this cache entry is stale based on a custom TTL.
     * 
     * @param staleTtl The TTL threshold for staleness
     * @return true if the entry is stale, false otherwise
     */
    fun isStale(staleTtl: Long): Boolean {
        return System.currentTimeMillis() - timestamp > staleTtl
    }
    
    /**
     * Creates a new cache entry with incremented access count.
     * 
     * @return A new CacheEntry with accessCount incremented by 1
     */
    fun incrementAccessCount(): CacheEntry<T> {
        return copy(accessCount = accessCount + 1)
    }
}
