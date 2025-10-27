package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.usecase.AddMealUseCase
import com.shoppit.app.domain.usecase.FakeMealRepository
import com.shoppit.app.domain.usecase.GetMealByIdUseCase
import com.shoppit.app.domain.usecase.UpdateMealUseCase
import com.shoppit.app.domain.validator.MealValidator
import com.shoppit.app.util.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AddEditMealViewModel.
 * Tests meal form state management, validation, and saving.
 *
 * Requirements:
 * - 1.1: Validate meal name contains at least one non-whitespace character
 * - 4.1: Pre-populate form with existing meal data in edit mode
 * - 4.5: Allow adding, removing, or modifying ingredients
 * - 6.1: Provide interface to add ingredient entries
 * - 6.4: Allow removing ingredients from the list
 */
@ExperimentalCoroutinesApi
class AddEditMealViewModelTest : ViewModelTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var addMealUseCase: AddMealUseCase
    private lateinit var updateMealUseCase: UpdateMealUseCase
    private lateinit var getMealByIdUseCase: GetMealByIdUseCase
    private lateinit var validator: MealValidator
    private lateinit var viewModel: AddEditMealViewModel

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        validator = MealValidator()
        addMealUseCase = AddMealUseCase(repository, validator)
        updateMealUseCase = UpdateMealUseCase(repository, validator)
        getMealByIdUseCase = GetMealByIdUseCase(repository)
    }

    @Test
    fun `loads existing meal in edit mode`() = runTest {
        // Given - repository has a meal
        val existingMeal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(
                Ingredient(name = "Pasta", quantity = "400", unit = "g"),
                Ingredient(name = "Eggs", quantity = "4", unit = "pcs")
            ),
            notes = "Classic Italian dish"
        )
        repository.setMeals(listOf(existingMeal))

        // When - ViewModel is created in edit mode
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then - state should contain the existing meal
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Pasta Carbonara", state.meal.name)
        assertEquals(2, state.meal.ingredients.size)
        assertEquals("Classic Italian dish", state.meal.notes)
        assertNull(state.error)
    }

    @Test
    fun `updateMealName updates state`() = runTest {
        // Given - ViewModel in add mode
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )

        // When - update meal name
        viewModel.updateMealName("New Meal Name")

        // Then - state should reflect the new name
        assertEquals("New Meal Name", viewModel.uiState.value.meal.name)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `updateMealNotes updates state`() = runTest {
        // Given - ViewModel in add mode
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )

        // When - update meal notes
        viewModel.updateMealNotes("These are my notes")

        // Then - state should reflect the new notes
        assertEquals("These are my notes", viewModel.uiState.value.meal.notes)
    }

    @Test
    fun `addIngredient adds to list`() = runTest {
        // Given - ViewModel in add mode
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )

        // When - add ingredients
        viewModel.addIngredient(Ingredient(name = "Pasta", quantity = "400", unit = "g"))
        viewModel.addIngredient(Ingredient(name = "Eggs", quantity = "4", unit = "pcs"))

        // Then - state should contain both ingredients
        val ingredients = viewModel.uiState.value.meal.ingredients
        assertEquals(2, ingredients.size)
        assertEquals("Pasta", ingredients[0].name)
        assertEquals("Eggs", ingredients[1].name)
    }

    @Test
    fun `removeIngredient removes from list`() = runTest {
        // Given - ViewModel with ingredients
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.addIngredient(Ingredient(name = "Pasta"))
        viewModel.addIngredient(Ingredient(name = "Eggs"))
        viewModel.addIngredient(Ingredient(name = "Cheese"))

        // When - remove middle ingredient
        viewModel.removeIngredient(1)

        // Then - ingredient should be removed
        val ingredients = viewModel.uiState.value.meal.ingredients
        assertEquals(2, ingredients.size)
        assertEquals("Pasta", ingredients[0].name)
        assertEquals("Cheese", ingredients[1].name)
    }

    @Test
    fun `saveMeal validates and calls add use case for new meal`() = runTest {
        // Given - ViewModel in add mode with valid meal
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.updateMealName("Pasta Carbonara")
        viewModel.addIngredient(Ingredient(name = "Pasta"))

        // When - save meal
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - meal should be saved successfully
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, repository.getMealsList().size)
        assertEquals("Pasta Carbonara", repository.getMealsList()[0].name)
    }

    @Test
    fun `saveMeal validates and calls update use case for existing meal`() = runTest {
        // Given - ViewModel in edit mode with existing meal
        val existingMeal = Meal(
            id = 1,
            name = "Original Name",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(existingMeal))

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When - update meal name and save
        viewModel.updateMealName("Updated Name")
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - meal should be updated
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
        assertEquals(1, repository.getMealsList().size)
        assertEquals("Updated Name", repository.getMealsList()[0].name)
    }

    @Test
    fun `saveMeal shows validation error for empty name`() = runTest {
        // Given - ViewModel with invalid meal (empty name)
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.addIngredient(Ingredient(name = "Pasta"))

        // When - save meal with empty name
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - should show validation error
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Meal name cannot be empty", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("name"))
        assertEquals(0, repository.getMealsList().size)
    }

    @Test
    fun `saveMeal shows validation error for empty ingredients`() = runTest {
        // Given - ViewModel with invalid meal (no ingredients)
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.updateMealName("Pasta Carbonara")

        // When - save meal without ingredients
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - should show validation error
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Meal must have at least one ingredient", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("ingredients"))
        assertEquals(0, repository.getMealsList().size)
    }

    @Test
    fun `saveMeal shows validation error for ingredient with empty name`() = runTest {
        // Given - ViewModel with invalid ingredient
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.updateMealName("Pasta Carbonara")
        viewModel.addIngredient(Ingredient(name = "Pasta"))
        viewModel.addIngredient(Ingredient(name = ""))

        // When - save meal with empty ingredient name
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - should show validation error
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Ingredient name cannot be empty", viewModel.uiState.value.error)
        assertEquals(0, repository.getMealsList().size)
    }

    @Test
    fun `updateMealName clears validation errors`() = runTest {
        // Given - ViewModel with validation error
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.addIngredient(Ingredient(name = "Pasta"))
        viewModel.saveMeal()
        advanceUntilIdle()

        // Verify error exists
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("name"))

        // When - update meal name
        viewModel.updateMealName("New Name")

        // Then - validation error should be cleared
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("name"))
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `addIngredient clears validation errors`() = runTest {
        // Given - ViewModel with validation error for ingredients
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.updateMealName("Pasta")
        viewModel.saveMeal()
        advanceUntilIdle()

        // Verify error exists
        assertTrue(viewModel.uiState.value.validationErrors.containsKey("ingredients"))

        // When - add ingredient
        viewModel.addIngredient(Ingredient(name = "Pasta"))

        // Then - validation error should be cleared
        assertFalse(viewModel.uiState.value.validationErrors.containsKey("ingredients"))
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `initial state in add mode has empty meal`() {
        // When - ViewModel is created in add mode
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )

        // Then - state should have empty meal
        val state = viewModel.uiState.value
        assertEquals("", state.meal.name)
        assertTrue(state.meal.ingredients.isEmpty())
        assertEquals("", state.meal.notes)
        assertFalse(state.isLoading)
        assertFalse(state.isSaving)
        assertNull(state.error)
        assertTrue(state.validationErrors.isEmpty())
    }

    @Test
    fun `handles repository error when saving`() = runTest {
        // Given - ViewModel with valid meal but repository will fail
        val savedStateHandle = SavedStateHandle()
        viewModel = AddEditMealViewModel(
            addMealUseCase,
            updateMealUseCase,
            getMealByIdUseCase,
            savedStateHandle
        )
        viewModel.updateMealName("Pasta")
        viewModel.addIngredient(Ingredient(name = "Pasta"))
        
        repository.setShouldFail(true, Exception("Database error"))

        // When - save meal
        viewModel.saveMeal()
        advanceUntilIdle()

        // Then - should show error
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Database error", viewModel.uiState.value.error)
    }
}
