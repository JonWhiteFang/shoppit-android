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
 * Unit tests for AddMealUseCase.
 * Tests validation and addition of meals.
 */
@ExperimentalCoroutinesApi
class AddMealUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var ingredientValidator: IngredientValidator
    private lateinit var validator: MealValidator
    private lateinit var useCase: AddMealUseCase

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        ingredientValidator = IngredientValidator()
        validator = MealValidator(ingredientValidator)
        useCase = AddMealUseCase(repository, validator)
    }

    @Test
    fun `validates before adding meal`() = runTest {
        // Given - invalid meal with empty name
        val meal = Meal(
            name = "",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )

        // When
        val result = useCase(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("name: Meal name cannot be empty", result.exceptionOrNull()?.message)
        // Verify meal was not added to repository
        assertTrue(repository.getMealsList().isEmpty())
    }

    @Test
    fun `adds valid meal to repository`() = runTest {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "Eggs", quantity = "4", unit = "pcs")
            )
        )

        // When
        val result = useCase(meal)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        // Verify meal was added to repository
        assertEquals(1, repository.getMealsList().size)
        assertEquals("Pasta Carbonara", repository.getMealsList()[0].name)
    }

    @Test
    fun `returns error when meal has empty ingredient list`() = runTest {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = emptyList()
        )

        // When
        val result = useCase(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("ingredients: Meal must have at least one ingredient", result.exceptionOrNull()?.message)
        assertTrue(repository.getMealsList().isEmpty())
    }

    @Test
    fun `returns error when ingredient has empty name`() = runTest {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta"),
                Ingredient(name = "")
            )
        )

        // When
        val result = useCase(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("ingredients[1].name: Ingredient name cannot be empty", result.exceptionOrNull()?.message)
        assertTrue(repository.getMealsList().isEmpty())
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(meal)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
