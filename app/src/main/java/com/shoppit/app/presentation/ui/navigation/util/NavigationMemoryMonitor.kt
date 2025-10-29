package com.shoppit.app.presentation.ui.navigation.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Monitors memory usage during navigation to detect memory leaks and excessive allocations.
 * Tracks heap usage and provides alerts for memory issues.
 *
 * Requirements:
 * - 7.5: Monitor memory usage during navigation
 */
object NavigationMemoryMonitor {
    
    private const val MEMORY_WARNING_THRESHOLD_MB = 100 // Alert if navigation increases memory by 100MB
    
    private val _metrics = MutableStateFlow<MemoryMetrics>(MemoryMetrics())
    val metrics: StateFlow<MemoryMetrics> = _metrics.asStateFlow()
    
    private var startMemoryBytes = 0L
    private var isMonitoring = false
    
    /**
     * Starts monitoring memory usage before a navigation transition.
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        // Force garbage collection to get accurate baseline
        System.gc()
        Thread.sleep(100) // Give GC time to complete
        
        startMemoryBytes = getCurrentMemoryUsage()
        
        Timber.d("Memory monitoring started - Baseline: ${formatBytes(startMemoryBytes)}")
    }
    
    /**
     * Stops monitoring and calculates memory metrics.
     */
    fun stopMonitoring() {
        if (!isMonitoring) return
        
        isMonitoring = false
        
        val endMemoryBytes = getCurrentMemoryUsage()
        val memoryDelta = endMemoryBytes - startMemoryBytes
        val memoryDeltaMB = memoryDelta / (1024.0 * 1024.0)
        
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val memoryUsagePercentage = (usedMemory.toFloat() / maxMemory) * 100f
        
        _metrics.value = MemoryMetrics(
            startMemoryBytes = startMemoryBytes,
            endMemoryBytes = endMemoryBytes,
            memoryDeltaBytes = memoryDelta,
            maxMemoryBytes = maxMemory,
            usedMemoryBytes = usedMemory,
            memoryUsagePercentage = memoryUsagePercentage
        )
        
        if (memoryDeltaMB > MEMORY_WARNING_THRESHOLD_MB) {
            Timber.w("High memory increase during navigation: ${"%.2f".format(memoryDeltaMB)} MB")
        }
        
        if (memoryUsagePercentage > 80f) {
            Timber.w("High memory usage: ${"%.1f".format(memoryUsagePercentage)}%")
        }
        
        Timber.d("Memory monitoring stopped - Delta: ${"%.2f".format(memoryDeltaMB)} MB, Usage: ${"%.1f".format(memoryUsagePercentage)}%")
    }
    
    /**
     * Gets current memory usage in bytes.
     */
    private fun getCurrentMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    /**
     * Gets detailed memory information using Debug API.
     */
    fun getDetailedMemoryInfo(): MemoryDetailInfo {
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        
        return MemoryDetailInfo(
            dalvikPrivateDirtyKB = memInfo.dalvikPrivateDirty,
            dalvikPssKB = memInfo.dalvikPss,
            nativePrivateDirtyKB = memInfo.nativePrivateDirty,
            nativePssKB = memInfo.nativePss,
            otherPrivateDirtyKB = memInfo.otherPrivateDirty,
            otherPssKB = memInfo.otherPss,
            totalPrivateDirtyKB = memInfo.totalPrivateDirty,
            totalPssKB = memInfo.totalPss
        )
    }
    
    /**
     * Gets memory report with detailed information.
     */
    fun getMemoryReport(context: Context? = null): String {
        val currentMetrics = _metrics.value
        val detailInfo = getDetailedMemoryInfo()
        
        return buildString {
            appendLine("Memory Usage Report")
            appendLine("===================")
            appendLine("Start Memory: ${formatBytes(currentMetrics.startMemoryBytes)}")
            appendLine("End Memory: ${formatBytes(currentMetrics.endMemoryBytes)}")
            appendLine("Delta: ${formatBytes(currentMetrics.memoryDeltaBytes)}")
            appendLine("Max Memory: ${formatBytes(currentMetrics.maxMemoryBytes)}")
            appendLine("Used Memory: ${formatBytes(currentMetrics.usedMemoryBytes)}")
            appendLine("Usage: ${"%.1f".format(currentMetrics.memoryUsagePercentage)}%")
            
            appendLine("\nDetailed Memory Info:")
            appendLine("  Dalvik Private Dirty: ${detailInfo.dalvikPrivateDirtyKB} KB")
            appendLine("  Dalvik PSS: ${detailInfo.dalvikPssKB} KB")
            appendLine("  Native Private Dirty: ${detailInfo.nativePrivateDirtyKB} KB")
            appendLine("  Native PSS: ${detailInfo.nativePssKB} KB")
            appendLine("  Total Private Dirty: ${detailInfo.totalPrivateDirtyKB} KB")
            appendLine("  Total PSS: ${detailInfo.totalPssKB} KB")
            
            context?.let {
                val activityManager = it.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                activityManager?.let { am ->
                    val memInfo = ActivityManager.MemoryInfo()
                    am.getMemoryInfo(memInfo)
                    appendLine("\nSystem Memory:")
                    appendLine("  Available: ${formatBytes(memInfo.availMem)}")
                    appendLine("  Total: ${formatBytes(memInfo.totalMem)}")
                    appendLine("  Low Memory: ${memInfo.lowMemory}")
                }
            }
        }
    }
    
    /**
     * Formats bytes to human-readable format.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${"%.2f".format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.2f".format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
    
    /**
     * Clears all memory metrics.
     */
    fun clearMetrics() {
        _metrics.value = MemoryMetrics()
        Timber.d("Memory metrics cleared")
    }
}

/**
 * Data class holding memory usage metrics.
 */
data class MemoryMetrics(
    val startMemoryBytes: Long = 0L,
    val endMemoryBytes: Long = 0L,
    val memoryDeltaBytes: Long = 0L,
    val maxMemoryBytes: Long = 0L,
    val usedMemoryBytes: Long = 0L,
    val memoryUsagePercentage: Float = 0f
)

/**
 * Detailed memory information from Debug API.
 */
data class MemoryDetailInfo(
    val dalvikPrivateDirtyKB: Int,
    val dalvikPssKB: Int,
    val nativePrivateDirtyKB: Int,
    val nativePssKB: Int,
    val otherPrivateDirtyKB: Int,
    val otherPssKB: Int,
    val totalPrivateDirtyKB: Int,
    val totalPssKB: Int
)
