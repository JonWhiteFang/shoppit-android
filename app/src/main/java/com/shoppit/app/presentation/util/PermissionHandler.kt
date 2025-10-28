package com.shoppit.app.presentation.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.shoppit.app.domain.error.AppError

/**
 * Handles runtime permissions for camera, microphone, and location.
 * Provides user-friendly error messages and fallback options.
 */
object PermissionHandler {
    
    /**
     * Permission types supported by the app.
     */
    enum class Permission(val manifestPermission: String, val displayName: String) {
        CAMERA(Manifest.permission.CAMERA, "Camera"),
        MICROPHONE(Manifest.permission.RECORD_AUDIO, "Microphone"),
        LOCATION(Manifest.permission.ACCESS_FINE_LOCATION, "Location")
    }
    
    /**
     * Checks if a permission is granted.
     */
    fun isPermissionGranted(context: Context, permission: Permission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Gets a user-friendly error message for permission denial.
     */
    fun getPermissionDeniedMessage(permission: Permission): String {
        return when (permission) {
            Permission.CAMERA -> 
                "Camera permission is required to scan barcodes. Please enable it in Settings."
            Permission.MICROPHONE -> 
                "Microphone permission is required for voice input. Please enable it in Settings."
            Permission.LOCATION -> 
                "Location permission is required for store notifications. Please enable it in Settings."
        }
    }
    
    /**
     * Gets a fallback option message when permission is denied.
     */
    fun getFallbackMessage(permission: Permission): String {
        return when (permission) {
            Permission.CAMERA -> 
                "You can manually enter item names instead of scanning barcodes."
            Permission.MICROPHONE -> 
                "You can type item names instead of using voice input."
            Permission.LOCATION -> 
                "You can manually check your shopping list when near a store."
        }
    }
    
    /**
     * Creates an AppError for permission denial.
     */
    fun createPermissionError(permission: Permission): AppError.PermissionDenied {
        return AppError.PermissionDenied(permission.displayName)
    }
}

/**
 * Composable function to handle permission requests.
 * Returns a launcher that can be used to request permissions.
 */
@Composable
fun rememberPermissionLauncher(
    permission: PermissionHandler.Permission,
    onGranted: () -> Unit,
    onDenied: () -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted()
        } else {
            onDenied()
        }
    }
}

/**
 * Composable function to check and request permission if needed.
 */
@Composable
fun rememberPermissionState(
    permission: PermissionHandler.Permission,
    onGranted: () -> Unit,
    onDenied: () -> Unit
): PermissionState {
    val context = LocalContext.current
    val isGranted = remember(permission) {
        PermissionHandler.isPermissionGranted(context, permission)
    }
    
    val launcher = rememberPermissionLauncher(
        permission = permission,
        onGranted = onGranted,
        onDenied = onDenied
    )
    
    return remember(isGranted, launcher) {
        PermissionState(
            permission = permission,
            isGranted = isGranted,
            launcher = launcher
        )
    }
}

/**
 * State holder for permission status and launcher.
 */
data class PermissionState(
    val permission: PermissionHandler.Permission,
    val isGranted: Boolean,
    val launcher: ManagedActivityResultLauncher<String, Boolean>
) {
    /**
     * Requests the permission if not already granted.
     */
    fun requestIfNeeded() {
        if (!isGranted) {
            launcher.launch(permission.manifestPermission)
        }
    }
}
