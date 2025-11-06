package com.shoppit.app.data.cache

import com.shoppit.app.data.performance.PerformanceMonitor
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.MealRepository
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
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
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingListRepository: ShoppingListRepository,
    private val performanceMonitor: PerformanceMonitor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    private var warmingStats = CacheWarmingStats()
    
    /**
     * Warms up caches by preloading frequently accessed data.
     * Should be called during app initialization in a background thread.
     * 
     * Warms the following caches in parallel:
     * - Meal cache: Loads recent meals
     * - Meal plan cache: Loads current week's meal plans
     * - Shopping list cache: Loads current shopping list
     * 
     * @return CacheWarmingStats with effectiveness metrics
     */
    suspend fun warmCaches(): CacheWarmingStats = withContext(ioDispatcher) {
        if (!CacheConfig.CACHE_WARMING_ENABLED) {
            Timber.d("Cache warming is disabled")
            return@withContext CacheWarmingStats(enabled = false)
        }
        
        val startTime = System.currentTimeMillis()
        Timber.d("Starting cache warming...")
        
        try {
            // Warm caches in parallel for better performance
            coroutineScope {
                val mealJob = async { warmMealCache() }
                val mealPlanJob = async { warmMealPlanCache() }
                val shoppingListJob = async { warmShoppingListCache() }
                
                // Wait for all warming operations to complete
                awaitAll(mealJob, mealPlanJob, shoppingListJob)
            }
            
            val duration = System.currentTimeMillis() - startTime
            warmingStats = warmingStats.copy(
                totalDuration = duration,
                success = true
            )
            
            Timber.d("Cache warming completed in ${duration}ms: $warmingStats")
            
            // Track cache warming performance
            performanceMonitor.trackQuery("cache_warming", duration)
            
            warmingStats
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            warmingStats = warmingStats.copy(
                totalDuration = duration,
                success = false
            )
            Timber.e(e, "Cache warming failed after ${duration}ms")
            warmingStats
        }
    }
    
    /**
     * Warms the meal cache by loading recent meals.
     */
    private suspend fun warmMealCache() {
        val startTime = System.currentTimeMillis()
        try {
            // Load meals to populate cache
            // The repository will cache the results automatically
            mealRepository.getMeals().collect { result ->
                result.fold(
                    onSuccess = { meals ->
                        val count = meals.take(CacheConfig.CACHE_WARMING_LIMIT).size
                        val duration = System.currentTimeMillis() - startTime
                        warmingStats = warmingStats.copy(
                            mealsWarmed = count,
                            mealCacheDuration = duration
                        )
                        Timber.d("Warmed meal cache with $count meals in ${duration}ms")
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
    
    /**
     * Warms the meal plan cache by loading current week's meal plans.
     */
    private suspend fun warmMealPlanCache() {
        val startTime = System.currentTimeMillis()
        try {
            // TODO: Implement when getMealPlansForDateRange is available
            // Get current week's date range
            // val today = LocalDate.now()
            // val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            // val endOfWeek = startOfWeek.plusDays(6)
            
            // val startDate = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // val endDate = endOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            // Load meal plans for current week
            // mealPlanRepository.getMealPlansForDateRange(startDate, endDate).collect { result ->
            //     result.fold(
            //         onSuccess = { plans ->
            //             val count = plans.size
            //             val duration = System.currentTimeMillis() - startTime
            //             warmingStats = warmingStats.copy(
            //                 mealPlansWarmed = count,
            //                 mealPlanCacheDuration = duration
            //             )
            //             Timber.d("Warmed meal plan cache with $count plans in ${duration}ms")
            //         },
            //         onFailure = { error ->
            //             Timber.w(error, "Failed to warm meal plan cache")
            //         }
            //     )
            // }
            Timber.d("Meal plan cache warming not yet implemented")
        } catch (e: Exception) {
            Timber.e(e, "Error warming meal plan cache")
        }
    }
    
    /**
     * Warms the shopping list cache by loading current shopping list.
     */
    private suspend fun warmShoppingListCache() {
        val startTime = System.currentTimeMillis()
        try {
            // TODO: Implement when getShoppingListItems is available
            // Load shopping list items
            // shoppingListRepository.getShoppingListItems().collect { result ->
            //     result.fold(
            //         onSuccess = { items ->
            //             val count = items.size
            //             val duration = System.currentTimeMillis() - startTime
            //             warmingStats = warmingStats.copy(
            //                 shoppingItemsWarmed = count,
            //                 shoppingListCacheDuration = duration
            //             )
            //             Timber.d("Warmed shopping list cache with $count items in ${duration}ms")
            //         },
            //         onFailure = { error ->
            //             Timber.w(error, "Failed to warm shopping list cache")
            //         }
            //     )
            // }
            Timber.d("Shopping list cache warming not yet implemented")
        } catch (e: Exception) {
            Timber.e(e, "Error warming shopping list cache")
        }
    }
    
    /**
     * Gets the current cache warming statistics.
     * 
     * @return Current cache warming stats
     */
    fun getWarmingStats(): CacheWarmingStats = warmingStats
}

/**
 * Statistics for cache warming effectiveness.
 * 
 * @property enabled Whether cache warming is enabled
 * @property success Whether cache warming completed successfully
 * @property totalDuration Total time taken for cache warming in milliseconds
 * @property mealsWarmed Number of meals loaded into cache
 * @property mealPlansWarmed Number of meal plans loaded into cache
 * @property shoppingItemsWarmed Number of shopping list items loaded into cache
 * @property mealCacheDuration Time taken to warm meal cache in milliseconds
 * @property mealPlanCacheDuration Time taken to warm meal plan cache in milliseconds
 * @property shoppingListCacheDuration Time taken to warm shopping list cache in milliseconds
 */
data class CacheWarmingStats(
    val enabled: Boolean = true,
    val success: Boolean = false,
    val totalDuration: Long = 0,
    val mealsWarmed: Int = 0,
    val mealPlansWarmed: Int = 0,
    val shoppingItemsWarmed: Int = 0,
    val mealCacheDuration: Long = 0,
    val mealPlanCacheDuration: Long = 0,
    val shoppingListCacheDuration: Long = 0
) {
    /**
     * Total number of items warmed across all caches.
     */
    val totalItemsWarmed: Int
        get() = mealsWarmed + mealPlansWarmed + shoppingItemsWarmed
}
