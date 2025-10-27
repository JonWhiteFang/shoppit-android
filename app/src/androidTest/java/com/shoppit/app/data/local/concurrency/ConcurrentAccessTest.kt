package com.shoppit.app.data.local.concurrency

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.data.local.database.ShoppitDatabase
import com.shoppit.app.data.local.entity.MealEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for concurrent database access.
 * Tests that the database handles multiple simultaneous operations correctly.
 */
@RunWith(AndroidJUnit4::class)
class ConcurrentAccessTest {
    
    private lateinit var database: ShoppitDatabase
    
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
    fun `concurrent reads do not block each other`() = runTest {
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
        
        // When - Perform 10 concurrent reads
        val jobs = List(10) {
            async(Dispatchers.IO) {
                mealDao.getMealById(1)
            }
        }
        
        val results = jobs.awaitAll()
        
        // Then - All reads should succeed
        assertEquals(10, results.size)
        results.forEach { flow ->
            assertNotNull(flow)
        }
    }
    
    @Test
    fun `concurrent writes are serialized correctly`() = runTest {
        // Given
        val mealDao = database.mealDao()
        
        // When - Perform 10 concurrent inserts
        val jobs = List(10) { index ->
            async(Dispatchers.IO) {
                val meal = MealEntity(
                    id = 0,
                    name = "Meal $index",
                    ingredients = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                mealDao.insertMeal(meal)
            }
        }
        
        val ids = jobs.awaitAll()
        
        // Then - All inserts should succeed with unique IDs
        assertEquals(10, ids.size)
        assertEquals(10, ids.distinct().size) // All IDs should be unique
    }
    
    @Test
    fun `concurrent read and write operations work correctly`() = runTest {
        // Given
        val mealDao = database.mealDao()
        val initialMeal = MealEntity(
            id = 1,
            name = "Initial Meal",
            ingredients = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        mealDao.insertMeal(initialMeal)
        
        // When - Perform concurrent reads and writes
        val readJobs = List(5) {
            async(Dispatchers.IO) {
                mealDao.getMealById(1)
            }
        }
        
        val writeJobs = List(5) { index ->
            async(Dispatchers.IO) {
                val meal = MealEntity(
                    id = 0,
                    name = "New Meal $index",
                    ingredients = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                mealDao.insertMeal(meal)
            }
        }
        
        // Then - All operations should complete successfully
        val readResults = readJobs.awaitAll()
        val writeResults = writeJobs.awaitAll()
        
        assertEquals(5, readResults.size)
        assertEquals(5, writeResults.size)
    }
    
    @Test
    fun `concurrent updates to same record are handled correctly`() = runTest {
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
        
        // When - Perform 10 concurrent updates to the same record
        val jobs = List(10) { index ->
            async(Dispatchers.IO) {
                val updatedMeal = meal.copy(
                    name = "Updated Name $index",
                    updatedAt = System.currentTimeMillis()
                )
                mealDao.updateMeal(updatedMeal)
            }
        }
        
        jobs.awaitAll()
        
        // Then - The record should exist and have one of the updated names
        val cursor = database.openHelper.writableDatabase.query(
            "SELECT name FROM meals WHERE id = 1"
        )
        
        assertTrue(cursor.moveToFirst())
        val name = cursor.getString(0)
        assertTrue(name.startsWith("Updated Name"))
        cursor.close()
    }
    
    @Test
    fun `concurrent deletes do not cause errors`() = runTest {
        // Given
        val mealDao = database.mealDao()
        
        // Insert 10 meals
        val ids = List(10) { index ->
            val meal = MealEntity(
                id = 0,
                name = "Meal $index",
                ingredients = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            mealDao.insertMeal(meal)
        }
        
        // When - Perform concurrent deletes
        val jobs = ids.map { id ->
            async(Dispatchers.IO) {
                mealDao.deleteMealById(id)
            }
        }
        
        jobs.awaitAll()
        
        // Then - All meals should be deleted
        val cursor = database.openHelper.writableDatabase.query(
            "SELECT COUNT(*) FROM meals"
        )
        
        assertTrue(cursor.moveToFirst())
        assertEquals(0, cursor.getInt(0))
        cursor.close()
    }
    
    @Test
    fun `database handles high concurrent load`() = runTest {
        // Given
        val mealDao = database.mealDao()
        
        // When - Perform 100 concurrent operations (mix of reads and writes)
        val jobs = List(100) { index ->
            async(Dispatchers.IO) {
                if (index % 2 == 0) {
                    // Write operation
                    val meal = MealEntity(
                        id = 0,
                        name = "Meal $index",
                        ingredients = emptyList(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    mealDao.insertMeal(meal)
                } else {
                    // Read operation
                    mealDao.getAllMeals()
                }
            }
        }
        
        // Then - All operations should complete without errors
        val results = jobs.awaitAll()
        assertEquals(100, results.size)
    }
}
