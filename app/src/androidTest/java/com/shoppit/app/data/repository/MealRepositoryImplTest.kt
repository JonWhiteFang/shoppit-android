package com.shoppit.app.data.repository

import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.domain.error.DatabaseException
import com.shoppit.app.domain.error.NotFoundException
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.util.DatabaseTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Inject

/**
 * Instrumented tests for MealRepositoryImpl.
 * Tests repository operations with real database to verify entity/model mapping
 * and error handling.
 */
@HiltAndroidTest
class MealRepositoryImplTest : DatabaseTest() {

    @Inject
    lateinit var repository: MealRepositoryImpl

    override fun setUp() {
        super.setUp()
        hiltRule.inject()
    }

    @Test
    fun getMeals_mapsEntitiesToDomainModels() = runTest {
        // Given - Insert entities directly into database
        val entity1 = createTestMealEntity(
            name = "Pasta",
            ingredients = listOf(
                IngredientEntity("Spaghetti", "400", "g"),
                IngredientEntity("Tomato Sauce", "1", "jar")
            )
        )
        val entity2 = createTestMealEntity(
            name = "Salad",
            ingredients = listOf(
                IngredientEntity("Lettuce", "1", "head")
            )
        )
        
        database.mealDao().insertMeal(entity1)
        database.mealDao().insertMeal(entity2)

        // When
        val result = repository.getMeals().first()

        // Then
        assertTrue(result.isSuccess)
        val meals = result.getOrNull()!!
        assertEquals(2, meals.size)
        
        // Verify mapping to domain models
        assertEquals("Pasta", meals[0].name)
        assertEquals(2, meals[0].ingredients.size)
        assertEquals("Spaghetti", meals[0].ingredients[0].name)
        assertEquals("400", meals[0].ingredients[0].quantity)
        assertEquals("g", meals[0].ingredients[0].unit)
        
        assertEquals("Salad", meals[1].name)
        assertEquals(1, meals[1].ingredients.size)
    }

    @Test
    fun getMealById_mapsEntityToDomainModel() = runTest {
        // Given
        val entity = createTestMealEntity(
            name = "Pizza",
            ingredients = listOf(
                IngredientEntity("Dough", "1", "ball"),
                IngredientEntity("Cheese", "200", "g")
            ),
            notes = "Homemade pizza"
        )
        val id = database.mealDao().insertMeal(entity)

        // When
        val result = repository.getMealById(id).first()

        // Then
        assertTrue(result.isSuccess)
        val meal = result.getOrNull()!!
        assertEquals("Pizza", meal.name)
        assertEquals("Homemade pizza", meal.notes)
        assertEquals(2, meal.ingredients.size)
        assertEquals("Dough", meal.ingredients[0].name)
    }

    @Test
    fun getMealById_withNonExistentId_returnsNotFoundError() = runTest {
        // When
        val result = repository.getMealById(999L).first()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NotFoundException)
    }

    @Test
    fun addMeal_persistsToDatabase() = runTest {
        // Given
        val meal = createTestMeal(
            name = "Burger",
            ingredients = listOf(
                Ingredient("Bun", "1", "pcs"),
                Ingredient("Patty", "1", "pcs"),
                Ingredient("Lettuce", "2", "leaves")
            )
        )

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isSuccess)
        val mealId = result.getOrNull()!!
        
        // Verify meal was persisted
        val persistedMeal = database.mealDao().getMealById(mealId).first()
        assertEquals("Burger", persistedMeal?.name)
        assertEquals(3, persistedMeal?.ingredients?.size)
        assertEquals("Bun", persistedMeal?.ingredients?.get(0)?.name)
    }

    @Test
    fun addMeal_returnsMealId() = runTest {
        // Given
        val meal = createTestMeal(name = "Soup")

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isSuccess)
        val mealId = result.getOrNull()!!
        assertTrue(mealId > 0)
    }

    @Test
    fun updateMeal_modifiesExistingMeal() = runTest {
        // Given - Add initial meal
        val initialMeal = createTestMeal(
            name = "Original",
            ingredients = listOf(Ingredient("Old Ingredient", "1", "unit"))
        )
        val addResult = repository.addMeal(initialMeal)
        val mealId = addResult.getOrNull()!!
        
        // Create updated meal
        val updatedMeal = initialMeal.copy(
            id = mealId,
            name = "Updated",
            ingredients = listOf(
                Ingredient("New Ingredient 1", "2", "cups"),
                Ingredient("New Ingredient 2", "3", "tbsp")
            ),
            notes = "Updated notes"
        )

        // When
        val updateResult = repository.updateMeal(updatedMeal)

        // Then
        assertTrue(updateResult.isSuccess)
        
        // Verify update was persisted
        val persistedMeal = database.mealDao().getMealById(mealId).first()
        assertEquals("Updated", persistedMeal?.name)
        assertEquals("Updated notes", persistedMeal?.notes)
        assertEquals(2, persistedMeal?.ingredients?.size)
        assertEquals("New Ingredient 1", persistedMeal?.ingredients?.get(0)?.name)
    }

    @Test
    fun deleteMeal_removesFromDatabase() = runTest {
        // Given
        val meal = createTestMeal(name = "To Delete")
        val addResult = repository.addMeal(meal)
        val mealId = addResult.getOrNull()!!
        
        // Verify meal exists
        var persistedMeal = database.mealDao().getMealById(mealId).first()
        assertEquals("To Delete", persistedMeal?.name)

        // When
        val deleteResult = repository.deleteMeal(mealId)

        // Then
        assertTrue(deleteResult.isSuccess)
        
        // Verify meal was deleted
        persistedMeal = database.mealDao().getMealById(mealId).first()
        assertEquals(null, persistedMeal)
    }

    @Test
    fun getMeals_withEmptyDatabase_returnsEmptyList() = runTest {
        // When
        val result = repository.getMeals().first()

        // Then
        assertTrue(result.isSuccess)
        val meals = result.getOrNull()!!
        assertTrue(meals.isEmpty())
    }

    @Test
    fun addMeal_withComplexIngredients_preservesAllData() = runTest {
        // Given
        val meal = createTestMeal(
            name = "Complex Recipe",
            ingredients = listOf(
                Ingredient("Flour", "2.5", "cups"),
                Ingredient("Sugar", "1/2", "cup"),
                Ingredient("Eggs", "3", "large"),
                Ingredient("Vanilla Extract", "1", "tsp"),
                Ingredient("Milk", "250", "ml")
            ),
            notes = "Bake at 350°F for 30 minutes"
        )

        // When
        val result = repository.addMeal(meal)

        // Then
        assertTrue(result.isSuccess)
        val mealId = result.getOrNull()!!
        
        // Verify all data was preserved
        val retrievedResult = repository.getMealById(mealId).first()
        assertTrue(retrievedResult.isSuccess)
        val retrievedMeal = retrievedResult.getOrNull()!!
        
        assertEquals("Complex Recipe", retrievedMeal.name)
        assertEquals("Bake at 350°F for 30 minutes", retrievedMeal.notes)
        assertEquals(5, retrievedMeal.ingredients.size)
        assertEquals("Flour", retrievedMeal.ingredients[0].name)
        assertEquals("2.5", retrievedMeal.ingredients[0].quantity)
        assertEquals("cups", retrievedMeal.ingredients[0].unit)
    }

    /**
     * Helper function to create test meal entities.
     */
    private fun createTestMealEntity(
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

    /**
     * Helper function to create test domain meals.
     */
    private fun createTestMeal(
        name: String,
        ingredients: List<Ingredient> = listOf(
            Ingredient("Test Ingredient", "1", "unit")
        ),
        notes: String = ""
    ): Meal {
        return Meal(
            id = 0,
            name = name,
            ingredients = ingredients,
            notes = notes
        )
    }
}
