package com.shoppit.app.presentation.ui.shopping

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

/**
 * Dialog for voice input functionality.
 * Displays a microphone animation while listening and shows recognized text.
 *
 * @param isListening Whether the app is currently listening for voice input
 * @param isProcessing Whether the voice input is being processed
 * @param recognizedText The text recognized from voice input
 * @param onConfirm Callback when the user confirms the recognized text
 * @param onDismiss Callback when the dialog is dismissed
 * @param onStartListening Callback to start listening for voice input
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun VoiceInputDialog(
    isListening: Boolean,
    isProcessing: Boolean,
    recognizedText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editedText by remember(recognizedText) { mutableStateOf(recognizedText) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Voice Input",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isListening -> {
                        // Microphone animation while listening
                        MicrophoneAnimation()
                        Text(
                            text = "Listening...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Say something like \"add 2 pounds of chicken\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    isProcessing -> {
                        // Processing indicator
                        CircularProgressIndicator()
                        Text(
                            text = "Processing...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    recognizedText.isNotBlank() -> {
                        // Show recognized text for editing
                        Text(
                            text = "Recognized text:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            label = { Text("Item") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Text(
                            text = "Edit if needed, then confirm to add to your list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    else -> {
                        // Initial state - ready to start listening
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tap 'Start' to begin voice input",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Say something like \"add 2 pounds of chicken\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                recognizedText.isNotBlank() && !isProcessing -> {
                    TextButton(
                        onClick = { onConfirm(editedText) },
                        enabled = editedText.isNotBlank()
                    ) {
                        Text("Add to List")
                    }
                }
                !isListening && !isProcessing -> {
                    TextButton(onClick = onStartListening) {
                        Text("Start")
                    }
                }
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

/**
 * Animated microphone icon that pulses while listening.
 */
@Composable
fun MicrophoneAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micAnimation")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .scale(scale),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
