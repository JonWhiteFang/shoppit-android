package com.shoppit.app.presentation.ui.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.usecase.AssignMealToPlanUseCase
import com.shoppit.app.domain.usecase.ClearDayPlansUseCase
import com.shoppit.app.domain.usecase.CopyDayPlansUseCase
import com.shoppit.app.domain.usecase.DeleteMealPlanUseCase
import com.shoppit.app.domain.usecase.GenerateShoppingListUseCase
import com.shoppit.app.domain.usecase.GetMealPlansForWeekUseCase
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
     * Handles click on a meal slot to show meal selection dialog.
     */
    fun onSlotClick(date: LocalDate, mealType: MealType) {
        val existingPlan = uiState.value.weekData?.plansByDate?.get(date)
            ?.find { it.mealPlan.mealType == mealType }?.mealPlan

        _uiState.update {
            it.copy(
                showMealSelection = true,
                selectedSlot = MealSlot(date, mealType, existingPlan)
            )
        }
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
    
    companion object {
        private const val KEY_CURRENT_WEEK_START = "current_week_start"
    }
}
