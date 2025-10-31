package com.shoppit.app.data.error

import com.shoppit.app.BuildConfig
import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.error.ErrorLogger
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ErrorLogger that uses Timber for logging.
 * Logs errors with appropriate severity levels and sends to crash reporting in production.
 */
@Singleton
class ErrorLoggerImpl @Inject constructor() : ErrorLogger {

    override fun logError(
        error: Throwable,
        context: String,
        additionalData: Map<String, Any>
    ) {
        // Log based on error type
        when (error) {
            is AppError.ValidationError -> {
                // Validation errors are warnings, not critical errors
                Timber.w(error, "[$context] Validation error: ${error.message}")
            }
            is AppError.NetworkError -> {
                Timber.e(error, "[$context] Network error: ${error.message}")
            }
            is AppError.DatabaseError -> {
                Timber.e(error, "[$context] Database error: ${error.message}")
            }
            is AppError.AuthenticationError -> {
                Timber.e(error, "[$context] Authentication error: ${error.message}")
            }
            is AppError.NotFoundError -> {
                Timber.w(error, "[$context] Not found: ${error.message}")
            }
            else -> {
                Timber.e(error, "[$context] Unexpected error: ${error.message}")
            }
        }

        // Log additional data if present
        if (additionalData.isNotEmpty()) {
            Timber.d("[$context] Additional data: $additionalData")
        }

        // Send to crash reporting service in production builds
        if (!BuildConfig.DEBUG) {
            // TODO: Integrate with Firebase Crashlytics or similar service
            // Example:
            // crashlytics.recordException(error)
            // additionalData.forEach { (key, value) ->
            //     crashlytics.setCustomKey(key, value.toString())
            // }
        }
    }

    override fun logWarning(message: String, context: String) {
        Timber.w("[$context] $message")
    }

    override fun logInfo(message: String, context: String) {
        Timber.i("[$context] $message")
    }
}
