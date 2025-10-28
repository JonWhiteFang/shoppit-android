package com.shoppit.app.presentation.ui.shopping

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Dialog for voice input functionality.
 * Displays a microphone animation while listening and shows recognized text.
 * Handles microphone permission requests and displays rationale when needed.
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
    val context = LocalContext.current
    var editedText by remember(recognizedText) { mutableStateOf(recognizedText) }
    
    var hasMicrophonePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicrophonePermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        } else {
            // Permission granted, start listening
            onStartListening()
        }
    }
    
    // Check permission when dialog opens
    LaunchedEffect(Unit) {
        if (!hasMicrophonePermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    
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
                    showPermissionRationale -> {
                        // Permission rationale content
                        PermissionRationaleContent(
                            onRequestPermission = {
                                showPermissionRationale = false
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        )
                    }
                    
                    !hasMicrophonePermission -> {
                        // Waiting for permission
                        CircularProgressIndicator()
                        Text(
                            text = "Requesting microphone permission...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
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
                showPermissionRationale -> {
                    // No confirm button when showing rationale
                }
                
                recognizedText.isNotBlank() && !isProcessing -> {
                    TextButton(
                        onClick = { onConfirm(editedText) },
                        enabled = editedText.isNotBlank()
                    ) {
                        Text("Add to List")
                    }
                }
                
                !isListening && !isProcessing && hasMicrophonePermission -> {
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
 * Permission rationale content shown when microphone permission is denied.
 */
@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = "Microphone Permission Required",
            style = MaterialTheme.typography.titleMedium
        )
        
        Text(
            text = "To use voice input, Shoppit needs access to your microphone. " +
                    "This permission is only used for voice commands to add items to your shopping list.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permission")
        }
    }
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
