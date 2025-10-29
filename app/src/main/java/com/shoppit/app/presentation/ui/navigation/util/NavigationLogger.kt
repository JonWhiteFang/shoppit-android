package com.shoppit.app.presentation.ui.navigation.util

import timber.log.Timber

/**
 * Utility object for logging navigation-related events and errors.
 * Provides consistent logging format for navigation issues.
 */
object NavigationLogger {
    
    /**
     * Logs a navigation error with context information.
     *
     * @param message Description of what went wrong
     * @param route The route that caused the error (if available)
     * @param arguments The arguments that were passed (if available)
     * @param exception The exception that was thrown (if available)
     */
    fun logNavigationError(
        message: String,
        route: String? = null,
        arguments: Map<String, Any?>? = null,
        exception: Throwable? = null
    ) {
        val logMessage = buildString {
            append("Navigation Error: ")
            append(message)
            if (route != null) {
                append(" | Route: $route")
            }
            if (arguments != null) {
                append(" | Arguments: $arguments")
            }
        }
        
        if (exception != null) {
            Timber.e(exception, logMessage)
        } else {
            Timber.e(logMessage)
        }
    }
    
    /**
     * Logs a navigation warning that doesn't necessarily stop navigation
     * but might indicate a potential issue.
     *
     * @param message Description of the warning
     * @param route The route that caused the warning (if available)
     * @param arguments The arguments that were passed (if available)
     */
    fun logNavigationWarning(
        message: String,
        route: String? = null,
        arguments: Map<String, Any?>? = null
    ) {
        val logMessage = buildString {
            append("Navigation Warning: ")
            append(message)
            if (route != null) {
                append(" | Route: $route")
            }
            if (arguments != null) {
                append(" | Arguments: $arguments")
            }
        }
        
        Timber.w(logMessage)
    }
    
    /**
     * Logs successful navigation events for tracking and debugging.
     *
     * @param route The route that was navigated to
     * @param arguments The arguments that were passed
     */
    fun logNavigationSuccess(
        route: String,
        arguments: Map<String, Any?>? = null
    ) {
        val logMessage = buildString {
            append("Navigation Success: ")
            append("Navigated to $route")
            if (arguments != null) {
                append(" with arguments: $arguments")
            }
        }
        
        Timber.d(logMessage)
    }
    
    /**
     * Logs back stack recovery operations.
     *
     * @param message Description of the recovery action
     * @param exception The exception that triggered the recovery (if available)
     */
    fun logBackStackRecovery(
        message: String,
        exception: Throwable? = null
    ) {
        val logMessage = "Back Stack Recovery: $message"
        
        if (exception != null) {
            Timber.w(exception, logMessage)
        } else {
            Timber.w(logMessage)
        }
    }
    
    /**
     * Logs deep link navigation events.
     *
     * @param uri The deep link URI
     * @param action The intent action (if available)
     */
    fun logDeepLink(
        uri: String,
        action: String? = null
    ) {
        val logMessage = buildString {
            append("Deep Link: ")
            append(uri)
            if (action != null) {
                append(" | Action: $action")
            }
        }
        
        Timber.i(logMessage)
    }
    
    /**
     * Logs navigation from one screen to another.
     *
     * @param from The source screen or context
     * @param to The destination route
     * @param arguments The arguments passed to the destination
     */
    fun logNavigation(
        from: String,
        to: String,
        arguments: Map<String, Any?>? = null
    ) {
        val logMessage = buildString {
            append("Navigation: ")
            append("$from -> $to")
            if (arguments != null && arguments.isNotEmpty()) {
                append(" | Arguments: $arguments")
            }
        }
        
        Timber.d(logMessage)
    }
}