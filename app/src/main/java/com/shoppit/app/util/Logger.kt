package com.shoppit.app.util

import timber.log.Timber

/**
 * Logging utility wrapper providing consistent logging interface across the application.
 * Uses Timber under the hood for flexible logging configuration.
 */
object Logger {
    
    /**
     * Log a debug message.
     * Only logged in debug builds.
     *
     * @param tag Optional tag for the log message
     * @param message The message to log
     * @param throwable Optional throwable to log
     */
    fun d(tag: String? = null, message: String, throwable: Throwable? = null) {
        if (tag != null) {
            Timber.tag(tag)
        }
        if (throwable != null) {
            Timber.d(throwable, message)
        } else {
            Timber.d(message)
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
        if (tag != null) {
            Timber.tag(tag)
        }
        if (throwable != null) {
            Timber.i(throwable, message)
        } else {
            Timber.i(message)
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
        if (tag != null) {
            Timber.tag(tag)
        }
        if (throwable != null) {
            Timber.w(throwable, message)
        } else {
            Timber.w(message)
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
        if (tag != null) {
            Timber.tag(tag)
        }
        if (throwable != null) {
            Timber.e(throwable, message)
        } else {
            Timber.e(message)
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
        if (tag != null) {
            Timber.tag(tag)
        }
        if (throwable != null) {
            Timber.v(throwable, message)
        } else {
            Timber.v(message)
        }
    }
}
