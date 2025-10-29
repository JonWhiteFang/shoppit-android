package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.usecase.DeleteMealUseCase
import com.shoppit.app.domain.usecase.FakeMealRepository
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.util.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MealViewModel.
 * Tests meal list loading, error handling, and meal deletion.
 *
 * Requirements:
 * - 2.1: Retrieve all meals from database
 * - 5.2: Remove meal from database when user confirms deletion
 * - 8.2: Handle database errors with user-friendly messages
 */
@ExperimentalCoroutinesApi
class MealViewModelTest : ViewModelTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var deleteMealUseCase: DeleteMealUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: MealViewModel

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        getMealsUseCase = GetMealsUseCase(repository)
        deleteMealUseCase = DeleteMealUseCase(repository)
        savedStateHandle = SavedStateHandle()
    }

    @Test
    fun `loads meals successfully and updates to Success state`() = runTest {
        // Given - repository has meals
        val meals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta"))
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(Ingredient(name = "Lettuce"))
            )
        )
        repository.setMeals(meals)

        // When - ViewModel is created and loads meals
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Success with meals
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Success)
        assertEquals(2, (state as MealListUiState.Success).meals.size)
        assertEquals("Pasta Carbonara", state.meals[0].name)
        assertEquals("Caesar Salad", state.meals[1].name)
    }

    @Test
    fun `handles error and updates to Error state`() = runTest {
        // Given - repository will fail
        repository.setShouldFail(true, Exception("Database connection failed"))

        // When - ViewModel is created and tries to load meals
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Error with message
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Error)
        assertEquals("Database connection failed", (state as MealListUiState.Error).message)
    }

    @Test
    fun `deleteMeal calls use case and removes meal from list`() = runTest {
        // Given - repository has meals
        val meals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta"))
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(Ingredient(name = "Lettuce"))
            )
        )
        repository.setMeals(meals)
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, savedStateHandle)
        advanceUntilIdle()

        // Verify initial state has 2 meals
        val initialState = viewModel.uiState.value
        assertTrue(initialState is MealListUiState.Success)
        assertEquals(2, (initialState as MealListUiState.Success).meals.size)

        // When - delete a meal
        viewModel.deleteMeal(1L)
        advanceUntilIdle()

        // Then - meal should be removed from the list
        val finalState = viewModel.uiState.value
        assertTrue(finalState is MealListUiState.Success)
        assertEquals(1, (finalState as MealListUiState.Success).meals.size)
        assertEquals("Caesar Salad", finalState.meals[0].name)
    }

    @Test
    fun `deleteMeal handles error and updates to Error state`() = runTest {
        // Given - repository has meals but will fail on delete
        val meals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta"))
            )
        )
        repository.setMeals(meals)
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, savedStateHandle)
        advanceUntilIdle()

        // Set repository to fail on next operation
        repository.setShouldFail(true, Exception("Failed to delete meal"))

        // When - delete a meal
        viewModel.deleteMeal(1L)
        advanceUntilIdle()

        // Then - state should be Error
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Error)
        assertEquals("Failed to delete meal", (state as MealListUiState.Error).message)
    }

    @Test
    fun `loads empty meal list successfully`() = runTest {
        // Given - repository has no meals
        repository.setMeals(emptyList())

        // When - ViewModel is created and loads meals
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase, savedStateHandle)
        advanceUntilIdle()

        // Then - state should be Success with empty list
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Success)
        assertTrue((state as MealListUiState.Success).meals.isEmpty())
    }
}
