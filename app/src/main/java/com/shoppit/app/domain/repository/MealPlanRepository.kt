package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.MealPlan
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for meal plan data operations.
 * Defines the contract for meal plan persistence and retrieval.
 * Implementations should handle data source details and error mapping.
 */
interface MealPlanRepository {
    /**
     * Retrieves all meal plans for a week as a reactive Flow.
     * Emits updates whenever meal plans in the date range change.
     *
     * @param startDate The first day of the week
     * @param endDate The last day of the week
     * @return Flow emitting Result with list of meal plans or error
     */
    fun getMealPlansForWeek(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<MealPlan>>>

    /**
     * Retrieves all meal plans for a specific date as a reactive Flow.
     * Emits updates whenever meal plans for the date change.
     *
     * @param date The date to retrieve meal plans for
     * @return Flow emitting Result with list of meal plans or error
     */
    fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>>

    /**
     * Retrieves a specific meal plan by its ID as a reactive Flow.
     * Emits updates whenever the meal plan changes.
     *
     * @param id The unique identifier of the meal plan
     * @return Flow emitting Result with the meal plan or error (including not found)
     */
    fun getMealPlanById(id: Long): Flow<Result<MealPlan>>

    /**
     * Adds a new meal plan to the repository.
     *
     * @param mealPlan The meal plan to add
     * @return Result with the ID of the newly created meal plan or error
     */
    suspend fun addMealPlan(mealPlan: MealPlan): Result<Long>

    /**
     * Updates an existing meal plan in the repository.
     *
     * @param mealPlan The meal plan with updated data (must have valid ID)
     * @return Result indicating success or error
     */
    suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit>

    /**
     * Deletes a meal plan from the repository.
     *
     * @param mealPlanId The ID of the meal plan to delete
     * @return Result indicating success or error
     */
    suspend fun deleteMealPlan(mealPlanId: Long): Result<Unit>

    /**
     * Deletes all meal plans for a specific date.
     * Used for clearing a day's meal plans.
     *
     * @param date The date to clear meal plans for
     * @return Result indicating success or error
     */
    suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit>

    /**
     * Adds multiple meal plans to the repository in a single batch operation.
     * More efficient than adding meal plans one by one.
     *
     * @param mealPlans The list of meal plans to add
     * @return Result with list of IDs of the newly created meal plans or error
     */
    suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<List<Long>>
}
