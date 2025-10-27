package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.usecase.GetMealByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the meal detail screen.
 * Manages the state of a single meal's details.
 *
 * Requirements:
 * - 3.1: Retrieve specific meal from database by identifier
 * - 3.2: Display meal name, complete ingredient list, and notes
 * - 3.5: Display error message and navigate back if meal not found
 * - 8.2: Handle database errors with user-friendly messages
 */
@HiltViewModel
class MealDetailViewModel @Inject constructor(
    private val getMealByIdUseCase: GetMealByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extract mealId from navigation arguments
    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: 0L

    // Private mutable state
    private val _uiState = MutableStateFlow<MealDetailUiState>(MealDetailUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()

    init {
        loadMeal()
    }

    /**
     * Loads the meal details from the repository.
     * Updates UI state to Loading, then Success or Error based on the result.
     */
    private fun loadMeal() {
        viewModelScope.launch {
            getMealByIdUseCase(mealId)
                .catch { error ->
                    _uiState.update { 
                        MealDetailUiState.Error(error.message ?: "Unknown error occurred")
                    }
                }
                .collect { result ->
                    _uiState.update {
                        result.fold(
                            onSuccess = { meal -> MealDetailUiState.Success(meal) },
                            onFailure = { error -> 
                                MealDetailUiState.Error(error.message ?: "Failed to load meal")
                            }
                        )
                    }
                }
        }
    }
}
