package com.shoppit.app.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.MealPlanDao
import com.shoppit.app.data.local.entity.MealPlanEntity
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.repository.SyncEngine
import io.mockk.coEvery
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
import java.time.LocalDate

/**
 * Unit tests for error logging in MealPlanRepositoryImpl.
 * Tests verify that ErrorLogger is called when exceptions occur.
 */
@ExperimentalCoroutinesApi
class MealPlanRepositoryImplErrorLoggingTest {

    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var syncEngine: SyncEngine
    private lateinit var errorLogger: ErrorLogger
    private lateinit var repository: MealPlanRepositoryImpl

    @Before
    fun setup() {
        mealPlanDao = mockk()
        syncEngine = mockk(relaxed = true)
        errorLogger = mockk(relaxed = true)
        
        repository = MealPlanRepositoryImpl(
            mealPlanDao,
            syncEngine,
            errorLogger
        )
    }

    @Test
    fun `getMealPlansForWeek logs error with date context`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 1, 1)
        val endDate = LocalDate.of(2025, 1, 7)
        val exception = RuntimeException("Query failed")
        every { 
            mealPlanDao.getMealPlansForWeek(startDate.toEpochDay(), endDate.toEpochDay()) 
        } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When
        val result = repository.getMealPlansForWeek(startDate, endDate).first()

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.getMealPlansForWeek", 
                mapOf("startDate" to startDate, "endDate" to endDate)
            ) 
        }
    }

    @Test
    fun `getMealPlansForDate logs error with date context`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 1)
        val exception = RuntimeException("Query failed")
        every { 
            mealPlanDao.getMealPlansForDate(date.toEpochDay()) 
        } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When
        val result = repository.getMealPlansForDate(date).first()

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.getMealPlansForDate", 
                mapOf("date" to date)
            ) 
        }
    }

    @Test
    fun `getMealPlanById logs error with meal plan ID`() = runTest {
        // Given
        val mealPlanId = 123L
        val exception = RuntimeException("Query failed")
        every { mealPlanDao.getMealPlanById(mealPlanId) } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When
        val result = repository.getMealPlanById(mealPlanId).first()

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.getMealPlanById", 
                mapOf("mealPlanId" to mealPlanId)
            ) 
        }
    }

    @Test
    fun `addMealPlan logs error on SQLiteConstraintException`() = runTest {
        // Given
        val mealPlan = MealPlan(
            id = 0,
            mealId = 1,
            date = LocalDate.of(2025, 1, 1),
            mealType = MealType.BREAKFAST
        )
        coEvery { mealPlanDao.insertMealPlan(any()) } throws SQLiteConstraintException("Constraint violation")

        // When
        val result = repository.addMealPlan(mealPlan)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PersistenceError.ConstraintViolation)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.addMealPlan", 
                mapOf("date" to mealPlan.date, "mealType" to mealPlan.mealType)
            ) 
        }
    }

    @Test
    fun `addMealPlan logs error on generic exception`() = runTest {
        // Given
        val mealPlan = MealPlan(
            id = 0,
            mealId = 1,
            date = LocalDate.of(2025, 1, 1),
            mealType = MealType.LUNCH
        )
        coEvery { mealPlanDao.insertMealPlan(any()) } throws RuntimeException("Insert failed")

        // When
        val result = repository.addMealPlan(mealPlan)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PersistenceError.WriteFailed)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.addMealPlan", 
                mapOf("date" to mealPlan.date, "mealType" to mealPlan.mealType)
            ) 
        }
    }

    @Test
    fun `updateMealPlan logs error with full context`() = runTest {
        // Given
        val mealPlan = MealPlan(
            id = 1,
            mealId = 2,
            date = LocalDate.of(2025, 1, 1),
            mealType = MealType.DINNER
        )
        coEvery { mealPlanDao.updateMealPlan(any()) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateMealPlan(mealPlan)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.updateMealPlan", 
                mapOf(
                    "mealPlanId" to mealPlan.id, 
                    "date" to mealPlan.date, 
                    "mealType" to mealPlan.mealType
                )
            ) 
        }
    }

    @Test
    fun `deleteMealPlan logs error with meal plan ID`() = runTest {
        // Given
        val mealPlanId = 456L
        coEvery { mealPlanDao.deleteMealPlanById(mealPlanId) } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteMealPlan(mealPlanId)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.deleteMealPlan", 
                mapOf("mealPlanId" to mealPlanId)
            ) 
        }
    }

    @Test
    fun `deleteMealPlansForDate logs error with date`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 1)
        coEvery { mealPlanDao.deleteMealPlansForDate(date.toEpochDay()) } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteMealPlansForDate(date)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.deleteMealPlansForDate", 
                mapOf("date" to date)
            ) 
        }
    }

    @Test
    fun `addMealPlans logs error with meal plan count`() = runTest {
        // Given
        val mealPlans = listOf(
            MealPlan(id = 0, mealId = 1, date = LocalDate.of(2025, 1, 1), mealType = MealType.BREAKFAST),
            MealPlan(id = 0, mealId = 2, date = LocalDate.of(2025, 1, 1), mealType = MealType.LUNCH)
        )
        coEvery { mealPlanDao.insertMealPlans(any()) } throws RuntimeException("Batch insert failed")

        // When
        val result = repository.addMealPlans(mealPlans)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "MealPlanRepositoryImpl.addMealPlans", 
                mapOf("mealPlanCount" to mealPlans.size)
            ) 
        }
    }

    @Test
    fun `error includes cause parameter in PersistenceError`() = runTest {
        // Given
        val mealPlan = MealPlan(
            id = 0,
            mealId = 1,
            date = LocalDate.of(2025, 1, 1),
            mealType = MealType.BREAKFAST
        )
        val cause = RuntimeException("Root cause")
        coEvery { mealPlanDao.insertMealPlan(any()) } throws cause

        // When
        val result = repository.addMealPlan(mealPlan)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as PersistenceError.WriteFailed
        assertTrue(error.cause == cause)
    }
}
