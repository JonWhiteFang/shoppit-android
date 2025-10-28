package com.shoppit.app.presentation.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.ShoppingListItem

/**
 * Dialog for adding or editing notes on a shopping list item.
 * Provides a multiline text field for entering item-specific notes.
 *
 * @param item The shopping list item to add/edit notes for
 * @param onConfirm Callback when the note is saved with item ID and note text
 * @param onDismiss Callback when the dialog is dismissed
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun ItemNoteDialog(
    item: ShoppingListItem,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember(item.notes) { mutableStateOf(item.notes) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (item.notes.isNotBlank()) "Edit Note" else "Add Note",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item name for context
                Text(
                    text = "Item: ${item.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Multiline note input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    placeholder = { Text("e.g., Get the organic version, Brand preference, etc.") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Helper text
                Text(
                    text = "Add specific preferences or details about this item.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(item.id, noteText.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}
