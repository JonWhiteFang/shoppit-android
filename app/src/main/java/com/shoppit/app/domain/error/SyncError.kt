package com.shoppit.app.domain.error

/**
 * Sealed class representing all possible synchronization errors.
 * Provides type-safe error handling for sync operations.
 *
 * Error types are categorized by their nature and recovery strategy:
 * - Network errors: Retryable with exponential backoff
 * - Authentication errors: Require token refresh or re-authentication
 * - Server errors: Retryable for 5xx, non-retryable for 4xx
 * - Conflict errors: Resolved automatically using Last-Write-Wins
 * - Database errors: Logged and reported to user
 */
sealed class SyncError : Exception() {
    
    /**
     * Network connectivity errors (no internet, timeout, connection failed).
     * Recovery: Retry with exponential backoff up to 5 attempts.
     */
    data class NetworkError(
        override val message: String = "Network connection failed",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * No internet connection available.
     * Recovery: Queue changes locally, retry when connectivity restored.
     */
    data object NoInternetError : SyncError() {
        override val message: String
            get() = "No internet connection"
    }
    
    /**
     * Request timeout error.
     * Recovery: Retry with exponential backoff.
     */
    data class TimeoutError(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Authentication errors (invalid token, expired token, unauthorized).
     * Recovery: Attempt token refresh, then prompt re-authentication.
     */
    data class AuthenticationError(
        override val message: String = "Authentication failed",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Token expired and refresh failed.
     * Recovery: Clear tokens and prompt user to sign in again.
     */
    data class TokenExpiredError(
        override val message: String = "Session expired, please sign in again",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Server errors (5xx status codes).
     * Recovery: Retry with exponential backoff up to 5 attempts.
     */
    data class ServerError(
        val code: Int,
        override val message: String = "Server error (code: $code)",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Client errors (4xx status codes except 401/403).
     * Recovery: Log error, don't retry, notify user.
     */
    data class ClientError(
        val code: Int,
        override val message: String = "Client error (code: $code)",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Conflict detected during synchronization.
     * Recovery: Apply Last-Write-Wins resolution strategy.
     */
    data class ConflictError(
        val entityType: String,
        val entityId: Long,
        override val message: String = "Conflict detected for $entityType $entityId",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Database operation failed during sync.
     * Recovery: Log error, attempt recovery, notify user.
     */
    data class DatabaseError(
        override val message: String = "Database error during sync",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Rate limit exceeded.
     * Recovery: Wait for rate limit reset, then retry.
     */
    data class RateLimitError(
        val retryAfterSeconds: Int,
        override val message: String = "Rate limit exceeded, retry after $retryAfterSeconds seconds",
        override val cause: Throwable? = null
    ) : SyncError()
    
    /**
     * Sync operation cancelled by user or system.
     * Recovery: None, operation was intentionally cancelled.
     */
    data object CancelledError : SyncError() {
        override val message: String
            get() = "Sync operation cancelled"
    }
    
    /**
     * Unknown or unexpected error.
     * Recovery: Log error with full context, notify user.
     */
    data class UnknownError(
        override val message: String = "An unexpected error occurred during sync",
        override val cause: Throwable? = null
    ) : SyncError()
}

/**
 * Extension function to determine if a SyncError is retryable.
 */
fun SyncError.isRetryable(): Boolean = when (this) {
    is SyncError.NetworkError -> true
    is SyncError.TimeoutError -> true
    is SyncError.ServerError -> code in 500..599 // Only retry 5xx errors
    is SyncError.RateLimitError -> true
    is SyncError.NoInternetError -> false // Don't retry, wait for connectivity
    is SyncError.AuthenticationError -> false // Requires token refresh
    is SyncError.TokenExpiredError -> false // Requires re-authentication
    is SyncError.ClientError -> false // 4xx errors are not retryable
    is SyncError.ConflictError -> false // Handled by conflict resolution
    is SyncError.DatabaseError -> false // Requires manual intervention
    is SyncError.CancelledError -> false // Intentionally cancelled
    is SyncError.UnknownError -> false // Unknown errors are not retryable
}

/**
 * Extension function to get the recommended retry delay in milliseconds.
 */
fun SyncError.getRetryDelay(attemptNumber: Int): Long = when (this) {
    is SyncError.RateLimitError -> retryAfterSeconds * 1000L
    else -> {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        val baseDelay = 1000L
        val maxDelay = 16000L
        val delay = baseDelay * (1 shl attemptNumber) // 2^attemptNumber
        delay.coerceAtMost(maxDelay)
    }
}
