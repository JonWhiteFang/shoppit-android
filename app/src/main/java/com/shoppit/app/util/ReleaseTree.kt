package com.shoppit.app.util

import android.util.Log
import timber.log.Timber

/**
 * Custom Timber tree for release builds.
 * Filters out verbose and debug logs, and can be extended to send errors to crash reporting services.
 */
class ReleaseTree : Timber.Tree() {
    
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Only log warnings and errors in release builds
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        
        // Log to system
        if (t != null) {
            Log.println(priority, tag ?: "Shoppit", "$message\n${Log.getStackTraceString(t)}")
        } else {
            Log.println(priority, tag ?: "Shoppit", message)
        }
        
        // TODO: Send errors to crash reporting service (e.g., Firebase Crashlytics)
        if (priority == Log.ERROR && t != null) {
            // Example: FirebaseCrashlytics.getInstance().recordException(t)
        }
    }
}
