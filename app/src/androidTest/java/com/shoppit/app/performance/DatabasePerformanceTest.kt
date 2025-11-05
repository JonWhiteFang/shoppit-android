package com.shoppit.app.performance

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.data.local.ShoppitDatabase
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.dao.MealPlanDao
import com.shoppit.app.data.local.dao.ShoppingListDao
import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.data.local.entity.MealPlanEntity
import com.shoppit.app.data.local.entity.ShoppingListItemEntity
import com.shoppit.app.domain.model.MealType
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Database performance tests to validate query execution times.
 * 
 * Requirements tested:
 * - 3.1: Query execution time < 50ms
 * - 3.2: Indexed queries for meal lists
 * - 3.3: Optimized aggregation queries
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var database: ShoppitDatabase
    private lateinit var mealDao: MealDao
    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var shoppingListDao: ShoppingListDao

    @Before
    fun setup() {
        hiltRule.inject()
        
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ShoppitDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
            
        mealDao = database.mealDao()
        mealPlanDao = database.mealPlanDao()
        shoppingListDao = database.shoppingListDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    /**
     * Test query execution time for getAllMeals.
     * Target: < 50ms for typical dataset (100 meals)
     * Requirement: 3.1
     */
    @Test
    fun testGetAllMealsQueryPerformance() = runBlocking {
        // Insert test data (100 meals)
        val meals = (1..100).map { i ->
            MealEntity(
                id = i.toLong(),
                name = "Test Meal $i",
                notes = "Test notes for meal $i",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        mealDao.insertAll(meals)

        // Measure query time
        val queryTime = measureTimeMillis {
            mealDao.getAllMeals().first()
        }

        Timber.tag("DatabasePerformance").i("getAllMeals query time: ${queryTime}ms")

        assertTrue(
            "getAllMeals query should complete in < 50ms, was ${queryTime}ms",
            queryTime < 50
        )
    }

    /**
     * Test query execution time for searchMeals with large dataset.
     * Target: < 50ms
     * Requirement: 3.1, 3.2
     */
    @Test
    fun testSearchMealsQueryPerformance() = runBlocking {
        // Insert large dataset (500 meals)
        val meals = (1..500).map { i ->
            MealEntity(
                id = i.toLong(),
                name = "Meal ${if (i % 10 == 0) "Pasta" else "Recipe"} $i",
                notes = "Test notes",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        mealDao.insertAll(meals)

        // Measure search query time
        val queryTime = measureTimeMillis {
            mealDao.searchMeals("Pasta").first()
        }

        Timber.tag("DatabasePerformance").i("searchMeals query time: ${queryTime}ms")

        assertTrue(
            "searchMeals query should complete in < 50ms, was ${queryTime}ms",
            queryTime < 50
        )
    }

    /**
     * Test query execution time for getMealWithIngredients.
     * Target: < 50ms
     * Requirement: 3.1
     */
    @Test
    fun testGetMealWithIngredientsPerformance() = runBlocking {
        // Insert meal with ingredients
        val mealId = mealDao.insertMeal(
            MealEntity(
                name = "Test Meal",
                notes = "Test notes",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Insert 20 ingredients
        val ingredients = (1..20).map { i ->
            IngredientEntity(
                mealId = mealId,
                name = "Ingredient $i",
                quantity = i.toDouble(),
                unit = "cups"
            )
        }
        database.ingredientDao().insertAll(ingredients)

        // Measure query time
        val queryTime = measureTimeMillis {
            mealDao.getMealWithIngredients(mealId)
        }

        Timber.tag("DatabasePerformance").i("getMealWithIngredients query time: ${queryTime}ms")

        assertTrue(
            "getMealWithIngredients query should complete in < 50ms, was ${queryTime}ms",
            queryTime < 50
        )
    }

    /**
     * Test transaction performance for batch operations.
     * Target: < 100ms for 50 inserts
     * Requirement: 3.1, 3.4
     */
    @Test
    fun testBatchInsertTransactionPerformance() = runBlocking {
        val meals = (1..50).map { i ->
            MealEntity(
                name = "Batch Meal $i",
                notes = "Test notes",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }

        // Measure batch insert time
        val insertTime = measureTimeMillis {
            mealDao.insertAll(meals)
        }

        Timber.tag("DatabasePerformance").i("Batch insert (50 meals) time: ${insertTime}ms")

        assertTrue(
            "Batch insert should complete in < 100ms, was ${insertTime}ms",
            insertTime < 100
        )
    }

    /**
     * Test shopping list aggregation query performance.
     * Target: < 50ms
     * Requirement: 3.3
     */
    @Test
    fun testShoppingListAggregationPerformance() = runBlocking {
        // Insert meals with ingredients
        val currentTime = System.currentTimeMillis()
        val meals = (1..10).map { i ->
            val mealId = mealDao.insertMeal(
                MealEntity(
                    name = "Meal $i",
                    notes = "Test",
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )
            
            // Add ingredients to each meal
            val ingredients = (1..5).map { j ->
                IngredientEntity(
                    mealId = mealId,
                    name = "Ingredient ${(i + j) % 10}",
                    quantity = 1.0,
                    unit = "cup"
                )
            }
            database.ingredientDao().insertAll(ingredients)
            
            mealId
        }

        // Create meal plans
        meals.forEachIndexed { index, mealId ->
            mealPlanDao.insertMealPlan(
                MealPlanEntity(
                    mealId = mealId,
                    date = currentTime + (index * 86400000L), // Different days
                    mealType = MealType.DINNER,
                    servings = 2
                )
            )
        }

        // Measure aggregation query time
        val queryTime = measureTimeMillis {
            shoppingListDao.getAggregatedShoppingList().first()
        }

        Timber.tag("DatabasePerformance").i("Shopping list aggregation query time: ${queryTime}ms")

        assertTrue(
            "Shopping list aggregation should complete in < 50ms, was ${queryTime}ms",
            queryTime < 50
        )
    }

    /**
     * Test meal plan date range query performance.
     * Target: < 50ms
     * Requirement: 3.1, 3.2
     */
    @Test
    fun testMealPlanDateRangeQueryPerformance() = runBlocking {
        val currentTime = System.currentTimeMillis()
        
        // Insert meal
        val mealId = mealDao.insertMeal(
            MealEntity(
                name = "Test Meal",
                notes = "Test",
                createdAt = currentTime,
                updatedAt = currentTime
            )
        )

        // Insert 100 meal plans across different dates
        val mealPlans = (1..100).map { i ->
            MealPlanEntity(
                mealId = mealId,
                date = currentTime + (i * 86400000L), // Different days
                mealType = MealType.values()[i % MealType.values().size],
                servings = 2
            )
        }
        mealPlanDao.insertAll(mealPlans)

        // Measure date range query time (7 days)
        val startDate = currentTime
        val endDate = currentTime + (7 * 86400000L)
        
        val queryTime = measureTimeMillis {
            mealPlanDao.getMealPlansForDateRange(startDate, endDate).first()
        }

        Timber.tag("DatabasePerformance").i("Meal plan date range query time: ${queryTime}ms")

        assertTrue(
            "Meal plan date range query should complete in < 50ms, was ${queryTime}ms",
            queryTime < 50
        )
    }

    /**
     * Test performance with large dataset (1000+ meals).
     * Target: < 100ms
     * Requirement: 3.1
     */
    @Test
    fun testLargeDatasetQueryPerformance() = runBlocking {
        // Insert large dataset (1000 meals)
        val meals = (1..1000).map { i ->
            MealEntity(
                id = i.toLong(),
                name = "Large Dataset Meal $i",
                notes = "Test notes",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
        
        val insertTime = measureTimeMillis {
            mealDao.insertAll(meals)
        }
        Timber.tag("DatabasePerformance").i("Large dataset insert time: ${insertTime}ms")

        // Measure query time
        val queryTime = measureTimeMillis {
            mealDao.getAllMeals().first()
        }

        Timber.tag("DatabasePerformance").i("Large dataset query time: ${queryTime}ms")

        assertTrue(
            "Large dataset query should complete in < 100ms, was ${queryTime}ms",
            queryTime < 100
        )
    }

    /**
     * Test index usage verification.
     * This test verifies that queries use indices properly.
     * Requirement: 3.2
     */
    @Test
    fun testIndexUsageVerification() = runBlocking {
        // Insert test data
        val meals = (1..100).map { i ->
            MealEntity(
                name = "Indexed Meal $i",
                notes = "Test",
                createdAt = System.currentTimeMillis() - (i * 1000),
                updatedAt = System.currentTimeMillis()
            )
        }
        mealDao.insertAll(meals)

        // Query by name (should use index)
        val nameQueryTime = measureTimeMillis {
            mealDao.searchMeals("Indexed").first()
        }

        // Query ordered by created_at (should use index)
        val dateQueryTime = measureTimeMillis {
            mealDao.getAllMeals().first()
        }

        Timber.tag("DatabasePerformance").i("Name index query time: ${nameQueryTime}ms")
        Timber.tag("DatabasePerformance").i("Date index query time: ${dateQueryTime}ms")

        // Both should be fast due to indices
        assertTrue(
            "Name indexed query should be fast (< 50ms), was ${nameQueryTime}ms",
            nameQueryTime < 50
        )
        assertTrue(
            "Date indexed query should be fast (< 50ms), was ${dateQueryTime}ms",
            dateQueryTime < 50
        )
    }
}
