package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FilterMealsByTagsUseCase.
 * Tests tag-based filtering with single and multiple tags.
 *
 * Requirements:
 * - 3.1: Display only meals with corresponding tag when filter selected
 * - 3.2: Remove filter and update results when filter deselected
 * - 3.3: Support multiple simultaneous active filters
 * - 3.4: Display meals matching all selected filters (AND logic)
 */
class FilterMealsByTagsUseCaseTest {

    private lateinit var useCase: FilterMealsByTagsUseCase
    private lateinit var testMeals: List<Meal>

    @Before
    fun setUp() {
        useCase = FilterMealsByTagsUseCase()
        
        // Create test meals with various tag combinations
        testMeals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta")),
                tags = setOf(MealTag.DINNER)
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(Ingredient(name = "Lettuce")),
                tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN)
            ),
            Meal(
                id = 3,
                name = "Vegan Buddha Bowl",
                ingredients = listOf(Ingredient(name = "Quinoa")),
                tags = setOf(MealTag.LUNCH, MealTag.VEGAN, MealTag.HEALTHY)
            ),
            Meal(
                id = 4,
                name = "Quick Breakfast Smoothie",
                ingredients = listOf(Ingredient(name = "Banana")),
                tags = setOf(MealTag.BREAKFAST, MealTag.QUICK, MealTag.HEALTHY)
            ),
            Meal(
                id = 5,
                name = "Gluten-Free Pizza",
                ingredients = listOf(Ingredient(name = "Rice Flour")),
                tags = setOf(MealTag.DINNER, MealTag.GLUTEN_FREE)
            ),
            Meal(
                id = 6,
                name = "Chocolate Cake",
                ingredients = listOf(Ingredient(name = "Chocolate")),
                tags = setOf(MealTag.DESSERT)
            ),
            Meal(
                id = 7,
                name = "Simple Pasta",
                ingredients = listOf(Ingredient(name = "Pasta")),
                tags = emptySet() // No tags
            )
        )
    }

    @Test
    fun `filter by single tag returns meals with that tag`() {
        // When - filter by DINNER tag
        val result = useCase(testMeals, setOf(MealTag.DINNER))

        // Then - should return meals with DINNER tag
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Gluten-Free Pizza" })
    }

    @Test
    fun `filter by BREAKFAST tag returns breakfast meals`() {
        // When - filter by BREAKFAST tag
        val result = useCase(testMeals, setOf(MealTag.BREAKFAST))

        // Then - should return breakfast meals
        assertEquals(1, result.size)
        assertEquals("Quick Breakfast Smoothie", result[0].name)
    }

    @Test
    fun `filter by LUNCH tag returns lunch meals`() {
        // When - filter by LUNCH tag
        val result = useCase(testMeals, setOf(MealTag.LUNCH))

        // Then - should return lunch meals
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Caesar Salad" })
        assertTrue(result.any { it.name == "Vegan Buddha Bowl" })
    }

    @Test
    fun `filter by VEGETARIAN tag returns vegetarian meals`() {
        // When - filter by VEGETARIAN tag
        val result = useCase(testMeals, setOf(MealTag.VEGETARIAN))

        // Then - should return vegetarian meals
        assertEquals(1, result.size)
        assertEquals("Caesar Salad", result[0].name)
    }

    @Test
    fun `filter by VEGAN tag returns vegan meals`() {
        // When - filter by VEGAN tag
        val result = useCase(testMeals, setOf(MealTag.VEGAN))

        // Then - should return vegan meals
        assertEquals(1, result.size)
        assertEquals("Vegan Buddha Bowl", result[0].name)
    }

    @Test
    fun `filter by DESSERT tag returns dessert meals`() {
        // When - filter by DESSERT tag
        val result = useCase(testMeals, setOf(MealTag.DESSERT))

        // Then - should return dessert meals
        assertEquals(1, result.size)
        assertEquals("Chocolate Cake", result[0].name)
    }

    @Test
    fun `filter by multiple tags uses AND logic`() {
        // When - filter by LUNCH AND VEGETARIAN
        val result = useCase(testMeals, setOf(MealTag.LUNCH, MealTag.VEGETARIAN))

        // Then - should return only meals with BOTH tags
        assertEquals(1, result.size)
        assertEquals("Caesar Salad", result[0].name)
    }

    @Test
    fun `filter by multiple tags returns meals with all tags`() {
        // When - filter by LUNCH AND VEGAN AND HEALTHY
        val result = useCase(testMeals, setOf(MealTag.LUNCH, MealTag.VEGAN, MealTag.HEALTHY))

        // Then - should return only meals with ALL three tags
        assertEquals(1, result.size)
        assertEquals("Vegan Buddha Bowl", result[0].name)
    }

    @Test
    fun `filter by multiple tags with no matching meals returns empty list`() {
        // When - filter by BREAKFAST AND VEGAN (no meal has both)
        val result = useCase(testMeals, setOf(MealTag.BREAKFAST, MealTag.VEGAN))

        // Then - should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter by QUICK and HEALTHY tags returns matching meals`() {
        // When - filter by QUICK AND HEALTHY
        val result = useCase(testMeals, setOf(MealTag.QUICK, MealTag.HEALTHY))

        // Then - should return meals with both tags
        assertEquals(1, result.size)
        assertEquals("Quick Breakfast Smoothie", result[0].name)
    }

    @Test
    fun `empty tag set returns all meals`() {
        // When - filter with empty tag set
        val result = useCase(testMeals, emptySet())

        // Then - should return all meals
        assertEquals(7, result.size)
    }

    @Test
    fun `filter excludes meals without the selected tag`() {
        // When - filter by GLUTEN_FREE
        val result = useCase(testMeals, setOf(MealTag.GLUTEN_FREE))

        // Then - should only return gluten-free meal
        assertEquals(1, result.size)
        assertEquals("Gluten-Free Pizza", result[0].name)
        // Should not include meals without the tag
        assertTrue(result.none { it.name == "Pasta Carbonara" })
        assertTrue(result.none { it.name == "Caesar Salad" })
    }

    @Test
    fun `filter on empty meal list returns empty list`() {
        // When - filter on empty list
        val result = useCase(emptyList(), setOf(MealTag.DINNER))

        // Then - should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter by tag that no meal has returns empty list`() {
        // When - filter by DAIRY_FREE (no meal has this tag)
        val result = useCase(testMeals, setOf(MealTag.DAIRY_FREE))

        // Then - should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `meals without tags are excluded when filtering`() {
        // When - filter by any tag
        val result = useCase(testMeals, setOf(MealTag.DINNER))

        // Then - should not include meal with no tags
        assertTrue(result.none { it.name == "Simple Pasta" })
    }

    @Test
    fun `filter by HEALTHY tag returns all healthy meals`() {
        // When - filter by HEALTHY
        val result = useCase(testMeals, setOf(MealTag.HEALTHY))

        // Then - should return meals with HEALTHY tag
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Vegan Buddha Bowl" })
        assertTrue(result.any { it.name == "Quick Breakfast Smoothie" })
    }

    @Test
    fun `filter preserves meal order`() {
        // When - filter by LUNCH
        val result = useCase(testMeals, setOf(MealTag.LUNCH))

        // Then - should preserve original order
        assertEquals(2, result.size)
        assertEquals("Caesar Salad", result[0].name) // ID 2
        assertEquals("Vegan Buddha Bowl", result[1].name) // ID 3
    }
}
