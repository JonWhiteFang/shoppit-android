package com.shoppit.app.presentation.ui.planner

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.WeekPlanData
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * UI state for the meal planner screen.
 * Manages weekly calendar view, meal selection, and user interactions.
 */
data class MealPlannerUiState(
    val weekData: WeekPlanData? = null,
    val currentWeekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showMealSelection: Boolean = false,
    val selectedSlot: MealSlot? = null,
    val availableMeals: List<Meal> = emptyList()
)

/**
 * Represents a slot in the meal planner calendar.
 * Each slot corresponds to a specific date and meal type combination.
 */
data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val existingPlan: MealPlan? = null
)
