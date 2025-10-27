package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppit.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for meal-related database operations.
 * Provides methods for CRUD operations on meals with reactive Flow-based queries.
 */
@Dao
interface MealDao {
    
    /**
     * Retrieves all meals from the database, sorted alphabetically by name.
     * Returns a Flow that emits the updated list whenever the data changes.
     *
     * @return Flow emitting list of all meals sorted by name
     */
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<MealEntity>>
    
    /**
     * Retrieves a specific meal by its ID.
     * Returns a Flow that emits the meal or null if not found.
     *
     * @param mealId The unique identifier of the meal
     * @return Flow emitting the meal entity or null if not found
     */
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealById(mealId: Long): Flow<MealEntity?>
    
    /**
     * Inserts a new meal into the database.
     * If a meal with the same ID exists, it will be replaced.
     *
     * @param meal The meal entity to insert
     * @return The row ID of the inserted meal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    /**
     * Updates an existing meal in the database.
     *
     * @param meal The meal entity with updated values
     */
    @Update
    suspend fun updateMeal(meal: MealEntity)
    
    /**
     * Deletes a meal from the database by its ID.
     *
     * @param mealId The unique identifier of the meal to delete
     */
    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)
}
