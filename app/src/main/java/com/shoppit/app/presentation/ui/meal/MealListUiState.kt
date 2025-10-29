package com.shoppit.app.presentation.ui.meal

import com.shoppit.app.domain.model.Meal

/**
 * UI state for the meal list screen.
 * Represents the different states the meal list can be in.
 */
sealed interface MealListUiState {
    /**
     * Loading state - data is being fetched
     */
    data object Loading : MealListUiState
    
    /**
     * Success state - meals have been loaded successfully
     * @property meals List of meals to display
     * @property totalCount Total number of meals in the library
     * @property filteredCount Number of meals after applying filters
     * @property isFiltered Whether any filters are currently active
     */
    data class Success(
        val meals: List<Meal>,
        val totalCount: Int = meals.size,
        val filteredCount: Int = meals.size,
        val isFiltered: Boolean = false
    ) : MealListUiState
    
    /**
     * Error state - an error occurred while loading meals
     * @property message User-friendly error message
     */
    data class Error(val message: String) : MealListUiState
}
