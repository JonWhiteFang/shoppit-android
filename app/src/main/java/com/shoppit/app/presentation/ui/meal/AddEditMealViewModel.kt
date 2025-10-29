package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.validator.ValidationException
import com.shoppit.app.domain.usecase.AddMealUseCase
import com.shoppit.app.domain.usecase.GetMealByIdUseCase
import com.shoppit.app.domain.usecase.UpdateMealUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the add/edit meal screen.
 * Manages the state of the meal form and handles saving meals.
 *
 * Requirements:
 * - 1.1: Validate meal name contains at least one non-whitespace character
 * - 1.2: Validate at least one ingredient is included
 * - 1.4: Display confirmation message on successful save
 * - 4.1: Pre-populate form with existing meal data in edit mode
 * - 4.2: Validate updated meal data
 * - 4.4: Display confirmation message on successful update
 * - 4.5: Allow adding, removing, or modifying ingredients
 * - 6.1: Provide interface to add ingredient entries
 * - 6.4: Allow removing ingredients from the list
 * - 8.1: Display specific error messages for validation failures
 * - 8.5: Clear error messages when user corrects input
 */
@HiltViewModel
class AddEditMealViewModel @Inject constructor(
    private val addMealUseCase: AddMealUseCase,
    private val updateMealUseCase: UpdateMealUseCase,
    private val getMealByIdUseCase: GetMealByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extract optional mealId from navigation arguments (null for add mode)
    private val mealId: Long? = savedStateHandle.get<Long>("mealId")

    // Private mutable state
    private val _uiState = MutableStateFlow(AddEditMealUiState())
    
    // Public immutable state
    val uiState: StateFlow<AddEditMealUiState> = _uiState.asStateFlow()

    init {
        // Load existing meal if in edit mode
        mealId?.let { loadMeal(it) }
    }

    /**
     * Loads an existing meal for editing.
     * Updates UI state with the meal data or shows an error if loading fails.
     * Requirement 6.3: Handle cases where saved state is unavailable
     *
     * @param id The ID of the meal to load
     */
    private fun loadMeal(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                getMealByIdUseCase(id).first().fold(
                    onSuccess = { meal ->
                        _uiState.update { 
                            it.copy(
                                meal = meal,
                                isLoading = false,
                                error = null
                            )
                        }
                    },
                    onFailure = { error ->
                        // Requirement 6.3: Handle cases where saved state is unavailable
                        // If meal cannot be loaded (e.g., deleted), show error and allow navigation back
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load meal. The meal may have been deleted."
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                // Requirement 6.3: Handle unexpected errors during state restoration
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to restore meal data. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Updates the meal name in the UI state.
     * Clears any validation errors for the name field.
     *
     * @param name The new meal name
     */
    fun updateMealName(name: String) {
        _uiState.update { state ->
            state.copy(
                meal = state.meal.copy(name = name),
                validationErrors = state.validationErrors - "name",
                error = null
            )
        }
    }

    /**
     * Updates the meal notes in the UI state.
     *
     * @param notes The new meal notes
     */
    fun updateMealNotes(notes: String) {
        _uiState.update { state ->
            state.copy(
                meal = state.meal.copy(notes = notes),
                error = null
            )
        }
    }

    /**
     * Adds an ingredient to the meal's ingredient list.
     * Clears any validation errors for the ingredients field.
     *
     * @param ingredient The ingredient to add
     */
    fun addIngredient(ingredient: Ingredient) {
        _uiState.update { state ->
            state.copy(
                meal = state.meal.copy(
                    ingredients = state.meal.ingredients + ingredient
                ),
                validationErrors = state.validationErrors - "ingredients",
                error = null
            )
        }
    }

    /**
     * Removes an ingredient from the meal's ingredient list by index.
     *
     * @param index The index of the ingredient to remove
     */
    fun removeIngredient(index: Int) {
        _uiState.update { state ->
            state.copy(
                meal = state.meal.copy(
                    ingredients = state.meal.ingredients.filterIndexed { i, _ -> i != index }
                ),
                error = null
            )
        }
    }

    /**
     * Saves the meal (either creates a new meal or updates an existing one).
     * Validates the meal data and displays validation errors if any.
     * On success, the UI should navigate away (handled by the screen).
     */
    fun saveMeal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, validationErrors = emptyMap()) }
            
            val result = if (mealId == null) {
                // Add new meal
                addMealUseCase(uiState.value.meal)
                    .map { Unit } // Convert Result<Long> to Result<Unit>
            } else {
                // Update existing meal
                updateMealUseCase(uiState.value.meal)
            }
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    // Navigation is handled by the screen observing the state
                },
                onFailure = { error ->
                    // Handle validation errors
                    val validationErrors = when (error) {
                        is ValidationException -> {
                            parseValidationError(error.message ?: "")
                        }
                        else -> emptyMap()
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save meal",
                            validationErrors = validationErrors
                        )
                    }
                }
            )
        }
    }

    /**
     * Parses validation error messages to extract field-specific errors.
     * Maps error messages to field names for inline display.
     *
     * @param errorMessage The validation error message
     * @return Map of field names to error messages
     */
    private fun parseValidationError(errorMessage: String): Map<String, String> {
        return when {
            errorMessage.contains("name", ignoreCase = true) -> 
                mapOf("name" to errorMessage)
            errorMessage.contains("ingredient", ignoreCase = true) -> 
                mapOf("ingredients" to errorMessage)
            else -> emptyMap()
        }
    }
}
