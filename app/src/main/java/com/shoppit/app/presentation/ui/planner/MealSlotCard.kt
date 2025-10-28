package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealPlanWithMeal
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate

/**
 * Card component for a meal slot in the planner calendar.
 * Shows meal name if assigned, or add icon if empty.
 *
 * @param date The date for this slot
 * @param mealType The meal type for this slot
 * @param mealPlanWithMeal The meal plan with meal details, if assigned
 * @param onClick Callback when the slot is clicked
 * @param onDelete Callback when delete button is clicked (null if empty)
 * @param onViewDetails Callback when view details button is clicked (null if empty)
 * @param modifier Optional modifier
 */
@Composable
fun MealSlotCard(
    date: LocalDate,
    mealType: MealType,
    mealPlanWithMeal: MealPlanWithMeal?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onViewDetails: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mealPlanWithMeal != null) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (mealPlanWithMeal != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mealPlanWithMeal.meal.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        onViewDetails?.let {
                            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View details",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSlotCardEmptyPreview() {
    ShoppitTheme {
        MealSlotCard(
            date = LocalDate.now(),
            mealType = MealType.LUNCH,
            mealPlanWithMeal = null,
            onClick = {},
            onDelete = null,
            onViewDetails = null
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSlotCardFilledPreview() {
    ShoppitTheme {
        MealSlotCard(
            date = LocalDate.now(),
            mealType = MealType.LUNCH,
            mealPlanWithMeal = MealPlanWithMeal(
                mealPlan = MealPlan(
                    id = 1,
                    mealId = 1,
                    date = LocalDate.now(),
                    mealType = MealType.LUNCH
                ),
                meal = Meal(
                    id = 1,
                    name = "Spaghetti Carbonara",
                    ingredients = emptyList()
                )
            ),
            onClick = {},
            onDelete = {},
            onViewDetails = {}
        )
    }
}
