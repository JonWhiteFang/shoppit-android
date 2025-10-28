package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for updating an existing meal plan with a new meal.
 * Replaces the meal in an existing plan while preserving date and meal type.
 *
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
class UpdateMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    /**
     * Invokes the use case to update a meal plan with a new meal.
     * Fetches the existing plan, updates the mealId, and persists the change.
     *
     * @param mealPlanId The ID of the meal plan to update
     * @param newMealId The ID of the new meal to assign
     * @return Result indicating success or error
     */
    suspend operator fun invoke(
        mealPlanId: Long,
        newMealId: Long
    ): Result<Unit> {
        return repository.getMealPlanById(mealPlanId).first().flatMap { mealPlan ->
            val updatedPlan = mealPlan.copy(mealId = newMealId)
            repository.updateMealPlan(updatedPlan)
        }
    }
}
