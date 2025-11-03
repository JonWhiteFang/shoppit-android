package com.shoppit.app.data.cache

/**
 * Configuration constants for cache management.
 * 
 * Defines cache size limits, TTL values, and other cache-related settings.
 * 
 * Requirements:
 * - 4.2: Use LRU cache with configurable size limits
 * - 8.1: Maintain cache hit rate above 80%
 * - 8.2: Track cache hits and misses
 */
object CacheConfig {
    /**
     * Maximum number of meal entries in the meal list cache.
     * Increased to 200 entries for better cache hit rate.
     * Requirement: 4.2, 8.1
     */
    const val MEAL_LIST_CACHE_SIZE = 200
    
    /**
     * Time-to-live for meal list cache entries (10 minutes).
     * Increased from 5 minutes to reduce cache misses.
     * Requirement: 8.1
     */
    const val MEAL_LIST_TTL_MS = 10 * 60 * 1000L
    
    /**
     * Time-to-live for individual meal detail cache entries (10 minutes).
     * Requirement: 8.1
     */
    const val MEAL_DETAIL_TTL_MS = 10 * 60 * 1000L
    
    /**
     * Maximum number of shopping list entries in the cache.
     * Set to 50 entries as shopping lists are more dynamic.
     * Requirement: 4.2, 8.2
     */
    const val SHOPPING_LIST_CACHE_SIZE = 50
    
    /**
     * Time-to-live for shopping list cache entries (2 minutes).
     * Shorter TTL due to more frequent updates.
     * Requirement: 8.2
     */
    const val SHOPPING_LIST_TTL_MS = 2 * 60 * 1000L
    
    /**
     * Whether to enable cache warming on app initialization.
     */
    const val CACHE_WARMING_ENABLED = true
    
    /**
     * Maximum number of meals to pre-load during cache warming.
     */
    const val CACHE_WARMING_LIMIT = 50
    
    /**
     * Interval for automatic cleanup of expired cache entries (1 minute).
     */
    const val CLEANUP_INTERVAL_MS = 60 * 1000L
}
