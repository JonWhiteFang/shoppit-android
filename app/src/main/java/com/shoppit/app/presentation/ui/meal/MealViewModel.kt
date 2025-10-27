package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.usecase.DeleteMealUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the meal list screen.
 * Manages the state of the meal list and handles user actions like deleting meals.
 *
 * Requirements:
 * - 2.1: Retrieve all meals from database
 * - 2.2: Display meals in alphabetical order
 * - 2.4: Display empty state when no meals exist
 * - 5.2: Remove meal from database when user confirms deletion
 * - 5.3: Display confirmation message on successful deletion
 * - 8.2: Handle database errors with user-friendly messages
 */
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow<MealListUiState>(MealListUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealListUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
    }

    /**
     * Loads meals from the repository.
     * Updates UI state to Loading, then Success or Error based on the result.
     */
    private fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { 
                        MealListUiState.Error(error.message ?: "Unknown error occurred")
                    }
                }
                .collect { result ->
                    _uiState.update {
                        result.fold(
                            onSuccess = { meals -> MealListUiState.Success(meals) },
                            onFailure = { error -> 
                                MealListUiState.Error(error.message ?: "Failed to load meals")
                            }
                        )
                    }
                }
        }
    }

    /**
     * Deletes a meal by its ID.
     * On success, the meal list will automatically update via the Flow.
     * On failure, updates the UI state to show an error.
     *
     * @param mealId The ID of the meal to delete
     */
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // Meal list updates automatically via Flow
                    // No need to manually update state
                },
                onFailure = { error ->
                    _uiState.update { 
                        MealListUiState.Error(error.message ?: "Failed to delete meal")
                    }
                }
            )
        }
    }
}
