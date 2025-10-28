package com.shoppit.app.presentation.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Section for displaying suggested items based on meal plans.
 * Shows a horizontal list of suggested items that can be added or dismissed.
 *
 * @param suggestedItems List of suggested item names
 * @param onAddItem Callback when a suggested item is added
 * @param onDismissItem Callback when a suggested item is dismissed
 * @param modifier Optional modifier for the section
 */
@Composable
fun SuggestionsSection(
    suggestedItems: List<String>,
    onAddItem: (String) -> Unit,
    onDismissItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestedItems.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Suggested Items",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Text(
                text = "Based on your meal plans, you might also need:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            
            // Horizontal scrolling list of suggestions
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = suggestedItems,
                    key = { it }
                ) { item ->
                    SuggestionItemChip(
                        itemName = item,
                        onAdd = { onAddItem(item) },
                        onDismiss = { onDismissItem(item) }
                    )
                }
            }
        }
    }
}

/**
 * Chip for displaying a single suggested item.
 * Shows the item name with add and dismiss actions.
 */
@Composable
fun SuggestionItemChip(
    itemName: String,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Add button
            IconButton(
                onClick = onAdd,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add $itemName",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss $itemName",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Alternative compact version using Material3 SuggestionChip.
 */
@Composable
fun CompactSuggestionChip(
    itemName: String,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        onClick = onAdd,
        label = { Text(itemName) },
        icon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )
        },
        modifier = modifier
    )
}
