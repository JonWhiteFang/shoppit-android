package com.shoppit.app.domain.validator

import com.shoppit.app.domain.model.Ingredient
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IngredientValidatorTest {
    
    private lateinit var validator: IngredientValidator
    
    @Before
    fun setup() {
        validator = IngredientValidator()
    }
    
    @Test
    fun `validate returns Valid for valid ingredient`() {
        // Given
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "2",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isValid())
        assertTrue(result is ValidationResult.Valid)
    }
    
    @Test
    fun `validate returns Invalid when ingredient name is empty`() {
        // Given
        val ingredient = Ingredient(
            name = "",
            quantity = "2",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].field)
        assertEquals("Ingredient name cannot be empty", errors[0].message)
        assertEquals(ValidationError.CODE_REQUIRED, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when ingredient name is blank`() {
        // Given
        val ingredient = Ingredient(
            name = "   ",
            quantity = "2",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("name", errors[0].field)
        assertEquals("Ingredient name cannot be empty", errors[0].message)
        assertEquals(ValidationError.CODE_REQUIRED, errors[0].code)
    }
    
    @Test
    fun `validate returns Valid when quantity is empty`() {
        // Given - quantity is optional
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validate returns Invalid when quantity is non-numeric`() {
        // Given
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "abc",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("quantity", errors[0].field)
        assertEquals("Quantity must be a valid number", errors[0].message)
        assertEquals(ValidationError.CODE_INVALID_FORMAT, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when quantity is zero`() {
        // Given
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "0",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("quantity", errors[0].field)
        assertEquals("Quantity must be greater than zero", errors[0].message)
        assertEquals(ValidationError.CODE_OUT_OF_RANGE, errors[0].code)
    }
    
    @Test
    fun `validate returns Invalid when quantity is negative`() {
        // Given
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "-5",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("quantity", errors[0].field)
        assertEquals("Quantity must be greater than zero", errors[0].message)
        assertEquals(ValidationError.CODE_OUT_OF_RANGE, errors[0].code)
    }
    
    @Test
    fun `validate returns Valid when unit is empty`() {
        // Given - unit is optional
        val ingredient = Ingredient(
            name = "Tomato",
            quantity = "2",
            unit = ""
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validate returns all errors for multiple invalid fields`() {
        // Given
        val ingredient = Ingredient(
            name = "",
            quantity = "abc",
            unit = "pcs"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(2, errors.size)
        
        val errorFields = errors.map { it.field }
        assertTrue(errorFields.contains("name"))
        assertTrue(errorFields.contains("quantity"))
    }
    
    @Test
    fun `validate accepts decimal quantities`() {
        // Given
        val ingredient = Ingredient(
            name = "Flour",
            quantity = "2.5",
            unit = "cups"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isValid())
    }
    
    @Test
    fun `validate accepts very small positive quantities`() {
        // Given
        val ingredient = Ingredient(
            name = "Salt",
            quantity = "0.001",
            unit = "tsp"
        )
        
        // When
        val result = validator.validate(ingredient)
        
        // Then
        assertTrue(result.isValid())
    }
}
