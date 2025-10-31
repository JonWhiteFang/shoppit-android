package com.shoppit.app.domain.validator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MealValidatorTest {
    
    private lateinit var ingredientValidator: IngredientValidator
    private lateinit var validator: MealValidator
    
    @Before
    fun setup() {
        ingredientValidator = IngredientValidator()
        validator = MealValidator(ingredientValidator)
    }
    
    @Test
    fun `validate returns Valid for valid meal`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Spaghetti Carbonara",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("Eggs", "4", "pcs")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isValid())
        assertTrue(result is ValidationResult.Valid)
    }
    
    @Test
    fun `validate returns Invalid when meal name is empty`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].field)
        assertEquals("Meal name cannot be empty", errors[0].message)
        assertEquals(ValidationError.CODE_REQUIRED, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when meal name is blank`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "   ",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].field)
        assertEquals("Meal name cannot be empty", errors[0].message)
    }
    
    @Test
    fun `validate returns Invalid when meal name exceeds 100 characters`() {
        // Given
        val longName = "a".repeat(101)
        val meal = Meal(
            id = 1,
            name = longName,
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].field)
        assertEquals("Meal name cannot exceed 100 characters", errors[0].message)
        assertEquals(ValidationError.CODE_TOO_LONG, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when meal has no ingredients`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Empty Meal",
            ingredients = emptyList()
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("ingredients", errors[0].field)
        assertEquals("Meal must have at least one ingredient", errors[0].message)
        assertEquals(ValidationError.CODE_REQUIRED, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when ingredient name is empty`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("", "400", "g")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("ingredients[0].name", errors[0].field)
        assertEquals("Ingredient name cannot be empty", errors[0].message)
    }
    
    @Test
    fun `validate returns Valid when ingredient quantity is empty`() {
        // Given - quantity is optional
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "", "g")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validate returns Valid when ingredient unit is empty`() {
        // Given - unit is optional
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "400", "")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validate returns all errors for multiple invalid fields`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "",
            ingredients = listOf(
                Ingredient("", "", ""),
                Ingredient("Eggs", "4", "pcs")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(2, errors.size) // name + ingredient name (quantity and unit are optional)
        
        val errorFields = errors.map { it.field }
        assertTrue(errorFields.contains("name"))
        assertTrue(errorFields.contains("ingredients[0].name"))
    }
    
    @Test
    fun `validate handles multiple ingredients with mixed validity`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("", "4", "pcs"),
                Ingredient("Salt", "", "tsp") // quantity is optional
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size) // Only ingredient name is required
        assertEquals("ingredients[1].name", errors[0].field)
    }
    
    @Test
    fun `validateLegacy returns success for valid meal`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = validator.validateLegacy(meal)
        
        // Then
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `validateLegacy returns failure for invalid meal`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = validator.validateLegacy(meal)
        
        // Then
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `validate returns Invalid when ingredient has invalid quantity`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "abc", "g") // Non-numeric quantity
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("ingredients[0].quantity", errors[0].field)
        assertEquals("Quantity must be a valid number", errors[0].message)
        assertEquals(ValidationError.CODE_INVALID_FORMAT, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when ingredient has zero quantity`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "0", "g")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("ingredients[0].quantity", errors[0].field)
        assertEquals("Quantity must be greater than zero", errors[0].message)
        assertEquals(ValidationError.CODE_OUT_OF_RANGE, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when ingredient has negative quantity`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "-5", "g")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("ingredients[0].quantity", errors[0].field)
        assertEquals("Quantity must be greater than zero", errors[0].message)
    }
    
    @Test
    fun `validate returns all errors for meal with multiple invalid ingredients`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("", "abc", "g"),      // Empty name + invalid quantity
                Ingredient("Eggs", "-2", "pcs"),  // Negative quantity
                Ingredient("Salt", "0", "tsp")    // Zero quantity
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(5, errors.size) // name + quantity for first, quantity for second and third
        
        val errorFields = errors.map { it.field }
        assertTrue(errorFields.contains("ingredients[0].name"))
        assertTrue(errorFields.contains("ingredients[0].quantity"))
        assertTrue(errorFields.contains("ingredients[1].quantity"))
        assertTrue(errorFields.contains("ingredients[2].quantity"))
    }
    
    @Test
    fun `validate returns Valid for meal with valid ingredients including quantities`() {
        // Given
        val meal = Meal(
            id = 1,
            name = "Test Meal",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("Eggs", "4", "pcs"),
                Ingredient("Salt", "1.5", "tsp")
            )
        )
        
        // When
        val result = validator.validate(meal)
        
        // Then
        assertTrue(result.isValid())
    }
}
