package com.shoppit.app.data.repository

import com.shoppit.app.data.cache.CacheConfig
import com.shoppit.app.data.cache.CacheManager
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.di.MealDetailCache
import com.shoppit.app.di.MealListCache
import com.shoppit.app.domain.error.DatabaseException
import com.shoppit.app.domain.error.NotFoundException
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import com.shoppit.app.domain.validator.MealValidator
import com.shoppit.app.domain.validator.ValidationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of MealRepository using Room database with in-memory caching.
 * Handles data persistence, retrieval, caching, and error mapping for meal operations.
 *
 * Implements cache-aside pattern:
 * - Read operations check cache first, then database
 * - Write operations validate data, update database, and invalidate cache
 *
 * @property mealDao The Room DAO for meal database operations
 * @property mealListCache Cache for the complete list of meals
 * @property mealDetailCache Cache for individual meal details
 * @property mealValidator Validator for meal data
 */
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    @MealListCache private val mealListCache: CacheManager<String, List<Meal>>,
    @MealDetailCache private val mealDetailCache: CacheManager<Long, Meal>,
    private val mealValidator: MealValidator
) : MealRepository {
    
    companion object {
        private const val MEAL_LIST_CACHE_KEY = "all_meals"
    }
    
    /**
     * Retrieves all meals from cache or database as a reactive Flow.
     * Implements cache-aside pattern with cache warming.
     *
     * @return Flow emitting Result with list of meals or DatabaseException
     */
    override fun getMeals(): Flow<Result<List<Meal>>> {
        return mealDao.getAllMeals()
            .onStart {
                // Try to emit cached data first for immediate UI update
                mealListCache.get(MEAL_LIST_CACHE_KEY)?.let { cachedMeals ->
                    Timber.d("Emitting cached meals: ${cachedMeals.size} items")
                    emit(Result.success(cachedMeals))
                }
            }
            .map { entities -> 
                val meals = entities.map { it.toDomainModel() }
                // Update cache with fresh data
                mealListCache.put(MEAL_LIST_CACHE_KEY, meals)
                Timber.d("Loaded ${meals.size} meals from database and updated cache")
                Result.success(meals)
            }
            .catch { e -> 
                Timber.e(e, "Failed to load meals from database")
                emit(Result.failure(DatabaseException("Failed to load meals", e)))
            }
    }
    
    /**
     * Retrieves a specific meal by ID from cache or database as a reactive Flow.
     * Checks cache first, then falls back to database.
     *
     * @param id The unique identifier of the meal
     * @return Flow emitting Result with meal or NotFoundException/DatabaseException
     */
    override fun getMealById(id: Long): Flow<Result<Meal>> {
        return mealDao.getMealById(id)
            .onStart {
                // Try to emit cached data first
                mealDetailCache.get(id)?.let { cachedMeal ->
                    Timber.d("Emitting cached meal: id=$id")
                    emit(Result.success(cachedMeal))
                }
            }
            .map { entity ->
                if (entity != null) {
                    val meal = entity.toDomainModel()
                    // Update cache with fresh data
                    mealDetailCache.put(id, meal)
                    Timber.d("Loaded meal from database and updated cache: id=$id")
                    Result.success(meal)
                } else {
                    Timber.w("Meal not found: id=$id")
                    Result.failure(NotFoundException("Meal not found"))
                }
            }
            .catch { e ->
                Timber.e(e, "Failed to load meal: id=$id")
                emit(Result.failure(DatabaseException("Failed to load meal", e)))
            }
    }
    
    /**
     * Adds a new meal to the database and invalidates cache.
     * Validates meal data before persisting.
     * Converts domain model to entity and handles database errors.
     *
     * @param meal The meal to add
     * @return Result with the ID of the newly created meal or ValidationException/DatabaseException
     */
    override suspend fun addMeal(meal: Meal): Result<Long> {
        // Validate meal data first
        val validationResult = mealValidator.validate(meal)
        if (validationResult.isInvalid()) {
            val errors = validationResult.getErrors()
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            Timber.w("Meal validation failed: $message")
            return Result.failure(ValidationException(message))
        }
        
        return try {
            val id = mealDao.insertMeal(meal.toEntity())
            // Invalidate meal list cache since we added a new meal
            mealListCache.invalidate(MEAL_LIST_CACHE_KEY)
            Timber.d("Added meal and invalidated cache: id=$id")
            Result.success(id)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add meal")
            Result.failure(DatabaseException("Failed to add meal", e))
        }
    }
    
    /**
     * Updates an existing meal in the database and invalidates cache.
     * Validates meal data before persisting.
     * Converts domain model to entity and handles database errors.
     *
     * @param meal The meal with updated data
     * @return Result indicating success or ValidationException/DatabaseException
     */
    override suspend fun updateMeal(meal: Meal): Result<Unit> {
        // Validate meal data first
        val validationResult = mealValidator.validate(meal)
        if (validationResult.isInvalid()) {
            val errors = validationResult.getErrors()
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            Timber.w("Meal validation failed: $message")
            return Result.failure(ValidationException(message))
        }
        
        return try {
            mealDao.updateMeal(meal.toEntity())
            // Invalidate both list and detail caches
            mealListCache.invalidate(MEAL_LIST_CACHE_KEY)
            mealDetailCache.invalidate(meal.id)
            Timber.d("Updated meal and invalidated cache: id=${meal.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update meal: id=${meal.id}")
            Result.failure(DatabaseException("Failed to update meal", e))
        }
    }
    
    /**
     * Deletes a meal from the database by its ID and invalidates cache.
     * Handles database errors during deletion.
     *
     * @param mealId The ID of the meal to delete
     * @return Result indicating success or DatabaseException
     */
    override suspend fun deleteMeal(mealId: Long): Result<Unit> {
        return try {
            mealDao.deleteMealById(mealId)
            // Invalidate both list and detail caches
            mealListCache.invalidate(MEAL_LIST_CACHE_KEY)
            mealDetailCache.invalidate(mealId)
            Timber.d("Deleted meal and invalidated cache: id=$mealId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete meal: id=$mealId")
            Result.failure(DatabaseException("Failed to delete meal", e))
        }
    }
}
