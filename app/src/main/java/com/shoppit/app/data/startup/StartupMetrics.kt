package com.shoppit.app.data.startup

/**
 * Data class representing startup performance metrics.
 * Tracks different types of app starts and their durations.
 *
 * Requirements:
 * - 1.1: Display first screen within 2000ms on cold start
 * - 1.2: Display first screen within 1000ms on warm start
 * - 1.3: Display first screen within 500ms on hot start
 * - 1.5: Log startup phase durations to identify bottlenecks
 */
data class StartupMetrics(
    /**
     * Cold start time: App launched from completely stopped state.
     * Target: < 2000ms
     */
    val coldStartTime: Long? = null,
    
    /**
     * Warm start time: App process in memory but activity destroyed.
     * Target: < 1000ms
     */
    val warmStartTime: Long? = null,
    
    /**
     * Hot start time: App brought to foreground from background.
     * Target: < 500ms
     */
    val hotStartTime: Long? = null,
    
    /**
     * Breakdown of time spent in each startup phase.
     */
    val phases: Map<StartupPhase, Long> = emptyMap(),
    
    /**
     * Timestamp when metrics were recorded.
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Gets the total startup time (whichever type was measured).
     */
    val totalStartupTime: Long?
        get() = coldStartTime ?: warmStartTime ?: hotStartTime
    
    /**
     * Determines the type of start based on which metric is available.
     */
    val startType: StartType?
        get() = when {
            coldStartTime != null -> StartType.COLD
            warmStartTime != null -> StartType.WARM
            hotStartTime != null -> StartType.HOT
            else -> null
        }
    
    /**
     * Checks if startup time meets the target for its type.
     */
    val meetsTarget: Boolean
        get() = when (startType) {
            StartType.COLD -> (coldStartTime ?: Long.MAX_VALUE) <= 2000L
            StartType.WARM -> (warmStartTime ?: Long.MAX_VALUE) <= 1000L
            StartType.HOT -> (hotStartTime ?: Long.MAX_VALUE) <= 500L
            null -> false
        }
}

/**
 * Enum representing different types of app starts.
 */
enum class StartType {
    /**
     * Cold start: App launched from completely stopped state.
     */
    COLD,
    
    /**
     * Warm start: App process in memory but activity destroyed.
     */
    WARM,
    
    /**
     * Hot start: App brought to foreground from background.
     */
    HOT
}
