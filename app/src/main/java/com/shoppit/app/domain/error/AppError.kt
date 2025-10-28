package com.shoppit.app.domain.error

/**
 * Sealed class representing all possible errors in the application.
 * Provides type-safe error handling across all layers.
 */
sealed class AppError : Exception() {
    /**
     * Network-related errors (connectivity, timeouts, etc.)
     */
    data object NetworkError : AppError() {
        override val message: String
            get() = "Network connection failed"
    }

    /**
     * Database-related errors (query failures, constraint violations, etc.)
     */
    data object DatabaseError : AppError() {
        override val message: String
            get() = "Database error occurred"
    }

    /**
     * Validation errors with descriptive messages
     */
    data class ValidationError(override val message: String) : AppError()

    /**
     * Permission denied errors (camera, microphone, location, etc.)
     */
    data class PermissionDenied(val permission: String) : AppError() {
        override val message: String
            get() = "Permission denied: $permission"
    }

    /**
     * Voice input parsing errors
     */
    data class VoiceParsingError(override val message: String) : AppError()

    /**
     * Barcode scanning errors
     */
    data class BarcodeScanError(override val message: String) : AppError()

    /**
     * Unknown or unexpected errors with the original throwable
     */
    data class UnknownError(val throwable: Throwable) : AppError() {
        override val message: String
            get() = "An unexpected error occurred: ${throwable.message ?: "Unknown error"}"
    }
}
