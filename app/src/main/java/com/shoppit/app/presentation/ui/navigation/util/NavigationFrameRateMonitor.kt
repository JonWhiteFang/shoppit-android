package com.shoppit.app.presentation.ui.navigation.util

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors frame rates during navigation transitions to ensure smooth 60fps performance.
 * Tracks frame drops and provides metrics for performance analysis.
 *
 * Requirements:
 * - 7.2: Display smooth animations without frame drops
 * - 7.5: Track frame rates during transitions
 */
object NavigationFrameRateMonitor {
    
    private const val TARGET_FPS = 60
    private const val TARGET_FRAME_TIME_MS = 16.67 // 1000ms / 60fps
    private const val FRAME_DROP_THRESHOLD_MS = 32.0 // 2 frames
    
    private val _metrics = MutableStateFlow<FrameRateMetrics>(FrameRateMetrics())
    val metrics: StateFlow<FrameRateMetrics> = _metrics.asStateFlow()
    
    private var isMonitoring = false
    private var frameCount = 0
    private var droppedFrameCount = 0
    private var lastFrameTimeNanos = 0L
    private var monitoringStartTime = 0L
    private val frameTimes = mutableListOf<Long>()
    
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return
            
            if (lastFrameTimeNanos != 0L) {
                val frameTimeMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000.0
                frameTimes.add((frameTimeMs * 1000).toLong()) // Store as microseconds
                
                frameCount++
                
                // Check for dropped frames
                if (frameTimeMs > FRAME_DROP_THRESHOLD_MS) {
                    droppedFrameCount++
                    Timber.w("Frame drop detected: ${frameTimeMs}ms (threshold: ${FRAME_DROP_THRESHOLD_MS}ms)")
                }
            }
            
            lastFrameTimeNanos = frameTimeNanos
            
            if (isMonitoring) {
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }
    
    /**
     * Starts monitoring frame rates during a navigation transition.
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        frameCount = 0
        droppedFrameCount = 0
        lastFrameTimeNanos = 0L
        monitoringStartTime = System.currentTimeMillis()
        frameTimes.clear()
        
        Choreographer.getInstance().postFrameCallback(frameCallback)
        Timber.d("Frame rate monitoring started")
    }
    
    /**
     * Stops monitoring and calculates metrics.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        
        val duration = System.currentTimeMillis() - monitoringStartTime
        val avgFps = if (duration > 0) {
            (frameCount * 1000.0) / duration
        } else {
            0.0
        }
        
        val avgFrameTime = if (frameTimes.isNotEmpty()) {
            frameTimes.average() / 1000.0 // Convert to milliseconds
        } else {
            0.0
        }
        
        val maxFrameTime = if (frameTimes.isNotEmpty()) {
            frameTimes.maxOrNull()?.toDouble()?.div(1000.0) ?: 0.0
        } else {
            0.0
        }
        
        val dropRate = if (frameCount > 0) {
            (droppedFrameCount.toFloat() / frameCount) * 100f
        } else {
            0f
        }
        
        _metrics.value = FrameRateMetrics(
            averageFps = avgFps,
            averageFrameTimeMs = avgFrameTime,
            maxFrameTimeMs = maxFrameTime,
            totalFrames = frameCount,
            droppedFrames = droppedFrameCount,
            frameDropPercentage = dropRate,
            monitoringDurationMs = duration
        )
        
        if (avgFps < TARGET_FPS * 0.9) { // Alert if below 90% of target
            Timber.w("Low frame rate detected: ${"%.1f".format(avgFps)} fps (target: $TARGET_FPS fps)")
        }
        
        if (dropRate > 5.0f) { // Alert if more than 5% frame drops
            Timber.w("High frame drop rate: ${"%.1f".format(dropRate)}%")
        }
        
        Timber.d("Frame rate monitoring stopped - Avg FPS: ${"%.1f".format(avgFps)}, Drops: $droppedFrameCount/$frameCount")
    }
    
    /**
     * Gets a detailed frame rate report.
     */
    fun getFrameRateReport(): String {
        val currentMetrics = _metrics.value
        return buildString {
            appendLine("Frame Rate Report")
            appendLine("=================")
            appendLine("Average FPS: ${"%.1f".format(currentMetrics.averageFps)} (target: $TARGET_FPS)")
            appendLine("Average Frame Time: ${"%.2f".format(currentMetrics.averageFrameTimeMs)}ms")
            appendLine("Max Frame Time: ${"%.2f".format(currentMetrics.maxFrameTimeMs)}ms")
            appendLine("Total Frames: ${currentMetrics.totalFrames}")
            appendLine("Dropped Frames: ${currentMetrics.droppedFrames} (${"%.1f".format(currentMetrics.frameDropPercentage)}%)")
            appendLine("Monitoring Duration: ${currentMetrics.monitoringDurationMs}ms")
            
            val performance = when {
                currentMetrics.averageFps >= TARGET_FPS * 0.95 -> "Excellent"
                currentMetrics.averageFps >= TARGET_FPS * 0.85 -> "Good"
                currentMetrics.averageFps >= TARGET_FPS * 0.70 -> "Fair"
                else -> "Poor"
            }
            appendLine("Performance: $performance")
        }
    }
    
    /**
     * Clears all frame rate metrics.
     */
    fun clearMetrics() {
        _metrics.value = FrameRateMetrics()
        frameTimes.clear()
        Timber.d("Frame rate metrics cleared")
    }
}

/**
 * Data class holding frame rate metrics.
 */
data class FrameRateMetrics(
    val averageFps: Double = 0.0,
    val averageFrameTimeMs: Double = 0.0,
    val maxFrameTimeMs: Double = 0.0,
    val totalFrames: Int = 0,
    val droppedFrames: Int = 0,
    val frameDropPercentage: Float = 0f,
    val monitoringDurationMs: Long = 0L
)
