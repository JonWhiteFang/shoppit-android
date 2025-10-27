package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.usecase.FakeMealRepository
import com.shoppit.app.domain.usecase.GetMealByIdUseCase
import com.shoppit.app.util.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MealDetailViewModel.
 * Tests meal detail loading and error handling.
 *
 * Requirements:
 * - 3.1: Retrieve specific meal from database by identifier
 * - 3.5: Display error message if meal not found
 */
@ExperimentalCoroutinesApi
class MealDetailViewModelTest : ViewModelTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var getMealByIdUseCase: GetMealByIdUseCase
    private lateinit var viewModel: MealDetailViewModel

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        getMealByIdUseCase = GetMealByIdUseCase(repository)
    }

    @Test
    fun `loads meal successfully`() = runTest {
        // Given - repository has a meal
        val meal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "Eggs", quantity = "4", unit = "pcs")
            ),
            notes = "Classic Italian dish"
        )
        repository.setMeals(listOf(meal))

        // When - ViewModel is created with mealId
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        viewModel = MealDetailViewModel(getMealByIdUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Success with the meal
        val state = viewModel.uiState.value
        assertTrue(state is MealDetailUiState.Success)
        val successState = state as MealDetailUiState.Success
        assertEquals("Pasta Carbonara", successState.meal.name)
        assertEquals(2, successState.meal.ingredients.size)
        assertEquals("Classic Italian dish", successState.meal.notes)
    }

    @Test
    fun `handles meal not found error`() = runTest {
        // Given - repository has no meals
        repository.setMeals(emptyList())

        // When - ViewModel is created with non-existent mealId
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 999L))
        viewModel = MealDetailViewModel(getMealByIdUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Error with meal not found message
        val state = viewModel.uiState.value
        assertTrue(state is MealDetailUiState.Error)
        assertEquals("Meal not found", (state as MealDetailUiState.Error).message)
    }

    @Test
    fun `handles database error`() = runTest {
        // Given - repository will fail
        repository.setShouldFail(true, Exception("Database connection failed"))

        // When - ViewModel is created
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        viewModel = MealDetailViewModel(getMealByIdUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Error with database error message
        val state = viewModel.uiState.value
        assertTrue(state is MealDetailUiState.Error)
        assertEquals("Database connection failed", (state as MealDetailUiState.Error).message)
    }

    @Test
    fun `loads meal with empty notes`() = runTest {
        // Given - repository has a meal with empty notes
        val meal = Meal(
            id = 1,
            name = "Simple Salad",
            ingredients = listOf(Ingredient(name = "Lettuce")),
            notes = ""
        )
        repository.setMeals(listOf(meal))

        // When - ViewModel is created
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        viewModel = MealDetailViewModel(getMealByIdUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Success with empty notes
        val state = viewModel.uiState.value
        assertTrue(state is MealDetailUiState.Success)
        val successState = state as MealDetailUiState.Success
        assertEquals("Simple Salad", successState.meal.name)
        assertEquals("", successState.meal.notes)
    }

    @Test
    fun `handles missing mealId parameter`() = runTest {
        // Given - SavedStateHandle without mealId
        val savedStateHandle = SavedStateHandle()

        // When - ViewModel is created (defaults to 0L)
        viewModel = MealDetailViewModel(getMealByIdUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Error (meal with id 0 not found)
        val state = viewModel.uiState.value
        assertTrue(state is MealDetailUiState.Error)
    }
}
