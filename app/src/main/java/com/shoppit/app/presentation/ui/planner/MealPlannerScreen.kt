package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.LoadingScreen
import java.time.LocalDate

/**
 * Stateful meal planner screen composable.
 * Manages ViewModel injection and state collection.
 *
 * @param viewModel The meal planner ViewModel (injected via Hilt)
 * @param onMealDetailClick Callback when a meal detail is requested
 * @param modifier Optional modifier
 */
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel = hiltViewModel(),
    onMealDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MealPlannerContent(
        uiState = uiState,
        onSlotClick = viewModel::onSlotClick,
        onMealSelected = viewModel::onMealSelected,
        onDismissMealSelection = viewModel::dismissMealSelection,
        onDeleteMealPlan = viewModel::deleteMealPlan,
        onNextWeek = viewModel::navigateToNextWeek,
        onPreviousWeek = viewModel::navigateToPreviousWeek,
        onToday = viewModel::navigateToToday,
        onCopyDay = viewModel::copyDay,
        onClearDay = viewModel::clearDay,
        onMealDetailClick = onMealDetailClick,
        modifier = modifier
    )
}

/**
 * Stateless meal planner content composable.
 * Handles UI rendering based on state.
 *
 * @param uiState Current UI state
 * @param onSlotClick Callback when a meal slot is clicked
 * @param onMealSelected Callback when a meal is selected from the dialog
 * @param onDismissMealSelection Callback to dismiss the meal selection dialog
 * @param onDeleteMealPlan Callback to delete a meal plan
 * @param onNextWeek Callback to navigate to next week
 * @param onPreviousWeek Callback to navigate to previous week
 * @param onToday Callback to navigate to current week
 * @param onCopyDay Callback to copy a day's plans
 * @param onClearDay Callback to clear a day's plans
 * @param onMealDetailClick Callback when meal details are requested
 * @param modifier Optional modifier
 */
@Composable
fun MealPlannerContent(
    uiState: MealPlannerUiState,
    onSlotClick: (LocalDate, MealType) -> Unit,
    onMealSelected: (Long) -> Unit,
    onDismissMealSelection: () -> Unit,
    onDeleteMealPlan: (Long) -> Unit,
    onNextWeek: () -> Unit,
    onPreviousWeek: () -> Unit,
    onToday: () -> Unit,
    onCopyDay: (LocalDate, LocalDate, Boolean) -> Unit,
    onClearDay: (LocalDate) -> Unit,
    onMealDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WeekNavigationBar(
                weekStart = uiState.currentWeekStart,
                weekEnd = uiState.currentWeekStart.plusDays(6),
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onToday = onToday
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingScreen(
                modifier = Modifier.padding(padding),
                message = "Loading meal plans..."
            )
            uiState.error != null -> ErrorScreen(
                message = uiState.error,
                onRetry = null,
                modifier = Modifier.padding(padding)
            )
            uiState.weekData != null -> {
                WeekCalendarView(
                    weekData = uiState.weekData,
                    currentDate = LocalDate.now(),
                    onSlotClick = onSlotClick,
                    onDeleteMealPlan = onDeleteMealPlan,
                    onCopyDay = onCopyDay,
                    onClearDay = onClearDay,
                    onMealDetailClick = onMealDetailClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }

        if (uiState.showMealSelection) {
            MealSelectionDialog(
                meals = uiState.availableMeals,
                selectedSlot = uiState.selectedSlot,
                onMealSelected = onMealSelected,
                onDismiss = onDismissMealSelection
            )
        }
    }
}
