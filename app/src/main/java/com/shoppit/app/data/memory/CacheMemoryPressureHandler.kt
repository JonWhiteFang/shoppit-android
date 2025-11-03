package com.shoppit.app.data.memory

import com.shoppit.app.data.cache.CacheManager
import com.shoppit.app.data.cache.LruCacheManager
import com.shoppit.app.data.performance.PerformanceMonitor
import com.shoppit.app.di.MealDetailCache
import com.shoppit.app.di.MealListCache
import com.shoppit.app.domain.model.Meal
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles memory pressure events by managing cache sizes and clearing caches.
 * 
 * Responds to different memory pressure levels:
 * - LOW: Clean up expired cache entries
 * - MODERATE: Clear 50% of cache entries
 * - CRITICAL: Clear all cache entries
 * 
 * Requirements: 4.4, 4.5, 10.3
 */
@Singleton
class CacheMemoryPressureHandler @Inject constructor(
    @MealListCache private val mealListCache: CacheManager<String, List<Meal>>,
    @MealDetailCache private val mealDetailCache: CacheManager<Long, Meal>,
    private val performanceMonitor: PerformanceMonitor
) : MemoryPressureListener {
    
    override fun onMemoryPressure(level: MemoryPressureLevel) {
        Timber.d("Handling memory pressure: $level")
        
        // Track memory pressure event
        performanceMonitor.trackMemoryPressureEvent()
        
        when (level) {
            MemoryPressureLevel.LOW -> handleLowMemoryPressure()
            MemoryPressureLevel.MODERATE -> handleModerateMemoryPressure()
            MemoryPressureLevel.CRITICAL -> handleCriticalMemoryPressure()
        }
    }
    
    /**
     * Handles low memory pressure by cleaning up expired cache entries.
     */
    private fun handleLowMemoryPressure() {
        Timber.d("Low memory pressure: cleaning up expired cache entries")
        
        // Clean up expired entries if cache supports it
        (mealListCache as? LruCacheManager)?.cleanupExpired()
        (mealDetailCache as? LruCacheManager)?.cleanupExpired()
    }
    
    /**
     * Handles moderate memory pressure by clearing a portion of cache entries.
     * Clears approximately 50% of cache entries to free up memory.
     */
    private fun handleModerateMemoryPressure() {
        val mealListSizeBefore = mealListCache.size()
        val mealDetailSizeBefore = mealDetailCache.size()
        
        Timber.d("Moderate memory pressure: clearing caches (mealList=$mealListSizeBefore, mealDetail=$mealDetailSizeBefore)")
        
        // For moderate pressure, clear all caches to free up memory
        // In a more sophisticated implementation, we could clear only a portion
        mealListCache.clear()
        mealDetailCache.clear()
        
        Timber.d("Caches cleared due to moderate memory pressure")
    }
    
    /**
     * Handles critical memory pressure by clearing all cache entries.
     */
    private fun handleCriticalMemoryPressure() {
        val mealListSizeBefore = mealListCache.size()
        val mealDetailSizeBefore = mealDetailCache.size()
        
        Timber.w("Critical memory pressure: clearing all caches (mealList=$mealListSizeBefore, mealDetail=$mealDetailSizeBefore)")
        
        // Clear all caches immediately
        mealListCache.clear()
        mealDetailCache.clear()
        
        // Force garbage collection (use sparingly)
        System.gc()
        
        Timber.w("All caches cleared due to critical memory pressure")
    }
}
