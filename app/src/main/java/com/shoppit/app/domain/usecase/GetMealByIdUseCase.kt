package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving a specific meal by its ID.
 * Returns a reactive Flow that emits updates whenever the meal changes.
 *
 * Requirements: 3.1 - Retrieve specific meal from database by identifier
 */
class GetMealByIdUseCase @Inject constructor(
    private val repository: MealRepository
) {
    /**
     * Invokes the use case to get a meal by its ID.
     *
     * @param mealId The unique identifier of the meal to retrieve
     * @return Flow emitting Result with the meal or error (including not found)
     */
    operator fun invoke(mealId: Long): Flow<Result<Meal>> {
        return repository.getMealById(mealId)
    }
}
