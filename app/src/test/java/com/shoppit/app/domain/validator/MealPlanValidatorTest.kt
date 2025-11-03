package com.shoppit.app.domain.validator

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for MealPlanValidator.
 * Tests validation rules for meal plan domain objects.
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
class MealPlanValidatorTest {

    private lateinit var validator: MealPlanValidator

    @Before
    fun setup() {
        validator = MealPlanValidator()
    }

    @Test
    fun `validate returns Valid for valid meal plan`() {
        // Given - meal plan with future date
        val mealPlan = MealPlan(
            id = 1,
            mealId = 100,
            date = LocalDate.now().plusDays(1),
            mealType = MealType.LUNCH
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isValid())
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validate returns Valid for meal plan with today's date`() {
        // Given - meal plan with today's date (not in the past)
        val mealPlan = MealPlan(
            id = 1,
            mealId = 100,
            date = LocalDate.now(),
            mealType = MealType.BREAKFAST
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isValid())
    }

    @Test
    fun `validate returns Invalid when date is in the past`() {
        // Given - meal plan with past date
        val mealPlan = MealPlan(
            id = 1,
            mealId = 100,
            date = LocalDate.now().minusDays(1),
            mealType = MealType.DINNER
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("date", errors[0].field)
        assertEquals("Date cannot be in the past", errors[0].message)
        assertEquals(ValidationError.CODE_INVALID_VALUE, errors[0].code)
    }

    @Test
    fun `validate returns Invalid when meal ID is zero`() {
        // Given - meal plan with invalid meal ID
        val mealPlan = MealPlan(
            id = 1,
            mealId = 0,
            date = LocalDate.now().plusDays(1),
            mealType = MealType.SNACK
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("mealId", errors[0].field)
        assertEquals("Meal ID must be positive", errors[0].message)
        assertEquals(ValidationError.CODE_INVALID_VALUE, errors[0].code)
    }

    @Test
    fun `validate returns Invalid when meal ID is negative`() {
        // Given - meal plan with negative meal ID
        val mealPlan = MealPlan(
            id = 1,
            mealId = -5,
            date = LocalDate.now().plusDays(1),
            mealType = MealType.BREAKFAST
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(1, errors.size)
        assertEquals("mealId", errors[0].field)
        assertEquals("Meal ID must be positive", errors[0].message)
        assertEquals(ValidationError.CODE_INVALID_VALUE, errors[0].code)
    }

    @Test
    fun `validate returns Valid for all meal types`() {
        // Given - meal plans with each meal type
        val mealTypes = listOf(
            MealType.BREAKFAST,
            MealType.LUNCH,
            MealType.DINNER,
            MealType.SNACK
        )

        mealTypes.forEach { mealType ->
            val mealPlan = MealPlan(
                id = 1,
                mealId = 100,
                date = LocalDate.now().plusDays(1),
                mealType = mealType
            )

            // When
            val result = validator.validate(mealPlan)

            // Then
            assertTrue("Meal type $mealType should be valid", result.isValid())
        }
    }

    @Test
    fun `validate returns all errors for multiple invalid fields`() {
        // Given - meal plan with multiple invalid fields
        val mealPlan = MealPlan(
            id = 1,
            mealId = -1,
            date = LocalDate.now().minusDays(5),
            mealType = MealType.LUNCH
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertEquals(2, errors.size)

        val errorFields = errors.map { it.field }
        assertTrue(errorFields.contains("date"))
        assertTrue(errorFields.contains("mealId"))
    }

    @Test
    fun `validate returns Valid for meal plan far in the future`() {
        // Given - meal plan with date far in the future
        val mealPlan = MealPlan(
            id = 1,
            mealId = 100,
            date = LocalDate.now().plusYears(1),
            mealType = MealType.DINNER
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isValid())
    }

    @Test
    fun `validate returns Invalid for date one day in the past`() {
        // Given - meal plan with yesterday's date
        val mealPlan = MealPlan(
            id = 1,
            mealId = 100,
            date = LocalDate.now().minusDays(1),
            mealType = MealType.BREAKFAST
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertTrue(errors.any { it.field == "date" })
    }

    @Test
    fun `validate returns Valid for new meal plan with ID zero`() {
        // Given - new meal plan (ID = 0 is valid for new entities)
        val mealPlan = MealPlan(
            id = 0,
            mealId = 100,
            date = LocalDate.now().plusDays(1),
            mealType = MealType.LUNCH
        )

        // When
        val result = validator.validate(mealPlan)

        // Then
        assertTrue(result.isValid())
    }
}
