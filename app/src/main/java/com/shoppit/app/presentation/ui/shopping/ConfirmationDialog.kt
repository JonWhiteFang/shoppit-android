package com.shoppit.app.presentation.ui.shopping

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Reusable confirmation dialog for destructive actions.
 *
 * @param title The dialog title
 * @param message The confirmation message
 * @param confirmText The text for the confirm button
 * @param onConfirm Callback when the user confirms
 * @param onDismiss Callback when the user dismisses the dialog
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
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
