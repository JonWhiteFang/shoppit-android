package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.validator.IngredientValidator
import com.shoppit.app.domain.validator.MealValidator
import com.shoppit.app.domain.validator.ValidationException
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UpdateMealUseCase.
 * Tests validation and updating of meals.
 */
@ExperimentalCoroutinesApi
class UpdateMealUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var ingredientValidator: IngredientValidator
    private lateinit var validator: MealValidator
    private lateinit var useCase: UpdateMealUseCase

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        ingredientValidator = IngredientValidator()
        validator = MealValidator(ingredientValidator)
        useCase = UpdateMealUseCase(repository, validator)
    }

    @Test
    fun `validates before updating meal`() = runTest {
        // Given - existing meal in repository
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))

        // Invalid update with empty name
        val updatedMeal = existingMeal.copy(name = "")

        // When
        val result = useCase(updatedMeal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Meal name cannot be empty", result.exceptionOrNull()?.message)
        // Verify meal was not updated in repository
        assertEquals("Pasta Carbonara", repository.getMealsList()[0].name)
    }

    @Test
    fun `updates valid meal in repository`() = runTest {
        // Given - existing meal in repository
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))

        // Valid update
        val updatedMeal = existingMeal.copy(
            name = "Spaghetti Carbonara",
            ingredients = listOf(
                Ingredient(name = "Spaghetti", quantity = "400", unit = "g"),
                Ingredient(name = "Eggs", quantity = "4", unit = "pcs")
            )
        )

        // When
        val result = useCase(updatedMeal)

        // Then
        assertTrue(result.isSuccess)
        // Verify meal was updated in repository
        assertEquals(1, repository.getMealsList().size)
        assertEquals("Spaghetti Carbonara", repository.getMealsList()[0].name)
        assertEquals(2, repository.getMealsList()[0].ingredients.size)
    }

    @Test
    fun `returns error when meal has empty ingredient list`() = runTest {
        // Given - existing meal in repository
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))

        // Invalid update with empty ingredients
        val updatedMeal = existingMeal.copy(ingredients = emptyList())

        // When
        val result = useCase(updatedMeal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Meal must have at least one ingredient", result.exceptionOrNull()?.message)
        // Verify meal was not updated
        assertEquals(1, repository.getMealsList()[0].ingredients.size)
    }

    @Test
    fun `returns error when ingredient has empty name`() = runTest {
        // Given - existing meal in repository
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))

        // Invalid update with empty ingredient name
        val updatedMeal = existingMeal.copy(
            ingredients = listOf(
                Ingredient(name = "Pasta"),
                Ingredient(name = "")
            )
        )

        // When
        val result = useCase(updatedMeal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Ingredient name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given - existing meal in repository
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))
        repository.setShouldFail(true, Exception("Database error"))

        val updatedMeal = existingMeal.copy(name = "Updated Name")

        // When
        val result = useCase(updatedMeal)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
