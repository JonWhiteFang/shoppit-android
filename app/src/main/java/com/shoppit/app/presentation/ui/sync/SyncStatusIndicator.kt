package com.shoppit.app.presentation.ui.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Compact sync status indicator for app bar or bottom navigation.
 * Shows current sync state with icon and optional text.
 */
@Composable
fun SyncStatusIndicator(
    syncState: SyncUiState,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        modifier = modifier
            .semantics {
                contentDescription = getSyncStatusDescription(syncState)
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            syncState.isSyncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                if (showText) {
                    Text(
                        text = "Syncing...",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            syncState.syncStatus == SyncStatus.SUCCESS && syncState.lastSyncTime != null -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                if (showText) {
                    Text(
                        text = formatSyncTime(syncState.lastSyncTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            syncState.syncStatus == SyncStatus.ERROR -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Sync error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                if (showText) {
                    TextButton(
                        onClick = onManualSync,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            syncState.syncStatus == SyncStatus.OFFLINE -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (showText && syncState.pendingChangesCount > 0) {
                    Text(
                        text = "${syncState.pendingChangesCount} pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            syncState.syncStatus == SyncStatus.IDLE && syncState.isAuthenticated -> {
                IconButton(
                    onClick = onManualSync,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync now",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            !syncState.isAuthenticated -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Not signed in",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (showText) {
                    Text(
                        text = "Offline mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Detailed sync status card with more information and actions.
 * Suitable for settings screen or dedicated sync status view.
 */
@Composable
fun SyncStatusCard(
    syncState: SyncUiState,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleMedium
                )
                
                SyncStatusBadge(syncState.syncStatus)
            }

            Divider()

            // Status details
            if (syncState.isAuthenticated) {
                SyncStatusDetails(syncState)
            } else {
                Text(
                    text = "Sign in to enable cloud backup and sync",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            if (syncState.isAuthenticated) {
                Button(
                    onClick = onManualSync,
                    enabled = !syncState.isSyncing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Syncing...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sync Now")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusDetails(syncState: SyncUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Last sync time
        if (syncState.lastSyncTime != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Last synced: ${formatFullSyncTime(syncState.lastSyncTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pending changes
        if (syncState.pendingChangesCount > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PendingActions,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${syncState.pendingChangesCount} changes pending sync",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Error message
        if (syncState.syncStatus == SyncStatus.ERROR) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Last sync failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SyncStatusBadge(status: SyncStatus) {
    val (text, color) = when (status) {
        SyncStatus.IDLE -> "Idle" to MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.SYNCING -> "Syncing" to MaterialTheme.colorScheme.primary
        SyncStatus.SUCCESS -> "Synced" to MaterialTheme.colorScheme.primary
        SyncStatus.ERROR -> "Error" to MaterialTheme.colorScheme.error
        SyncStatus.OFFLINE -> "Offline" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Snackbar for sync success/error messages.
 */
@Composable
fun SyncStatusSnackbar(
    syncState: SyncUiState,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    // Show success message
    LaunchedEffect(syncState.showSuccess, syncState.successMessage) {
        if (syncState.showSuccess && syncState.successMessage != null) {
            snackbarHostState.showSnackbar(
                message = syncState.successMessage,
                duration = SnackbarDuration.Short
            )
            onDismissSuccess()
        }
    }

    // Show error message
    LaunchedEffect(syncState.showError, syncState.errorMessage) {
        if (syncState.showError && syncState.errorMessage != null) {
            snackbarHostState.showSnackbar(
                message = syncState.errorMessage,
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            onDismissError()
        }
    }
}

/**
 * Format sync time for compact display (e.g., "2m ago", "1h ago").
 */
private fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}

/**
 * Format sync time for detailed display (e.g., "Today at 2:30 PM").
 */
private fun formatFullSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val date = Date(timestamp)
    
    return when {
        diff < 86400_000 -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Today at ${timeFormat.format(date)}"
        }
        diff < 172800_000 -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday at ${timeFormat.format(date)}"
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
            dateFormat.format(date)
        }
    }
}

/**
 * Get accessibility description for sync status.
 */
private fun getSyncStatusDescription(syncState: SyncUiState): String {
    return when {
        syncState.isSyncing -> "Syncing data"
        syncState.syncStatus == SyncStatus.SUCCESS && syncState.lastSyncTime != null -> 
            "Synced ${formatSyncTime(syncState.lastSyncTime)}"
        syncState.syncStatus == SyncStatus.ERROR -> "Sync failed, tap to retry"
        syncState.syncStatus == SyncStatus.OFFLINE && syncState.pendingChangesCount > 0 -> 
            "Offline with ${syncState.pendingChangesCount} pending changes"
        syncState.syncStatus == SyncStatus.IDLE && syncState.isAuthenticated -> 
            "Tap to sync now"
        !syncState.isAuthenticated -> "Offline mode, sign in to enable sync"
        else -> "Sync status unknown"
    }
}
