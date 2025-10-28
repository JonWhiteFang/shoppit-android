package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlanWithMeal
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dialog for confirming the clearing of all meal plans for a specific day.
 * Shows the date and list of meals that will be removed.
 *
 * @param date The date to clear meals from
 * @param mealPlans List of meal plans that will be cleared
 * @param onClearDay Callback when clear is confirmed
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ClearDayDialog(
    date: LocalDate,
    mealPlans: List<MealPlanWithMeal>,
    onClearDay: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Clear Day")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Clear all meals for ${date.format(dateFormatter)}?",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (mealPlans.isNotEmpty()) {
                    Text(
                        text = "This will remove ${mealPlans.size} meal${if (mealPlans.size > 1) "s" else ""}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        mealPlans.forEach { mealPlan ->
                            Text(
                                text = "• ${mealPlan.mealPlan.mealType.displayName()}: ${mealPlan.meal.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onClearDay
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ClearDayDialogPreview() {
    ShoppitTheme {
        ClearDayDialog(
            date = LocalDate.of(2024, 1, 15),
            mealPlans = listOf(
                MealPlanWithMeal(
                    mealPlan = com.shoppit.app.domain.model.MealPlan(
                        id = 1,
                        mealId = 1,
                        date = LocalDate.of(2024, 1, 15),
                        mealType = com.shoppit.app.domain.model.MealType.BREAKFAST
                    ),
                    meal = Meal(
                        id = 1,
                        name = "Oatmeal",
                        ingredients = emptyList()
                    )
                ),
                MealPlanWithMeal(
                    mealPlan = com.shoppit.app.domain.model.MealPlan(
                        id = 2,
                        mealId = 2,
                        date = LocalDate.of(2024, 1, 15),
                        mealType = com.shoppit.app.domain.model.MealType.LUNCH
                    ),
                    meal = Meal(
                        id = 2,
                        name = "Chicken Salad",
                        ingredients = emptyList()
                    )
                )
            ),
            onClearDay = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClearDayDialogEmptyPreview() {
    ShoppitTheme {
        ClearDayDialog(
            date = LocalDate.of(2024, 1, 16),
            mealPlans = emptyList(),
            onClearDay = {},
            onDismiss = {}
        )
    }
}
