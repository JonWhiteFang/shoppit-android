package com.shoppit.app.data.startup

import com.shoppit.app.data.performance.PerformanceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StartupOptimizer that manages application startup phases.
 * Tracks initialization durations and integrates with PerformanceMonitor.
 *
 * Requirements:
 * - 1.1: Display first screen within 2000ms on cold start
 * - 1.2: Display first screen within 1000ms on warm start
 * - 1.3: Display first screen within 500ms on hot start
 * - 1.4: Defer non-critical initialization tasks to background threads
 * - 1.5: Log startup phase durations to identify bottlenecks
 */
@Singleton
class StartupOptimizerImpl @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) : StartupOptimizer {
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    companion object {
        private const val COLD_START_TARGET = 2000L // milliseconds
        private const val WARM_START_TARGET = 1000L // milliseconds
        private const val HOT_START_TARGET = 500L // milliseconds
        
        // Trace markers for startup phases
        private const val TRACE_APP_CREATION = "AppCreation"
        private const val TRACE_HILT_INIT = "HiltInitialization"
        private const val TRACE_DATABASE_INIT = "DatabaseInitialization"
        private const val TRACE_CRITICAL_SERVICES = "CriticalServices"
        private const val TRACE_DEFERRED_SERVICES = "DeferredServices"
        private const val TRACE_FIRST_FRAME = "FirstFrame"
    }
    
    // Track phase durations
    private val phaseDurations = ConcurrentHashMap<StartupPhase, Long>()
    
    // Track app launch time
    private var appLaunchTime: Long = System.currentTimeMillis()
    private var firstFrameTime: Long? = null
    
    // Track start type (cold/warm/hot)
    private var startType: StartType? = null
    
    override suspend fun initializeCritical() = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.d("Starting critical initialization")
            addTraceMarker(TRACE_CRITICAL_SERVICES, "start")
            
            // Critical initialization happens here
            // Database is already initialized by Hilt
            // Essential preferences are loaded by Hilt
            
            val duration = System.currentTimeMillis() - startTime
            trackStartupPhase(StartupPhase.CRITICAL_SERVICES, duration)
            addTraceMarker(TRACE_CRITICAL_SERVICES, "end")
            
            Timber.i("Critical initialization completed in ${duration}ms")
        } catch (e: Exception) {
            Timber.e(e, "Error during critical initialization")
            throw e
        }
    }
    
    override suspend fun initializeDeferred() = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            Timber.d("Starting deferred initialization")
            addTraceMarker(TRACE_DEFERRED_SERVICES, "start")
            
            // Deferred initialization happens here
            // WorkManager initialization is handled by Application class
            // Other non-critical services can be initialized here
            
            val duration = System.currentTimeMillis() - startTime
            trackStartupPhase(StartupPhase.DEFERRED_SERVICES, duration)
            addTraceMarker(TRACE_DEFERRED_SERVICES, "end")
            
            Timber.i("Deferred initialization completed in ${duration}ms")
        } catch (e: Exception) {
            Timber.e(e, "Error during deferred initialization")
            // Don't throw - deferred initialization failures shouldn't crash the app
        }
    }
    
    override fun trackStartupPhase(phase: StartupPhase, duration: Long) {
        phaseDurations[phase] = duration
        
        // Track with PerformanceMonitor
        performanceMonitor.trackQuery("startup_${phase.name.lowercase()}", duration)
        
        // Add trace marker
        val traceName = getTraceNameForPhase(phase)
        addTraceMarker(traceName, "completed")
        
        // Log phase completion
        Timber.d("Startup phase ${sanitize(phase.name)} completed in ${duration}ms")
        
        // Check if we've completed first frame
        if (phase == StartupPhase.FIRST_FRAME) {
            firstFrameTime = System.currentTimeMillis()
            val totalStartupTime = getTotalStartupTime()
            
            if (totalStartupTime != null) {
                determineStartType(totalStartupTime)
                logStartupSummary(totalStartupTime)
            }
        }
    }
    
    override fun getTotalStartupTime(): Long? {
        return firstFrameTime?.let { it - appLaunchTime }
    }
    
    override fun getPhaseDuration(phase: StartupPhase): Long? {
        return phaseDurations[phase]
    }
    
    override fun getStartupMetrics(): Map<StartupPhase, Long> {
        return phaseDurations.toMap()
    }
    
    /**
     * Gets startup metrics as a structured data class.
     */
    fun getStartupMetricsData(): StartupMetrics {
        val totalTime = getTotalStartupTime()
        
        return StartupMetrics(
            coldStartTime = if (startType == StartType.COLD) totalTime else null,
            warmStartTime = if (startType == StartType.WARM) totalTime else null,
            hotStartTime = if (startType == StartType.HOT) totalTime else null,
            phases = phaseDurations.toMap(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Determines the type of start based on startup time.
     * This is a heuristic - actual detection would require more sophisticated logic.
     */
    private fun determineStartType(totalStartupTime: Long) {
        startType = when {
            totalStartupTime <= HOT_START_TARGET -> StartType.HOT
            totalStartupTime <= WARM_START_TARGET -> StartType.WARM
            else -> StartType.COLD
        }
        
        Timber.d("Detected ${startType?.name?.let { sanitize(it) }} start (${totalStartupTime}ms)")
    }
    
    /**
     * Logs a summary of startup performance.
     * Compares actual startup time against targets.
     */
    private fun logStartupSummary(totalStartupTime: Long) {
        val summary = buildString {
            appendLine("=== Startup Performance Summary ===")
            appendLine("Start type: ${startType?.name?.let { sanitize(it) } ?: "UNKNOWN"}")
            appendLine("Total startup time: ${totalStartupTime}ms")
            appendLine()
            
            // Compare against targets
            appendLine("Targets:")
            appendLine("  Cold start: $COLD_START_TARGET ms ${if (totalStartupTime <= COLD_START_TARGET) "✓" else "✗"}")
            appendLine("  Warm start: $WARM_START_TARGET ms ${if (totalStartupTime <= WARM_START_TARGET) "✓" else "✗"}")
            appendLine("  Hot start: $HOT_START_TARGET ms ${if (totalStartupTime <= HOT_START_TARGET) "✓" else "✗"}")
            appendLine()
            
            // Phase breakdown
            appendLine("Phase breakdown:")
            StartupPhase.values().forEach { phase ->
                val duration = phaseDurations[phase]
                if (duration != null) {
                    val percentage = (duration.toDouble() / totalStartupTime * 100).toInt()
                    appendLine("  ${sanitize(phase.name)}: ${duration}ms ($percentage%)")
                }
            }
            appendLine("===================================")
        }
        
        // Log as warning if we exceeded cold start target
        if (totalStartupTime > COLD_START_TARGET) {
            Timber.w(summary)
        } else {
            Timber.i(summary)
        }
    }
    
    /**
     * Adds a trace marker for profiling.
     * In production, this could integrate with Firebase Performance or similar.
     */
    private fun addTraceMarker(traceName: String, event: String) {
        Timber.v("[TRACE] ${sanitize(traceName)}: ${sanitize(event)}")
        // In production, add actual trace markers:
        // Trace.beginSection(traceName) / Trace.endSection()
        // or Firebase Performance Monitoring
    }
    
    /**
     * Gets the trace name for a startup phase.
     */
    private fun getTraceNameForPhase(phase: StartupPhase): String {
        return when (phase) {
            StartupPhase.APP_CREATION -> TRACE_APP_CREATION
            StartupPhase.HILT_INITIALIZATION -> TRACE_HILT_INIT
            StartupPhase.DATABASE_INITIALIZATION -> TRACE_DATABASE_INIT
            StartupPhase.CRITICAL_SERVICES -> TRACE_CRITICAL_SERVICES
            StartupPhase.DEFERRED_SERVICES -> TRACE_DEFERRED_SERVICES
            StartupPhase.FIRST_FRAME -> TRACE_FIRST_FRAME
        }
    }
    
    /**
     * Resets startup tracking for testing purposes.
     * Should only be used in debug builds.
     */
    fun reset() {
        phaseDurations.clear()
        appLaunchTime = System.currentTimeMillis()
        firstFrameTime = null
        startType = null
        Timber.d("Startup metrics reset")
    }
}
