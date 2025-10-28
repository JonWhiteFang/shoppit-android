package com.shoppit.app.presentation.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.usecase.AssignMealToPlanUseCase
import com.shoppit.app.domain.usecase.ClearDayPlansUseCase
import com.shoppit.app.domain.usecase.CopyDayPlansUseCase
import com.shoppit.app.domain.usecase.DeleteMealPlanUseCase
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
 */
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase,
    private val getMealsUseCase: GetMealsUseCase,
    private val assignMealToPlanUseCase: AssignMealToPlanUseCase,
    private val updateMealPlanUseCase: UpdateMealPlanUseCase,
    private val deleteMealPlanUseCase: DeleteMealPlanUseCase,
    private val copyDayPlansUseCase: CopyDayPlansUseCase,
    private val clearDayPlansUseCase: ClearDayPlansUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealPlannerUiState())
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
     * Navigates to the next week.
     */
    fun navigateToNextWeek() {
        _uiState.update {
            it.copy(currentWeekStart = it.currentWeekStart.plusWeeks(1))
        }
        loadWeekData()
    }

    /**
     * Navigates to the previous week.
     */
    fun navigateToPreviousWeek() {
        _uiState.update {
            it.copy(currentWeekStart = it.currentWeekStart.minusWeeks(1))
        }
        loadWeekData()
    }

    /**
     * Navigates to the current week (today).
     */
    fun navigateToToday() {
        _uiState.update {
            it.copy(currentWeekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY))
        }
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
                onSuccess = { /* List updates automatically via Flow */ },
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
                onSuccess = { /* List updates automatically via Flow */ },
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
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(error = error.message ?: "Failed to clear day")
                    }
                }
            )
        }
    }
}
