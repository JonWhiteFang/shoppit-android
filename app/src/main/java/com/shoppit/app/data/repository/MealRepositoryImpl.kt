package com.shoppit.app.data.repository

import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.domain.error.DatabaseException
import com.shoppit.app.domain.error.NotFoundException
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of MealRepository using Room database.
 * Handles data persistence, retrieval, and error mapping for meal operations.
 *
 * @property mealDao The Room DAO for meal database operations
 */
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    
    /**
     * Retrieves all meals from the database as a reactive Flow.
     * Maps database entities to domain models and handles errors.
     *
     * @return Flow emitting Result with list of meals or DatabaseException
     */
    override fun getMeals(): Flow<Result<List<Meal>>> {
        return mealDao.getAllMeals()
            .map { entities -> 
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> 
                emit(Result.failure(DatabaseException("Failed to load meals", e)))
            }
    }
    
    /**
     * Retrieves a specific meal by ID from the database as a reactive Flow.
     * Maps database entity to domain model and handles null/error cases.
     *
     * @param id The unique identifier of the meal
     * @return Flow emitting Result with meal or NotFoundException/DatabaseException
     */
    override fun getMealById(id: Long): Flow<Result<Meal>> {
        return mealDao.getMealById(id)
            .map { entity ->
                entity?.let { Result.success(it.toDomainModel()) }
                    ?: Result.failure(NotFoundException("Meal not found"))
            }
            .catch { e ->
                emit(Result.failure(DatabaseException("Failed to load meal", e)))
            }
    }
    
    /**
     * Adds a new meal to the database.
     * Converts domain model to entity and handles database errors.
     *
     * @param meal The meal to add
     * @return Result with the ID of the newly created meal or DatabaseException
     */
    override suspend fun addMeal(meal: Meal): Result<Long> {
        return try {
            val id = mealDao.insertMeal(meal.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to add meal", e))
        }
    }
    
    /**
     * Updates an existing meal in the database.
     * Converts domain model to entity and handles database errors.
     *
     * @param meal The meal with updated data
     * @return Result indicating success or DatabaseException
     */
    override suspend fun updateMeal(meal: Meal): Result<Unit> {
        return try {
            mealDao.updateMeal(meal.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to update meal", e))
        }
    }
    
    /**
     * Deletes a meal from the database by its ID.
     * Handles database errors during deletion.
     *
     * @param mealId The ID of the meal to delete
     * @return Result indicating success or DatabaseException
     */
    override suspend fun deleteMeal(mealId: Long): Result<Unit> {
        return try {
            mealDao.deleteMealById(mealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to delete meal", e))
        }
    }
}
