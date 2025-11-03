package com.shoppit.app.data.cache

import com.shoppit.app.data.performance.PerformanceMonitor
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warms up caches on app startup by preloading frequently accessed data.
 * 
 * Cache warming improves perceived performance by ensuring that commonly
 * accessed data is already in memory when users need it.
 * 
 * Requirements: 8.5
 */
@Singleton
class CacheWarmer @Inject constructor(
    private val mealRepository: MealRepository,
    private val performanceMonitor: PerformanceMonitor,
    private val ioDispatcher: CoroutineDispatcher
) {
    
    /**
     * Warms up caches by preloading frequently accessed data.
     * Should be called during app initialization in a background thread.
     * 
     * @return True if cache warming completed successfully, false otherwise
     */
    suspend fun warmCaches(): Boolean = withContext(ioDispatcher) {
        if (!CacheConfig.CACHE_WARMING_ENABLED) {
            Timber.d("Cache warming is disabled")
            return@withContext false
        }
        
        val startTime = System.currentTimeMillis()
        Timber.d("Starting cache warming...")
        
        try {
            // Warm meal cache by loading recent meals
            warmMealCache()
            
            val duration = System.currentTimeMillis() - startTime
            Timber.d("Cache warming completed in ${duration}ms")
            
            // Track cache warming performance
            performanceMonitor.trackQuery("cache_warming", duration)
            
            true
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Timber.e(e, "Cache warming failed after ${duration}ms")
            false
        }
    }
    
    /**
     * Warms the meal cache by loading recent meals.
     */
    private suspend fun warmMealCache() {
        try {
            // Load meals to populate cache
            // The repository will cache the results automatically
            mealRepository.getMeals().collect { result ->
                result.fold(
                    onSuccess = { meals ->
                        val count = meals.take(CacheConfig.CACHE_WARMING_LIMIT).size
                        Timber.d("Warmed meal cache with $count meals")
                    },
                    onFailure = { error ->
                        Timber.w(error, "Failed to warm meal cache")
                    }
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error warming meal cache")
        }
    }
}
