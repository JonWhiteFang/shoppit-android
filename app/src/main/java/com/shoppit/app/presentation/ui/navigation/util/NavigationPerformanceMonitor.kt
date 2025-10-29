package com.shoppit.app.presentation.ui.navigation.util

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors navigation performance metrics including transition times and frame rates.
 * Tracks navigation timing to ensure transitions complete within 300ms target.
 *
 * Requirements:
 * - 7.1: Complete navigation transitions within 300ms
 * - 7.2: Display smooth animations without frame drops
 * - 7.5: Handle rapid navigation requests
 */
object NavigationPerformanceMonitor {
    
    private const val TARGET_TRANSITION_TIME_MS = 300L
    private const val SLOW_TRANSITION_THRESHOLD_MS = 500L
    
    private val _metrics = MutableStateFlow<NavigationMetrics>(NavigationMetrics())
    val metrics: StateFlow<NavigationMetrics> = _metrics.asStateFlow()
    
    private val navigationTimings = mutableMapOf<String, Long>()
    private val transitionHistory = mutableListOf<TransitionRecord>()
    private const val MAX_HISTORY_SIZE = 50
    
    /**
     * Starts timing a navigation transition.
     * @param route The destination route being navigated to
     */
    fun startTransition(route: String) {
        val startTime = SystemClock.elapsedRealtime()
        navigationTimings[route] = startTime
        
        Timber.d("Navigation transition started to: $route")
    }
    
    /**
     * Ends timing a navigation transition and records metrics.
     * @param route The destination route that was navigated to
     */
    fun endTransition(route: String) {
        val endTime = SystemClock.elapsedRealtime()
        val startTime = navigationTimings.remove(route)
        
        if (startTime != null) {
            val duration = endTime - startTime
            recordTransition(route, duration)
            
            if (duration > TARGET_TRANSITION_TIME_MS) {
                Timber.w("Slow navigation transition to $route: ${duration}ms (target: ${TARGET_TRANSITION_TIME_MS}ms)")
            } else {
                Timber.d("Navigation transition to $route completed in ${duration}ms")
            }
        }
    }
    
    /**
     * Records a completed transition in the history.
     */
    private fun recordTransition(route: String, durationMs: Long) {
        val record = TransitionRecord(
            route = route,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis(),
            isSlowTransition = durationMs > SLOW_TRANSITION_THRESHOLD_MS
        )
        
        transitionHistory.add(record)
        
        // Keep history size manageable
        if (transitionHistory.size > MAX_HISTORY_SIZE) {
            transitionHistory.removeAt(0)
        }
        
        updateMetrics()
    }
    
    /**
     * Updates aggregate metrics based on transition history.
     */
    private fun updateMetrics() {
        if (transitionHistory.isEmpty()) return
        
        val durations = transitionHistory.map { it.durationMs }
        val avgDuration = durations.average()
        val maxDuration = durations.maxOrNull() ?: 0L
        val slowTransitions = transitionHistory.count { it.isSlowTransition }
        val totalTransitions = transitionHistory.size
        
        _metrics.value = NavigationMetrics(
            averageTransitionTimeMs = avgDuration,
            maxTransitionTimeMs = maxDuration,
            totalTransitions = totalTransitions,
            slowTransitionCount = slowTransitions,
            slowTransitionPercentage = (slowTransitions.toFloat() / totalTransitions) * 100f
        )
    }
    
    /**
     * Gets the most recent transition records.
     * @param count Number of records to retrieve
     * @return List of recent transition records
     */
    fun getRecentTransitions(count: Int = 10): List<TransitionRecord> {
        return transitionHistory.takeLast(count)
    }
    
    /**
     * Clears all recorded metrics and history.
     */
    fun clearMetrics() {
        navigationTimings.clear()
        transitionHistory.clear()
        _metrics.value = NavigationMetrics()
        Timber.d("Navigation performance metrics cleared")
    }
    
    /**
     * Gets a summary report of navigation performance.
     */
    fun getPerformanceReport(): String {
        val currentMetrics = _metrics.value
        return buildString {
            appendLine("Navigation Performance Report")
            appendLine("============================")
            appendLine("Total Transitions: ${currentMetrics.totalTransitions}")
            appendLine("Average Time: ${"%.2f".format(currentMetrics.averageTransitionTimeMs)}ms")
            appendLine("Max Time: ${currentMetrics.maxTransitionTimeMs}ms")
            appendLine("Slow Transitions: ${currentMetrics.slowTransitionCount} (${"%.1f".format(currentMetrics.slowTransitionPercentage)}%)")
            appendLine("Target Time: ${TARGET_TRANSITION_TIME_MS}ms")
            
            if (transitionHistory.isNotEmpty()) {
                appendLine("\nRecent Transitions:")
                getRecentTransitions(5).forEach { record ->
                    val status = if (record.isSlowTransition) "SLOW" else "OK"
                    appendLine("  [$status] ${record.route}: ${record.durationMs}ms")
                }
            }
        }
    }
}

/**
 * Data class holding aggregate navigation performance metrics.
 */
data class NavigationMetrics(
    val averageTransitionTimeMs: Double = 0.0,
    val maxTransitionTimeMs: Long = 0L,
    val totalTransitions: Int = 0,
    val slowTransitionCount: Int = 0,
    val slowTransitionPercentage: Float = 0f
)

/**
 * Record of a single navigation transition.
 */
data class TransitionRecord(
    val route: String,
    val durationMs: Long,
    val timestamp: Long,
    val isSlowTransition: Boolean
)
