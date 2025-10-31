package com.shoppit.app.presentation.ui.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.domain.usecase.AssignMealToPlanUseCase
import com.shoppit.app.domain.usecase.ClearDayPlansUseCase
import com.shoppit.app.domain.usecase.CopyDayPlansUseCase
import com.shoppit.app.domain.usecase.DeleteMealPlanUseCase
import com.shoppit.app.domain.usecase.GenerateShoppingListUseCase
import com.shoppit.app.domain.usecase.GetMealPlansForWeekUseCase
import com.shoppit.app.domain.usecase.GetMealSuggestionsUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.domain.usecase.UpdateMealPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the meal planner screen.
 * Manages weekly meal plan state and user interactions.
 * 
 * Requirements:
 * - 6.1: Preserve scroll position and UI state across navigation
 * - 6.2: Save current week selection in ViewModel
 * - 6.3: Restore state after process death using SavedStateHandle
 */
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase,
    private val getMealsUseCase: GetMealsUseCase,
    private val assignMealToPlanUseCase: AssignMealToPlanUseCase,
    private val updateMealPlanUseCase: UpdateMealPlanUseCase,
    private val deleteMealPlanUseCase: DeleteMealPlanUseCase,
    private val copyDayPlansUseCase: CopyDayPlansUseCase,
    private val clearDayPlansUseCase: ClearDayPlansUseCase,
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val getMealSuggestionsUseCase: GetMealSuggestionsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Restore current week from saved state or default to current week
    private val initialWeekStart = savedStateHandle.get<String>(KEY_CURRENT_WEEK_START)
        ?.let { LocalDate.parse(it) }
        ?: LocalDate.now().with(java.time.DayOfWeek.MONDAY)

    private val _uiState = MutableStateFlow(
        MealPlannerUiState(currentWeekStart = initialWeekStart)
    )
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    // Suggestion state management
    private val _suggestionState = MutableStateFlow<SuggestionUiState>(SuggestionUiState.Hidden)
    val suggestionState: StateFlow<SuggestionUiState> = _suggestionState.asStateFlow()

    private val _suggestionContext = MutableStateFlow<SuggestionContext?>(null)

    private val _selectedTags = MutableStateFlow<Set<MealTag>>(emptySet())

    private val _searchQuery = MutableStateFlow("")

    init {
        loadWeekData()
        loadAvailableMeals()
    }

    /**
     * Loads meal plan data for the current week.
     */
    private fun loadWeekData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getMealPlansForWeekUseCase(uiState.value.currentWeekStart)
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load meal plans"
                        )
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { weekData ->
                            _uiState.update {
                                it.copy(
                                    weekData = weekData,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load meal plans"
                                )
                            }
                        }
                    )
                }
        }
    }

    /**
     * Loads available meals for the meal selection dialog.
     */
    private fun loadAvailableMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            _uiState.update { it.copy(availableMeals = meals) }
                        },
                        onFailure = { /* Handle silently, show in meal selection */ }
                    )
                }
        }
    }

    /**
     * Navigates to the next week and saves the state.
     * Requirement 6.2: Save current week selection
     */
    fun navigateToNextWeek() {
        val newWeekStart = uiState.value.currentWeekStart.plusWeeks(1)
        _uiState.update {
            it.copy(currentWeekStart = newWeekStart)
        }
        savedStateHandle[KEY_CURRENT_WEEK_START] = newWeekStart.toString()
        loadWeekData()
    }

    /**
     * Navigates to the previous week and saves the state.
     * Requirement 6.2: Save current week selection
     */
    fun navigateToPreviousWeek() {
        val newWeekStart = uiState.value.currentWeekStart.minusWeeks(1)
        _uiState.update {
            it.copy(currentWeekStart = newWeekStart)
        }
        savedStateHandle[KEY_CURRENT_WEEK_START] = newWeekStart.toString()
        loadWeekData()
    }

    /**
     * Navigates to the current week (today) and saves the state.
     * Requirement 6.2: Save current week selection
     */
    fun navigateToToday() {
        val newWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY)
        _uiState.update {
            it.copy(currentWeekStart = newWeekStart)
        }
        savedStateHandle[KEY_CURRENT_WEEK_START] = newWeekStart.toString()
        loadWeekData()
    }

    /**
     * Handles click on a meal slot to show meal suggestions.
     * Requirements: 1.1, 10.1-10.2
     */
    fun onSlotClick(date: LocalDate, mealType: MealType) {
        // Show suggestions instead of meal selection dialog
        showSuggestions(date, mealType)
    }

    /**
     * Handles meal selection from the dialog.
     * Assigns a new meal or updates an existing plan.
     */
    fun onMealSelected(mealId: Long) {
        viewModelScope.launch {
            val slot = uiState.value.selectedSlot ?: return@launch

            val result = if (slot.existingPlan != null) {
                updateMealPlanUseCase(slot.existingPlan.id, mealId)
            } else {
                assignMealToPlanUseCase(mealId, slot.date, slot.mealType).map { Unit }
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            showMealSelection = false,
                            selectedSlot = null
                        )
                    }
                    // Trigger shopping list regeneration after meal plan change
                    regenerateShoppingList()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to assign meal")
                    }
                }
            )
        }
    }

    /**
     * Dismisses the meal selection dialog.
     */
    fun dismissMealSelection() {
        _uiState.update {
            it.copy(
                showMealSelection = false,
                selectedSlot = null
            )
        }
    }

    /**
     * Deletes a meal plan.
     */
    fun deleteMealPlan(mealPlanId: Long) {
        viewModelScope.launch {
            deleteMealPlanUseCase(mealPlanId).fold(
                onSuccess = { 
                    // Trigger shopping list regeneration after deletion
                    regenerateShoppingList()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to delete meal plan")
                    }
                }
            )
        }
    }

    /**
     * Copies all meal plans from one day to another.
     */
    fun copyDay(sourceDate: LocalDate, targetDate: LocalDate, replace: Boolean) {
        viewModelScope.launch {
            copyDayPlansUseCase(sourceDate, targetDate, replace).fold(
                onSuccess = { 
                    // Trigger shopping list regeneration after copy
                    regenerateShoppingList()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to copy day")
                    }
                }
            )
        }
    }

    /**
     * Clears all meal plans for a specific day.
     */
    fun clearDay(date: LocalDate) {
        viewModelScope.launch {
            clearDayPlansUseCase(date).fold(
                onSuccess = { 
                    // Trigger shopping list regeneration after clear
                    regenerateShoppingList()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to clear day")
                    }
                }
            )
        }
    }
    
    /**
     * Regenerates the shopping list based on current meal plans.
     * Called automatically when meal plans are modified.
     */
    private fun regenerateShoppingList() {
        viewModelScope.launch {
            generateShoppingListUseCase()
            // Silently fail - shopping list errors shouldn't block meal planning
        }
    }

    /**
     * Shows meal suggestions for a specific date and meal type.
     * Requirements: 1.1-1.5, 10.1-10.2
     */
    fun showSuggestions(date: LocalDate, mealType: MealType) {
        viewModelScope.launch {
            _suggestionState.update { SuggestionUiState.Loading }

            // Get existing plan IDs for the week to exclude them
            val weekStart = date.with(java.time.DayOfWeek.MONDAY)
            val weekEnd = weekStart.plusDays(6)
            val existingPlanIds = uiState.value.weekData?.plansByDate?.values
                ?.flatten()
                ?.map { it.meal.id }
                ?.toSet() ?: emptySet()

            // Create suggestion context
            val context = SuggestionContext(
                targetDate = date,
                targetMealType = mealType,
                selectedTags = _selectedTags.value,
                searchQuery = _searchQuery.value,
                existingPlanIds = existingPlanIds
            )
            _suggestionContext.value = context

            // Collect suggestions
            getMealSuggestionsUseCase(context)
                .catch { error ->
                    _suggestionState.update {
                        SuggestionUiState.Error(error.message ?: "Failed to load suggestions")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { suggestions ->
                            if (suggestions.isEmpty()) {
                                // Determine empty reason
                                val reason = when {
                                    uiState.value.availableMeals.isEmpty() -> EmptyReason.NO_MEALS
                                    _selectedTags.value.isNotEmpty() || _searchQuery.value.isNotBlank() -> EmptyReason.NO_MATCHES
                                    else -> EmptyReason.ALL_PLANNED
                                }
                                _suggestionState.update { SuggestionUiState.Empty(reason) }
                            } else {
                                _suggestionState.update {
                                    SuggestionUiState.Success(suggestions, context)
                                }
                            }
                        },
                        onFailure = { error ->
                            _suggestionState.update {
                                SuggestionUiState.Error(error.message ?: "Failed to load suggestions")
                            }
                        }
                    )
                }
        }
    }

    /**
     * Updates the tag filter and refreshes suggestions.
     * Requirements: 2.1-2.5, 9.1-9.2
     */
    fun updateTagFilter(tag: MealTag) {
        _selectedTags.update { currentTags ->
            if (currentTags.contains(tag)) {
                currentTags - tag
            } else {
                currentTags + tag
            }
        }

        // Refresh suggestions if currently showing
        _suggestionContext.value?.let { context ->
            showSuggestions(context.targetDate, context.targetMealType)
        }
    }

    /**
     * Updates the search query and refreshes suggestions.
     * Requirements: 5.1-5.3
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        // Refresh suggestions if currently showing
        _suggestionContext.value?.let { context ->
            showSuggestions(context.targetDate, context.targetMealType)
        }
    }

    /**
     * Selects a suggested meal and adds it to the plan.
     * Requirements: 1.5, 10.3
     */
    fun selectSuggestion(meal: Meal) {
        viewModelScope.launch {
            val context = _suggestionContext.value ?: return@launch

            // Check if there's an existing plan for this slot
            val existingPlan = uiState.value.weekData?.plansByDate?.get(context.targetDate)
                ?.find { it.mealPlan.mealType == context.targetMealType }?.mealPlan

            val result = if (existingPlan != null) {
                updateMealPlanUseCase(existingPlan.id, meal.id)
            } else {
                assignMealToPlanUseCase(meal.id, context.targetDate, context.targetMealType).map { Unit }
            }

            result.fold(
                onSuccess = {
                    // Clear search query after successful selection
                    _searchQuery.value = ""
                    // Hide suggestions
                    hideSuggestions()
                    // Trigger shopping list regeneration
                    regenerateShoppingList()
                },
                onFailure = { error ->
                    _suggestionState.update {
                        SuggestionUiState.Error(error.message ?: "Failed to add meal to plan")
                    }
                }
            )
        }
    }

    /**
     * Hides the suggestions bottom sheet.
     * Requirements: 9.4
     */
    fun hideSuggestions() {
        _suggestionState.update { SuggestionUiState.Hidden }
        _suggestionContext.value = null
    }
    
    companion object {
        private const val KEY_CURRENT_WEEK_START = "current_week_start"
    }
}
