package com.shoppit.app.presentation.ui.navigation.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks navigation analytics including screen views, navigation paths, and error rates.
 * Provides insights into user navigation patterns and potential issues.
 *
 * Requirements:
 * - 7.5: Monitor navigation performance metrics
 * - 10.1: Log navigation errors and failures
 */
object NavigationAnalytics {
    
    private val _analyticsState = MutableStateFlow(AnalyticsState())
    val analyticsState: StateFlow<AnalyticsState> = _analyticsState.asStateFlow()
    
    // Track screen view counts
    private val screenViewCounts = ConcurrentHashMap<String, Int>()
    
    // Track navigation paths (from -> to)
    private val navigationPaths = ConcurrentHashMap<String, Int>()
    
    // Track navigation errors by route
    private val errorsByRoute = ConcurrentHashMap<String, Int>()
    
    // Track navigation failures by type
    private val failuresByType = ConcurrentHashMap<String, Int>()
    
    // Recent navigation events for analysis
    private val recentEvents = mutableListOf<NavigationEvent>()
    private const val MAX_RECENT_EVENTS = 100
    
    // Session tracking
    private var sessionStartTime = System.currentTimeMillis()
    private var totalNavigations = 0
    private var totalErrors = 0
    
    /**
     * Records a screen view event.
     * @param route The route of the screen being viewed
     * @param arguments Optional arguments passed to the screen
     */
    fun trackScreenView(route: String, arguments: Map<String, Any?>? = null) {
        val cleanRoute = cleanRoute(route)
        screenViewCounts[cleanRoute] = (screenViewCounts[cleanRoute] ?: 0) + 1
        totalNavigations++
        
        val event = NavigationEvent(
            type = EventType.SCREEN_VIEW,
            route = cleanRoute,
            arguments = arguments,
            timestamp = System.currentTimeMillis()
        )
        addRecentEvent(event)
        
        updateAnalyticsState()
        
        Timber.d("Screen view tracked: $cleanRoute (total views: ${screenViewCounts[cleanRoute]})")
    }
    
    /**
     * Records a navigation path from one screen to another.
     * @param fromRoute The source route
     * @param toRoute The destination route
     * @param arguments Optional arguments passed to the destination
     */
    fun trackNavigationPath(fromRoute: String, toRoute: String, arguments: Map<String, Any?>? = null) {
        val cleanFrom = cleanRoute(fromRoute)
        val cleanTo = cleanRoute(toRoute)
        val pathKey = "$cleanFrom -> $cleanTo"
        
        navigationPaths[pathKey] = (navigationPaths[pathKey] ?: 0) + 1
        
        val event = NavigationEvent(
            type = EventType.NAVIGATION,
            route = pathKey,
            arguments = arguments,
            timestamp = System.currentTimeMillis()
        )
        addRecentEvent(event)
        
        updateAnalyticsState()
        
        Timber.d("Navigation path tracked: $pathKey (count: ${navigationPaths[pathKey]})")
    }
    
    /**
     * Records a navigation error.
     * @param route The route where the error occurred
     * @param errorType The type of error
     * @param message Error message
     * @param exception Optional exception
     */
    fun trackNavigationError(
        route: String,
        errorType: String,
        message: String,
        exception: Throwable? = null
    ) {
        val cleanRoute = cleanRoute(route)
        errorsByRoute[cleanRoute] = (errorsByRoute[cleanRoute] ?: 0) + 1
        failuresByType[errorType] = (failuresByType[errorType] ?: 0) + 1
        totalErrors++
        
        val event = NavigationEvent(
            type = EventType.ERROR,
            route = cleanRoute,
            errorType = errorType,
            errorMessage = message,
            timestamp = System.currentTimeMillis()
        )
        addRecentEvent(event)
        
        updateAnalyticsState()
        
        Timber.e("Navigation error tracked: $cleanRoute - $errorType: $message")
    }
    
    /**
     * Records a navigation failure (when navigation couldn't complete).
     * @param route The route that failed to navigate
     * @param failureType The type of failure
     * @param message Failure message
     */
    fun trackNavigationFailure(
        route: String,
        failureType: String,
        message: String
    ) {
        val cleanRoute = cleanRoute(route)
        failuresByType[failureType] = (failuresByType[failureType] ?: 0) + 1
        totalErrors++
        
        val event = NavigationEvent(
            type = EventType.FAILURE,
            route = cleanRoute,
            errorType = failureType,
            errorMessage = message,
            timestamp = System.currentTimeMillis()
        )
        addRecentEvent(event)
        
        updateAnalyticsState()
        
        Timber.e("Navigation failure tracked: $cleanRoute - $failureType: $message")
    }
    
    /**
     * Gets the most viewed screens.
     * @param limit Maximum number of screens to return
     * @return List of screen routes sorted by view count
     */
    fun getMostViewedScreens(limit: Int = 10): List<Pair<String, Int>> {
        return screenViewCounts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
    
    /**
     * Gets the most common navigation paths.
     * @param limit Maximum number of paths to return
     * @return List of navigation paths sorted by frequency
     */
    fun getMostCommonPaths(limit: Int = 10): List<Pair<String, Int>> {
        return navigationPaths.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
    
    /**
     * Gets screens with the most errors.
     * @param limit Maximum number of screens to return
     * @return List of screen routes sorted by error count
     */
    fun getScreensWithMostErrors(limit: Int = 10): List<Pair<String, Int>> {
        return errorsByRoute.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
    
    /**
     * Gets the most common failure types.
     * @param limit Maximum number of types to return
     * @return List of failure types sorted by frequency
     */
    fun getMostCommonFailures(limit: Int = 10): List<Pair<String, Int>> {
        return failuresByType.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }
    
    /**
     * Gets recent navigation events.
     * @param limit Maximum number of events to return
     * @return List of recent navigation events
     */
    fun getRecentEvents(limit: Int = 20): List<NavigationEvent> {
        return recentEvents.takeLast(limit)
    }
    
    /**
     * Calculates the error rate as a percentage.
     * @return Error rate (0-100)
     */
    fun getErrorRate(): Float {
        if (totalNavigations == 0) return 0f
        return (totalErrors.toFloat() / totalNavigations) * 100f
    }
    
    /**
     * Gets the session duration in milliseconds.
     */
    fun getSessionDuration(): Long {
        return System.currentTimeMillis() - sessionStartTime
    }
    
    /**
     * Generates a comprehensive analytics report.
     */
    fun generateReport(): String {
        return buildString {
            appendLine("Navigation Analytics Report")
            appendLine("===========================")
            appendLine()
            
            // Session info
            appendLine("Session Information:")
            appendLine("  Duration: ${formatDuration(getSessionDuration())}")
            appendLine("  Total Navigations: $totalNavigations")
            appendLine("  Total Errors: $totalErrors")
            appendLine("  Error Rate: ${"%.2f".format(getErrorRate())}%")
            appendLine()
            
            // Most viewed screens
            appendLine("Most Viewed Screens:")
            getMostViewedScreens(5).forEachIndexed { index, (route, count) ->
                appendLine("  ${index + 1}. $route: $count views")
            }
            appendLine()
            
            // Most common paths
            appendLine("Most Common Navigation Paths:")
            getMostCommonPaths(5).forEachIndexed { index, (path, count) ->
                appendLine("  ${index + 1}. $path: $count times")
            }
            appendLine()
            
            // Screens with errors
            if (errorsByRoute.isNotEmpty()) {
                appendLine("Screens with Most Errors:")
                getScreensWithMostErrors(5).forEachIndexed { index, (route, count) ->
                    appendLine("  ${index + 1}. $route: $count errors")
                }
                appendLine()
            }
            
            // Common failures
            if (failuresByType.isNotEmpty()) {
                appendLine("Most Common Failure Types:")
                getMostCommonFailures(5).forEachIndexed { index, (type, count) ->
                    appendLine("  ${index + 1}. $type: $count occurrences")
                }
                appendLine()
            }
            
            // Recent events
            appendLine("Recent Events (last 10):")
            getRecentEvents(10).reversed().forEach { event ->
                val timeAgo = formatTimeAgo(System.currentTimeMillis() - event.timestamp)
                appendLine("  [$timeAgo] ${event.type}: ${event.route}")
                if (event.errorMessage != null) {
                    appendLine("    Error: ${event.errorMessage}")
                }
            }
        }
    }
    
    /**
     * Resets all analytics data.
     */
    fun reset() {
        screenViewCounts.clear()
        navigationPaths.clear()
        errorsByRoute.clear()
        failuresByType.clear()
        recentEvents.clear()
        sessionStartTime = System.currentTimeMillis()
        totalNavigations = 0
        totalErrors = 0
        
        updateAnalyticsState()
        
        Timber.i("Navigation analytics reset")
    }
    
    /**
     * Exports analytics data for external analysis.
     */
    fun exportData(): AnalyticsExport {
        return AnalyticsExport(
            sessionDurationMs = getSessionDuration(),
            totalNavigations = totalNavigations,
            totalErrors = totalErrors,
            errorRate = getErrorRate(),
            screenViews = screenViewCounts.toMap(),
            navigationPaths = navigationPaths.toMap(),
            errorsByRoute = errorsByRoute.toMap(),
            failuresByType = failuresByType.toMap(),
            recentEvents = recentEvents.toList()
        )
    }
    
    // Private helper methods
    
    private fun addRecentEvent(event: NavigationEvent) {
        recentEvents.add(event)
        if (recentEvents.size > MAX_RECENT_EVENTS) {
            recentEvents.removeAt(0)
        }
    }
    
    private fun updateAnalyticsState() {
        _analyticsState.value = AnalyticsState(
            totalNavigations = totalNavigations,
            totalErrors = totalErrors,
            errorRate = getErrorRate(),
            sessionDurationMs = getSessionDuration(),
            mostViewedScreens = getMostViewedScreens(5),
            mostCommonPaths = getMostCommonPaths(5),
            screensWithErrors = getScreensWithMostErrors(5)
        )
    }
    
    private fun cleanRoute(route: String): String {
        // Remove parameter values from route for cleaner analytics
        return route.split("/").mapIndexed { index, part ->
            if (part.toLongOrNull() != null && index > 0) {
                "{id}"
            } else {
                part
            }
        }.joinToString("/")
    }
    
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    private fun formatTimeAgo(ms: Long): String {
        val seconds = ms / 1000
        return when {
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }
}

/**
 * Represents the current state of navigation analytics.
 */
data class AnalyticsState(
    val totalNavigations: Int = 0,
    val totalErrors: Int = 0,
    val errorRate: Float = 0f,
    val sessionDurationMs: Long = 0L,
    val mostViewedScreens: List<Pair<String, Int>> = emptyList(),
    val mostCommonPaths: List<Pair<String, Int>> = emptyList(),
    val screensWithErrors: List<Pair<String, Int>> = emptyList()
)

/**
 * Represents a single navigation event.
 */
data class NavigationEvent(
    val type: EventType,
    val route: String,
    val arguments: Map<String, Any?>? = null,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val timestamp: Long
)

/**
 * Types of navigation events.
 */
enum class EventType {
    SCREEN_VIEW,
    NAVIGATION,
    ERROR,
    FAILURE
}

/**
 * Export format for analytics data.
 */
data class AnalyticsExport(
    val sessionDurationMs: Long,
    val totalNavigations: Int,
    val totalErrors: Int,
    val errorRate: Float,
    val screenViews: Map<String, Int>,
    val navigationPaths: Map<String, Int>,
    val errorsByRoute: Map<String, Int>,
    val failuresByType: Map<String, Int>,
    val recentEvents: List<NavigationEvent>
)
