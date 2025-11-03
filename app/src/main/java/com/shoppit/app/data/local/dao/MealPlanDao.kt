package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.shoppit.app.data.local.entity.MealPlanEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for meal plan operations.
 * Provides methods to query, insert, update, and delete meal plans.
 * All query methods return Flow for reactive updates.
 */
@Dao
interface MealPlanDao {
    
    /**
     * Retrieves all meal plans for a week as a reactive Flow.
     * Returns plans within the specified date range, ordered by date and meal type.
     * Optimized with index on date column for fast range queries.
     *
     * @param startDate The first day of the week (days since epoch)
     * @param endDate The last day of the week (days since epoch)
     * @return Flow emitting list of meal plans whenever data changes
     */
    @Query("""
        SELECT * FROM meal_plans 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date ASC, meal_type ASC
    """)
    fun getMealPlansForWeek(startDate: Long, endDate: Long): Flow<List<MealPlanEntity>>
    
    /**
     * Retrieves all meal plans for a specific date as a reactive Flow.
     * Returns plans ordered by meal type.
     * Optimized with index on date column for fast filtering.
     *
     * @param date The date to retrieve meal plans for (days since epoch)
     * @return Flow emitting list of meal plans whenever data changes
     */
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY meal_type ASC")
    fun getMealPlansForDate(date: Long): Flow<List<MealPlanEntity>>
    
    /**
     * Retrieves meal plans for a date range as a reactive Flow.
     * Useful for custom date ranges beyond a week.
     * Optimized with index on date column for fast range queries.
     *
     * @param startDate The start date (days since epoch)
     * @param endDate The end date (days since epoch)
     * @return Flow emitting list of meal plans whenever data changes
     */
    @Query("""
        SELECT * FROM meal_plans 
        WHERE date BETWEEN :startDate AND :endDate 
        ORDER BY date ASC, meal_type ASC
    """)
    fun getMealPlansForDateRange(startDate: Long, endDate: Long): Flow<List<MealPlanEntity>>
    
    /**
     * Retrieves a specific meal plan by its ID as a reactive Flow.
     *
     * @param mealPlanId The unique identifier of the meal plan
     * @return Flow emitting the meal plan or null if not found
     */
    @Query("SELECT * FROM meal_plans WHERE id = :mealPlanId")
    fun getMealPlanById(mealPlanId: Long): Flow<MealPlanEntity?>
    
    /**
     * Inserts a new meal plan into the database.
     * Uses REPLACE conflict strategy to handle unique constraint violations
     * on (date, meal_type) by replacing the existing plan.
     *
     * @param mealPlan The meal plan to insert
     * @return The ID of the newly inserted meal plan
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity): Long
    
    /**
     * Inserts multiple meal plans into the database in a single batch operation.
     * Uses REPLACE conflict strategy to handle unique constraint violations.
     * More efficient than inserting meal plans one by one.
     * Wrapped in a transaction for atomicity.
     *
     * @param mealPlans The list of meal plans to insert
     * @return List of IDs of the newly inserted meal plans
     */
    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlanEntity>): List<Long>
    
    /**
     * Updates an existing meal plan in the database.
     * The meal plan must have a valid ID.
     *
     * @param mealPlan The meal plan with updated data
     */
    @Update
    suspend fun updateMealPlan(mealPlan: MealPlanEntity)
    
    /**
     * Updates multiple meal plans in a single transaction.
     * More efficient than updating meal plans one by one.
     *
     * @param mealPlans The list of meal plans with updated data
     */
    @Transaction
    @Update
    suspend fun updateMealPlans(mealPlans: List<MealPlanEntity>)
    
    /**
     * Deletes a meal plan from the database by its ID.
     *
     * @param mealPlanId The ID of the meal plan to delete
     */
    @Query("DELETE FROM meal_plans WHERE id = :mealPlanId")
    suspend fun deleteMealPlanById(mealPlanId: Long)
    
    /**
     * Deletes multiple meal plans by their IDs in a single transaction.
     * More efficient than deleting meal plans one by one.
     *
     * @param mealPlanIds The list of meal plan IDs to delete
     */
    @Transaction
    @Query("DELETE FROM meal_plans WHERE id IN (:mealPlanIds)")
    suspend fun deleteMealPlansByIds(mealPlanIds: List<Long>)
    
    /**
     * Deletes all meal plans for a specific date.
     * Used for clearing a day's meal plans.
     *
     * @param date The date to clear meal plans for (days since epoch)
     */
    @Query("DELETE FROM meal_plans WHERE date = :date")
    suspend fun deleteMealPlansForDate(date: Long)
    
    /**
     * Deletes all meal plans for a date range in a single transaction.
     * Useful for clearing multiple days at once.
     *
     * @param startDate The start date (days since epoch)
     * @param endDate The end date (days since epoch)
     */
    @Transaction
    @Query("DELETE FROM meal_plans WHERE date BETWEEN :startDate AND :endDate")
    suspend fun deleteMealPlansForDateRange(startDate: Long, endDate: Long)
    
    /**
     * Counts the number of meal plans for a specific date.
     * Useful for checking if a date has any plans.
     *
     * @param date The date to count meal plans for (days since epoch)
     * @return The number of meal plans for the date
     */
    @Query("SELECT COUNT(*) FROM meal_plans WHERE date = :date")
    suspend fun getMealPlanCountForDate(date: Long): Int
    
    /**
     * Counts the number of meal plans in a date range.
     * Useful for statistics and validation.
     *
     * @param startDate The start date (days since epoch)
     * @param endDate The end date (days since epoch)
     * @return The number of meal plans in the range
     */
    @Query("SELECT COUNT(*) FROM meal_plans WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMealPlanCountForDateRange(startDate: Long, endDate: Long): Int
}
