package com.shoppit.app.domain.validator

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MealValidator.
 * Tests validation logic for meal business rules.
 */
class MealValidatorTest {

    private lateinit var validator: MealValidator

    @Before
    fun setUp() {
        validator = MealValidator()
    }

    @Test
    fun `validation passes for valid meal`() {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "Eggs", quantity = "4", unit = "pcs")
            )
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validation fails for empty meal name`() {
        // Given
        val meal = Meal(
            name = "",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Meal name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation fails for blank meal name with whitespace`() {
        // Given
        val meal = Meal(
            name = "   ",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Meal name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation fails for empty ingredient list`() {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = emptyList()
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Meal must have at least one ingredient", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation fails for ingredient with empty name`() {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "", quantity = "4", unit = "pcs")
            )
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Ingredient name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation fails for ingredient with blank name`() {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "  ", quantity = "4", unit = "pcs")
            )
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        assertEquals("Ingredient name cannot be empty", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validation passes for ingredient with empty quantity and unit`() {
        // Given
        val meal = Meal(
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta") // quantity and unit are optional
            )
        )

        // When
        val result = validator.validate(meal)

        // Then
        assertTrue(result.isSuccess)
    }
}
