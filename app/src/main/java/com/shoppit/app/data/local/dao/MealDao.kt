package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
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
     * Optimized with index on name column for fast sorting.
     *
     * @return Flow emitting list of all meals sorted by name
     */
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<MealEntity>>
    
    /**
     * Searches for meals by name using case-insensitive pattern matching.
     * Optimized with index on name column for fast LIKE queries.
     * Uses compiled query for better performance on frequently executed searches.
     *
     * @param searchQuery The search pattern (use % for wildcards)
     * @return Flow emitting list of matching meals sorted by name
     */
    @Query("SELECT * FROM meals WHERE name LIKE :searchQuery ORDER BY name ASC")
    fun searchMeals(searchQuery: String): Flow<List<MealEntity>>
    
    /**
     * Searches for meals by name or tags using case-insensitive pattern matching.
     * Optimized for full-text search across multiple fields.
     *
     * @param searchQuery The search pattern (use % for wildcards)
     * @return Flow emitting list of matching meals sorted by relevance
     */
    @Query("""
        SELECT * FROM meals 
        WHERE name LIKE :searchQuery OR tags LIKE :searchQuery
        ORDER BY 
            CASE WHEN name LIKE :searchQuery THEN 0 ELSE 1 END,
            name ASC
    """)
    fun searchMealsWithTags(searchQuery: String): Flow<List<MealEntity>>
    
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
    
    /**
     * Inserts multiple meals into the database in a single transaction.
     * If meals with the same IDs exist, they will be replaced.
     * This is more efficient than inserting meals one by one.
     *
     * @param meals The list of meal entities to insert
     * @return List of row IDs of the inserted meals
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>): List<Long>
    
    /**
     * Retrieves a paginated list of meals, sorted alphabetically by name.
     * Useful for loading large datasets efficiently.
     *
     * @param limit Maximum number of meals to retrieve
     * @param offset Number of meals to skip
     * @return Flow emitting paginated list of meals
     */
    @Query("SELECT * FROM meals ORDER BY name ASC LIMIT :limit OFFSET :offset")
    fun getMealsPaginated(limit: Int, offset: Int): Flow<List<MealEntity>>
}
