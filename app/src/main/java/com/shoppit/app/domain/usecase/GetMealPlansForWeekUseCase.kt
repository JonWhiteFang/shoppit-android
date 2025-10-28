package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlanWithMeal
import com.shoppit.app.domain.model.WeekPlanData
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving all meal plans for a week with meal details.
 * Combines meal plans with their associated meal information and groups by date.
 * Handles cases where meals are deleted but plans still exist.
 *
 * Requirements: 1.1, 1.2, 1.3, 1.4, 5.2
 */
class GetMealPlansForWeekUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository
) {
    /**
     * Invokes the use case to get all meal plans for a week.
     * Calculates week start (Monday) and end (Sunday) dates from the provided start date.
     * Combines meal plans with meal details and filters out plans for deleted meals.
     *
     * @param startDate The starting date for the week (will be adjusted to Monday)
     * @return Flow emitting Result with WeekPlanData containing plans grouped by date
     */
    operator fun invoke(startDate: LocalDate): Flow<Result<WeekPlanData>> {
        // Calculate week start (Monday) and end (Sunday)
        val weekStart = startDate.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        
        return mealPlanRepository.getMealPlansForWeek(weekStart, weekEnd)
            .combine(mealRepository.getMeals()) { plansResult, mealsResult ->
                if (plansResult.isFailure) {
                    Result.failure(plansResult.exceptionOrNull() ?: Exception("Unknown error"))
                } else if (mealsResult.isFailure) {
                    Result.failure(mealsResult.exceptionOrNull() ?: Exception("Unknown error"))
                } else {
                    val plans = plansResult.getOrNull() ?: emptyList()
                    val meals = mealsResult.getOrNull() ?: emptyList()
                    
                    // Create a map for efficient meal lookup
                    val mealMap = meals.associateBy { it.id }
                    
                    // Combine plans with meals, filtering out plans for deleted meals
                    val plansByDate = plans
                        .mapNotNull { plan ->
                            mealMap[plan.mealId]?.let { meal ->
                                MealPlanWithMeal(plan, meal)
                            }
                        }
                        .groupBy { it.mealPlan.date }
                    
                    Result.success(WeekPlanData(weekStart, weekEnd, plansByDate))
                }
            }
    }
}
