package com.shoppit.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for MealType enum.
 * Tests display name formatting and enum completeness.
 */
class MealTypeTest {

    @Test
    fun `displayName returns correct string for BREAKFAST`() {
        assertEquals("Breakfast", MealType.BREAKFAST.displayName())
    }

    @Test
    fun `displayName returns correct string for LUNCH`() {
        assertEquals("Lunch", MealType.LUNCH.displayName())
    }

    @Test
    fun `displayName returns correct string for DINNER`() {
        assertEquals("Dinner", MealType.DINNER.displayName())
    }

    @Test
    fun `displayName returns correct string for SNACK`() {
        assertEquals("Snack", MealType.SNACK.displayName())
    }

    @Test
    fun `all enum values are defined`() {
        val values = MealType.values()
        assertEquals(4, values.size)
        assertEquals(MealType.BREAKFAST, values[0])
        assertEquals(MealType.LUNCH, values[1])
        assertEquals(MealType.DINNER, values[2])
        assertEquals(MealType.SNACK, values[3])
    }
}
