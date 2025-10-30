package com.shoppit.app.data.sync

import com.shoppit.app.domain.error.SyncError
import com.shoppit.app.domain.repository.AuthRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strategy for recovering from sync errors.
 *
 * Implements error-specific recovery strategies:
 * - Network errors: Retry with exponential backoff
 * - Authentication errors: Attempt token refresh, then prompt re-login
 * - Server errors (5xx): Retry with backoff, max 5 attempts
 * - Client errors (4xx): Log error, don't retry, notify user
 * - Conflict errors: Apply Last-Write-Wins resolution
 * - Database errors: Log error, attempt recovery, notify user
 *
 * Requirements: 4.4, 8.5
 */
@Singleton
class SyncErrorRecoveryStrategy @Inject constructor(
    private val authRepository: AuthRepository,
    private val retryPolicy: RetryPolicy
) {
    
    /**
     * Attempts to recover from a sync error.
     *
     * @param error The sync error to recover from
     * @param attemptNumber Current attempt number (0-indexed)
     * @return RecoveryAction indicating what action should be taken
     */
    suspend fun recover(error: SyncError, attemptNumber: Int): RecoveryAction {
        Timber.d("Attempting recovery for error: ${error.javaClass.simpleName}")
        
        return when (error) {
            // Network errors: Retry with exponential backoff
            is SyncError.NetworkError -> handleNetworkError(error, attemptNumber)
            
            // No internet: Queue locally, don't retry immediately
            is SyncError.NoInternetError -> handleNoInternetError(error)
            
            // Timeout: Retry with exponential backoff
            is SyncError.TimeoutError -> handleTimeoutError(error, attemptNumber)
            
            // Authentication errors: Attempt token refresh
            is SyncError.AuthenticationError -> handleAuthenticationError(error)
            
            // Token expired: Prompt re-authentication
            is SyncError.TokenExpiredError -> handleTokenExpiredError(error)
            
            // Server errors: Retry 5xx, fail on 4xx
            is SyncError.ServerError -> handleServerError(error, attemptNumber)
            
            // Client errors: Don't retry, notify user
            is SyncError.ClientError -> handleClientError(error)
            
            // Conflict errors: Apply resolution strategy
            is SyncError.ConflictError -> handleConflictError(error)
            
            // Database errors: Log and notify
            is SyncError.DatabaseError -> handleDatabaseError(error)
            
            // Rate limit: Wait and retry
            is SyncError.RateLimitError -> handleRateLimitError(error)
            
            // Cancelled: Don't retry
            is SyncError.CancelledError -> handleCancelledError(error)
            
            // Unknown errors: Log with full context
            is SyncError.UnknownError -> handleUnknownError(error)
        }
    }
    
    // ========== Error-Specific Handlers ==========
    
    private fun handleNetworkError(error: SyncError.NetworkError, attemptNumber: Int): RecoveryAction {
        return if (retryPolicy.shouldRetry(error, attemptNumber)) {
            val delay = retryPolicy.calculateDelay(error, attemptNumber)
            Timber.w("Network error, will retry after ${delay}ms")
            RecoveryAction.Retry(delay)
        } else {
            Timber.e("Network error, max retries exceeded")
            RecoveryAction.Fail(error, "Network connection failed after multiple attempts")
        }
    }
    
    private fun handleNoInternetError(error: SyncError.NoInternetError): RecoveryAction {
        Timber.w("No internet connection, changes will be queued")
        return RecoveryAction.QueueForLater(
            error,
            "No internet connection. Changes will sync when online."
        )
    }
    
    private fun handleTimeoutError(error: SyncError.TimeoutError, attemptNumber: Int): RecoveryAction {
        return if (retryPolicy.shouldRetry(error, attemptNumber)) {
            val delay = retryPolicy.calculateDelay(error, attemptNumber)
            Timber.w("Request timeout, will retry after ${delay}ms")
            RecoveryAction.Retry(delay)
        } else {
            Timber.e("Request timeout, max retries exceeded")
            RecoveryAction.Fail(error, "Request timed out after multiple attempts")
        }
    }
    
    private suspend fun handleAuthenticationError(error: SyncError.AuthenticationError): RecoveryAction {
        Timber.w("Authentication error, attempting token refresh")
        
        return try {
            // Attempt to refresh the access token
            val refreshResult = authRepository.refreshAccessToken()
            
            refreshResult.fold(
                onSuccess = {
                    Timber.i("Token refresh successful, will retry sync")
                    RecoveryAction.Retry(0) // Retry immediately with new token
                },
                onFailure = { refreshError ->
                    Timber.e(refreshError, "Token refresh failed")
                    RecoveryAction.RequireReauthentication(
                        error,
                        "Session expired. Please sign in again."
                    )
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Exception during token refresh")
            RecoveryAction.RequireReauthentication(
                error,
                "Session expired. Please sign in again."
            )
        }
    }
    
    private fun handleTokenExpiredError(error: SyncError.TokenExpiredError): RecoveryAction {
        Timber.w("Token expired, user must re-authenticate")
        return RecoveryAction.RequireReauthentication(
            error,
            "Your session has expired. Please sign in again."
        )
    }
    
    private fun handleServerError(error: SyncError.ServerError, attemptNumber: Int): RecoveryAction {
        return if (error.code in 500..599) {
            // 5xx errors are retryable
            if (retryPolicy.shouldRetry(error, attemptNumber)) {
                val delay = retryPolicy.calculateDelay(error, attemptNumber)
                Timber.w("Server error ${error.code}, will retry after ${delay}ms")
                RecoveryAction.Retry(delay)
            } else {
                Timber.e("Server error ${error.code}, max retries exceeded")
                RecoveryAction.Fail(error, "Server is experiencing issues. Please try again later.")
            }
        } else {
            // Other server errors are not retryable
            Timber.e("Server error ${error.code}, not retryable")
            RecoveryAction.Fail(error, "Server error: ${error.message}")
        }
    }
    
    private fun handleClientError(error: SyncError.ClientError): RecoveryAction {
        Timber.e("Client error ${error.code}: ${error.message}")
        return RecoveryAction.Fail(
            error,
            "Request failed: ${error.message}"
        )
    }
    
    private fun handleConflictError(error: SyncError.ConflictError): RecoveryAction {
        Timber.w("Conflict detected for ${error.entityType} ${error.entityId}")
        return RecoveryAction.ResolveConflict(
            error,
            "Data conflict detected. Using most recent version."
        )
    }
    
    private fun handleDatabaseError(error: SyncError.DatabaseError): RecoveryAction {
        Timber.e("Database error during sync: ${error.message}")
        return RecoveryAction.Fail(
            error,
            "Failed to save sync data. Please try again."
        )
    }
    
    private fun handleRateLimitError(error: SyncError.RateLimitError): RecoveryAction {
        val delay = error.retryAfterSeconds * 1000L
        Timber.w("Rate limit exceeded, will retry after ${delay}ms")
        return RecoveryAction.Retry(delay)
    }
    
    private fun handleCancelledError(error: SyncError.CancelledError): RecoveryAction {
        Timber.i("Sync operation was cancelled")
        return RecoveryAction.Cancel(error)
    }
    
    private fun handleUnknownError(error: SyncError.UnknownError): RecoveryAction {
        Timber.e(error.cause, "Unknown error during sync: ${error.message}")
        return RecoveryAction.Fail(
            error,
            "An unexpected error occurred. Please try again."
        )
    }
}

/**
 * Sealed class representing possible recovery actions for sync errors.
 */
sealed class RecoveryAction {
    /**
     * Retry the operation after the specified delay.
     *
     * @param delayMs Delay in milliseconds before retrying
     */
    data class Retry(val delayMs: Long) : RecoveryAction()
    
    /**
     * Queue the change for later synchronization.
     * Used when device is offline or network is unavailable.
     *
     * @param error The error that caused this action
     * @param userMessage User-friendly message to display
     */
    data class QueueForLater(
        val error: SyncError,
        val userMessage: String
    ) : RecoveryAction()
    
    /**
     * Fail the operation and notify the user.
     *
     * @param error The error that caused the failure
     * @param userMessage User-friendly message to display
     */
    data class Fail(
        val error: SyncError,
        val userMessage: String
    ) : RecoveryAction()
    
    /**
     * Require user to re-authenticate.
     * Used when token refresh fails or session is invalid.
     *
     * @param error The error that caused this action
     * @param userMessage User-friendly message to display
     */
    data class RequireReauthentication(
        val error: SyncError,
        val userMessage: String
    ) : RecoveryAction()
    
    /**
     * Resolve conflict using Last-Write-Wins strategy.
     *
     * @param error The conflict error
     * @param userMessage User-friendly message to display
     */
    data class ResolveConflict(
        val error: SyncError.ConflictError,
        val userMessage: String
    ) : RecoveryAction()
    
    /**
     * Cancel the operation (user or system initiated).
     *
     * @param error The cancellation error
     */
    data class Cancel(val error: SyncError.CancelledError) : RecoveryAction()
}
