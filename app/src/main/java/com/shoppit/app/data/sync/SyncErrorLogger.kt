package com.shoppit.app.data.sync

import com.shoppit.app.domain.error.SyncError
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error logging for sync operations.
 *
 * Provides structured logging with:
 * - Error categorization by type
 * - Context information (entity type, operation, attempt number)
 * - User-friendly error messages
 * - Integration with crash reporting (future)
 *
 * Requirements: 4.4, 8.5
 */
@Singleton
class SyncErrorLogger @Inject constructor() {
    
    /**
     * Logs a sync error with full context.
     *
     * @param error The sync error that occurred
     * @param context Additional context about the operation
     */
    fun logError(error: SyncError, context: SyncErrorContext) {
        val logMessage = buildLogMessage(error, context)
        
        when (error) {
            // Critical errors that require immediate attention
            is SyncError.DatabaseError,
            is SyncError.UnknownError -> {
                Timber.e(error.cause, logMessage)
                // TODO: Send to crash reporting service (Firebase Crashlytics, Sentry, etc.)
            }
            
            // Authentication errors that require user action
            is SyncError.AuthenticationError,
            is SyncError.TokenExpiredError -> {
                Timber.w(error.cause, logMessage)
            }
            
            // Network errors that are expected and retryable
            is SyncError.NetworkError,
            is SyncError.NoInternetError,
            is SyncError.TimeoutError -> {
                Timber.w(error.cause, logMessage)
            }
            
            // Server errors that may be temporary
            is SyncError.ServerError -> {
                if (error.code in 500..599) {
                    Timber.w(error.cause, logMessage)
                } else {
                    Timber.e(error.cause, logMessage)
                }
            }
            
            // Client errors that indicate bugs
            is SyncError.ClientError -> {
                Timber.e(error.cause, logMessage)
            }
            
            // Conflict errors that are handled automatically
            is SyncError.ConflictError -> {
                Timber.i(error.cause, logMessage)
            }
            
            // Rate limit errors
            is SyncError.RateLimitError -> {
                Timber.w(error.cause, logMessage)
            }
            
            // Cancelled operations (informational)
            is SyncError.CancelledError -> {
                Timber.d(logMessage)
            }
        }
    }
    
    /**
     * Logs a successful recovery from an error.
     *
     * @param error The error that was recovered from
     * @param context Context about the operation
     * @param attemptNumber Number of attempts it took to recover
     */
    fun logRecovery(error: SyncError, context: SyncErrorContext, attemptNumber: Int) {
        val message = buildString {
            append("Successfully recovered from ${error.javaClass.simpleName}")
            append(" after $attemptNumber attempts")
            if (context.entityType != null) {
                append(" for ${context.entityType} ${context.entityId}")
            }
            if (context.operation != null) {
                append(" (${context.operation})")
            }
        }
        
        Timber.i(message)
    }
    
    /**
     * Logs a failed recovery attempt.
     *
     * @param error The error that could not be recovered from
     * @param context Context about the operation
     * @param maxAttempts Maximum number of attempts that were made
     */
    fun logFailedRecovery(error: SyncError, context: SyncErrorContext, maxAttempts: Int) {
        val message = buildString {
            append("Failed to recover from ${error.javaClass.simpleName}")
            append(" after $maxAttempts attempts")
            if (context.entityType != null) {
                append(" for ${context.entityType} ${context.entityId}")
            }
            if (context.operation != null) {
                append(" (${context.operation})")
            }
            append(": ${error.message}")
        }
        
        Timber.e(error.cause, message)
        // TODO: Send to crash reporting service
    }
    
    /**
     * Logs a retry attempt.
     *
     * @param error The error that triggered the retry
     * @param context Context about the operation
     * @param attemptNumber Current attempt number
     * @param delayMs Delay before retry in milliseconds
     */
    fun logRetry(error: SyncError, context: SyncErrorContext, attemptNumber: Int, delayMs: Long) {
        val message = buildString {
            append("Retrying after ${error.javaClass.simpleName}")
            append(" (attempt $attemptNumber)")
            if (context.entityType != null) {
                append(" for ${context.entityType} ${context.entityId}")
            }
            if (context.operation != null) {
                append(" (${context.operation})")
            }
            append(", delay: ${delayMs}ms")
        }
        
        Timber.d(message)
    }
    
    /**
     * Logs a conflict resolution.
     *
     * @param entityType Type of entity with conflict
     * @param entityId ID of the entity
     * @param resolution Resolution strategy used (e.g., "server_wins", "local_wins")
     */
    fun logConflictResolution(entityType: String, entityId: Long, resolution: String) {
        val message = "Conflict resolved for $entityType $entityId using $resolution strategy"
        Timber.i(message)
    }
    
    /**
     * Logs when changes are queued for later sync.
     *
     * @param entityType Type of entity queued
     * @param entityId ID of the entity
     * @param operation Operation that was queued
     */
    fun logQueuedChange(entityType: String, entityId: Long, operation: String) {
        val message = "Queued $operation for $entityType $entityId (will sync when online)"
        Timber.d(message)
    }
    
    /**
     * Logs sync statistics.
     *
     * @param stats Sync statistics to log
     */
    fun logSyncStats(stats: SyncStats) {
        val message = buildString {
            append("Sync completed: ")
            append("${stats.syncedEntities} synced, ")
            append("${stats.failedEntities} failed, ")
            append("${stats.conflicts} conflicts, ")
            append("duration: ${stats.durationMs}ms")
        }
        
        if (stats.failedEntities > 0) {
            Timber.w(message)
        } else {
            Timber.i(message)
        }
    }
    
    /**
     * Builds a detailed log message from error and context.
     */
    private fun buildLogMessage(error: SyncError, context: SyncErrorContext): String {
        return buildString {
            append("Sync error: ${error.javaClass.simpleName}")
            
            if (context.entityType != null) {
                append(" for ${context.entityType}")
                if (context.entityId != null) {
                    append(" $context.entityId")
                }
            }
            
            if (context.operation != null) {
                append(" (${context.operation})")
            }
            
            if (context.attemptNumber != null) {
                append(" [attempt ${context.attemptNumber}]")
            }
            
            append(": ${error.message}")
            
            if (context.additionalInfo != null) {
                append(" - ${context.additionalInfo}")
            }
        }
    }
    
    /**
     * Gets a user-friendly error message for display in UI.
     *
     * @param error The sync error
     * @return User-friendly error message
     */
    fun getUserFriendlyMessage(error: SyncError): String {
        return when (error) {
            is SyncError.NetworkError -> "Network connection failed. Please check your internet connection."
            is SyncError.NoInternetError -> "No internet connection. Changes will sync when you're back online."
            is SyncError.TimeoutError -> "Request timed out. Please try again."
            is SyncError.AuthenticationError -> "Authentication failed. Please sign in again."
            is SyncError.TokenExpiredError -> "Your session has expired. Please sign in again."
            is SyncError.ServerError -> "Server is experiencing issues. Please try again later."
            is SyncError.ClientError -> "Request failed. Please try again."
            is SyncError.ConflictError -> "Data conflict detected. Using most recent version."
            is SyncError.DatabaseError -> "Failed to save data. Please try again."
            is SyncError.RateLimitError -> "Too many requests. Please wait a moment and try again."
            is SyncError.CancelledError -> "Sync cancelled."
            is SyncError.UnknownError -> "An unexpected error occurred. Please try again."
        }
    }
}

/**
 * Context information for error logging.
 */
data class SyncErrorContext(
    val entityType: String? = null,
    val entityId: Long? = null,
    val operation: String? = null,
    val attemptNumber: Int? = null,
    val additionalInfo: String? = null
)

/**
 * Statistics about a sync operation.
 */
data class SyncStats(
    val syncedEntities: Int,
    val failedEntities: Int,
    val conflicts: Int,
    val durationMs: Long
)
