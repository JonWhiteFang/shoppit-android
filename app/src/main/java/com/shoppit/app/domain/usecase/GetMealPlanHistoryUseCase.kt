package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for retrieving historical meal planning data.
 * Analyzes meal plans from the past 30 days to determine:
 * - How many times each meal was planned
 * - When each meal was last planned
 * - All dates when each meal was planned
 *
 * This data is used by the suggestion scoring algorithm to:
 * - Apply frequency penalties (meals planned too often)
 * - Apply recency penalties (meals planned too recently)
 * - Promote variety in meal planning
 *
 * Performance Optimization:
 * - Caches history data for the current session
 * - Cache is invalidated when meal plans change
 * - Reduces database queries for repeated suggestion requests
 *
 * Requirements: 3.1-3.5, 7.3
 */
@Singleton
class GetMealPlanHistoryUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) {
    // Cache for meal plan history
    private var cachedHistory: Map<Long, MealPlanHistory>? = null
    private var cacheTimestamp: Long = 0
    private val cacheMutex = Mutex()
    /**
     * Retrieves meal plan history for the past 30 days.
     * Uses cached data if available and fresh (within current session).
     *
     * @param forceRefresh If true, bypasses cache and fetches fresh data
     * @return Result with map of meal ID to MealPlanHistory, or error if query fails
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<Map<Long, MealPlanHistory>> {
        // Check cache first (unless force refresh)
        if (!forceRefresh) {
            cacheMutex.withLock {
                cachedHistory?.let { cached ->
                    // Cache is valid for the current session
                    return Result.success(cached)
                }
            }
        }
        return try {
            // Calculate date range: past 30 days
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(30)
            
            // Fetch meal plans for the date range
            val mealPlansResult = mealPlanRepository
                .getMealPlansForWeek(startDate, endDate)
                .first()
            
            // Handle repository errors
            if (mealPlansResult.isFailure) {
                return Result.failure(
                    mealPlansResult.exceptionOrNull() 
                        ?: Exception("Failed to fetch meal plans")
                )
            }
            
            val mealPlans = mealPlansResult.getOrNull() ?: emptyList()
            
            // Group meal plans by meal ID
            val historyMap = mealPlans
                .groupBy { it.mealId }
                .mapValues { (mealId, plans) ->
                    // Sort plans by date to find the most recent
                    val sortedPlans = plans.sortedByDescending { it.date }
                    
                    MealPlanHistory(
                        mealId = mealId,
                        lastPlannedDate = sortedPlans.firstOrNull()?.date,
                        planCount = plans.size,
                        planDates = plans.map { it.date }.sorted()
                    )
                }
            
            // Update cache
            cacheMutex.withLock {
                cachedHistory = historyMap
                cacheTimestamp = System.currentTimeMillis()
            }
            
            Result.success(historyMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Invalidates the cached history data.
     * Should be called when meal plans are added, updated, or deleted.
     *
     * Requirement 7.3: Implement cache invalidation when plans change
     */
    suspend fun invalidateCache() {
        cacheMutex.withLock {
            cachedHistory = null
            cacheTimestamp = 0
        }
    }
}
