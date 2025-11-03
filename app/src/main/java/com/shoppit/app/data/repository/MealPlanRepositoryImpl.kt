package com.shoppit.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.error.PersistenceLogger
import com.shoppit.app.data.local.dao.MealPlanDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.EntityType
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.SyncOperation
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.SyncEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementation of MealPlanRepository using Room database.
 * Handles data persistence, retrieval, and error mapping for meal plan operations.
 *
 * All query methods return Flow for reactive updates.
 * All mutation methods handle errors and return Result types.
 *
 * @property mealPlanDao The Room DAO for meal plan database operations
 */
class MealPlanRepositoryImpl @Inject constructor(
    private val mealPlanDao: MealPlanDao,
    private val syncEngine: SyncEngine,
    private val errorLogger: ErrorLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : MealPlanRepository {
    
    /**
     * Retrieves all meal plans for a week as a reactive Flow.
     * Emits updates whenever meal plans in the date range change.
     *
     * @param startDate The first day of the week
     * @param endDate The last day of the week
     * @return Flow emitting Result with list of meal plans or PersistenceError.QueryFailed
     */
    override fun getMealPlansForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<MealPlan>>> {
        return mealPlanDao.getMealPlansForWeek(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        )
            .map { entities -> 
                val mealPlans = entities.map { it.toDomainModel() }
                PersistenceLogger.logOperationSuccess("getMealPlansForWeek", 0)
                Result.success(mealPlans)
            }
            .catch { e -> 
                PersistenceLogger.logQueryFailure("getMealPlansForWeek", e)
                errorLogger.logError(e, "MealPlanRepositoryImpl.getMealPlansForWeek", mapOf("startDate" to startDate, "endDate" to endDate))
                emit(Result.failure(PersistenceError.QueryFailed("getMealPlansForWeek", e)))
            }
    }
    
    /**
     * Retrieves all meal plans for a specific date as a reactive Flow.
     * Emits updates whenever meal plans for the date change.
     *
     * @param date The date to retrieve meal plans for
     * @return Flow emitting Result with list of meal plans or PersistenceError.QueryFailed
     */
    override fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>> {
        return mealPlanDao.getMealPlansForDate(date.toEpochDay())
            .map { entities -> 
                val mealPlans = entities.map { it.toDomainModel() }
                PersistenceLogger.logOperationSuccess("getMealPlansForDate", 0)
                Result.success(mealPlans)
            }
            .catch { e -> 
                PersistenceLogger.logQueryFailure("getMealPlansForDate", e)
                errorLogger.logError(e, "MealPlanRepositoryImpl.getMealPlansForDate", mapOf("date" to date))
                emit(Result.failure(PersistenceError.QueryFailed("getMealPlansForDate", e)))
            }
    }
    
    /**
     * Retrieves a specific meal plan by its ID as a reactive Flow.
     * Emits updates whenever the meal plan changes.
     *
     * @param id The unique identifier of the meal plan
     * @return Flow emitting Result with the meal plan or PersistenceError.QueryFailed
     */
    override fun getMealPlanById(id: Long): Flow<Result<MealPlan>> {
        return mealPlanDao.getMealPlanById(id)
            .map { entity ->
                if (entity != null) {
                    val mealPlan = entity.toDomainModel()
                    PersistenceLogger.logOperationSuccess("getMealPlanById", 0)
                    Result.success(mealPlan)
                } else {
                    PersistenceLogger.logQueryFailure("getMealPlanById", Exception("Meal plan not found: id=$id"))
                    errorLogger.logError(Exception("Meal plan not found: id=$id"), "MealPlanRepositoryImpl.getMealPlanById", mapOf("mealPlanId" to id))
                    Result.failure(PersistenceError.QueryFailed("getMealPlanById", Exception("Meal plan not found: id=$id")))
                }
            }
            .catch { e ->
                PersistenceLogger.logQueryFailure("getMealPlanById", e)
                errorLogger.logError(e, "MealPlanRepositoryImpl.getMealPlanById", mapOf("mealPlanId" to id))
                emit(Result.failure(PersistenceError.QueryFailed("getMealPlanById", e)))
            }
    }
    
    /**
     * Adds a new meal plan to the database.
     * Uses REPLACE conflict strategy to handle unique constraint violations
     * on (date, meal_type) by replacing the existing plan.
     *
     * @param mealPlan The meal plan to add
     * @return Result with the ID of the newly created meal plan or PersistenceError
     */
    override suspend fun addMealPlan(mealPlan: MealPlan): Result<Long> {
        PersistenceLogger.logOperationStart("addMealPlan")
        
        return try {
            val id = mealPlanDao.insertMealPlan(mealPlan.toEntity())
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.MEAL_PLAN, id, SyncOperation.CREATE)
            
            PersistenceLogger.logOperationSuccess("addMealPlan", 0)
            Result.success(id)
        } catch (e: SQLiteConstraintException) {
            PersistenceLogger.logOperationFailure("addMealPlan", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.addMealPlan", mapOf("date" to mealPlan.date, "mealType" to mealPlan.mealType))
            Result.failure(PersistenceError.ConstraintViolation("meal_plan_insert", e.message ?: "Unknown constraint violation"))
        } catch (e: Exception) {
            PersistenceLogger.logOperationFailure("addMealPlan", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.addMealPlan", mapOf("date" to mealPlan.date, "mealType" to mealPlan.mealType))
            Result.failure(PersistenceError.WriteFailed("addMealPlan", e))
        }
    }
    
    /**
     * Updates an existing meal plan in the database.
     * The meal plan must have a valid ID.
     *
     * @param mealPlan The meal plan with updated data
     * @return Result indicating success or PersistenceError
     */
    override suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit> {
        PersistenceLogger.logOperationStart("updateMealPlan", "id=${mealPlan.id}")
        
        return try {
            mealPlanDao.updateMealPlan(mealPlan.toEntity())
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.MEAL_PLAN, mealPlan.id, SyncOperation.UPDATE)
            
            PersistenceLogger.logOperationSuccess("updateMealPlan", 0)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            PersistenceLogger.logOperationFailure("updateMealPlan", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.updateMealPlan", mapOf("mealPlanId" to mealPlan.id, "date" to mealPlan.date, "mealType" to mealPlan.mealType))
            Result.failure(PersistenceError.ConstraintViolation("meal_plan_update", e.message ?: "Unknown constraint violation"))
        } catch (e: Exception) {
            PersistenceLogger.logOperationFailure("updateMealPlan", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.updateMealPlan", mapOf("mealPlanId" to mealPlan.id, "date" to mealPlan.date, "mealType" to mealPlan.mealType))
            Result.failure(PersistenceError.WriteFailed("updateMealPlan", e))
        }
    }
    
    /**
     * Deletes a meal plan from the database by its ID.
     *
     * @param mealPlanId The ID of the meal plan to delete
     * @return Result indicating success or PersistenceError
     */
    override suspend fun deleteMealPlan(mealPlanId: Long): Result<Unit> {
        PersistenceLogger.logOperationStart("deleteMealPlan", "id=$mealPlanId")
        
        return try {
            mealPlanDao.deleteMealPlanById(mealPlanId)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.MEAL_PLAN, mealPlanId, SyncOperation.DELETE)
            
            PersistenceLogger.logOperationSuccess("deleteMealPlan", 0)
            Result.success(Unit)
        } catch (e: Exception) {
            PersistenceLogger.logOperationFailure("deleteMealPlan", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.deleteMealPlan", mapOf("mealPlanId" to mealPlanId))
            Result.failure(PersistenceError.WriteFailed("deleteMealPlan", e))
        }
    }
    
    /**
     * Deletes all meal plans for a specific date.
     * Used for clearing a day's meal plans.
     *
     * @param date The date to clear meal plans for
     * @return Result indicating success or PersistenceError
     */
    override suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit> {
        PersistenceLogger.logOperationStart("deleteMealPlansForDate", "date=$date")
        
        return try {
            mealPlanDao.deleteMealPlansForDate(date.toEpochDay())
            PersistenceLogger.logOperationSuccess("deleteMealPlansForDate", 0)
            Result.success(Unit)
        } catch (e: Exception) {
            PersistenceLogger.logOperationFailure("deleteMealPlansForDate", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.deleteMealPlansForDate", mapOf("date" to date))
            Result.failure(PersistenceError.WriteFailed("deleteMealPlansForDate", e))
        }
    }
    
    /**
     * Adds multiple meal plans to the database in a single batch operation.
     * Uses REPLACE conflict strategy to handle unique constraint violations.
     * More efficient than adding meal plans one by one.
     *
     * @param mealPlans The list of meal plans to add
     * @return Result with list of IDs of newly created meal plans or PersistenceError
     */
    override suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<List<Long>> {
        PersistenceLogger.logOperationStart("addMealPlans", "count=${mealPlans.size}")
        
        return try {
            val entities = mealPlans.map { it.toEntity() }
            val ids = mealPlanDao.insertMealPlans(entities)
            
            // Queue changes for sync
            ids.forEach { id ->
                syncEngine.queueChange(EntityType.MEAL_PLAN, id, SyncOperation.CREATE)
            }
            
            PersistenceLogger.logOperationSuccess("addMealPlans", 0)
            Result.success(ids)
        } catch (e: SQLiteConstraintException) {
            PersistenceLogger.logOperationFailure("addMealPlans", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.addMealPlans", mapOf("mealPlanCount" to mealPlans.size))
            Result.failure(PersistenceError.ConstraintViolation("meal_plan_batch_insert", e.message ?: "Unknown constraint violation"))
        } catch (e: Exception) {
            PersistenceLogger.logOperationFailure("addMealPlans", e)
            errorLogger.logError(e, "MealPlanRepositoryImpl.addMealPlans", mapOf("mealPlanCount" to mealPlans.size))
            Result.failure(PersistenceError.WriteFailed("addMealPlans", e))
        }
    }
}
