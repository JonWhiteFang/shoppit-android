package com.shoppit.app.data.sync

import com.shoppit.app.domain.error.SyncError
import com.shoppit.app.domain.error.getRetryDelay
import com.shoppit.app.domain.error.isRetryable
import kotlinx.coroutines.delay
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retry policy for sync operations with exponential backoff.
 *
 * Implements a robust retry strategy:
 * - Exponential backoff: 1s, 2s, 4s, 8s, 16s
 * - Maximum 5 retry attempts
 * - Only retries retryable errors (network, timeout, 5xx server errors)
 * - Respects rate limit delays
 * - Logs all retry attempts for debugging
 *
 * Requirements: 4.4, 8.5
 */
@Singleton
class RetryPolicy @Inject constructor() {
    
    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 16000L
        private const val BACKOFF_FACTOR = 2.0
    }
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    /**
     * Executes a block with retry logic and exponential backoff.
     *
     * @param maxAttempts Maximum number of retry attempts (default: 5)
     * @param initialDelay Initial delay in milliseconds (default: 1000ms)
     * @param maxDelay Maximum delay in milliseconds (default: 16000ms)
     * @param factor Backoff multiplication factor (default: 2.0)
     * @param block The suspend function to execute with retry logic
     * @return Result containing the successful value or the final error
     */
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = MAX_ATTEMPTS,
        initialDelay: Long = INITIAL_DELAY_MS,
        maxDelay: Long = MAX_DELAY_MS,
        factor: Double = BACKOFF_FACTOR,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        var lastError: Throwable? = null
        
        repeat(maxAttempts) { attempt ->
            try {
                Timber.d("Executing operation (attempt ${attempt + 1}/$maxAttempts)")
                val result = block()
                
                if (attempt > 0) {
                    Timber.i("Operation succeeded after ${attempt + 1} attempts")
                }
                
                return Result.success(result)
                
            } catch (e: Exception) {
                lastError = e
                
                // Check if this is the last attempt
                if (attempt == maxAttempts - 1) {
                    Timber.e(e, "Operation failed after $maxAttempts attempts")
                    return Result.failure(e)
                }
                
                // Determine if error is retryable
                val syncError = mapToSyncError(e)
                if (!syncError.isRetryable()) {
                    Timber.w(e, "Non-retryable error encountered: ${sanitize(syncError.message)}")
                    return Result.failure(syncError)
                }
                
                // Calculate delay for this attempt
                val delayMs = if (syncError is SyncError.RateLimitError) {
                    syncError.getRetryDelay(attempt)
                } else {
                    currentDelay
                }
                
                Timber.w(
                    e,
                    "Operation failed (attempt ${attempt + 1}/$maxAttempts), " +
                            "retrying in ${delayMs}ms: ${sanitize(syncError.message)}"
                )
                
                // Wait before retrying
                delay(delayMs)
                
                // Calculate next delay with exponential backoff
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        // This should never be reached, but handle it just in case
        return Result.failure(
            lastError ?: SyncError.UnknownError("Max retries exceeded")
        )
    }
    
    /**
     * Executes a block with retry logic for a specific entity.
     * Includes entity context in logging for better debugging.
     *
     * @param entityType Type of entity being synced
     * @param entityId ID of the entity being synced
     * @param operation Operation being performed (CREATE, UPDATE, DELETE)
     * @param block The suspend function to execute with retry logic
     * @return Result containing the successful value or the final error
     */
    suspend fun <T> executeWithRetryForEntity(
        entityType: String,
        entityId: Long,
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        Timber.d("Starting sync operation: ${sanitize(operation)} for ${sanitize(entityType)} $entityId")
        
        val result = executeWithRetry {
            block()
        }
        
        result.fold(
            onSuccess = {
                Timber.i("Successfully synced: ${sanitize(operation)} for ${sanitize(entityType)} $entityId")
            },
            onFailure = { error ->
                Timber.e(error, "Failed to sync: ${sanitize(operation)} for ${sanitize(entityType)} $entityId")
            }
        )
        
        return result
    }
    
    /**
     * Determines if an operation should be retried based on the error and attempt count.
     *
     * @param error The error that occurred
     * @param attemptNumber Current attempt number (0-indexed)
     * @return True if the operation should be retried, false otherwise
     */
    fun shouldRetry(error: Throwable, attemptNumber: Int): Boolean {
        if (attemptNumber >= MAX_ATTEMPTS) {
            return false
        }
        
        val syncError = mapToSyncError(error)
        return syncError.isRetryable()
    }
    
    /**
     * Calculates the delay before the next retry attempt.
     *
     * @param error The error that occurred
     * @param attemptNumber Current attempt number (0-indexed)
     * @return Delay in milliseconds before next retry
     */
    fun calculateDelay(error: Throwable, attemptNumber: Int): Long {
        val syncError = mapToSyncError(error)
        return syncError.getRetryDelay(attemptNumber)
    }
    
    /**
     * Maps a generic exception to a SyncError.
     * This provides consistent error handling across the sync system.
     */
    private fun mapToSyncError(error: Throwable): SyncError {
        return when (error) {
            is SyncError -> error
            is java.net.UnknownHostException -> SyncError.NoInternetError
            is java.net.SocketTimeoutException -> SyncError.TimeoutError(cause = error)
            is java.io.IOException -> SyncError.NetworkError(
                message = error.message ?: "Network error",
                cause = error
            )
            else -> SyncError.UnknownError(
                message = error.message ?: "Unknown error during sync",
                cause = error
            )
        }
    }
}
