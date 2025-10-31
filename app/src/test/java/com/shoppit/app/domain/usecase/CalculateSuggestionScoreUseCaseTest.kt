package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for CalculateSuggestionScoreUseCase.
 * Tests the scoring algorithm for meal suggestions.
 *
 * Requirements:
 * - 3.1-3.5: Frequency-based suggestions
 * - 4.1-4.3: Contextual meal type matching
 * - 6.1-6.4: Suggestion ranking algorithm
 */
class CalculateSuggestionScoreUseCaseTest {

    private lateinit var useCase: CalculateSuggestionScoreUseCase
    private lateinit var targetDate: LocalDate

    @Before
    fun setUp() {
        useCase = CalculateSuggestionScoreUseCase()
        targetDate = LocalDate.of(2024, 1, 15)
    }

    // Base Score Tests

    @Test
    fun `base score is always 100`() {
        // Given
        val meal = createMeal(tags = emptySet())
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
    }

    // Meal Type Bonus Tests

    @Test
    fun `meal with matching BREAKFAST tag gets 100 bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.BREAKFAST))
        val context = createContext(MealType.BREAKFAST)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(200.0, score.totalScore, 0.01) // 100 base + 100 bonus
    }

    @Test
    fun `meal with matching LUNCH tag gets 100 bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.LUNCH))
        val context = createContext(MealType.LUNCH)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(200.0, score.totalScore, 0.01)
    }

    @Test
    fun `meal with matching DINNER tag gets 100 bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.DINNER))
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(200.0, score.totalScore, 0.01)
    }

    @Test
    fun `meal with matching SNACK tag gets 100 bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.SNACK))
        val context = createContext(MealType.SNACK)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(200.0, score.totalScore, 0.01)
    }

    @Test
    fun `meal without matching meal type tag gets no bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.DINNER))
        val context = createContext(MealType.BREAKFAST)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.mealTypeBonus, 0.01)
        assertEquals(100.0, score.totalScore, 0.01) // Only base score
    }

    @Test
    fun `meal with no tags gets no meal type bonus`() {
        // Given
        val meal = createMeal(tags = emptySet())
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.mealTypeBonus, 0.01)
    }

    @Test
    fun `meal with multiple tags including matching type gets bonus`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.BREAKFAST, MealTag.QUICK, MealTag.HEALTHY))
        val context = createContext(MealType.BREAKFAST)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.mealTypeBonus, 0.01)
    }

    // Frequency Penalty Tests

    @Test
    fun `meal never planned has no frequency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned once has 10 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 1)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(10.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned twice has 10 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 2)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(10.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned three times has 30 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 3)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(30.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned four times has 30 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 4)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(30.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned five times has 50 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 5)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(50.0, score.frequencyPenalty, 0.01)
    }

    @Test
    fun `meal planned ten times has 50 point penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 10)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(50.0, score.frequencyPenalty, 0.01)
    }

    // Recency Penalty Tests

    @Test
    fun `meal never planned has no recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0, lastPlannedDate = null)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned yesterday has 50 point recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(1)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(50.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned 3 days ago has 50 point recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(3)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(50.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned 6 days ago has 50 point recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(6)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(50.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned exactly 7 days ago has no recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(7)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned 8 days ago has no recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(8)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.recencyPenalty, 0.01)
    }

    @Test
    fun `meal planned 30 days ago has no recency penalty`() {
        // Given
        val meal = createMeal()
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(30)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(0.0, score.recencyPenalty, 0.01)
    }

    // Combined Scoring Tests

    @Test
    fun `perfect score - matching type, never planned`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.DINNER))
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(0.0, score.frequencyPenalty, 0.01)
        assertEquals(0.0, score.recencyPenalty, 0.01)
        assertEquals(200.0, score.totalScore, 0.01)
    }

    @Test
    fun `good score - matching type, planned once long ago`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.BREAKFAST))
        val context = createContext(MealType.BREAKFAST)
        val lastPlanned = targetDate.minusDays(20)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(10.0, score.frequencyPenalty, 0.01)
        assertEquals(0.0, score.recencyPenalty, 0.01)
        assertEquals(190.0, score.totalScore, 0.01) // 100 + 100 - 10
    }

    @Test
    fun `medium score - no matching type, never planned`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.LUNCH))
        val context = createContext(MealType.DINNER)
        val history = createHistory(planCount = 0)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(0.0, score.mealTypeBonus, 0.01)
        assertEquals(0.0, score.frequencyPenalty, 0.01)
        assertEquals(0.0, score.recencyPenalty, 0.01)
        assertEquals(100.0, score.totalScore, 0.01)
    }

    @Test
    fun `low score - matching type, planned recently and frequently`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.DINNER))
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(2)
        val history = createHistory(planCount = 5, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(50.0, score.frequencyPenalty, 0.01)
        assertEquals(50.0, score.recencyPenalty, 0.01)
        assertEquals(100.0, score.totalScore, 0.01) // 100 + 100 - 50 - 50
    }

    @Test
    fun `worst score - no matching type, planned recently and frequently`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.LUNCH))
        val context = createContext(MealType.DINNER)
        val lastPlanned = targetDate.minusDays(1)
        val history = createHistory(planCount = 10, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(0.0, score.mealTypeBonus, 0.01)
        assertEquals(50.0, score.frequencyPenalty, 0.01)
        assertEquals(50.0, score.recencyPenalty, 0.01)
        assertEquals(0.0, score.totalScore, 0.01) // 100 - 50 - 50
    }

    @Test
    fun `score with moderate frequency penalty`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.SNACK))
        val context = createContext(MealType.SNACK)
        val lastPlanned = targetDate.minusDays(15)
        val history = createHistory(planCount = 3, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(30.0, score.frequencyPenalty, 0.01)
        assertEquals(0.0, score.recencyPenalty, 0.01)
        assertEquals(170.0, score.totalScore, 0.01) // 100 + 100 - 30
    }

    @Test
    fun `score with only recency penalty`() {
        // Given
        val meal = createMeal(tags = setOf(MealTag.BREAKFAST))
        val context = createContext(MealType.BREAKFAST)
        val lastPlanned = targetDate.minusDays(5)
        val history = createHistory(planCount = 1, lastPlannedDate = lastPlanned)

        // When
        val score = useCase(meal, context, history)

        // Then
        assertEquals(100.0, score.baseScore, 0.01)
        assertEquals(100.0, score.mealTypeBonus, 0.01)
        assertEquals(10.0, score.frequencyPenalty, 0.01)
        assertEquals(50.0, score.recencyPenalty, 0.01)
        assertEquals(140.0, score.totalScore, 0.01) // 100 + 100 - 10 - 50
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
        date: LocalDate = targetDate
    ): SuggestionContext {
        return SuggestionContext(
            targetDate = date,
            targetMealType = mealType
        )
    }

    private fun createHistory(
        mealId: Long = 1,
        planCount: Int = 0,
        lastPlannedDate: LocalDate? = null
    ): MealPlanHistory {
        val dates = if (planCount > 0 && lastPlannedDate != null) {
            List(planCount) { lastPlannedDate.minusDays(it.toLong()) }
        } else {
            emptyList()
        }
        
        return MealPlanHistory(
            mealId = mealId,
            lastPlannedDate = lastPlannedDate,
            planCount = planCount,
            planDates = dates
        )
    }
}
