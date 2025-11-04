package com.shoppit.app.data.cache

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages cache refresh strategies including background refresh and optimistic updates.
 * 
 * Implements cache-aside pattern with automatic refresh for stale entries.
 * Supports both scheduled refresh and on-demand refresh.
 * 
 * Requirements: 8.5
 */
@Singleton
class CacheRefreshStrategy @Inject constructor(
    private val ioDispatcher: CoroutineDispatcher
) {
    
    private val refreshJobs = mutableMapOf<String, Job>()
    
    /**
     * Schedules periodic background refresh for a cache.
     * 
     * @param cacheKey Unique identifier for the cache
     * @param intervalMs Refresh interval in milliseconds
     * @param scope CoroutineScope for the refresh job
     * @param refreshAction Suspend function to refresh the cache
     */
    fun schedulePeriodicRefresh(
        cacheKey: String,
        intervalMs: Long,
        scope: CoroutineScope,
        refreshAction: suspend () -> Unit
    ) {
        // Cancel existing job if any
        refreshJobs[cacheKey]?.cancel()
        
        // Start new periodic refresh job
        val job = scope.launch(ioDispatcher) {
            while (isActive) {
                try {
                    delay(intervalMs)
                    Timber.d("Executing periodic refresh for cache: $cacheKey")
                    refreshAction()
                } catch (e: Exception) {
                    Timber.e(e, "Error during periodic refresh for cache: $cacheKey")
                }
            }
        }
        
        refreshJobs[cacheKey] = job
        Timber.d("Scheduled periodic refresh for cache: $cacheKey (interval: ${intervalMs}ms)")
    }
    
    /**
     * Cancels scheduled refresh for a cache.
     * 
     * @param cacheKey Unique identifier for the cache
     */
    fun cancelRefresh(cacheKey: String) {
        refreshJobs[cacheKey]?.cancel()
        refreshJobs.remove(cacheKey)
        Timber.d("Cancelled refresh for cache: $cacheKey")
    }
    
    /**
     * Cancels all scheduled refreshes.
     */
    fun cancelAllRefreshes() {
        refreshJobs.values.forEach { it.cancel() }
        refreshJobs.clear()
        Timber.d("Cancelled all cache refreshes")
    }
    
    /**
     * Refreshes stale cache entries in the background.
     * 
     * @param cacheManager The cache manager to refresh
     * @param staleTtl TTL threshold for staleness in milliseconds
     * @param refreshAction Function to refresh a specific cache entry
     */
    suspend fun <K, V> refreshStaleEntries(
        cacheManager: LruCacheManager<K, V>,
        staleTtl: Long,
        refreshAction: suspend (K) -> V?
    ) = withContext(ioDispatcher) {
        try {
            val staleKeys = findStaleKeys(cacheManager, staleTtl)
            
            if (staleKeys.isEmpty()) {
                Timber.v("No stale cache entries found")
                return@withContext
            }
            
            Timber.d("Refreshing ${staleKeys.size} stale cache entries")
            
            staleKeys.forEach { key ->
                try {
                    val freshValue = refreshAction(key)
                    if (freshValue != null) {
                        cacheManager.put(key, freshValue)
                        Timber.v("Refreshed stale cache entry: $key")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to refresh cache entry: $key")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing stale cache entries")
        }
    }
    
    /**
     * Performs optimistic cache update.
     * Updates cache immediately with new value, then validates in background.
     * 
     * @param cacheManager The cache manager to update
     * @param key The cache key
     * @param optimisticValue The optimistic value to cache immediately
     * @param validateAction Function to validate and get the actual value
     */
    suspend fun <K, V> optimisticUpdate(
        cacheManager: CacheManager<K, V>,
        key: K,
        optimisticValue: V,
        validateAction: suspend () -> V?
    ) = withContext(ioDispatcher) {
        // Update cache immediately with optimistic value
        cacheManager.put(key, optimisticValue)
        Timber.d("Optimistic cache update: $key")
        
        try {
            // Validate in background
            val actualValue = validateAction()
            
            if (actualValue != null) {
                // Update with actual value if different
                if (actualValue != optimisticValue) {
                    cacheManager.put(key, actualValue)
                    Timber.d("Updated cache with validated value: $key")
                }
            } else {
                // Invalidate if validation failed
                cacheManager.invalidate(key)
                Timber.w("Invalidated optimistic cache entry: $key")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error validating optimistic cache update: $key")
            // Keep optimistic value on validation error
        }
    }
    
    /**
     * Implements cache-aside pattern with automatic refresh.
     * Gets value from cache, or loads from source and caches it.
     * Refreshes in background if value is stale.
     * 
     * @param cacheManager The cache manager
     * @param key The cache key
     * @param staleTtl TTL threshold for staleness in milliseconds
     * @param loadAction Function to load value from source
     * @return The cached or loaded value
     */
    suspend fun <K, V> getWithAutoRefresh(
        cacheManager: LruCacheManager<K, V>,
        key: K,
        staleTtl: Long,
        scope: CoroutineScope,
        loadAction: suspend () -> V?
    ): V? = withContext(ioDispatcher) {
        // Try to get from cache
        val cachedValue = cacheManager.get(key)
        
        if (cachedValue != null) {
            // Check if stale and refresh in background
            if (isStale(cacheManager, key, staleTtl)) {
                scope.launch(ioDispatcher) {
                    try {
                        val freshValue = loadAction()
                        if (freshValue != null) {
                            cacheManager.put(key, freshValue)
                            Timber.d("Background refresh completed for: $key")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Background refresh failed for: $key")
                    }
                }
            }
            return@withContext cachedValue
        }
        
        // Cache miss - load from source
        try {
            val value = loadAction()
            if (value != null) {
                cacheManager.put(key, value)
            }
            value
        } catch (e: Exception) {
            Timber.e(e, "Failed to load value for: $key")
            null
        }
    }
    
    /**
     * Finds stale cache keys based on TTL threshold.
     */
    private fun <K, V> findStaleKeys(
        cacheManager: LruCacheManager<K, V>,
        staleTtl: Long
    ): List<K> {
        return cacheManager.getStaleKeys(staleTtl)
    }
    
    /**
     * Checks if a cache entry is stale.
     */
    private fun <K, V> isStale(
        cacheManager: LruCacheManager<K, V>,
        key: K,
        staleTtl: Long
    ): Boolean {
        return cacheManager.isStale(key, staleTtl)
    }
}

/**
 * Configuration for cache refresh behavior.
 * 
 * @property enablePeriodicRefresh Whether to enable periodic background refresh
 * @property refreshIntervalMs Interval for periodic refresh in milliseconds
 * @property staleTtl TTL threshold for considering entries stale
 * @property enableOptimisticUpdates Whether to enable optimistic cache updates
 */
data class CacheRefreshConfig(
    val enablePeriodicRefresh: Boolean = false,
    val refreshIntervalMs: Long = 5 * 60 * 1000L, // 5 minutes
    val staleTtl: Long = 3 * 60 * 1000L, // 3 minutes
    val enableOptimisticUpdates: Boolean = true
)
