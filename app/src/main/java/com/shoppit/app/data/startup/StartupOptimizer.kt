package com.shoppit.app.data.startup

/**
 * Interface for managing application startup optimization.
 * Separates critical initialization from deferred initialization to improve startup time.
 *
 * Requirements:
 * - 1.1: Display first screen within 2000ms on cold start
 * - 1.2: Display first screen within 1000ms on warm start
 * - 1.3: Display first screen within 500ms on hot start
 * - 1.4: Defer non-critical initialization tasks to background threads
 * - 1.5: Log startup phase durations to identify bottlenecks
 */
interface StartupOptimizer {
    /**
     * Initializes critical components synchronously on main thread.
     * Should complete in <100ms to meet startup time requirements.
     * 
     * Critical components include:
     * - Database initialization
     * - Essential preferences
     * - Core services needed for first screen
     */
    suspend fun initializeCritical()
    
    /**
     * Initializes non-critical components asynchronously in background.
     * Can take longer as it doesn't block app startup.
     * 
     * Non-critical components include:
     * - WorkManager
     * - Analytics
     * - Crash reporting
     * - Background sync
     */
    suspend fun initializeDeferred()
    
    /**
     * Tracks the duration of a startup phase.
     * Integrates with PerformanceMonitor for metrics collection.
     *
     * @param phase The startup phase being tracked
     * @param duration The duration in milliseconds
     */
    fun trackStartupPhase(phase: StartupPhase, duration: Long)
    
    /**
     * Gets the total startup time from app launch to first frame.
     *
     * @return Total startup time in milliseconds, or null if not yet complete
     */
    fun getTotalStartupTime(): Long?
    
    /**
     * Gets the duration of a specific startup phase.
     *
     * @param phase The startup phase to query
     * @return Duration in milliseconds, or null if phase hasn't been tracked
     */
    fun getPhaseDuration(phase: StartupPhase): Long?
    
    /**
     * Gets all startup metrics.
     *
     * @return Map of startup phases to their durations
     */
    fun getStartupMetrics(): Map<StartupPhase, Long>
}
