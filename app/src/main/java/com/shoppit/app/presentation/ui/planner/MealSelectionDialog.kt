package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate

/**
 * Dialog for selecting a meal to assign to a meal plan slot.
 * Includes search functionality and displays meal list.
 *
 * @param meals List of available meals
 * @param selectedSlot The slot being filled (contains date, meal type, and existing plan if any)
 * @param onMealSelected Callback when a meal is selected
 * @param onDismiss Callback to dismiss the dialog
 * @param modifier Optional modifier
 */
@Composable
fun MealSelectionDialog(
    meals: List<Meal>,
    selectedSlot: MealSlot?,
    onMealSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredMeals = remember(meals, searchQuery) {
        if (searchQuery.isBlank()) {
            meals
        } else {
            meals.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedSlot?.existingPlan != null) {
                    "Replace Meal"
                } else {
                    "Select Meal for ${selectedSlot?.mealType?.displayName() ?: "Meal"}"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search meals") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredMeals.isEmpty()) {
                    Text(
                        text = if (meals.isEmpty()) {
                            "No meals available. Create meals first."
                        } else {
                            "No meals match your search."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(filteredMeals, key = { it.id }) { meal ->
                            MealSelectionItem(
                                meal = meal,
                                onClick = { onMealSelected(meal.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun MealSelectionDialogPreview() {
    val meals = listOf(
        Meal(
            id = 1,
            name = "Spaghetti Carbonara",
            ingredients = emptyList()
        ),
        Meal(
            id = 2,
            name = "Caesar Salad",
            ingredients = emptyList()
        ),
        Meal(
            id = 3,
            name = "Grilled Chicken",
            ingredients = emptyList()
        )
    )

    ShoppitTheme {
        MealSelectionDialog(
            meals = meals,
            selectedSlot = MealSlot(
                date = LocalDate.now(),
                mealType = MealType.LUNCH,
                existingPlan = null
            ),
            onMealSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSelectionDialogEmptyPreview() {
    ShoppitTheme {
        MealSelectionDialog(
            meals = emptyList(),
            selectedSlot = MealSlot(
                date = LocalDate.now(),
                mealType = MealType.DINNER,
                existingPlan = null
            ),
            onMealSelected = {},
            onDismiss = {}
        )
    }
}
