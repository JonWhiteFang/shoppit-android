package com.shoppit.app.domain.error

/**
 * Sealed class representing all possible errors in the application.
 * Provides type-safe error handling across all layers.
 */
sealed class AppError {
    /**
     * Network-related errors (connectivity, timeouts, etc.)
     */
    data object NetworkError : AppError()

    /**
     * Database-related errors (query failures, constraint violations, etc.)
     */
    data object DatabaseError : AppError()

    /**
     * Validation errors with descriptive messages
     */
    data class ValidationError(val message: String) : AppError()

    /**
     * Unknown or unexpected errors with the original throwable
     */
    data class UnknownError(val throwable: Throwable) : AppError()
}
