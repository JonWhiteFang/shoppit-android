package com.shoppit.app.domain.error

/**
 * Interface for centralized error logging across the application.
 * Provides methods to log errors, warnings, and info messages with context.
 */
interface ErrorLogger {
    /**
     * Logs an error with context and optional additional data.
     *
     * @param error The throwable error to log
     * @param context The context where the error occurred (e.g., "MealViewModel.loadMeals")
     * @param additionalData Optional map of additional data to include in the log
     */
    fun logError(
        error: Throwable,
        context: String,
        additionalData: Map<String, Any> = emptyMap()
    )

    /**
     * Logs a warning message with context.
     *
     * @param message The warning message
     * @param context The context where the warning occurred
     */
    fun logWarning(message: String, context: String)

    /**
     * Logs an informational message with context.
     *
     * @param message The info message
     * @param context The context where the info occurred
     */
    fun logInfo(message: String, context: String)
}
