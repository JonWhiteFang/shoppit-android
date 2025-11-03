package com.shoppit.app.data.memory

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import com.shoppit.app.data.performance.PerformanceMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MemoryManager that monitors and manages application memory usage.
 * 
 * This class implements ComponentCallbacks2 to receive system memory pressure callbacks
 * and notifies registered listeners when memory conditions change.
 * 
 * Memory pressure levels are determined by available memory percentage:
 * - CRITICAL: < 10% available
 * - MODERATE: < 15% available
 * - LOW: < 25% available
 * 
 * Requirements: 4.1, 4.5, 10.3
 */
@Singleton
class MemoryManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val performanceMonitor: PerformanceMonitor
) : MemoryManager, ComponentCallbacks2 {
    
    private val runtime = Runtime.getRuntime()
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val listeners = CopyOnWriteArrayList<MemoryPressureListener>()
    
    private var lastPressureLevel: MemoryPressureLevel? = null
    
    init {
        // Register for system memory callbacks
        context.registerComponentCallbacks(this)
        Timber.d("MemoryManager initialized")
        logMemoryStatus()
    }
    
    override fun getCurrentMemoryUsage(): Long {
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    override fun getAvailableMemory(): Long {
        return runtime.maxMemory() - getCurrentMemoryUsage()
    }
    
    override fun getMaxMemory(): Long {
        return runtime.maxMemory()
    }
    
    override fun getMemoryUsagePercent(): Double {
        val used = getCurrentMemoryUsage().toDouble()
        val max = getMaxMemory().toDouble()
        return if (max > 0) used / max else 0.0
    }
    
    override fun onLowMemory() {
        Timber.w("System low memory callback received")
        handleMemoryPressure(MemoryPressureLevel.CRITICAL)
    }
    
    override fun onTrimMemory(level: Int) {
        Timber.d("onTrimMemory called with level: $level")
        
        val pressureLevel = when (level) {
            // Running states - app is visible
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> MemoryPressureLevel.CRITICAL
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> MemoryPressureLevel.MODERATE
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> MemoryPressureLevel.LOW
            
            // Background states - app is in background
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> MemoryPressureLevel.MODERATE
            
            // UI hidden - app just went to background
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> MemoryPressureLevel.LOW
            
            else -> null
        }
        
        pressureLevel?.let { handleMemoryPressure(it) }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        // No action needed for configuration changes
    }
    
    override fun registerMemoryPressureListener(listener: MemoryPressureListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Timber.d("Registered memory pressure listener: ${listener.javaClass.simpleName}")
        }
    }
    
    override fun unregisterMemoryPressureListener(listener: MemoryPressureListener) {
        if (listeners.remove(listener)) {
            Timber.d("Unregistered memory pressure listener: ${listener.javaClass.simpleName}")
        }
    }
    
    override fun getCurrentPressureLevel(): MemoryPressureLevel? {
        val availablePercent = (getAvailableMemory().toDouble() / getMaxMemory().toDouble()) * 100
        
        return when {
            availablePercent < 10 -> MemoryPressureLevel.CRITICAL
            availablePercent < 15 -> MemoryPressureLevel.MODERATE
            availablePercent < 25 -> MemoryPressureLevel.LOW
            else -> null
        }
    }
    
    /**
     * Handles memory pressure by notifying all registered listeners.
     * Only notifies if the pressure level has changed since last notification.
     * 
     * @param level The current memory pressure level
     */
    private fun handleMemoryPressure(level: MemoryPressureLevel) {
        // Only notify if pressure level changed
        if (level != lastPressureLevel) {
            lastPressureLevel = level
            
            Timber.w("Memory pressure: $level")
            logMemoryStatus()
            
            // Track memory usage with PerformanceMonitor
            performanceMonitor.trackMemoryUsage(
                usedMemory = getCurrentMemoryUsage(),
                availableMemory = getAvailableMemory()
            )
            
            // Notify all listeners
            listeners.forEach { listener ->
                try {
                    listener.onMemoryPressure(level)
                } catch (e: Exception) {
                    Timber.e(e, "Error notifying memory pressure listener: ${listener.javaClass.simpleName}")
                }
            }
        }
    }
    
    /**
     * Logs current memory status for debugging.
     */
    private fun logMemoryStatus() {
        val usedMB = getCurrentMemoryUsage() / (1024 * 1024)
        val availableMB = getAvailableMemory() / (1024 * 1024)
        val maxMB = getMaxMemory() / (1024 * 1024)
        val usagePercent = (getMemoryUsagePercent() * 100).toInt()
        
        Timber.d("Memory: ${usedMB}MB used, ${availableMB}MB available, ${maxMB}MB max ($usagePercent% used)")
    }
    
    /**
     * Cleans up resources when the manager is no longer needed.
     * Should be called when the application is being destroyed.
     */
    fun cleanup() {
        context.unregisterComponentCallbacks(this)
        listeners.clear()
        Timber.d("MemoryManager cleaned up")
    }
}
