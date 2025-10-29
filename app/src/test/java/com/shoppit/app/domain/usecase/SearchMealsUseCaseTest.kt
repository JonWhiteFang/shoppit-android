package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SearchMealsUseCase.
 * Tests search functionality by meal name and ingredient names.
 *
 * Requirements:
 * - 1.1: Filter meals by name containing search text
 * - 1.2: Filter meals by ingredient names containing search text
 * - 1.3: Perform case-insensitive matching
 * - 1.4: Return empty list when no matches found
 * - 2.1: Match search query against ingredient names
 * - 2.2: Match against all ingredients within each meal
 * - 2.3: Perform case-insensitive matching for ingredients
 * - 2.4: Display meal once even if multiple ingredients match
 * - 2.5: Display meals matching by name or ingredient
 */
class SearchMealsUseCaseTest {

    private lateinit var useCase: SearchMealsUseCase
    private lateinit var testMeals: List<Meal>

    @Before
    fun setUp() {
        useCase = SearchMealsUseCase()
        
        // Create test meals with various names and ingredients
        testMeals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(
                    Ingredient(name = "Pasta"),
                    Ingredient(name = "Eggs"),
                    Ingredient(name = "Bacon")
                ),
                tags = setOf(MealTag.DINNER)
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(
                    Ingredient(name = "Lettuce"),
                    Ingredient(name = "Croutons"),
                    Ingredient(name = "Parmesan Cheese")
                ),
                tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN)
            ),
            Meal(
                id = 3,
                name = "Chicken Pasta",
                ingredients = listOf(
                    Ingredient(name = "Pasta"),
                    Ingredient(name = "Chicken"),
                    Ingredient(name = "Tomato Sauce")
                ),
                tags = setOf(MealTag.DINNER)
            ),
            Meal(
                id = 4,
                name = "Vegetable Stir Fry",
                ingredients = listOf(
                    Ingredient(name = "Broccoli"),
                    Ingredient(name = "Carrots"),
                    Ingredient(name = "Soy Sauce")
                ),
                tags = setOf(MealTag.DINNER, MealTag.VEGAN)
            )
        )
    }

    @Test
    fun `search by meal name returns matching meals - case insensitive`() {
        // When - search for "pasta" (lowercase)
        val result = useCase(testMeals, "pasta")

        // Then - should return both pasta meals
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `search by meal name with uppercase returns matching meals`() {
        // When - search for "PASTA" (uppercase)
        val result = useCase(testMeals, "PASTA")

        // Then - should return both pasta meals (case-insensitive)
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `search by meal name with mixed case returns matching meals`() {
        // When - search for "PaStA" (mixed case)
        val result = useCase(testMeals, "PaStA")

        // Then - should return both pasta meals (case-insensitive)
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `search by partial meal name returns matching meals`() {
        // When - search for "salad"
        val result = useCase(testMeals, "salad")

        // Then - should return Caesar Salad
        assertEquals(1, result.size)
        assertEquals("Caesar Salad", result[0].name)
    }

    @Test
    fun `search by ingredient name returns meals containing that ingredient`() {
        // When - search for "pasta" (ingredient)
        val result = useCase(testMeals, "pasta")

        // Then - should return meals with pasta ingredient
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `search by ingredient name is case insensitive`() {
        // When - search for "LETTUCE" (uppercase)
        val result = useCase(testMeals, "LETTUCE")

        // Then - should return Caesar Salad
        assertEquals(1, result.size)
        assertEquals("Caesar Salad", result[0].name)
    }

    @Test
    fun `search by partial ingredient name returns matching meals`() {
        // When - search for "chee" (partial match for "Cheese")
        val result = useCase(testMeals, "chee")

        // Then - should return Caesar Salad (has Parmesan Cheese)
        assertEquals(1, result.size)
        assertEquals("Caesar Salad", result[0].name)
    }

    @Test
    fun `search matches meal with multiple matching ingredients only once`() {
        // When - search for "sauce" (matches both Tomato Sauce and Soy Sauce)
        val result = useCase(testMeals, "sauce")

        // Then - should return 2 meals, each appearing once
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Chicken Pasta" })
        assertTrue(result.any { it.name == "Vegetable Stir Fry" })
    }

    @Test
    fun `search matches by meal name or ingredient name`() {
        // When - search for "chicken" (matches meal name and ingredient)
        val result = useCase(testMeals, "chicken")

        // Then - should return Chicken Pasta (matches both name and ingredient)
        assertEquals(1, result.size)
        assertEquals("Chicken Pasta", result[0].name)
    }

    @Test
    fun `empty query returns all meals`() {
        // When - search with empty string
        val result = useCase(testMeals, "")

        // Then - should return all meals
        assertEquals(4, result.size)
    }

    @Test
    fun `blank query returns all meals`() {
        // When - search with blank string (spaces)
        val result = useCase(testMeals, "   ")

        // Then - should return all meals
        assertEquals(4, result.size)
    }

    @Test
    fun `query with leading and trailing spaces is trimmed`() {
        // When - search with spaces around query
        val result = useCase(testMeals, "  pasta  ")

        // Then - should return pasta meals (spaces trimmed)
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Pasta Carbonara" })
        assertTrue(result.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `no matches returns empty list`() {
        // When - search for something that doesn't exist
        val result = useCase(testMeals, "pizza")

        // Then - should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search on empty meal list returns empty list`() {
        // When - search on empty list
        val result = useCase(emptyList(), "pasta")

        // Then - should return empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `search matches meals by name even if ingredient also matches`() {
        // Given - meal with "Pasta" in name and "Pasta" as ingredient
        val meals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta"))
            )
        )

        // When - search for "pasta"
        val result = useCase(meals, "pasta")

        // Then - should return the meal once (not duplicated)
        assertEquals(1, result.size)
        assertEquals("Pasta Carbonara", result[0].name)
    }
}
