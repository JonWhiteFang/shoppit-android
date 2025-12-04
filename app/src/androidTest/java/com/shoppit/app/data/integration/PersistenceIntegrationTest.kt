package com.shoppit.app.data.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.data.cache.CacheManager
import com.shoppit.app.data.cache.LruCacheManager
import com.shoppit.app.data.local.database.ShoppitDatabase
import com.shoppit.app.data.repository.MealRepositoryImpl
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.validator.MealValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for complete persistence flows.
 * Tests the interaction between repository, cache, and database layers.
 */
@RunWith(AndroidJUnit4::class)
class PersistenceIntegrationTest {
    
    private lateinit var database: ShoppitDatabase
    private lateinit var mealListCache: CacheManager<String, List<Meal>>
    private lateinit var mealDetailCache: CacheManager<Long, Meal>
    private lateinit var repository: MealRepositoryImpl
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ShoppitDatabase::class.java
        )
            .allowMainThreadQueries()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE)
            .build()
        
        mealListCache = LruCacheManager(maxSize = 10)
        mealDetailCache = LruCacheManager(maxSize = 50)
        
        repository = MealRepositoryImpl(
            mealDao = database.mealDao(),
            mealListCache = mealListCache,
            mealDetailCache = mealDetailCache,
            mealValidator = MealValidator()
        )
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun `complete flow - add meal, retrieve from cache, then from database`() = runTest {
        // Given
        val meal = Meal(
            id = 0,
            name = "Spaghetti Carbonara",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("Eggs", "4", "pcs")
            )
        )
        
        // When - Add meal
        val addResult = repository.addMeal(meal)
        assertTrue(addResult.isSuccess)
        val mealId = addResult.getOrNull()!!
        
        // Then - Retrieve from database (cache miss on first load)
        val getResult = repository.getMealById(mealId).first()
        assertTrue(getResult.isSuccess)
        val retrievedMeal = getResult.getOrNull()!!
        assertEquals("Spaghetti Carbonara", retrievedMeal.name)
        
        // And - Second retrieval should hit cache
        val cachedMeal = mealDetailCache.get(mealId)
        assertNotNull(cachedMeal)
        assertEquals("Spaghetti Carbonara", cachedMeal?.name)
    }
    
    @Test
    fun `cache is invalidated on update`() = runTest {
        // Given - Add a meal
        val meal = Meal(
            id = 0,
            name = "Original Name",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        val addResult = repository.addMeal(meal)
        val mealId = addResult.getOrNull()!!
        
        // Load meal to populate cache
        repository.getMealById(mealId).first()
        assertNotNull(mealDetailCache.get(mealId))
        
        // When - Update meal
        val updatedMeal = meal.copy(id = mealId, name = "Updated Name")
        repository.updateMeal(updatedMeal)
        
        // Then - Cache should be invalidated
        assertNull(mealDetailCache.get(mealId))
        
        // And - Fresh data should be retrieved from database
        val getResult = repository.getMealById(mealId).first()
        assertEquals("Updated Name", getResult.getOrNull()?.name)
    }
    
    @Test
    fun `cache is invalidated on delete`() = runTest {
        // Given - Add a meal
        val meal = Meal(
            id = 0,
            name = "Test Meal",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        val addResult = repository.addMeal(meal)
        val mealId = addResult.getOrNull()!!
        
        // Load meal to populate cache
        repository.getMealById(mealId).first()
        assertNotNull(mealDetailCache.get(mealId))
        
        // When - Delete meal
        repository.deleteMeal(mealId)
        
        // Then - Cache should be invalidated
        assertNull(mealDetailCache.get(mealId))
    }
    
    @Test
    fun `validation prevents invalid data from being persisted`() = runTest {
        // Given - Invalid meal (empty name)
        val invalidMeal = Meal(
            id = 0,
            name = "",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = repository.addMeal(invalidMeal)
        
        // Then - Operation should fail
        assertTrue(result.isFailure)
        
        // And - No data should be in database
        val allMeals = repository.getMeals().first()
        assertTrue(allMeals.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun `batch operations work correctly with cache`() = runTest {
        // Given
        val meals = listOf(
            Meal(0, "Meal 1", listOf(Ingredient("Ingredient 1", "1", "unit"))),
            Meal(0, "Meal 2", listOf(Ingredient("Ingredient 2", "2", "unit"))),
            Meal(0, "Meal 3", listOf(Ingredient("Ingredient 3", "3", "unit")))
        )
        
        // When - Add meals in batch
        val result = repository.addMeals(meals)
        assertTrue(result.isSuccess)
        
        // Then - All meals should be retrievable
        val allMeals = repository.getMeals().first()
        assertTrue(allMeals.isSuccess)
        assertEquals(3, allMeals.getOrNull()?.size)
        
        // And - List cache should be populated
        val cachedList = mealListCache.get("all_meals")
        assertNotNull(cachedList)
        assertEquals(3, cachedList?.size)
    }
    
    @Test
    fun `pagination works correctly`() = runTest {
        // Given - Add 10 meals
        val meals = List(10) { index ->
            Meal(
                id = 0,
                name = "Meal $index",
                ingredients = listOf(Ingredient("Ingredient", "1", "unit"))
            )
        }
        repository.addMeals(meals)
        
        // When - Get first page
        val page1 = repository.getMealsPaginated(limit = 5, offset = 0).first()
        assertTrue(page1.isSuccess)
        assertEquals(5, page1.getOrNull()?.size)
        
        // And - Get second page
        val page2 = repository.getMealsPaginated(limit = 5, offset = 5).first()
        assertTrue(page2.isSuccess)
        assertEquals(5, page2.getOrNull()?.size)
        
        // Then - Pages should contain different meals
        val page1Ids = page1.getOrNull()?.map { it.id } ?: emptyList()
        val page2Ids = page2.getOrNull()?.map { it.id } ?: emptyList()
        assertTrue(page1Ids.intersect(page2Ids.toSet()).isEmpty())
    }
    
    @Test
    fun `repository handles database errors gracefully`() = runTest {
        // Given - Close database to simulate error
        database.close()
        
        // When - Try to add meal
        val meal = Meal(
            id = 0,
            name = "Test Meal",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        val result = repository.addMeal(meal)
        
        // Then - Should return failure
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `complete CRUD flow works end-to-end`() = runTest {
        // Create
        val meal = Meal(
            id = 0,
            name = "Test Meal",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        val createResult = repository.addMeal(meal)
        assertTrue(createResult.isSuccess)
        val mealId = createResult.getOrNull()!!
        
        // Read
        val readResult = repository.getMealById(mealId).first()
        assertTrue(readResult.isSuccess)
        assertEquals("Test Meal", readResult.getOrNull()?.name)
        
        // Update
        val updatedMeal = meal.copy(id = mealId, name = "Updated Meal")
        val updateResult = repository.updateMeal(updatedMeal)
        assertTrue(updateResult.isSuccess)
        
        // Verify update
        val verifyResult = repository.getMealById(mealId).first()
        assertEquals("Updated Meal", verifyResult.getOrNull()?.name)
        
        // Delete
        val deleteResult = repository.deleteMeal(mealId)
        assertTrue(deleteResult.isSuccess)
        
        // Verify delete
        val allMeals = repository.getMeals().first()
        assertTrue(allMeals.getOrNull()?.isEmpty() == true)
    }
}
