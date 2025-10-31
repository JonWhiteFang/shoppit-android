package com.shoppit.app.presentation.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.usecase.DeleteMealUseCase
import com.shoppit.app.domain.usecase.FilterMealsByTagsUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.domain.usecase.SearchMealsUseCase
import com.shoppit.app.presentation.ui.meal.MealListUiState
import com.shoppit.app.presentation.ui.meal.MealViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for state management edge cases.
 * 
 * Requirements:
 * - 6.4: Test rapid navigation between screens
 * - 6.4: Test state preservation with low memory conditions
 * - 6.4: Verify state clearing on data deletion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateManagementEdgeCasesTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var deleteMealUseCase: DeleteMealUseCase
    private lateinit var searchMealsUseCase: SearchMealsUseCase
    private lateinit var filterMealsByTagsUseCase: FilterMealsByTagsUseCase
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getMealsUseCase = mockk()
        deleteMealUseCase = mockk()
        searchMealsUseCase = SearchMealsUseCase()
        filterMealsByTagsUseCase = FilterMealsByTagsUseCase()
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Test rapid navigation between screens doesn't cause state corruption.
     * Requirement 6.4: Test rapid navigation between screens
     */
    @Test
    fun `rapid navigation preserves state correctly`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Meal 1", ingredients = emptyList()),
            Meal(id = 2, name = "Meal 2", ingredients = emptyList())
        )
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // When - Simulate rapid state changes
        repeat(10) {
            viewModel.updateSearchQuery("query$it")
            viewModel.toggleTag(MealTag.DINNER)
        }
        advanceUntilIdle()

        // Then - State should be consistent
        assertEquals("query9", viewModel.searchQuery.value)
        assertEquals(false, viewModel.selectedTags.value.contains(MealTag.DINNER)) // Toggled 10 times (even)
        assertTrue(viewModel.uiState.value is MealListUiState.Success)
    }

    /**
     * Test state preservation when SavedStateHandle is empty (simulating low memory).
     * Requirement 6.4: Test state preservation with low memory conditions
     */
    @Test
    fun `state restoration with empty SavedStateHandle uses defaults`() = runTest {
        // Given - Empty SavedStateHandle (simulating cleared state)
        val emptyStateHandle = SavedStateHandle()
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        // When
        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, emptyStateHandle)
        advanceUntilIdle()

        // Then - Should use default values
        assertEquals("", viewModel.searchQuery.value)
        assertTrue(viewModel.selectedTags.value.isEmpty())
        assertTrue(viewModel.uiState.value is MealListUiState.Success)
    }

    /**
     * Test state restoration with partial SavedStateHandle data.
     * Requirement 6.4: Test state preservation with low memory conditions
     */
    @Test
    fun `state restoration with partial SavedStateHandle uses defaults for missing values`() = runTest {
        // Given - SavedStateHandle with only search query
        val partialStateHandle = SavedStateHandle().apply {
            set("search_query", "test query")
            // selected_tags is missing
        }
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        // When
        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, partialStateHandle)
        advanceUntilIdle()

        // Then - Should restore available state and use defaults for missing
        assertEquals("test query", viewModel.searchQuery.value)
        assertTrue(viewModel.selectedTags.value.isEmpty()) // Default value
    }

    /**
     * Test state clearing when data is deleted.
     * Requirement 6.4: Verify state clearing on data deletion
     */
    @Test
    fun `state updates correctly when meal is deleted`() = runTest {
        // Given
        val initialMeals = listOf(
            Meal(id = 1, name = "Meal 1", ingredients = emptyList()),
            Meal(id = 2, name = "Meal 2", ingredients = emptyList())
        )
        val mealsAfterDeletion = listOf(
            Meal(id = 2, name = "Meal 2", ingredients = emptyList())
        )
        
        coEvery { getMealsUseCase() } returnsMany listOf(
            flowOf(Result.success(initialMeals)),
            flowOf(Result.success(mealsAfterDeletion))
        )
        coEvery { deleteMealUseCase(1) } returns Result.success(Unit)

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // Verify initial state
        val initialState = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, initialState.meals.size)

        // When - Delete a meal
        viewModel.deleteMeal(1)
        advanceUntilIdle()

        // Then - State should update to reflect deletion
        val updatedState = viewModel.uiState.value as MealListUiState.Success
        assertEquals(1, updatedState.meals.size)
        assertEquals(2, updatedState.meals[0].id)
    }

    /**
     * Test concurrent state updates don't cause race conditions.
     * Requirement 6.4: Test rapid navigation between screens
     */
    @Test
    fun `concurrent state updates are handled correctly`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // When - Multiple concurrent updates
        viewModel.updateSearchQuery("query1")
        viewModel.updateSearchQuery("query2")
        viewModel.updateSearchQuery("query3")
        viewModel.toggleTag(MealTag.DINNER)
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then - Final state should be consistent
        assertEquals("query3", viewModel.searchQuery.value)
        assertEquals(false, viewModel.selectedTags.value.contains(MealTag.DINNER))
    }

    /**
     * Test state persistence across multiple save/restore cycles.
     * Requirement 6.4: Test state preservation with low memory conditions
     */
    @Test
    fun `state persists across multiple save and restore cycles`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        // First cycle
        val viewModel1 = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()
        viewModel1.updateSearchQuery("test query")
        viewModel1.toggleTag(MealTag.VEGETARIAN)
        advanceUntilIdle()

        // Second cycle - Simulate process death and recreation
        val viewModel2 = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - State should be restored
        assertEquals("test query", viewModel2.searchQuery.value)
        assertTrue(viewModel2.selectedTags.value.contains(MealTag.VEGETARIAN))

        // Third cycle - Update and restore again
        viewModel2.updateSearchQuery("updated query")
        advanceUntilIdle()

        val viewModel3 = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        assertEquals("updated query", viewModel3.searchQuery.value)
        assertTrue(viewModel3.selectedTags.value.contains(MealTag.VEGETARIAN))
    }

    /**
     * Test state behavior when ViewModel is cleared.
     * Requirement 6.4: Verify state clearing on data deletion
     */
    @Test
    fun `state is properly saved before ViewModel is cleared`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // When - Update state
        viewModel.updateSearchQuery("important query")
        viewModel.toggleTag(MealTag.QUICK)
        advanceUntilIdle()

        // Simulate ViewModel being cleared (but SavedStateHandle persists)
        // Create new ViewModel with same SavedStateHandle
        val newViewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - State should be restored from SavedStateHandle
        assertEquals("important query", newViewModel.searchQuery.value)
        assertTrue(newViewModel.selectedTags.value.contains(MealTag.QUICK))
    }

    /**
     * Test error state doesn't corrupt saved state.
     * Requirement 6.4: Test state preservation with low memory conditions
     */
    @Test
    fun `error state doesn't corrupt saved state`() = runTest {
        // Given
        coEvery { getMealsUseCase() } returns flowOf(Result.failure(Exception("Network error")))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // When - Update state even in error state
        viewModel.updateSearchQuery("query in error state")
        advanceUntilIdle()

        // Then - State should still be saved
        assertEquals("query in error state", viewModel.searchQuery.value)
        assertTrue(viewModel.uiState.value is MealListUiState.Error)

        // Verify state persists after recreation
        val newViewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()
        assertEquals("query in error state", newViewModel.searchQuery.value)
    }

    /**
     * Test state updates during loading state.
     * Requirement 6.4: Test rapid navigation between screens
     */
    @Test
    fun `state updates during loading are preserved`() = runTest {
        // Given - Slow loading
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        
        // When - Update state while loading
        viewModel.updateSearchQuery("query during loading")
        advanceUntilIdle()

        // Then - State should be preserved
        assertEquals("query during loading", viewModel.searchQuery.value)
        assertTrue(viewModel.uiState.value is MealListUiState.Success)
    }

    /**
     * Test state with special characters and edge case values.
     * Requirement 6.4: Test state preservation with low memory conditions
     */
    @Test
    fun `state handles special characters and edge case values`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Meal 1", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))

        val viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()

        // When - Use special characters and edge cases
        val specialQuery = "Test 🍕 with émojis & spëcial çhars"
        viewModel.updateSearchQuery(specialQuery)
        advanceUntilIdle()

        // Then - State should handle special characters
        assertEquals(specialQuery, viewModel.searchQuery.value)

        // Verify persistence
        val newViewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, searchMealsUseCase, filterMealsByTagsUseCase, savedStateHandle)
        advanceUntilIdle()
        assertEquals(specialQuery, newViewModel.searchQuery.value)
    }
}
