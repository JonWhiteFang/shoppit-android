package com.shoppit.app.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shoppit.app.R
import com.shoppit.app.domain.error.SyncError
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for displaying sync-related notifications to users.
 *
 * Provides notifications for:
 * - Sync errors that require user attention
 * - Authentication failures requiring re-login
 * - Successful sync completion (optional)
 * - Offline status with pending changes
 *
 * Requirements: 4.4, 8.5
 */
@Singleton
class SyncNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncErrorLogger: SyncErrorLogger
) {
    
    companion object {
        private const val CHANNEL_ID = "sync_notifications"
        private const val CHANNEL_NAME = "Sync Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications about data synchronization"
        
        private const val NOTIFICATION_ID_SYNC_ERROR = 1001
        private const val NOTIFICATION_ID_AUTH_REQUIRED = 1002
        private const val NOTIFICATION_ID_SYNC_SUCCESS = 1003
        private const val NOTIFICATION_ID_OFFLINE = 1004
    }
    
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Creates the notification channel for sync notifications.
     * Required for Android O and above.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(false)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Timber.d("Sync notification channel created")
        }
    }
    
    /**
     * Shows a notification for a sync error.
     *
     * @param error The sync error to notify about
     * @param canRetry Whether the user can retry the sync
     */
    fun notifySyncError(error: SyncError, canRetry: Boolean = true) {
        // Only show notifications for errors that require user attention
        if (!shouldNotifyUser(error)) {
            return
        }
        
        val title = "Sync Failed"
        val message = syncErrorLogger.getUserFriendlyMessage(error)
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Using system icon temporarily
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        if (canRetry) {
            // TODO: Add retry action when MainActivity is available
            // builder.addAction(createRetryAction())
        }
        
        val notification = builder.build()
        
        notificationManager.notify(NOTIFICATION_ID_SYNC_ERROR, notification)
        Timber.d("Sync error notification shown: ${error.javaClass.simpleName}")
    }
    
    /**
     * Shows a notification requiring user to re-authenticate.
     *
     * @param error The authentication error
     */
    fun notifyAuthenticationRequired(error: SyncError) {
        val title = "Sign In Required"
        val message = "Your session has expired. Please sign in again to continue syncing."
        
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock) // Using system icon temporarily
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        // TODO: Add action to open sign-in screen when available
        // builder.setContentIntent(createSignInIntent())
        
        val notification = builder.build()
        
        notificationManager.notify(NOTIFICATION_ID_AUTH_REQUIRED, notification)
        Timber.d("Authentication required notification shown")
    }
    
    /**
     * Shows a notification for successful sync completion.
     * This is optional and can be disabled in user preferences.
     *
     * @param syncedCount Number of items synced
     */
    fun notifySyncSuccess(syncedCount: Int) {
        // Only show if user has enabled success notifications
        // TODO: Check user preference
        
        val title = "Sync Complete"
        val message = "Successfully synced $syncedCount items"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync) // Using system icon temporarily
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Auto-dismiss after 3 seconds
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_SYNC_SUCCESS, notification)
        Timber.d("Sync success notification shown")
    }
    
    /**
     * Shows a notification when device is offline with pending changes.
     *
     * @param pendingCount Number of pending changes
     */
    fun notifyOfflineWithPendingChanges(pendingCount: Int) {
        val title = "Offline"
        val message = "$pendingCount changes will sync when you're back online"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync_noanim) // Using system icon temporarily
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Keep notification until online
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_OFFLINE, notification)
        Timber.d("Offline notification shown with $pendingCount pending changes")
    }
    
    /**
     * Dismisses the offline notification.
     * Called when device comes back online.
     */
    fun dismissOfflineNotification() {
        notificationManager.cancel(NOTIFICATION_ID_OFFLINE)
        Timber.d("Offline notification dismissed")
    }
    
    /**
     * Dismisses all sync notifications.
     */
    fun dismissAllNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_SYNC_ERROR)
        notificationManager.cancel(NOTIFICATION_ID_AUTH_REQUIRED)
        notificationManager.cancel(NOTIFICATION_ID_SYNC_SUCCESS)
        notificationManager.cancel(NOTIFICATION_ID_OFFLINE)
        Timber.d("All sync notifications dismissed")
    }
    
    /**
     * Determines if a notification should be shown for the given error.
     */
    private fun shouldNotifyUser(error: SyncError): Boolean {
        return when (error) {
            // Don't notify for expected/temporary errors
            is SyncError.NetworkError -> false
            is SyncError.NoInternetError -> false
            is SyncError.TimeoutError -> false
            is SyncError.CancelledError -> false
            
            // Notify for errors requiring user action
            is SyncError.AuthenticationError -> true
            is SyncError.TokenExpiredError -> true
            
            // Notify for persistent errors
            is SyncError.ServerError -> error.code !in 500..599 // Don't notify for temporary 5xx
            is SyncError.ClientError -> true
            is SyncError.DatabaseError -> true
            is SyncError.UnknownError -> true
            
            // Don't notify for auto-resolved conflicts
            is SyncError.ConflictError -> false
            
            // Don't notify for rate limits (will retry automatically)
            is SyncError.RateLimitError -> false
        }
    }
    
    /**
     * Creates a pending intent to open the main activity.
     * TODO: Implement when MainActivity is available.
     */
    private fun createMainActivityIntent(): PendingIntent? {
        return try {
            // val intent = Intent(context, MainActivity::class.java)
            // PendingIntent.getActivity(
            //     context,
            //     0,
            //     intent,
            //     PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            // )
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to create main activity intent")
            null
        }
    }
}
