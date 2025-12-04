package com.shoppit.app.util

import timber.log.Timber

/**
 * Logging utility wrapper providing consistent logging interface across the application.
 * Uses Timber under the hood for flexible logging configuration.
 */
object Logger {
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    /**
     * Log a debug message.
     * Only logged in debug builds.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun d(tag: String? = null, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        if (tag != null) {
            Timber.tag(sanitize(tag))
        }
        if (throwable != null) {
            Timber.d(throwable, sanitizedMessage)
        } else {
            Timber.d(sanitizedMessage)
        }
    }
    
    /**
     * Log an info message.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun i(tag: String? = null, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        if (tag != null) {
            Timber.tag(sanitize(tag))
        }
        if (throwable != null) {
            Timber.i(throwable, sanitizedMessage)
        } else {
            Timber.i(sanitizedMessage)
        }
    }
    
    /**
     * Log a warning message.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun w(tag: String? = null, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        if (tag != null) {
            Timber.tag(sanitize(tag))
        }
        if (throwable != null) {
            Timber.w(throwable, sanitizedMessage)
        } else {
            Timber.w(sanitizedMessage)
        }
    }
    
    /**
     * Log an error message.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun e(tag: String? = null, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        if (tag != null) {
            Timber.tag(sanitize(tag))
        }
        if (throwable != null) {
            Timber.e(throwable, sanitizedMessage)
        } else {
            Timber.e(sanitizedMessage)
        }
    }
    
    /**
     * Log a verbose message.
     * Only logged in debug builds.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun v(tag: String? = null, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitize(message)
        if (tag != null) {
            Timber.tag(sanitize(tag))
        }
        if (throwable != null) {
            Timber.v(throwable, sanitizedMessage)
        } else {
            Timber.v(sanitizedMessage)
        }
    }
}
