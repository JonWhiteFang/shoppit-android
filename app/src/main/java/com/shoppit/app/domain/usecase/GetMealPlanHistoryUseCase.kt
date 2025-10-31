package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

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
 * Requirements: 3.1-3.5
 */
class GetMealPlanHistoryUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository
) {
    /**
     * Retrieves meal plan history for the past 30 days.
     *
     * @return Result with map of meal ID to MealPlanHistory, or error if query fails
     */
    suspend operator fun invoke(): Result<Map<Long, MealPlanHistory>> {
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
            
            Result.success(historyMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
