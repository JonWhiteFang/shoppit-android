package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlanWithMeal
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for retrieving all meal plans for a specific date with meal details.
 * Combines meal plans with their associated meal information.
 * Handles cases where meals are deleted but plans still exist.
 *
 * Requirements: 6.2, 10.1
 */
class GetMealPlansForDateUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository
) {
    /**
     * Invokes the use case to get all meal plans for a specific date.
     * Combines meal plans with meal details and filters out plans for deleted meals.
     *
     * @param date The date to retrieve meal plans for
     * @return Flow emitting Result with list of MealPlanWithMeal
     */
    operator fun invoke(date: LocalDate): Flow<Result<List<MealPlanWithMeal>>> {
        return mealPlanRepository.getMealPlansForDate(date)
            .combine(mealRepository.getMeals()) { plansResult, mealsResult ->
                plansResult.fold(
                    onSuccess = { plans ->
                        mealsResult.fold(
                            onSuccess = { meals ->
                                // Create a map for efficient meal lookup
                                val mealMap = meals.associateBy { it.id }
                                
                                // Combine plans with meals, filtering out plans for deleted meals
                                val plansWithMeals = plans.mapNotNull { plan ->
                                    mealMap[plan.mealId]?.let { meal ->
                                        MealPlanWithMeal(plan, meal)
                                    }
                                }
                                
                                Result.success(plansWithMeals)
                            },
                            onFailure = { error -> Result.failure(error) }
                        )
                    },
                    onFailure = { error -> Result.failure(error) }
                )
            }
    }
}
