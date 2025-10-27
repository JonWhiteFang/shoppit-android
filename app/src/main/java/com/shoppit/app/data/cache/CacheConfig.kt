package com.shoppit.app.data.cache

/**
 * Configuration constants for cache management.
 * 
 * Defines cache size limits, TTL values, and other cache-related settings.
 */
object CacheConfig {
    /**
     * Maximum number of meal entries in the cache.
     */
    const val MEAL_LIST_CACHE_SIZE = 100
    
    /**
     * Time-to-live for meal list cache entries (5 minutes).
     */
    const val MEAL_LIST_TTL_MS = 5 * 60 * 1000L
    
    /**
     * Time-to-live for individual meal detail cache entries (10 minutes).
     */
    const val MEAL_DETAIL_TTL_MS = 10 * 60 * 1000L
    
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
