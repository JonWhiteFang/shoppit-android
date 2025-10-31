package com.shoppit.app.presentation.ui.planner

import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.usecase.AssignMealToPlanUseCase
import com.shoppit.app.domain.usecase.ClearDayPlansUseCase
import com.shoppit.app.domain.usecase.CopyDayPlansUseCase
import com.shoppit.app.domain.usecase.DeleteMealPlanUseCase
import com.shoppit.app.domain.usecase.FakeMealPlanRepository
import com.shoppit.app.domain.usecase.FakeMealRepository
import com.shoppit.app.domain.usecase.GenerateShoppingListUseCase
import com.shoppit.app.domain.usecase.GetMealPlanHistoryUseCase
import com.shoppit.app.domain.usecase.GetMealPlansForWeekUseCase
import com.shoppit.app.domain.usecase.GetMealSuggestionsUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.domain.usecase.UpdateMealPlanUseCase
import com.shoppit.app.util.ViewModelTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for MealPlannerViewModel.
 * Tests state management, navigation, and meal plan operations.
 */
@ExperimentalCoroutinesApi
class MealPlannerViewModelTest : ViewModelTest() {

    private lateinit var mealPlanRepository: FakeMealPlanRepository
    private lateinit var mealRepository: FakeMealRepository
    private lateinit var getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var assignMealToPlanUseCase: AssignMealToPlanUseCase
    private lateinit var updateMealPlanUseCase: UpdateMealPlanUseCase
    private lateinit var deleteMealPlanUseCase: DeleteMealPlanUseCase
    private lateinit var copyDayPlansUseCase: CopyDayPlansUseCase
    private lateinit var clearDayPlansUseCase: ClearDayPlansUseCase
    private lateinit var generateShoppingListUseCase: GenerateShoppingListUseCase
    private lateinit var getMealSuggestionsUseCase: GetMealSuggestionsUseCase
    private lateinit var getMealPlanHistoryUseCase: GetMealPlanHistoryUseCase
    private lateinit var viewModel: MealPlannerViewModel

    @Before
    fun setUp() {
        mealPlanRepository = FakeMealPlanRepository()
        mealRepository = FakeMealRepository()
        
        getMealPlansForWeekUseCase = GetMealPlansForWeekUseCase(mealPlanRepository, mealRepository)
        getMealsUseCase = GetMealsUseCase(mealRepository)
        assignMealToPlanUseCase = AssignMealToPlanUseCase(mealPlanRepository)
        updateMealPlanUseCase = UpdateMealPlanUseCase(mealPlanRepository)
        deleteMealPlanUseCase = DeleteMealPlanUseCase(mealPlanRepository)
        copyDayPlansUseCase = CopyDayPlansUseCase(mealPlanRepository)
        clearDayPlansUseCase = ClearDayPlansUseCase(mealPlanRepository)
        generateShoppingListUseCase = GenerateShoppingListUseCase(
            shoppingListRepository = mockk(relaxed = true),
            mealPlanRepository = mealPlanRepository,
            mealRepository = mealRepository
        )
        getMealSuggestionsUseCase = mockk(relaxed = true)
        getMealPlanHistoryUseCase = mockk(relaxed = true)
    }

    @Test
    fun `initial state loads current week`() = runTest {
        // Given
        val expectedMonday = LocalDate.now().with(DayOfWeek.MONDAY)
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(expectedMonday, state.currentWeekStart)
        assertNotNull(state.weekData)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loads week data successfully and updates state`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))

        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val plan = MealPlan(id = 1, mealId = 1, date = monday, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(plan))

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.weekData)
        assertEquals(1, state.weekData?.plansByDate?.get(monday)?.size)
    }

    @Test
    fun `handles error and updates error state`() = runTest {
        // Given
        mealPlanRepository.setShouldFail(true, Exception("Database error"))
        mealRepository.setMeals(emptyList())

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Database error", state.error)
    }

    @Test
    fun `navigateToNextWeek updates currentWeekStart`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialWeekStart = viewModel.uiState.value.currentWeekStart

        // When
        viewModel.navigateToNextWeek()
        advanceUntilIdle()

        // Then
        val newWeekStart = viewModel.uiState.value.currentWeekStart
        assertEquals(initialWeekStart.plusWeeks(1), newWeekStart)
    }

    @Test
    fun `navigateToPreviousWeek updates currentWeekStart`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        val initialWeekStart = viewModel.uiState.value.currentWeekStart

        // When
        viewModel.navigateToPreviousWeek()
        advanceUntilIdle()

        // Then
        val newWeekStart = viewModel.uiState.value.currentWeekStart
        assertEquals(initialWeekStart.minusWeeks(1), newWeekStart)
    }

    @Test
    fun `navigateToToday resets to current week`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        // Navigate to next week first
        viewModel.navigateToNextWeek()
        advanceUntilIdle()

        val expectedMonday = LocalDate.now().with(DayOfWeek.MONDAY)

        // When
        viewModel.navigateToToday()
        advanceUntilIdle()

        // Then
        val newWeekStart = viewModel.uiState.value.currentWeekStart
        assertEquals(expectedMonday, newWeekStart)
    }

    @Test
    fun `onSlotClick shows meal suggestions`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")), tags = setOf(MealTag.LUNCH))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Perfect for lunch"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.now()
        val mealType = MealType.LUNCH

        // When
        viewModel.onSlotClick(date, mealType)
        advanceUntilIdle()

        // Then
        val state = viewModel.suggestionState.value
        assertTrue(state is SuggestionUiState.Success)
        assertEquals(1, (state as SuggestionUiState.Success).suggestions.size)
        assertEquals(date, state.context.targetDate)
        assertEquals(mealType, state.context.targetMealType)
    }

    @Test
    fun `onMealSelected assigns meal for empty slot`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        
        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.now()
        viewModel.onSlotClick(date, MealType.LUNCH)
        advanceUntilIdle() // Wait for async operation

        // When
        viewModel.onMealSelected(1L)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showMealSelection)
        assertNull(state.selectedSlot)
        
        val plans = mealPlanRepository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(1L, plans[0].mealId)
        assertEquals(date, plans[0].date)
        assertEquals(MealType.LUNCH, plans[0].mealType)
    }

    @Test
    fun `onMealSelected updates meal for filled slot`() = runTest {
        // Given
        val meal1 = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val meal2 = Meal(id = 2, name = "Salad", ingredients = listOf(Ingredient(name = "Lettuce")))
        mealRepository.setMeals(listOf(meal1, meal2))

        val date = LocalDate.now()
        val existingPlan = MealPlan(id = 1, mealId = 1, date = date, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(existingPlan))
        
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSlotClick(date, MealType.LUNCH)
        advanceUntilIdle() // Wait for async operation

        // When
        viewModel.onMealSelected(2L)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showMealSelection)
        
        val plans = mealPlanRepository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(2L, plans[0].mealId) // Updated to meal2
    }

    @Test
    fun `deleteMealPlan calls use case`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))

        val plan = MealPlan(id = 1, mealId = 1, date = LocalDate.now(), mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(plan))
        
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.deleteMealPlan(1L)
        advanceUntilIdle()

        // Then
        val plans = mealPlanRepository.getMealPlansList()
        assertTrue(plans.isEmpty())
    }

    @Test
    fun `copyDay calls use case with correct parameters`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))

        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        val plan = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(plan))
        
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.copyDay(sourceDate, targetDate, replace = false)
        advanceUntilIdle()

        // Then
        val plans = mealPlanRepository.getMealPlansList()
        assertEquals(2, plans.size) // Original + copied
        assertTrue(plans.any { it.date == targetDate })
    }

    @Test
    fun `clearDay calls use case`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))

        val date = LocalDate.now()
        val plan1 = MealPlan(id = 1, mealId = 1, date = date, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 1, date = date, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(plan1, plan2))
        
        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.clearDay(date)
        advanceUntilIdle()

        // Then
        val plans = mealPlanRepository.getMealPlansList()
        assertTrue(plans.isEmpty())
    }

    @Test
    fun `dismissMealSelection hides dialog and clears selected slot`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSlotClick(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle() // Wait for async operation
        assertTrue(viewModel.uiState.value.showMealSelection)

        // When
        viewModel.dismissMealSelection()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.showMealSelection)
        assertNull(state.selectedSlot)
    }

    @Test
    fun `loads available meals on initialization`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta"))),
            Meal(id = 2, name = "Salad", ingredients = listOf(Ingredient(name = "Lettuce")))
        )
        mealRepository.setMeals(meals)
        mealPlanRepository.setMealPlans(emptyList())

        // When
        viewModel = createViewModel()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertEquals(2, state.availableMeals.size)
        assertEquals("Pasta", state.availableMeals[0].name)
        assertEquals("Salad", state.availableMeals[1].name)
    }

    // Suggestion Tests

    @Test
    fun `showSuggestions updates state to Loading then Success`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")), tags = setOf(MealTag.LUNCH))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Perfect for lunch"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.now()

        // When
        viewModel.showSuggestions(date, MealType.LUNCH)
        advanceUntilIdle()

        // Then
        val state = viewModel.suggestionState.value
        assertTrue(state is SuggestionUiState.Success)
        assertEquals(1, (state as SuggestionUiState.Success).suggestions.size)
        assertEquals("Pasta", state.suggestions[0].meal.name)
    }

    @Test
    fun `showSuggestions displays Empty state when no meals exist`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(emptyList()))

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // Then
        val state = viewModel.suggestionState.value
        assertTrue(state is SuggestionUiState.Empty)
        assertEquals(EmptyReason.NO_MEALS, (state as SuggestionUiState.Empty).reason)
    }

    @Test
    fun `showSuggestions displays Empty state when no matches found`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(emptyList()))

        viewModel = createViewModel()
        advanceUntilIdle()

        // Set a filter
        viewModel.updateTagFilter(MealTag.VEGETARIAN)

        // When
        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // Then
        val state = viewModel.suggestionState.value
        assertTrue(state is SuggestionUiState.Empty)
        assertEquals(EmptyReason.NO_MATCHES, (state as SuggestionUiState.Empty).reason)
    }

    @Test
    fun `showSuggestions displays Error state on failure`() = runTest {
        // Given
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.failure(Exception("Test error")))

        viewModel = createViewModel()
        advanceUntilIdle()

        // When
        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // Then
        val state = viewModel.suggestionState.value
        assertTrue(state is SuggestionUiState.Error)
        assertEquals("Test error", (state as SuggestionUiState.Error).message)
    }

    @Test
    fun `updateTagFilter toggles tag selection`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")), tags = setOf(MealTag.VEGETARIAN))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Vegetarian"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // When - Add tag
        viewModel.updateTagFilter(MealTag.VEGETARIAN)
        advanceUntilIdle()

        // Then - Tag should be selected and suggestions refreshed
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Success)

        // When - Remove tag
        viewModel.updateTagFilter(MealTag.VEGETARIAN)
        advanceUntilIdle()

        // Then - Tag should be deselected and suggestions refreshed
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Success)
    }

    @Test
    fun `updateSearchQuery refreshes suggestions`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Great choice"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("Pasta")
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Success)
    }

    @Test
    fun `selectSuggestion adds meal to plan and hides suggestions`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Great choice"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        val date = LocalDate.now()
        viewModel.showSuggestions(date, MealType.LUNCH)
        advanceUntilIdle()

        // When
        viewModel.selectSuggestion(meal)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Hidden)
        val plans = mealPlanRepository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(1L, plans[0].mealId)
        assertEquals(date, plans[0].date)
        assertEquals(MealType.LUNCH, plans[0].mealType)
    }

    @Test
    fun `selectSuggestion updates existing plan`() = runTest {
        // Given
        val meal1 = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val meal2 = Meal(id = 2, name = "Salad", ingredients = listOf(Ingredient(name = "Lettuce")))
        val suggestion = MealSuggestion(
            meal = meal2,
            score = 150.0,
            reasons = listOf("Great choice"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal1, meal2))

        val date = LocalDate.now()
        val existingPlan = MealPlan(id = 1, mealId = 1, date = date, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(existingPlan))
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showSuggestions(date, MealType.LUNCH)
        advanceUntilIdle()

        // When
        viewModel.selectSuggestion(meal2)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Hidden)
        val plans = mealPlanRepository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(2L, plans[0].mealId) // Updated to meal2
    }

    @Test
    fun `selectSuggestion clears search query after success`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Great choice"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("Pasta")
        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()

        // When
        viewModel.selectSuggestion(meal)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Hidden)
    }

    @Test
    fun `hideSuggestions updates state to Hidden`() = runTest {
        // Given
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val suggestion = MealSuggestion(
            meal = meal,
            score = 150.0,
            reasons = listOf("Great choice"),
            lastPlannedDate = null,
            planCount = 0
        )
        mealRepository.setMeals(listOf(meal))
        mealPlanRepository.setMealPlans(emptyList())
        coEvery { getMealSuggestionsUseCase(any()) } returns flowOf(Result.success(listOf(suggestion)))

        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showSuggestions(LocalDate.now(), MealType.LUNCH)
        advanceUntilIdle()
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Success)

        // When
        viewModel.hideSuggestions()

        // Then
        assertTrue(viewModel.suggestionState.value is SuggestionUiState.Hidden)
    }

    private fun createViewModel(): MealPlannerViewModel {
        return MealPlannerViewModel(
            getMealPlansForWeekUseCase,
            getMealsUseCase,
            assignMealToPlanUseCase,
            updateMealPlanUseCase,
            deleteMealPlanUseCase,
            copyDayPlansUseCase,
            clearDayPlansUseCase,
            generateShoppingListUseCase,
            getMealSuggestionsUseCase,
            getMealPlanHistoryUseCase,
            SavedStateHandle()
        )
    }
}
