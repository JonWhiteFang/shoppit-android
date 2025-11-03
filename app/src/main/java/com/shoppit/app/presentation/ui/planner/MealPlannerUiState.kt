package com.shoppit.app.presentation.ui.planner

import androidx.compose.runtime.Stable
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.domain.model.WeekPlanData
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * UI state for the meal planner screen.
 * Manages weekly calendar view, meal selection, and user interactions.
 */
@Stable
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
@Stable
data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val existingPlan: MealPlan? = null
)

/**
 * UI state for meal suggestions.
 * Manages the display of the suggestions bottom sheet.
 */
sealed interface SuggestionUiState {
    /**
     * Suggestions are hidden (bottom sheet not shown).
     */
    data object Hidden : SuggestionUiState

    /**
     * Loading suggestions from the use case.
     */
    data object Loading : SuggestionUiState

    /**
     * Successfully loaded suggestions.
     *
     * @property suggestions List of meal suggestions with scores
     * @property context The context used to generate these suggestions
     */
    @Stable
    data class Success(
        val suggestions: List<MealSuggestion>,
        val context: SuggestionContext
    ) : SuggestionUiState

    /**
     * Error occurred while loading suggestions.
     *
     * @property message Error message to display
     */
    data class Error(val message: String) : SuggestionUiState

    /**
     * No suggestions available.
     *
     * @property reason The reason why no suggestions are available
     */
    data class Empty(val reason: EmptyReason) : SuggestionUiState
}

/**
 * Reasons why suggestions might be empty.
 */
enum class EmptyReason {
    /**
     * User has no meals in their library.
     */
    NO_MEALS,

    /**
     * No meals match the current filters/search.
     */
    NO_MATCHES,

    /**
     * All meals are already planned for the week.
     */
    ALL_PLANNED
}
