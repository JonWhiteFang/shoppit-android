package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for GetMealSuggestionsUseCase.
 * Tests the orchestration logic for generating meal suggestions.
 *
 * Requirements:
 * - 1.1-1.5: Basic meal suggestions
 * - 2.1-2.5: Tag-based meal filtering
 * - 5.1-5.5: Search and manual selection
 * - 6.1-6.5: Suggestion ranking algorithm
 */
class GetMealSuggestionsUseCaseTest {

    private lateinit var mealRepository: FakeMealRepository
    private lateinit var mealPlanRepository: FakeMealPlanRepository
    private lateinit var calculateScoreUseCase: CalculateSuggestionScoreUseCase
    private lateinit var getHistoryUseCase: GetMealPlanHistoryUseCase
    private lateinit var filterByTagsUseCase: FilterMealsByTagsUseCase
    private lateinit var useCase: GetMealSuggestionsUseCase
    
    private lateinit var targetDate: LocalDate

    @Before
    fun setUp() {
        mealRepository = FakeMealRepository()
        mealPlanRepository = FakeMealPlanRepository()
        calculateScoreUseCase = CalculateSuggestionScoreUseCase()
        filterByTagsUseCase = FilterMealsByTagsUseCase()
        
        // Mock GetMealPlanHistoryUseCase since it's suspend and needs special handling
        getHistoryUseCase = mockk()
        
        useCase = GetMealSuggestionsUseCase(
            mealRepository = mealRepository,
            mealPlanRepository = mealPlanRepository,
            calculateScoreUseCase = calculateScoreUseCase,
            getHistoryUseCase = getHistoryUseCase,
            filterByTagsUseCase = filterByTagsUseCase
        )
        
        targetDate = LocalDate.of(2024, 1, 15) // Monday
    }

    // Basic Suggestion Tests

    @Test
    fun `returns empty list when no meals exist`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `returns suggestions for available meals`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta", tags = setOf(MealTag.DINNER)),
            createMeal(id = 2, name = "Salad", tags = setOf(MealTag.LUNCH)),
            createMeal(id = 3, name = "Oatmeal", tags = setOf(MealTag.BREAKFAST))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `suggestions are sorted by score descending`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta", tags = setOf(MealTag.DINNER)), // Score: 200 (matching type)
            createMeal(id = 2, name = "Salad", tags = setOf(MealTag.LUNCH)), // Score: 100 (no match)
            createMeal(id = 3, name = "Steak", tags = setOf(MealTag.DINNER)) // Score: 200 (matching type)
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        
        // First two should have score 200 (matching type), last should have 100
        assertEquals(200.0, suggestions[0].score, 0.01)
        assertEquals(200.0, suggestions[1].score, 0.01)
        assertEquals(100.0, suggestions[2].score, 0.01)
    }

    @Test
    fun `suggestions with same score are sorted alphabetically`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Zucchini Pasta", tags = setOf(MealTag.DINNER)),
            createMeal(id = 2, name = "Apple Pie", tags = setOf(MealTag.DINNER)),
            createMeal(id = 3, name = "Meatballs", tags = setOf(MealTag.DINNER))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        
        // All have same score, should be alphabetically sorted
        assertEquals("Apple Pie", suggestions[0].meal.name)
        assertEquals("Meatballs", suggestions[1].meal.name)
        assertEquals("Zucchini Pasta", suggestions[2].meal.name)
    }

    @Test
    fun `limits results to top 10 suggestions`() = runTest {
        // Given
        val meals = (1..20).map { i ->
            createMeal(id = i.toLong(), name = "Meal $i", tags = setOf(MealTag.DINNER))
        }
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(10, result.getOrNull()?.size)
    }

    // Tag Filtering Tests

    @Test
    fun `filters meals by single selected tag`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta", tags = setOf(MealTag.DINNER, MealTag.VEGETARIAN)),
            createMeal(id = 2, name = "Steak", tags = setOf(MealTag.DINNER)),
            createMeal(id = 3, name = "Salad", tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            selectedTags = setOf(MealTag.VEGETARIAN)
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.all { it.meal.tags.contains(MealTag.VEGETARIAN) })
    }

    @Test
    fun `filters meals by multiple selected tags with AND logic`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Veggie Pasta", tags = setOf(MealTag.DINNER, MealTag.VEGETARIAN, MealTag.QUICK)),
            createMeal(id = 2, name = "Slow Veggie Stew", tags = setOf(MealTag.DINNER, MealTag.VEGETARIAN)),
            createMeal(id = 3, name = "Quick Steak", tags = setOf(MealTag.DINNER, MealTag.QUICK))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            selectedTags = setOf(MealTag.VEGETARIAN, MealTag.QUICK)
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(1, suggestions.size)
        assertEquals("Veggie Pasta", suggestions[0].meal.name)
    }

    @Test
    fun `returns empty list when no meals match selected tags`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta", tags = setOf(MealTag.DINNER)),
            createMeal(id = 2, name = "Steak", tags = setOf(MealTag.DINNER))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            selectedTags = setOf(MealTag.VEGAN)
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    // Search Query Filtering Tests

    @Test
    fun `filters meals by search query case-insensitive`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Spaghetti Carbonara"),
            createMeal(id = 2, name = "Chicken Pasta"),
            createMeal(id = 3, name = "Caesar Salad")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            searchQuery = "pasta"
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.any { it.meal.name.contains("Spaghetti", ignoreCase = true) })
        assertTrue(suggestions.any { it.meal.name.contains("Chicken Pasta", ignoreCase = true) })
    }

    @Test
    fun `search query performs partial matching`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Spaghetti Carbonara"),
            createMeal(id = 2, name = "Carbonara Pizza"),
            createMeal(id = 3, name = "Caesar Salad")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            searchQuery = "carbon"
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(2, suggestions.size)
    }

    @Test
    fun `returns empty list when no meals match search query`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            searchQuery = "pizza"
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `combines tag filter and search query`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Veggie Pasta", tags = setOf(MealTag.DINNER, MealTag.VEGETARIAN)),
            createMeal(id = 2, name = "Meat Pasta", tags = setOf(MealTag.DINNER)),
            createMeal(id = 3, name = "Veggie Salad", tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(
            mealType = MealType.DINNER,
            selectedTags = setOf(MealTag.VEGETARIAN),
            searchQuery = "pasta"
        )

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(1, suggestions.size)
        assertEquals("Veggie Pasta", suggestions[0].meal.name)
    }

    // Excluding Already Planned Meals Tests

    @Test
    fun `excludes meals already planned in the week`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad"),
            createMeal(id = 3, name = "Steak")
        )
        mealRepository.setMeals(meals)
        
        // Pasta is already planned on Tuesday
        val plans = listOf(
            createMealPlan(id = 1, mealId = 1, date = targetDate.plusDays(1))
        )
        mealPlanRepository.setMealPlans(plans)
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.none { it.meal.id == 1L })
    }

    @Test
    fun `includes all meals when none are planned in the week`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad"),
            createMeal(id = 3, name = "Steak")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()?.size)
    }

    @Test
    fun `excludes multiple meals planned in the week`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad"),
            createMeal(id = 3, name = "Steak"),
            createMeal(id = 4, name = "Chicken")
        )
        mealRepository.setMeals(meals)
        
        // Pasta and Salad are already planned
        val plans = listOf(
            createMealPlan(id = 1, mealId = 1, date = targetDate.plusDays(1)),
            createMealPlan(id = 2, mealId = 2, date = targetDate.plusDays(2))
        )
        mealPlanRepository.setMealPlans(plans)
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        assertEquals(2, suggestions.size)
        assertTrue(suggestions.none { it.meal.id == 1L || it.meal.id == 2L })
    }

    @Test
    fun `returns empty list when all meals are already planned`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad")
        )
        mealRepository.setMeals(meals)
        
        // Both meals are already planned
        val plans = listOf(
            createMealPlan(id = 1, mealId = 1, date = targetDate.plusDays(1)),
            createMealPlan(id = 2, mealId = 2, date = targetDate.plusDays(2))
        )
        mealPlanRepository.setMealPlans(plans)
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    // Score-Based Ranking Tests

    @Test
    fun `ranks meals with matching meal type higher`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Breakfast Burrito", tags = setOf(MealTag.BREAKFAST)),
            createMeal(id = 2, name = "Dinner Pasta", tags = setOf(MealTag.DINNER)),
            createMeal(id = 3, name = "Lunch Salad", tags = setOf(MealTag.LUNCH))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.BREAKFAST)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        
        // Breakfast Burrito should be first (score 200)
        assertEquals("Breakfast Burrito", suggestions[0].meal.name)
        assertEquals(200.0, suggestions[0].score, 0.01)
    }

    @Test
    fun `ranks less frequently planned meals higher`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad"),
            createMeal(id = 3, name = "Steak")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        
        // Pasta planned 5 times, Salad once, Steak never
        val history = mapOf(
            1L to MealPlanHistory(1L, targetDate.minusDays(10), 5, emptyList()),
            2L to MealPlanHistory(2L, targetDate.minusDays(20), 1, emptyList())
        )
        coEvery { getHistoryUseCase.invoke() } returns Result.success(history)
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        
        // Steak (never planned) should be first, then Salad, then Pasta
        assertEquals("Steak", suggestions[0].meal.name)
        assertEquals(100.0, suggestions[0].score, 0.01) // Base score only
        
        assertEquals("Salad", suggestions[1].meal.name)
        assertEquals(90.0, suggestions[1].score, 0.01) // Base - 10 frequency penalty
        
        assertEquals("Pasta", suggestions[2].meal.name)
        assertEquals(50.0, suggestions[2].score, 0.01) // Base - 50 frequency penalty
    }

    @Test
    fun `ranks meals not planned recently higher`() = runTest {
        // Given
        val meals = listOf(
            createMeal(id = 1, name = "Pasta"),
            createMeal(id = 2, name = "Salad")
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        
        // Pasta planned yesterday, Salad planned 10 days ago
        val history = mapOf(
            1L to MealPlanHistory(1L, targetDate.minusDays(1), 1, emptyList()),
            2L to MealPlanHistory(2L, targetDate.minusDays(10), 1, emptyList())
        )
        coEvery { getHistoryUseCase.invoke() } returns Result.success(history)
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()!!
        
        // Salad should be first (no recency penalty)
        assertEquals("Salad", suggestions[0].meal.name)
        assertEquals(90.0, suggestions[0].score, 0.01) // Base - 10 frequency
        
        // Pasta should be second (has recency penalty)
        assertEquals("Pasta", suggestions[1].meal.name)
        assertEquals(40.0, suggestions[1].score, 0.01) // Base - 10 frequency - 50 recency
    }

    // Error Handling Tests

    @Test
    fun `returns failure when meal repository fails`() = runTest {
        // Given
        mealRepository.setShouldFail(true, Exception("Database error"))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to fetch meals") == true)
    }

    @Test
    fun `returns failure when meal plan repository fails`() = runTest {
        // Given
        val meals = listOf(createMeal(id = 1, name = "Pasta"))
        mealRepository.setMeals(meals)
        mealPlanRepository.setShouldFail(true, Exception("Database error"))
        coEvery { getHistoryUseCase.invoke() } returns Result.success(emptyMap())
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to fetch meal plans") == true)
    }

    @Test
    fun `returns failure when history use case fails`() = runTest {
        // Given
        val meals = listOf(createMeal(id = 1, name = "Pasta"))
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getHistoryUseCase.invoke() } returns Result.failure(Exception("History error"))
        
        val context = createContext(MealType.DINNER)

        // When
        val result = useCase(context).first()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to fetch meal history") == true)
    }

    // Helper Methods

    private fun createMeal(
        id: Long = 1,
        name: String = "Test Meal",
        tags: Set<MealTag> = emptySet()
    ): Meal {
        return Meal(
            id = id,
            name = name,
            ingredients = listOf(Ingredient(name = "Test Ingredient")),
            tags = tags
        )
    }

    private fun createContext(
        mealType: MealType,
        date: LocalDate = targetDate,
        selectedTags: Set<MealTag> = emptySet(),
        searchQuery: String = "",
        existingPlanIds: Set<Long> = emptySet()
    ): SuggestionContext {
        return SuggestionContext(
            targetDate = date,
            targetMealType = mealType,
            selectedTags = selectedTags,
            searchQuery = searchQuery,
            existingPlanIds = existingPlanIds
        )
    }

    private fun createMealPlan(
        id: Long = 1,
        mealId: Long = 1,
        date: LocalDate = targetDate,
        mealType: MealType = MealType.DINNER
    ): MealPlan {
        return MealPlan(
            id = id,
            mealId = mealId,
            date = date,
            mealType = mealType
        )
    }
}
