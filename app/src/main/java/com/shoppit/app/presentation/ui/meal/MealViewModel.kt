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
    
    // Search query state - using SavedStateHandle.getStateFlow for automatic persistence
    // Note: getStateFlow reads from SavedStateHandle on creation, ensuring state persistence
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")
    
    // Selected tags state - using SavedStateHandle.getStateFlow for automatic persistence  
    // Note: getStateFlow reads from SavedStateHandle on creation, ensuring state persistence
    val selectedTags: StateFlow<Set<MealTag>> = savedStateHandle.getStateFlow(KEY_SELECTED_TAGS, emptySet())
    
    // All meals from repository (unfiltered)
    private var allMeals: List<com.shoppit.app.domain.model.Meal> = emptyList()
    
    // Filtered meals (after search and tag filters)
    private var filteredMeals: List<com.shoppit.app.domain.model.Meal> = emptyList()
    
    // Current pagination state
    private var paginationState = PaginationState()

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
     * On success, reloads the meal list to reflect the deletion and emits success event.
     * On failure, updates the UI state to show an error, logs the error, and emits error event.
     *
     * Requirements:
     * - 1.1: Display user-friendly error messages
     * - 5.1: Reload meal list after successful deletion
     * - 5.2: Update UI state with new data
     * - 5.3: Maintain search query and selected tags during reload
     * - 5.4: Display error without corrupting state (search/filter state preserved)
     * - 9.2: Display success message on successful deletion
     * - 10.1: Log errors with context
     *
     * @param mealId The ID of the meal to delete
     */
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // Reload meals after successful deletion (Requirement 5.1)
                    // This preserves search query and selected tags (Requirement 5.3)
                    loadMeals()
                    
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
                    
                    // Update UI state to error (Requirement 5.4)
                    // Note: Search query and selected tags are preserved as separate state
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
     * Supports pagination for large datasets.
     * 
     * Requirements: 5.5
     */
    private fun applyFilters(query: String? = null, tags: Set<MealTag>? = null, resetPagination: Boolean = true) {
        val searchQueryValue = query ?: searchQuery.value
        val selectedTagsValue = tags ?: selectedTags.value
        
        // Apply search filter
        val searchFiltered = searchMealsUseCase(allMeals, searchQueryValue)
        
        // Apply tag filter
        filteredMeals = filterMealsByTagsUseCase(searchFiltered, selectedTagsValue)
        
        // Reset pagination when filters change
        if (resetPagination) {
            paginationState = PaginationState(
                totalItems = filteredMeals.size
            )
        }
        
        // Get paginated meals
        val paginatedMeals = filteredMeals.take(paginationState.loadedItemsCount)
        
        _uiState.update { 
            MealListUiState.Success(
                meals = paginatedMeals,
                totalCount = allMeals.size,
                filteredCount = filteredMeals.size,
                isFiltered = searchQueryValue.isNotBlank() || selectedTagsValue.isNotEmpty(),
                paginationState = paginationState
            )
        }
    }
    
    /**
     * Updates the search query and saves it to SavedStateHandle.
     * Applies filters to update the meal list.
     * 
     * Requirements:
     * - 6.3: Update state in SavedStateHandle immediately when changed
     */
    fun updateSearchQuery(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
        applyFilters(query = query)
    }
    
    /**
     * Toggles a tag in the selected tags set.
     * Adds the tag if not present, removes it if present.
     * Saves state and applies filters.
     * 
     * Requirements:
     * - 6.3: Update state in SavedStateHandle immediately when changed
     */
    fun toggleTag(tag: MealTag) {
        val currentTags = selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        savedStateHandle[KEY_SELECTED_TAGS] = currentTags
        applyFilters(tags = currentTags)
    }
    
    /**
     * Clears all filters (search query and selected tags).
     * Resets state and applies filters to show all meals.
     */
    fun clearFilters() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        savedStateHandle[KEY_SELECTED_TAGS] = emptySet<MealTag>()
        applyFilters()
    }
    
    /**
     * Loads the next page of meals.
     * Updates pagination state and applies filters without resetting pagination.
     * 
     * Requirements: 5.5
     */
    fun loadNextPage() {
        if (!paginationState.hasMorePages || paginationState.isLoadingMore) {
            return
        }
        
        // Update pagination state to loading
        paginationState = paginationState.copy(
            isLoadingMore = true
        )
        
        // Simulate async loading (in real scenario, this would be a suspend function)
        viewModelScope.launch {
            // Update pagination state with next page
            paginationState = paginationState.copy(
                currentPage = paginationState.currentPage + 1,
                isLoadingMore = false
            )
            
            // Apply filters without resetting pagination
            applyFilters(resetPagination = false)
        }
    }
    
    companion object {
        private const val KEY_SEARCH_QUERY = "search_query"
        private const val KEY_SELECTED_TAGS = "selected_tags"
    }
}
