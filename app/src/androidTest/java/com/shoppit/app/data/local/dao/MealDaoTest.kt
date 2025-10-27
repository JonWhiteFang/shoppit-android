package com.shoppit.app.data.local.dao

import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.util.DatabaseTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Instrumented tests for MealDao.
 * Tests CRUD operations with in-memory database.
 */
@HiltAndroidTest
class MealDaoTest : DatabaseTest() {

    private lateinit var mealDao: MealDao

    override fun setUp() {
        super.setUp()
        mealDao = database.mealDao()
    }

    @Test
    fun insertMeal_andGetAllMeals_returnsInsertedMeal() = runTest {
        // Given
        val meal = createTestMeal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                IngredientEntity("Pasta", "400", "g"),
                IngredientEntity("Eggs", "4", "pcs")
            )
        )

        // When
        mealDao.insertMeal(meal)
        val meals = mealDao.getAllMeals().first()

        // Then
        assertEquals(1, meals.size)
        assertEquals("Pasta Carbonara", meals[0].name)
        assertEquals(2, meals[0].ingredients.size)
        assertEquals("Pasta", meals[0].ingredients[0].name)
    }

    @Test
    fun getMealById_returnsCorrectMeal() = runTest {
        // Given
        val meal1 = createTestMeal(name = "Pasta")
        val meal2 = createTestMeal(name = "Salad")
        
        val id1 = mealDao.insertMeal(meal1)
        val id2 = mealDao.insertMeal(meal2)

        // When
        val retrievedMeal = mealDao.getMealById(id2).first()

        // Then
        assertEquals("Salad", retrievedMeal?.name)
        assertEquals(id2, retrievedMeal?.id)
    }

    @Test
    fun getMealById_withNonExistentId_returnsNull() = runTest {
        // When
        val retrievedMeal = mealDao.getMealById(999L).first()

        // Then
        assertNull(retrievedMeal)
    }

    @Test
    fun updateMeal_modifiesExistingMeal() = runTest {
        // Given
        val meal = createTestMeal(name = "Original Name")
        val id = mealDao.insertMeal(meal)
        
        val updatedMeal = meal.copy(
            id = id,
            name = "Updated Name",
            ingredients = listOf(
                IngredientEntity("New Ingredient", "1", "cup")
            )
        )

        // When
        mealDao.updateMeal(updatedMeal)
        val retrievedMeal = mealDao.getMealById(id).first()

        // Then
        assertEquals("Updated Name", retrievedMeal?.name)
        assertEquals(1, retrievedMeal?.ingredients?.size)
        assertEquals("New Ingredient", retrievedMeal?.ingredients?.get(0)?.name)
    }

    @Test
    fun deleteMealById_removesMeal() = runTest {
        // Given
        val meal = createTestMeal(name = "To Delete")
        val id = mealDao.insertMeal(meal)
        
        // Verify meal exists
        var retrievedMeal = mealDao.getMealById(id).first()
        assertEquals("To Delete", retrievedMeal?.name)

        // When
        mealDao.deleteMealById(id)
        retrievedMeal = mealDao.getMealById(id).first()

        // Then
        assertNull(retrievedMeal)
    }

    @Test
    fun getAllMeals_returnsMealsSortedByName() = runTest {
        // Given
        val meal1 = createTestMeal(name = "Zucchini Pasta")
        val meal2 = createTestMeal(name = "Apple Pie")
        val meal3 = createTestMeal(name = "Mushroom Soup")
        
        mealDao.insertMeal(meal1)
        mealDao.insertMeal(meal2)
        mealDao.insertMeal(meal3)

        // When
        val meals = mealDao.getAllMeals().first()

        // Then
        assertEquals(3, meals.size)
        assertEquals("Apple Pie", meals[0].name)
        assertEquals("Mushroom Soup", meals[1].name)
        assertEquals("Zucchini Pasta", meals[2].name)
    }

    @Test
    fun getAllMeals_withEmptyDatabase_returnsEmptyList() = runTest {
        // When
        val meals = mealDao.getAllMeals().first()

        // Then
        assertTrue(meals.isEmpty())
    }

    @Test
    fun insertMeal_withMultipleIngredients_storesAllIngredients() = runTest {
        // Given
        val ingredients = listOf(
            IngredientEntity("Flour", "2", "cups"),
            IngredientEntity("Sugar", "1", "cup"),
            IngredientEntity("Eggs", "3", "pcs"),
            IngredientEntity("Milk", "250", "ml")
        )
        val meal = createTestMeal(name = "Cake", ingredients = ingredients)

        // When
        val id = mealDao.insertMeal(meal)
        val retrievedMeal = mealDao.getMealById(id).first()

        // Then
        assertEquals(4, retrievedMeal?.ingredients?.size)
        assertEquals("Flour", retrievedMeal?.ingredients?.get(0)?.name)
        assertEquals("2", retrievedMeal?.ingredients?.get(0)?.quantity)
        assertEquals("cups", retrievedMeal?.ingredients?.get(0)?.unit)
    }

    /**
     * Helper function to create test meal entities.
     */
    private fun createTestMeal(
        name: String,
        ingredients: List<IngredientEntity> = listOf(
            IngredientEntity("Test Ingredient", "1", "unit")
        ),
        notes: String = ""
    ): MealEntity {
        val currentTime = System.currentTimeMillis()
        return MealEntity(
            id = 0,
            name = name,
            ingredients = ingredients,
            notes = notes,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }
}
