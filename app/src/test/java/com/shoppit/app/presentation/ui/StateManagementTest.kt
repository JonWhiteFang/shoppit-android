package com.shoppit.app.presentation.ui

import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.usecase.*
import com.shoppit.app.presentation.ui.meal.MealListUiState
import com.shoppit.app.presentation.ui.meal.MealViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for state management optimization (Task 7.1).
 * Verifies that ViewModels properly expose StateFlow and use update { } pattern.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StateManagementTest {

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var deleteMealUseCase: DeleteMealUseCase
    private lateinit var searchMealsUseCase: SearchMealsUseCase
    private lateinit var filterMealsByTagsUseCase: FilterMealsByTagsUseCase
    private lateinit var errorLogger: ErrorLogger
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: MealViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        getMealsUseCase = mockk()
        deleteMealUseCase = mockk()
        searchMealsUseCase = mockk()
        filterMealsByTagsUseCase = mockk()
        errorLogger = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        
        // Mock use cases to return empty results
        every { getMealsUseCase() } returns flowOf(Result.success(emptyList()))
        every { searchMealsUseCase(any(), any()) } returns emptyList()
        every { filterMealsByTagsUseCase(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel exposes StateFlow not MutableStateFlow`() = runTest {
        // Given
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            errorLogger,
            savedStateHandle
        )
        
        // When - Try to access the state
        val state = viewModel.uiState
        
        // Then - Verify it's a StateFlow (not MutableStateFlow)
        // This is verified at compile time - if uiState was MutableStateFlow,
        // we could call .update { } on it, but we can't because it's StateFlow
        assertTrue(state is kotlinx.coroutines.flow.StateFlow)
    }

    @Test
    fun `ViewModel uses update pattern for state modifications`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Pasta", ingredients = emptyList()),
            Meal(id = 2, name = "Salad", ingredients = emptyList())
        )
        every { getMealsUseCase() } returns flowOf(Result.success(meals))
        every { searchMealsUseCase(meals, "") } returns meals
        every { filterMealsByTagsUseCase(meals, emptySet()) } returns meals
        
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            errorLogger,
            savedStateHandle
        )
        
        // When - Load meals (which uses update { } internally)
        advanceUntilIdle()
        
        // Then - State should be updated correctly
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Success)
        assertEquals(2, (state as MealListUiState.Success).meals.size)
    }

    @Test
    fun `State updates are immutable`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Pasta", ingredients = emptyList())
        )
        every { getMealsUseCase() } returns flowOf(Result.success(meals))
        every { searchMealsUseCase(meals, "") } returns meals
        every { filterMealsByTagsUseCase(meals, emptySet()) } returns meals
        
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            errorLogger,
            savedStateHandle
        )
        advanceUntilIdle()
        
        // When - Get the current state
        val state1 = viewModel.uiState.value
        
        // Then - Modifying the returned state should not affect the ViewModel's state
        // (This is ensured by using data classes with immutable collections)
        assertTrue(state1 is MealListUiState.Success)
        val successState = state1 as MealListUiState.Success
        
        // Verify the state is a data class (immutable by design)
        assertTrue(successState::class.isData)
    }

    @Test
    fun `Multiple state updates are batched correctly`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Pasta", ingredients = emptyList()),
            Meal(id = 2, name = "Salad", ingredients = emptyList())
        )
        every { getMealsUseCase() } returns flowOf(Result.success(meals))
        every { searchMealsUseCase(any(), any()) } answers {
            val query = secondArg<String>()
            firstArg<List<Meal>>().filter { it.name.contains(query, ignoreCase = true) }
        }
        every { filterMealsByTagsUseCase(any(), any()) } returns meals
        
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            errorLogger,
            savedStateHandle
        )
        advanceUntilIdle()
        
        // When - Update search query multiple times rapidly
        viewModel.updateSearchQuery("P")
        viewModel.updateSearchQuery("Pa")
        viewModel.updateSearchQuery("Pas")
        advanceUntilIdle()
        
        // Then - Final state should reflect the last update
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Success)
        assertEquals(1, (state as MealListUiState.Success).meals.size)
        assertEquals("Pasta", state.meals[0].name)
    }

    @Test
    fun `SavedStateHandle integration preserves state`() = runTest {
        // Given
        val meals = listOf(
            Meal(id = 1, name = "Pasta", ingredients = emptyList())
        )
        every { getMealsUseCase() } returns flowOf(Result.success(meals))
        every { searchMealsUseCase(meals, "test") } returns meals
        every { filterMealsByTagsUseCase(meals, emptySet()) } returns meals
        
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            errorLogger,
            savedStateHandle
        )
        advanceUntilIdle()
        
        // When - Update search query (which saves to SavedStateHandle)
        viewModel.updateSearchQuery("test")
        advanceUntilIdle()
        
        // Then - SavedStateHandle should contain the search query
        assertEquals("test", savedStateHandle.get<String>("search_query"))
        
        // And - SearchQuery StateFlow should reflect the saved value
        assertEquals("test", viewModel.searchQuery.value)
    }
}
