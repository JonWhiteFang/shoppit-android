package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.validator.ValidationException
import com.shoppit.app.domain.usecase.AddMealUseCase
import com.shoppit.app.domain.usecase.GetMealByIdUseCase
import com.shoppit.app.domain.usecase.UpdateMealUseCase
import com.shoppit.app.presentation.ui.common.ErrorEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val errorLogger: ErrorLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extract optional mealId from navigation arguments (null for add mode)
    private val mealId: Long? = savedStateHandle.get<Long>("mealId")

    // Private mutable state
    private val _uiState = MutableStateFlow(AddEditMealUiState())
    
    // Public immutable state
    val uiState: StateFlow<AddEditMealUiState> = _uiState.asStateFlow()
    
    // Error events for snackbar display (one-time events)
    private val _errorEvent = MutableSharedFlow<ErrorEvent>()
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent.asSharedFlow()

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
                                originalMeal = meal, // Store original for change tracking
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
     * Requirement 4.3: Clear field errors when user updates that field
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
     * Requirement 4.3: Clear field errors when user updates that field
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
     * Updates an ingredient at a specific index.
     * Clears validation errors for the updated ingredient fields.
     * 
     * Requirement 4.3: Clear field errors when user updates that field
     *
     * @param index The index of the ingredient to update
     * @param ingredient The updated ingredient
     */
    fun updateIngredient(index: Int, ingredient: Ingredient) {
        _uiState.update { state ->
            val newIngredients = state.meal.ingredients.toMutableList()
            if (index in newIngredients.indices) {
                newIngredients[index] = ingredient
                
                // Clear validation errors for this ingredient's fields
                val newValidationErrors = state.validationErrors
                    .filterKeys { key ->
                        !key.startsWith("ingredients[$index]")
                    }
                
                state.copy(
                    meal = state.meal.copy(ingredients = newIngredients),
                    validationErrors = newValidationErrors,
                    error = null
                )
            } else {
                state
            }
        }
    }

    /**
     * Removes an ingredient from the meal's ingredient list by index.
     * Clears validation errors for the removed ingredient and adjusts indices for remaining ingredients.
     * 
     * Requirement 4.3: Clear field errors when user updates that field
     *
     * @param index The index of the ingredient to remove
     */
    fun removeIngredient(index: Int) {
        _uiState.update { state ->
            // Remove the ingredient
            val newIngredients = state.meal.ingredients.filterIndexed { i, _ -> i != index }
            
            // Clear validation errors for the removed ingredient and adjust indices
            val newValidationErrors = state.validationErrors
                .filterKeys { key ->
                    // Remove errors for the deleted ingredient
                    !key.startsWith("ingredients[$index]")
                }
                .mapKeys { (key, _) ->
                    // Adjust indices for ingredients after the removed one
                    if (key.startsWith("ingredients[")) {
                        val ingredientIndexMatch = "ingredients\\[(\\d+)\\]".toRegex().find(key)
                        if (ingredientIndexMatch != null) {
                            val ingredientIndex = ingredientIndexMatch.groupValues[1].toInt()
                            if (ingredientIndex > index) {
                                // Decrement the index
                                key.replace("ingredients[$ingredientIndex]", "ingredients[${ingredientIndex - 1}]")
                            } else {
                                key
                            }
                        } else {
                            key
                        }
                    } else {
                        key
                    }
                }
            
            state.copy(
                meal = state.meal.copy(ingredients = newIngredients),
                validationErrors = newValidationErrors,
                error = null
            )
        }
    }

    /**
     * Saves the meal (either creates a new meal or updates an existing one).
     * Validates the meal data and displays validation errors if any.
     * On success, emits a success event and the UI should navigate away.
     * 
     * Requirements:
     * - 3.1, 3.2, 3.3, 3.4: Validate meal name and ingredients
     * - 3.5, 3.6, 3.7: Validate individual ingredient fields
     * - 4.1, 4.2, 4.3, 4.4: Display field-specific validation errors inline
     * - 9.1, 9.2: Emit success message on successful save/update
     * - 10.1, 10.2, 10.3, 10.4, 10.5: Log errors with context
     */
    fun saveMeal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, validationErrors = emptyMap()) }
            
            val operationName = if (mealId == null) "addMeal" else "updateMeal"
            
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
                    
                    // Emit success event (Requirement 9.1, 9.2)
                    val successMessage = if (mealId == null) {
                        "Meal saved successfully"
                    } else {
                        "Meal updated successfully"
                    }
                    _errorEvent.emit(ErrorEvent.Success(successMessage))
                    
                    // Navigation is handled by the screen observing the state
                },
                onFailure = { error ->
                    // Log error with context (Requirement 10.1, 10.2, 10.3, 10.4, 10.5)
                    errorLogger.logError(
                        error = error,
                        context = "AddEditMealViewModel.$operationName",
                        additionalData = mapOf(
                            "mealId" to (mealId?.toString() ?: "new"),
                            "mealName" to uiState.value.meal.name
                        )
                    )
                    
                    // Extract field-specific validation errors (Requirements 3.1-3.7, 4.1-4.4)
                    val validationErrors = when (error) {
                        is ValidationException -> {
                            extractFieldErrors(error.message ?: "")
                        }
                        else -> emptyMap()
                    }
                    
                    // Get the first error message for the general error field
                    val firstErrorMessage = if (validationErrors.isNotEmpty()) {
                        validationErrors.values.first()
                    } else {
                        error.message ?: "Failed to save meal"
                    }
                    
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            error = firstErrorMessage,
                            validationErrors = validationErrors
                        )
                    }
                    
                    // Emit error event for snackbar display
                    _errorEvent.emit(
                        ErrorEvent.Error(
                            if (validationErrors.isEmpty()) {
                                error.message ?: "Failed to save meal"
                            } else {
                                "Please correct the errors in the form"
                            }
                        )
                    )
                }
            )
        }
    }

    /**
     * Extracts field-specific errors from validation error message.
     * The ValidationException message format is: "field1: message1; field2: message2"
     * 
     * This method parses that format and creates a map of field names to error messages
     * for inline display in the UI.
     *
     * Requirements:
     * - 3.1, 3.2, 3.3, 3.4: Extract meal-level validation errors
     * - 3.5, 3.6, 3.7: Extract ingredient-level validation errors with indexed field names
     * - 4.1, 4.2, 4.3: Map errors to specific form fields
     *
     * @param errorMessage The validation error message from ValidationException
     * @return Map of field names to error messages
     */
    private fun extractFieldErrors(errorMessage: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        
        // Split by semicolon to get individual field errors
        val fieldErrors = errorMessage.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        
        for (fieldError in fieldErrors) {
            // Each error is in format "field: message"
            val parts = fieldError.split(":", limit = 2)
            if (parts.size == 2) {
                val field = parts[0].trim()
                val message = parts[1].trim()
                errors[field] = message
            }
        }
        
        return errors
    }
}
