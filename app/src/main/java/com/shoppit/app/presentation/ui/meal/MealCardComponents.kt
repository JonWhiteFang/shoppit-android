package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Meal

/**
 * Optimized meal card content component.
 * Extracted to minimize recomposition scope.
 * 
 * Requirements: 5.3
 * 
 * @param meal The meal to display
 * @param modifier Optional modifier
 */
@Composable
fun MealCardContent(
    meal: Meal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = meal.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${meal.ingredients.size} ingredient${if (meal.ingredients.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Optimized ingredient count text component.
 * Extracted to minimize recomposition scope.
 * 
 * Requirements: 5.3
 * 
 * @param count Number of ingredients
 * @param modifier Optional modifier
 */
@Composable
fun IngredientCountText(
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$count ingredient${if (count != 1) "s" else ""}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}
