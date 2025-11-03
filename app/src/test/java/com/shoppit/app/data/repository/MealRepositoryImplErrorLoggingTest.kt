package com.shoppit.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.shoppit.app.data.cache.CacheManager
import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.SyncEngine
import com.shoppit.app.domain.validator.MealValidator
import com.shoppit.app.domain.validator.ValidationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for error logging in MealRepositoryImpl.
 * Tests verify that ErrorLogger is called when exceptions occur.
 */
@ExperimentalCoroutinesApi
class MealRepositoryImplErrorLoggingTest {

    private lateinit var mealDao: MealDao
    private lateinit var mealListCache: CacheManager<String, List<Meal>>
    private lateinit var mealDetailCache: CacheManager<Long, Meal>
    private lateinit var mealValidator: MealValidator
    private lateinit var syncEngine: SyncEngine
    private lateinit var errorLogger: ErrorLogger
    private lateinit var repository: MealRepositoryImpl

    @Before
    fun setup() {
        mealDao = mockk()
        mealListCache = mockk(relaxed = true)
        mealDetailCache = mockk(relaxed = true)
        mealValidator = mockk()
        syncEngine = mockk(relaxed = true)
        errorLogger = mockk(relaxed = true)
        
        repository = MealRepositoryImpl(
            mealDao,
            mealListCache,
            mealDetailCache,
            mealValidator,
            syncEngine,
            errorLogger
        )
    }

    @Test
    fun `getMeals logs error when query fails`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        every { mealDao.getAllMeals() } returns kotlinx.coroutines.flow.flow {
            throw exception
        }
        every { mealListCache.get(any()) } returns null

        // When
        val result = repository.getMeals().first()

        // Then
        assertTrue(result.isFailure)
        verify { errorLogger.logError(any(), "MealRepositoryImpl.getMeals", any()) }
    }

    @Test
    fun `getMealById logs error with context when query fails`() = runTest {
        // Given
        val mealId = 123L
        val exception = RuntimeException("Database error")
        every { mealDao.getMealById(mealId) } returns kotlinx.coroutines.flow.flow {
            throw exception
        }
        every { mealDetailCache.get(mealId) } returns null

        // When
        val result = repository.getMealById(mealId).first()

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.getMealById", 
                mapOf("mealId" to mealId)
            ) 
        }
    }

    @Test
    fun `addMeal logs error on SQLiteConstraintException`() = runTest {
        // Given
        val meal = Meal(id = 0, name = "Test Meal", ingredients = emptyList())
        every { mealValidator.validate(meal) } returns ValidationResult.Valid
        coEvery { mealDao.insertMeal(any()) } throws SQLiteConstraintException("Constraint violation")

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PersistenceError.ConstraintViolation)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.addMeal", 
                mapOf("mealName" to meal.name)
            ) 
        }
    }

    @Test
    fun `addMeal logs error on generic exception`() = runTest {
        // Given
        val meal = Meal(id = 0, name = "Test Meal", ingredients = emptyList())
        every { mealValidator.validate(meal) } returns ValidationResult.Valid
        coEvery { mealDao.insertMeal(any()) } throws RuntimeException("Unexpected error")

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PersistenceError.WriteFailed)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.addMeal", 
                mapOf("mealName" to meal.name)
            ) 
        }
    }

    @Test
    fun `updateMeal logs error with meal context`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Updated Meal", ingredients = emptyList())
        every { mealValidator.validate(meal) } returns ValidationResult.Valid
        coEvery { mealDao.updateMeal(any()) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateMeal(meal)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.updateMeal", 
                mapOf("mealId" to meal.id, "mealName" to meal.name)
            ) 
        }
    }

    @Test
    fun `deleteMeal logs error with meal ID`() = runTest {
        // Given
        val mealId = 456L
        coEvery { mealDao.deleteMealById(mealId) } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteMeal(mealId)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.deleteMeal", 
                mapOf("mealId" to mealId)
            ) 
        }
    }

    @Test
    fun `addMeals logs error with meal count`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 0, name = "Meal 1", ingredients = emptyList()),
            Meal(id = 0, name = "Meal 2", ingredients = emptyList())
        )
        every { mealValidator.validate(any()) } returns ValidationResult.Valid
        coEvery { mealDao.insertMeals(any()) } throws RuntimeException("Batch insert failed")

        // When
        val result = repository.addMeals(meals)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.addMeals", 
                mapOf("mealCount" to meals.size)
            ) 
        }
    }

    @Test
    fun `getMealsPaginated logs error with pagination context`() = runTest {
        // Given
        val limit = 10
        val offset = 20
        val exception = RuntimeException("Pagination query failed")
        every { mealDao.getMealsPaginated(limit, offset) } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When
        val result = repository.getMealsPaginated(limit, offset).first()

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealRepositoryImpl.getMealsPaginated", 
                mapOf("limit" to limit, "offset" to offset)
            ) 
        }
    }

    @Test
    fun `error includes cause parameter in PersistenceError`() = runTest {
        // Given
        val meal = Meal(id = 0, name = "Test Meal", ingredients = emptyList())
        val cause = RuntimeException("Root cause")
        every { mealValidator.validate(meal) } returns ValidationResult.Valid
        coEvery { mealDao.insertMeal(any()) } throws cause

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as PersistenceError.WriteFailed
        assertTrue(error.cause == cause)
    }
}
