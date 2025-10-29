package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
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
 * - 6.1: Preserve scroll position and search state across navigation
 * - 6.2: Save filter and search states in ViewModels
 * - 8.2: Handle database errors with user-friendly messages
 */
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow<MealListUiState>(MealListUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealListUiState> = _uiState.asStateFlow()
    
    // Saved state for search query
    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: ""
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Saved state for filter options
    private val _filterByIngredientCount = MutableStateFlow(
        savedStateHandle.get<Boolean>(KEY_FILTER_BY_INGREDIENT_COUNT) ?: false
    )
    val filterByIngredientCount: StateFlow<Boolean> = _filterByIngredientCount.asStateFlow()

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
    
    /**
     * Updates the search query and saves it to SavedStateHandle.
     * Requirement 6.2: Save search state in ViewModel
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }
    
    /**
     * Toggles the filter by ingredient count and saves it to SavedStateHandle.
     * Requirement 6.2: Save filter state in ViewModel
     */
    fun toggleFilterByIngredientCount() {
        val newValue = !_filterByIngredientCount.value
        _filterByIngredientCount.value = newValue
        savedStateHandle[KEY_FILTER_BY_INGREDIENT_COUNT] = newValue
    }
    
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_FILTER_BY_INGREDIENT_COUNT = "filter_by_ingredient_count"
    }
}
