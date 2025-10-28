package com.shoppit.app.presentation.ui.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.presentation.ui.common.AnimatedPriorityBadge
import com.shoppit.app.presentation.ui.common.AnimatedQuantityChange

/**
 * Enhanced composable for displaying a shopping list item card.
 * Shows item name, quantity, unit, checked status, priority indicator,
 * note indicator, price, and quantity adjustment buttons.
 *
 * @param item The shopping list item to display
 * @param onCheckedChange Callback when the checked status changes
 * @param onClick Callback when the item is clicked
 * @param onIncrementQuantity Callback when quantity is incremented
 * @param onDecrementQuantity Callback when quantity is decremented
 * @param onTogglePriority Callback when priority is toggled
 * @param onAddNote Callback when add/edit note is selected
 * @param onUpdatePrice Callback when update price is selected
 * @param onDuplicate Callback when duplicate is selected
 * @param onDelete Callback when delete is selected (only for manual items)
 * @param modifier Optional modifier for the card
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListItemCard(
    item: ShoppingListItem,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onIncrementQuantity: ((Long) -> Unit)? = null,
    onDecrementQuantity: ((Long) -> Unit)? = null,
    onTogglePriority: ((Long, Boolean) -> Unit)? = null,
    onAddNote: ((ShoppingListItem) -> Unit)? = null,
    onUpdatePrice: ((ShoppingListItem) -> Unit)? = null,
    onDuplicate: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Animate color transitions
    val containerColor by animateColorAsState(
        targetValue = if (item.isChecked) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else if (item.isPriority) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
    
    // Check if quantity is numeric for increment/decrement buttons
    val isNumericQuantity = remember(item.quantity) {
        item.quantity.toIntOrNull() != null
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
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
                    // Priority indicator with animation
                    AnimatedPriorityBadge(visible = item.isPriority) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = "Priority item",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = textDecoration,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    )
                    
                    // Note indicator
                    if (item.notes.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = "Has notes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
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
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quantity with increment/decrement buttons and animation
                    if (item.quantity.isNotBlank() || item.unit.isNotBlank()) {
                        if (isNumericQuantity && onIncrementQuantity != null && onDecrementQuantity != null) {
                            AnimatedQuantityChange(targetScale = 1f) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onDecrementQuantity(item.id) },
                                        modifier = Modifier.size(24.dp),
                                        enabled = !item.isChecked
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease quantity",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    
                                    Text(
                                        text = "${item.quantity} ${item.unit}".trim(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = quantityAlpha)
                                    )
                                    
                                    IconButton(
                                        onClick = { onIncrementQuantity(item.id) },
                                        modifier = Modifier.size(24.dp),
                                        enabled = !item.isChecked
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase quantity",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "${item.quantity} ${item.unit}".trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = quantityAlpha)
                            )
                        }
                    }
                    
                    // Price display
                    if (item.estimatedPrice != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format("%.2f", item.estimatedPrice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            // Long-press menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (onTogglePriority != null) {
                    DropdownMenuItem(
                        text = { 
                            Text(if (item.isPriority) "Remove Priority" else "Mark as Priority") 
                        },
                        onClick = {
                            onTogglePriority(item.id, !item.isPriority)
                            showMenu = false
                        }
                    )
                }
                
                if (onAddNote != null) {
                    DropdownMenuItem(
                        text = { 
                            Text(if (item.notes.isNotBlank()) "Edit Note" else "Add Note") 
                        },
                        onClick = {
                            onAddNote(item)
                            showMenu = false
                        }
                    )
                }
                
                if (onUpdatePrice != null) {
                    DropdownMenuItem(
                        text = { 
                            Text(if (item.estimatedPrice != null) "Update Price" else "Add Price") 
                        },
                        onClick = {
                            onUpdatePrice(item)
                            showMenu = false
                        }
                    )
                }
                
                if (onDuplicate != null) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            onDuplicate(item.id)
                            showMenu = false
                        }
                    )
                }
                
                if (item.isManual && onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete(item.id)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
