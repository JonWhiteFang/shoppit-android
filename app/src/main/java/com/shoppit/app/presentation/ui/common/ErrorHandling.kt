package com.shoppit.app.presentation.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shoppit.app.domain.error.ErrorMapper

/**
 * Displays an error dialog with user-friendly message and optional retry action.
 * Automatically maps technical errors to user-friendly messages.
 */
@Composable
fun ErrorDialog(
    error: Throwable,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val userMessage = ErrorMapper.toUserMessage(error)
    val suggestedAction = ErrorMapper.getSuggestedAction(error)
    val isRecoverable = ErrorMapper.isRecoverable(error)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = buildString {
                    append(userMessage)
                    if (suggestedAction != null) {
                        append("\n\n")
                        append(suggestedAction)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            if (isRecoverable && onRetry != null) {
                Button(onClick = {
                    onDismiss()
                    onRetry()
                }) {
                    Text("Retry")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            if (isRecoverable && onRetry != null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Displays a permission rationale dialog explaining why permission is needed.
 */
@Composable
fun PermissionRationaleDialog(
    permissionName: String,
    rationale: String,
    fallbackMessage: String,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$permissionName Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = buildString {
                    append(rationale)
                    append("\n\n")
                    append("Alternative: ")
                    append(fallbackMessage)
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onRequestPermission()
            }) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
        modifier = modifier
    )
}

/**
 * Displays a voice input error dialog with helpful suggestions.
 */
@Composable
fun VoiceInputErrorDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onManualInput: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Voice Input Error",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = buildString {
                    append(errorMessage)
                    append("\n\n")
                    append("Tips for better recognition:")
                    append("\n• Speak clearly and slowly")
                    append("\n• Say 'add' before the item name")
                    append("\n• Include quantity: 'add 2 pounds of chicken'")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onRetry()
            }) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onManualInput()
            }) {
                Text("Type Instead")
            }
        },
        modifier = modifier
    )
}

/**
 * Displays a barcode scan error dialog with fallback option.
 */
@Composable
fun BarcodeScanErrorDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onManualInput: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Barcode Scan Error",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = buildString {
                    append(errorMessage)
                    append("\n\n")
                    append("Tips for better scanning:")
                    append("\n• Ensure good lighting")
                    append("\n• Hold camera steady")
                    append("\n• Keep barcode in focus")
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onRetry()
            }) {
                Text("Try Again")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                onManualInput()
            }) {
                Text("Enter Manually")
            }
        },
        modifier = modifier
    )
}
