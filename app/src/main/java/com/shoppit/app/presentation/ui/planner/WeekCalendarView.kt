package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealPlanWithMeal
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.WeekPlanData
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate

/**
 * Week calendar view composable showing a grid of meal slots.
 * Displays 7 days (columns) x 4 meal types (rows).
 *
 * @param weekData The week's meal plan data
 * @param currentDate Today's date for highlighting
 * @param onSlotClick Callback when a meal slot is clicked
 * @param onDeleteMealPlan Callback to delete a meal plan
 * @param onCopyDay Callback to copy a day's plans
 * @param onClearDay Callback to clear a day's plans
 * @param onMealDetailClick Callback when meal details are requested
 * @param modifier Optional modifier
 */
@Composable
fun WeekCalendarView(
    weekData: WeekPlanData,
    currentDate: LocalDate,
    onSlotClick: (LocalDate, MealType) -> Unit,
    onDeleteMealPlan: (Long) -> Unit,
    onCopyDay: (LocalDate, LocalDate, Boolean) -> Unit,
    onClearDay: (LocalDate) -> Unit,
    onMealDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = (0..6).map { weekData.startDate.plusDays(it.toLong()) }

    LazyColumn(modifier = modifier) {
        // Header row with dates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dates.forEach { date ->
                    DayHeader(
                        date = date,
                        isToday = date == currentDate,
                        planCount = weekData.plansByDate[date]?.size ?: 0,
                        onCopyDay = { targetDate -> onCopyDay(date, targetDate, false) },
                        onClearDay = { onClearDay(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Meal type rows
        items(MealType.entries.toTypedArray()) { mealType ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dates.forEach { date ->
                    val mealPlan = weekData.plansByDate[date]
                        ?.find { it.mealPlan.mealType == mealType }

                    MealSlotCard(
                        date = date,
                        mealType = mealType,
                        mealPlanWithMeal = mealPlan,
                        onClick = { onSlotClick(date, mealType) },
                        onDelete = mealPlan?.let { { onDeleteMealPlan(it.mealPlan.id) } },
                        onViewDetails = mealPlan?.let { { onMealDetailClick(it.meal.id) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekCalendarViewPreview() {
    val startDate = LocalDate.of(2024, 1, 1)
    val weekData = WeekPlanData(
        startDate = startDate,
        endDate = startDate.plusDays(6),
        plansByDate = mapOf(
            startDate to listOf(
                MealPlanWithMeal(
                    mealPlan = MealPlan(
                        id = 1,
                        mealId = 1,
                        date = startDate,
                        mealType = MealType.BREAKFAST
                    ),
                    meal = Meal(
                        id = 1,
                        name = "Oatmeal",
                        ingredients = emptyList()
                    )
                ),
                MealPlanWithMeal(
                    mealPlan = MealPlan(
                        id = 2,
                        mealId = 2,
                        date = startDate,
                        mealType = MealType.LUNCH
                    ),
                    meal = Meal(
                        id = 2,
                        name = "Salad",
                        ingredients = emptyList()
                    )
                )
            ),
            startDate.plusDays(1) to listOf(
                MealPlanWithMeal(
                    mealPlan = MealPlan(
                        id = 3,
                        mealId = 3,
                        date = startDate.plusDays(1),
                        mealType = MealType.DINNER
                    ),
                    meal = Meal(
                        id = 3,
                        name = "Pasta",
                        ingredients = emptyList()
                    )
                )
            )
        )
    )

    ShoppitTheme {
        WeekCalendarView(
            weekData = weekData,
            currentDate = startDate,
            onSlotClick = { _, _ -> },
            onDeleteMealPlan = {},
            onCopyDay = { _, _, _ -> },
            onClearDay = {},
            onMealDetailClick = {}
        )
    }
}
