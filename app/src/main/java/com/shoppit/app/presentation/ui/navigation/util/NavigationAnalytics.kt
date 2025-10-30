package com.shoppit.app.presentation.ui.navigation.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Data class representing the current analytics state.
 */
data class AnalyticsState(
    val sessionDurationMs: Long = 0L,
    val totalNavigations: Int = 0,
    val totalErrors: Int = 0,
    val errorRate: Float = 0f,
    val mostViewedScreens: List<Pair<String, Int>> = emptyList(),
    val mostCommonPaths: List<Pair<String, Int>> = emptyList()
)

/**
 * Utility object for tracking navigation analytics.
 * Provides methods to track navigation events, errors, and failures.
 *
 * Requirements:
 * - 9.1: Track navigation events for analytics
 * - 9.2: Monitor navigation errors and failures
 * - 9.3: Collect navigation performance metrics
 */
object NavigationAnalytics {
    
    private val sessionStartTime = System.currentTimeMillis()
    private val totalNavigations = AtomicInteger(0)
    private val totalErrors = AtomicInteger(0)
    private val screenViews = ConcurrentHashMap<String, AtomicInteger>()
    private val navigationPaths = ConcurrentHashMap<String, AtomicInteger>()
    private val screenErrors = ConcurrentHashMap<String, AtomicInteger>()
    private val failureTypes = ConcurrentHashMap<String, AtomicInteger>()
    private val navigationHistory = mutableListOf<String>()
    
    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState.asStateFlow()
    
    init {
        updateAnalyticsState()
    }
    
    /**
     * Updates the analytics state with current metrics.
     */
    private fun updateAnalyticsState() {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime
        val navCount = totalNavigations.get()
        val errCount = totalErrors.get()
        val errorRate = if (navCount > 0) (errCount.toFloat() / navCount) * 100 else 0f
        
        _analyticsState.value = AnalyticsState(
            sessionDurationMs = sessionDuration,
            totalNavigations = navCount,
            totalErrors = errCount,
            errorRate = errorRate,
            mostViewedScreens = getMostViewedScreens(10),
            mostCommonPaths = getMostCommonPaths(10)
        )
    }
    
    /**
     * Tracks a navigation error event.
     *
     * @param route The route where the error occurred
     * @param errorType The type of error (e.g., "INVALID_ARGUMENTS", "MISSING_ARGUMENTS")
     * @param message A description of the error
     * @param exception The exception that occurred (if available)
     */
    fun trackNavigationError(
        route: String,
        errorType: String,
        message: String,
        exception: Throwable? = null
    ) {
        totalErrors.incrementAndGet()
        screenErrors.computeIfAbsent(route) { AtomicInteger(0) }.incrementAndGet()
        failureTypes.computeIfAbsent(errorType) { AtomicInteger(0) }.incrementAndGet()
        
        Timber.e(exception, "Navigation Error [$errorType] on route '$route': $message")
        updateAnalyticsState()
        
        // TODO: Send to analytics service (Firebase Analytics, etc.)
        // Example:
        // firebaseAnalytics.logEvent("navigation_error") {
        //     param("route", route)
        //     param("error_type", errorType)
        //     param("message", message)
        // }
    }
    
    /**
     * Tracks a navigation failure event.
     *
     * @param route The route where the failure occurred
     * @param failureType The type of failure (e.g., "GENERAL_FAILURE", "TIMEOUT")
     * @param message A description of the failure
     */
    fun trackNavigationFailure(
        route: String,
        failureType: String,
        message: String
    ) {
        totalErrors.incrementAndGet()
        screenErrors.computeIfAbsent(route) { AtomicInteger(0) }.incrementAndGet()
        failureTypes.computeIfAbsent(failureType) { AtomicInteger(0) }.incrementAndGet()
        
        Timber.w("Navigation Failure [$failureType] on route '$route': $message")
        updateAnalyticsState()
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("navigation_failure") {
        //     param("route", route)
        //     param("failure_type", failureType)
        //     param("message", message)
        // }
    }
    
    /**
     * Tracks a screen view event.
     *
     * @param route The route being viewed
     * @param arguments The navigation arguments (if any)
     */
    fun trackScreenView(
        route: String,
        arguments: Map<String, Any?>? = null
    ) {
        screenViews.computeIfAbsent(route) { AtomicInteger(0) }.incrementAndGet()
        
        Timber.d("Screen View: $route ${arguments?.let { "with args: $it" } ?: ""}")
        updateAnalyticsState()
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("screen_view") {
        //     param("screen_name", route)
        //     arguments?.forEach { (key, value) ->
        //         param("arg_$key", value.toString())
        //     }
        // }
    }
    
    /**
     * Tracks a navigation path between two screens.
     *
     * @param from The source route
     * @param to The destination route
     * @param arguments The navigation arguments (if any)
     */
    fun trackNavigationPath(
        from: String,
        to: String,
        arguments: Map<String, Any?>? = null
    ) {
        val path = "$from -> $to"
        navigationPaths.computeIfAbsent(path) { AtomicInteger(0) }.incrementAndGet()
        
        Timber.d("Navigation Path: $path ${arguments?.let { "with args: $it" } ?: ""}")
        updateAnalyticsState()
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("navigation_path") {
        //     param("from", from)
        //     param("to", to)
        //     arguments?.forEach { (key, value) ->
        //         param("arg_$key", value.toString())
        //     }
        // }
    }
    
    /**
     * Tracks a successful navigation event.
     *
     * @param from The source route
     * @param to The destination route
     * @param arguments The navigation arguments (if any)
     */
    fun trackNavigationSuccess(
        from: String,
        to: String,
        arguments: Map<String, Any?>? = null
    ) {
        totalNavigations.incrementAndGet()
        screenViews.computeIfAbsent(to) { AtomicInteger(0) }.incrementAndGet()
        
        // Track navigation path
        synchronized(navigationHistory) {
            navigationHistory.add(to)
            if (navigationHistory.size >= 2) {
                val path = "${navigationHistory[navigationHistory.size - 2]} -> $to"
                navigationPaths.computeIfAbsent(path) { AtomicInteger(0) }.incrementAndGet()
            }
            // Keep only last 100 navigations
            if (navigationHistory.size > 100) {
                navigationHistory.removeAt(0)
            }
        }
        
        Timber.d("Navigation Success: $from -> $to ${arguments?.let { "with args: $it" } ?: ""}")
        updateAnalyticsState()
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("navigation_success") {
        //     param("from", from)
        //     param("to", to)
        //     arguments?.forEach { (key, value) ->
        //         param("arg_$key", value.toString())
        //     }
        // }
    }
    
    /**
     * Tracks a deep link navigation event.
     *
     * @param uri The deep link URI
     * @param success Whether the deep link was handled successfully
     * @param destination The destination route (if successful)
     */
    fun trackDeepLinkNavigation(
        uri: String,
        success: Boolean,
        destination: String? = null
    ) {
        Timber.i("Deep Link Navigation: $uri -> ${if (success) "Success ($destination)" else "Failed"}")
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("deep_link_navigation") {
        //     param("uri", uri)
        //     param("success", success.toString())
        //     destination?.let { param("destination", it) }
        // }
    }
    
    /**
     * Tracks back navigation events.
     *
     * @param from The source route
     * @param to The destination route (if known)
     */
    fun trackBackNavigation(
        from: String,
        to: String? = null
    ) {
        Timber.d("Back Navigation: $from -> ${to ?: "previous"}")
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("back_navigation") {
        //     param("from", from)
        //     to?.let { param("to", it) }
        // }
    }
    
    /**
     * Tracks navigation cancellation events.
     *
     * @param route The route where navigation was cancelled
     * @param reason The reason for cancellation
     */
    fun trackNavigationCancellation(
        route: String,
        reason: String
    ) {
        Timber.d("Navigation Cancelled on route '$route': $reason")
        
        // TODO: Send to analytics service
        // Example:
        // firebaseAnalytics.logEvent("navigation_cancelled") {
        //     param("route", route)
        //     param("reason", reason)
        // }
    }
    
    /**
     * Gets the most viewed screens.
     *
     * @param limit Maximum number of results
     * @return List of route to view count pairs, sorted by count descending
     */
    fun getMostViewedScreens(limit: Int = 10): List<Pair<String, Int>> {
        return screenViews.entries
            .map { it.key to it.value.get() }
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Gets the most common navigation paths.
     *
     * @param limit Maximum number of results
     * @return List of path to count pairs, sorted by count descending
     */
    fun getMostCommonPaths(limit: Int = 10): List<Pair<String, Int>> {
        return navigationPaths.entries
            .map { it.key to it.value.get() }
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Gets screens with the most errors.
     *
     * @param limit Maximum number of results
     * @return List of route to error count pairs, sorted by count descending
     */
    fun getScreensWithMostErrors(limit: Int = 10): List<Pair<String, Int>> {
        return screenErrors.entries
            .map { it.key to it.value.get() }
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Gets the most common failure types.
     *
     * @param limit Maximum number of results
     * @return List of failure type to count pairs, sorted by count descending
     */
    fun getMostCommonFailures(limit: Int = 10): List<Pair<String, Int>> {
        return failureTypes.entries
            .map { it.key to it.value.get() }
            .sortedByDescending { it.second }
            .take(limit)
    }
    
    /**
     * Resets all analytics data.
     */
    fun reset() {
        totalNavigations.set(0)
        totalErrors.set(0)
        screenViews.clear()
        navigationPaths.clear()
        screenErrors.clear()
        failureTypes.clear()
        synchronized(navigationHistory) {
            navigationHistory.clear()
        }
        updateAnalyticsState()
        Timber.d("Analytics data reset")
    }
}
