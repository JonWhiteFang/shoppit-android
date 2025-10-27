package com.shoppit.app.data.local.performance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.data.local.database.ShoppitDatabase
import com.shoppit.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for database performance.
 * Measures query execution times and ensures they meet performance thresholds.
 */
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {
    
    private lateinit var database: ShoppitDatabase
    
    companion object {
        private const val QUERY_THRESHOLD_MS = 100L
        private const val BATCH_INSERT_THRESHOLD_MS = 500L
    }
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ShoppitDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `single insert completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meal = MealEntity(
            id = 0,
            name = "Test Meal",
            ingredients = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.insertMeal(meal)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Insert took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `single query completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meal = MealEntity(
            id = 1,
            name = "Test Meal",
            ingredients = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        mealDao.insertMeal(meal)
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.getMealById(1).first()
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Query took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `batch insert of 100 records completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meals = List(100) { index ->
            MealEntity(
                id = 0,
                name = "Meal $index",
                ingredients = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.insertMeals(meals)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Batch insert took ${duration}ms, expected < ${BATCH_INSERT_THRESHOLD_MS}ms", 
            duration < BATCH_INSERT_THRESHOLD_MS)
    }
    
    @Test
    fun `query all records completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meals = List(50) { index ->
            MealEntity(
                id = 0,
                name = "Meal $index",
                ingredients = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        mealDao.insertMeals(meals)
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.getAllMeals().first()
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Query all took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `update operation completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meal = MealEntity(
            id = 1,
            name = "Original Name",
            ingredients = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        mealDao.insertMeal(meal)
        
        // When
        val updatedMeal = meal.copy(name = "Updated Name")
        val startTime = System.currentTimeMillis()
        mealDao.updateMeal(updatedMeal)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Update took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `delete operation completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meal = MealEntity(
            id = 1,
            name = "Test Meal",
            ingredients = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        mealDao.insertMeal(meal)
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.deleteMealById(1)
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Delete took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `paginated query completes within threshold`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val meals = List(100) { index ->
            MealEntity(
                id = 0,
                name = "Meal $index",
                ingredients = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        mealDao.insertMeals(meals)
        
        // When
        val startTime = System.currentTimeMillis()
        mealDao.getMealsPaginated(limit = 50, offset = 0).first()
        val duration = System.currentTimeMillis() - startTime
        
        // Then
        assertTrue("Paginated query took ${duration}ms, expected < ${QUERY_THRESHOLD_MS}ms", 
            duration < QUERY_THRESHOLD_MS)
    }
    
    @Test
    fun `database scales well with increasing data size`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val durations = mutableListOf<Long>()
        
        // When - Insert and query at different data sizes
        listOf(10, 50, 100, 200).forEach { size ->
            // Clear database
            database.clearAllTables()
            
            // Insert records
            val meals = List(size) { index ->
                MealEntity(
                    id = 0,
                    name = "Meal $index",
                    ingredients = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            }
            mealDao.insertMeals(meals)
            
            // Measure query time
            val startTime = System.currentTimeMillis()
            mealDao.getAllMeals().first()
            val duration = System.currentTimeMillis() - startTime
            durations.add(duration)
        }
        
        // Then - Query time should not increase dramatically
        // Allow 2x increase for 20x data size
        assertTrue("Query time increased too much with data size", 
            durations.last() < durations.first() * 2)
    }
}
