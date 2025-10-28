package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.MealPlanRepository
import javax.inject.Inject

/**
 * Use case for deleting a meal plan assignment.
 * Removes the meal plan from the repository.
 *
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5
 */
class DeleteMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    /**
     * Invokes the use case to delete a meal plan.
     * Removes the meal plan from the repository by its ID.
     *
     * @param mealPlanId The ID of the meal plan to delete
     * @return Result indicating success or error
     */
    suspend operator fun invoke(mealPlanId: Long): Result<Unit> {
        return repository.deleteMealPlan(mealPlanId)
    }
}
