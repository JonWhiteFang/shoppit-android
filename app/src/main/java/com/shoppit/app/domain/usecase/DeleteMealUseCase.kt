package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.MealRepository
import javax.inject.Inject

/**
 * Use case for deleting a meal.
 * Removes the meal from the repository.
 *
 * Requirements:
 * - 5.2: Remove meal from database when user confirms deletion
 */
class DeleteMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    /**
     * Invokes the use case to delete a meal by its ID.
     *
     * @param mealId The ID of the meal to delete
     * @return Result indicating success or database error
     */
    suspend operator fun invoke(mealId: Long): Result<Unit> {
        return repository.deleteMeal(mealId)
    }
}
