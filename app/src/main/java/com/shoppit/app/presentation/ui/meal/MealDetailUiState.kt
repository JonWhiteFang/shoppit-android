package com.shoppit.app.presentation.ui.meal

import com.shoppit.app.domain.model.Meal

/**
 * UI state for the meal detail screen.
 * Represents the different states the meal detail view can be in.
 */
sealed interface MealDetailUiState {
    /**
     * Loading state - meal data is being fetched
     */
    data object Loading : MealDetailUiState
    
    /**
     * Success state - meal has been loaded successfully
     * @property meal The meal to display
     */
    data class Success(val meal: Meal) : MealDetailUiState
    
    /**
     * Error state - an error occurred while loading the meal
     * @property message User-friendly error message
     */
    data class Error(val message: String) : MealDetailUiState
}
