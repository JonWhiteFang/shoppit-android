package com.shoppit.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Utility object for handling runtime permissions in the app.
 * Provides helper functions and composables for requesting and checking permissions.
 */
object PermissionHandler {
    
    /**
     * Checks if a specific permission is granted.
     *
     * @param context The application context
     * @param permission The permission to check (e.g., Manifest.permission.CAMERA)
     * @return True if the permission is granted, false otherwise
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Checks if location permission (fine or coarse) is granted.
     *
     * @param context The application context
     * @return True if either fine or coarse location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    
    /**
     * Checks if camera permission is granted.
     *
     * @param context The application context
     * @return True if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.CAMERA)
    }
    
    /**
     * Checks if microphone permission is granted.
     *
     * @param context The application context
     * @return True if microphone permission is granted
     */
    fun hasMicrophonePermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO)
    }
}

/**
 * Composable for requesting location permission with rationale dialog.
 * This is prepared for future implementation of location-based store notifications.
 *
 * @param onPermissionResult Callback invoked with the permission result (true if granted)
 * @param onDismiss Callback when the permission flow is dismissed
 * @param modifier Optional modifier for the dialog
 */
@Composable
fun LocationPermissionDialog(
    onPermissionResult: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    var hasLocationPermission by remember {
        mutableStateOf(PermissionHandler.hasLocationPermission(context))
    }
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (!isGranted) {
            showPermissionRationale = true
        } else {
            onPermissionResult(true)
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            onPermissionResult(true)
        }
    }
    
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                onPermissionResult(false)
            },
            title = { 
                Text(
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Text(
                        text = "To receive notifications when you're near a store with items on your list, " +
                                "Shoppit needs access to your location. This permission is only used for " +
                                "store proximity notifications and can be disabled at any time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Your location data is never shared or stored on external servers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismiss()
                        onPermissionResult(false)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not Now")
                }
            },
            modifier = modifier
        )
    }
}

/**
 * State holder for permission request results.
 */
data class PermissionState(
    val isGranted: Boolean = false,
    val shouldShowRationale: Boolean = false,
    val isPermanentlyDenied: Boolean = false
)

/**
 * Permission types supported by the app.
 */
enum class AppPermission(val manifestPermission: String) {
    CAMERA(Manifest.permission.CAMERA),
    MICROPHONE(Manifest.permission.RECORD_AUDIO),
    LOCATION_FINE(Manifest.permission.ACCESS_FINE_LOCATION),
    LOCATION_COARSE(Manifest.permission.ACCESS_COARSE_LOCATION);
    
    /**
     * Gets a user-friendly name for the permission.
     */
    fun getDisplayName(): String = when (this) {
        CAMERA -> "Camera"
        MICROPHONE -> "Microphone"
        LOCATION_FINE, LOCATION_COARSE -> "Location"
    }
    
    /**
     * Gets a description of why the permission is needed.
     */
    fun getRationale(): String = when (this) {
        CAMERA -> "To scan barcodes, Shoppit needs access to your camera. " +
                "This permission is only used for barcode scanning."
        MICROPHONE -> "To use voice input, Shoppit needs access to your microphone. " +
                "This permission is only used for voice commands to add items to your shopping list."
        LOCATION_FINE, LOCATION_COARSE -> "To receive notifications when you're near a store with items on your list, " +
                "Shoppit needs access to your location. This permission is only used for " +
                "store proximity notifications and can be disabled at any time."
    }
}
