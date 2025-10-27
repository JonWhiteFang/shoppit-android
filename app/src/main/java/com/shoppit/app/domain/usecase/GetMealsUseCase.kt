package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving all meals.
 * Returns a reactive Flow that emits updates whenever the meal list changes.
 *
 * Requirements: 2.1 - Retrieve all meals from database
 */
class GetMealsUseCase @Inject constructor(
    private val repository: MealRepository
) {
    /**
     * Invokes the use case to get all meals.
     *
     * @return Flow emitting Result with list of meals or error
     */
    operator fun invoke(): Flow<Result<List<Meal>>> {
        return repository.getMeals()
    }
}
