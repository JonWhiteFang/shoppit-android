package com.shoppit.app.presentation.ui.meal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.usecase.DeleteMealUseCase
import com.shoppit.app.domain.usecase.FilterMealsByTagsUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.domain.usecase.SearchMealsUseCase
import com.shoppit.app.presentation.ui.common.ErrorEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
 * - 10.1, 10.2, 10.3, 10.4, 10.5: Log errors with context
 */
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val searchMealsUseCase: SearchMealsUseCase,
    private val filterMealsByTagsUseCase: FilterMealsByTagsUseCase,
    private val errorLogger: ErrorLogger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Private mutable state
    private val _uiState = MutableStateFlow<MealListUiState>(MealListUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealListUiState> = _uiState.asStateFlow()
    
    // Error events for snackbar display (one-time events)
    private val _errorEvent = MutableSharedFlow<ErrorEvent>()
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent
    
    // Search query state
    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: ""
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Selected tags state
    private val _selectedTags = MutableStateFlow<Set<MealTag>>(
        savedStateHandle.get<Set<MealTag>>(KEY_SELECTED_TAGS) ?: emptySet()
    )
    val selectedTags: StateFlow<Set<MealTag>> = _selectedTags.asStateFlow()
    
    // All meals from repository (unfiltered)
    private var allMeals: List<com.shoppit.app.domain.model.Meal> = emptyList()

    init {
        loadMeals()
    }

    /**
     * Loads meals from the repository.
     * Updates UI state to Loading, then Success or Error based on the result.
     * Logs errors and emits error events for snackbar display.
     * 
     * Requirements:
     * - 1.1, 1.2, 1.3: Map errors to user-friendly messages
     * - 2.1, 2.2, 2.3, 2.4: Display error state with retry option
     * - 10.1: Log errors with context
     */
    fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    // Log error with context (Requirement 10.1)
                    errorLogger.logError(
                        error = error,
                        context = "MealViewModel.loadMeals",
                        additionalData = emptyMap()
                    )
                    
                    // Update UI state to error
                    _uiState.update { 
                        MealListUiState.Error(error.message ?: "Unknown error occurred")
                    }
                    
                    // Emit error event for snackbar
                    _errorEvent.emit(
                        ErrorEvent.Error(error.message ?: "Failed to load meals")
                    )
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            allMeals = meals
                            applyFilters()
                        },
                        onFailure = { error -> 
                            // Log error with context (Requirement 10.1)
                            errorLogger.logError(
                                error = error,
                                context = "MealViewModel.loadMeals",
                                additionalData = emptyMap()
                            )
                            
                            // Update UI state to error
                            _uiState.update {
                                MealListUiState.Error(error.message ?: "Failed to load meals")
                            }
                            
                            // Emit error event for snackbar
                            _errorEvent.emit(
                                ErrorEvent.Error(error.message ?: "Failed to load meals")
                            )
                        }
                    )
                }
        }
    }

    /**
     * Deletes a meal by its ID.
     * On success, the meal list will automatically update via the Flow and emits success event.
     * On failure, updates the UI state to show an error, logs the error, and emits error event.
     *
     * Requirements:
     * - 1.1: Display user-friendly error messages
     * - 9.2: Display success message on successful deletion
     * - 10.1: Log errors with context
     *
     * @param mealId The ID of the meal to delete
     */
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // Meal list updates automatically via Flow
                    // Emit success event (Requirement 9.2)
                    _errorEvent.emit(
                        ErrorEvent.Success("Meal deleted successfully")
                    )
                },
                onFailure = { error ->
                    // Log error with context (Requirement 10.1)
                    errorLogger.logError(
                        error = error,
                        context = "MealViewModel.deleteMeal",
                        additionalData = mapOf("mealId" to mealId.toString())
                    )
                    
                    // Update UI state to error
                    _uiState.update { 
                        MealListUiState.Error(error.message ?: "Failed to delete meal")
                    }
                    
                    // Emit error event for snackbar (Requirement 1.1)
                    _errorEvent.emit(
                        ErrorEvent.Error(error.message ?: "Failed to delete meal")
                    )
                }
            )
        }
    }
    
    /**
     * Applies search and tag filters to the meal list.
     * Updates UI state with filtered results and counts.
     */
    private fun applyFilters() {
        val query = _searchQuery.value
        val tags = _selectedTags.value
        
        // Apply search filter
        val searchFiltered = searchMealsUseCase(allMeals, query)
        
        // Apply tag filter
        val tagFiltered = filterMealsByTagsUseCase(searchFiltered, tags)
        
        _uiState.update { 
            MealListUiState.Success(
                meals = tagFiltered,
                totalCount = allMeals.size,
                filteredCount = tagFiltered.size,
                isFiltered = query.isNotBlank() || tags.isNotEmpty()
            )
        }
    }
    
    /**
     * Updates the search query and saves it to SavedStateHandle.
     * Applies filters to update the meal list.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
        applyFilters()
    }
    
    /**
     * Toggles a tag in the selected tags set.
     * Adds the tag if not present, removes it if present.
     * Saves state and applies filters.
     */
    fun toggleTag(tag: MealTag) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _selectedTags.value = currentTags
        savedStateHandle[KEY_SELECTED_TAGS] = currentTags
        applyFilters()
    }
    
    /**
     * Clears all filters (search query and selected tags).
     * Resets state and applies filters to show all meals.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptySet()
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        savedStateHandle[KEY_SELECTED_TAGS] = emptySet<MealTag>()
        applyFilters()
    }
    
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SELECTED_TAGS = "selected_tags"
    }
}
