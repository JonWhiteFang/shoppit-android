package com.shoppit.app.presentation.ui.navigation.util

import android.os.SystemClock
import com.shoppit.app.data.performance.PerformanceMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors navigation performance metrics including transition times and frame rates.
 * Tracks navigation timing to ensure transitions complete within 100ms target.
 * Integrated with PerformanceMonitor for centralized metrics tracking.
 *
 * Requirements:
 * - 9.1: Complete navigation transitions within 100ms
 * - 7.2: Display smooth animations without frame drops
 * - 7.5: Handle rapid navigation requests
 * - 9.4: Track navigation duration with PerformanceMonitor
 * - 10.2: Add navigation duration tracking to PerformanceMonitor
 */
@Singleton
class NavigationPerformanceMonitor @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) {
    
    companion object {
        private const val TARGET_TRANSITION_TIME_MS = 100L // Updated from 300ms to 100ms (Requirement 9.1)
        private const val SLOW_TRANSITION_THRESHOLD_MS = 100L // Requirement 9.1
        private const val MAX_HISTORY_SIZE = 50
    }
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    private val _metrics = MutableStateFlow<NavigationMetrics>(NavigationMetrics())
    val metrics: StateFlow<NavigationMetrics> = _metrics.asStateFlow()
    
    private val navigationTimings = mutableMapOf<String, NavigationTiming>()
    private val transitionHistory = mutableListOf<TransitionRecord>()
    
    /**
     * Starts timing a navigation transition.
     * @param from The source route
     * @param to The destination route being navigated to
     */
    fun startTransition(from: String, to: String) {
        val startTime = SystemClock.elapsedRealtime()
        navigationTimings[to] = NavigationTiming(from, to, startTime)
        
        Timber.d("Navigation transition started: ${sanitize(from)} -> ${sanitize(to)}")
    }
    
    /**
     * Ends timing a navigation transition and records metrics.
     * @param to The destination route that was navigated to
     */
    fun endTransition(to: String) {
        val endTime = SystemClock.elapsedRealtime()
        val timing = navigationTimings.remove(to)
        
        if (timing != null) {
            val duration = endTime - timing.startTime
            recordTransition(timing.from, to, duration)
            
            // Track with PerformanceMonitor (Requirement 9.4, 10.2)
            performanceMonitor.trackNavigation(timing.from, to, duration)
            
            if (duration > TARGET_TRANSITION_TIME_MS) {
                Timber.w("Slow navigation transition: ${sanitize(timing.from)} -> ${sanitize(to)} (${duration}ms, target: ${TARGET_TRANSITION_TIME_MS}ms)")
            } else {
                Timber.d("Navigation transition completed: ${sanitize(timing.from)} -> ${sanitize(to)} (${duration}ms)")
            }
        }
    }
    
    /**
     * Records a completed transition in the history.
     */
    private fun recordTransition(from: String, to: String, durationMs: Long) {
        val record = TransitionRecord(
            from = from,
            to = to,
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
                    appendLine("  [$status] ${record.from} -> ${record.to}: ${record.durationMs}ms")
                }
            }
        }
    }
}

/**
 * Internal data class to track navigation timing.
 */
private data class NavigationTiming(
    val from: String,
    val to: String,
    val startTime: Long
)

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
    val from: String,
    val to: String,
    val durationMs: Long,
    val timestamp: Long,
    val isSlowTransition: Boolean
)
