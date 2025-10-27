package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.Meal
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for meal data operations.
 * Defines the contract for meal persistence and retrieval.
 * Implementations should handle data source details and error mapping.
 */
interface MealRepository {
    /**
     * Retrieves all meals as a reactive Flow.
     * Emits updates whenever the meal list changes.
     *
     * @return Flow emitting Result with list of meals or error
     */
    fun getMeals(): Flow<Result<List<Meal>>>

    /**
     * Retrieves a specific meal by its ID as a reactive Flow.
     * Emits updates whenever the meal changes.
     *
     * @param id The unique identifier of the meal
     * @return Flow emitting Result with the meal or error (including not found)
     */
    fun getMealById(id: Long): Flow<Result<Meal>>

    /**
     * Adds a new meal to the repository.
     *
     * @param meal The meal to add
     * @return Result with the ID of the newly created meal or error
     */
    suspend fun addMeal(meal: Meal): Result<Long>

    /**
     * Updates an existing meal in the repository.
     *
     * @param meal The meal with updated data (must have valid ID)
     * @return Result indicating success or error
     */
    suspend fun updateMeal(meal: Meal): Result<Unit>

    /**
     * Deletes a meal from the repository.
     *
     * @param mealId The ID of the meal to delete
     * @return Result indicating success or error
     */
    suspend fun deleteMeal(mealId: Long): Result<Unit>
}
