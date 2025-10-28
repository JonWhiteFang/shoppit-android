package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Item component for displaying a meal in the meal selection dialog.
 * Shows meal name and ingredient count.
 *
 * @param meal The meal to display
 * @param onClick Callback when the item is clicked
 * @param modifier Optional modifier
 */
@Composable
fun MealSelectionItem(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${meal.ingredients.size} ingredients",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSelectionItemPreview() {
    ShoppitTheme {
        MealSelectionItem(
            meal = Meal(
                id = 1,
                name = "Spaghetti Carbonara",
                ingredients = emptyList()
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSelectionItemWithIngredientsPreview() {
    ShoppitTheme {
        MealSelectionItem(
            meal = Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = List(5) { 
                    com.shoppit.app.domain.model.Ingredient(
                        name = "Ingredient $it",
                        quantity = "1",
                        unit = "cup"
                    )
                }
            ),
            onClick = {}
        )
    }
}
