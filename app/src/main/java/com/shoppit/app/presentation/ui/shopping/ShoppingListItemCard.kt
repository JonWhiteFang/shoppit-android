package com.shoppit.app.presentation.ui.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.ShoppingListItem

/**
 * Composable for displaying a shopping list item card.
 * Shows item name, quantity, unit, and checked status.
 *
 * @param item The shopping list item to display
 * @param onCheckedChange Callback when the checked status changes
 * @param onClick Callback when the item is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animate color transitions
    val containerColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 300),
        label = "containerColor"
    )
    
    // Animate text alpha
    val textAlpha by animateFloatAsState(
        targetValue = if (item.isChecked) 0.6f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "textAlpha"
    )
    
    val quantityAlpha by animateFloatAsState(
        targetValue = if (item.isChecked) 0.5f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "quantityAlpha"
    )
    
    val textDecoration = remember(item.isChecked) {
        if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = textDecoration,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    )
                    
                    // Show badge for items from multiple meals
                    if (item.mealIds.size > 1) {
                        Badge {
                            Text(
                                text = "${item.mealIds.size} meals",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    // Show manual indicator
                    if (item.isManual) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = "Manual",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                if (item.quantity.isNotBlank() || item.unit.isNotBlank()) {
                    Text(
                        text = "${item.quantity} ${item.unit}".trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = quantityAlpha)
                    )
                }
            }
        }
    }
}
