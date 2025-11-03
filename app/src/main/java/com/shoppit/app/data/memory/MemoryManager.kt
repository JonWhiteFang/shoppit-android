package com.shoppit.app.data.memory

/**
 * Interface for monitoring and managing application memory usage.
 * 
 * Provides methods to track current memory usage, available memory, and respond
 * to low memory conditions. Components can register as listeners to receive
 * notifications when memory pressure increases.
 * 
 * Requirements: 4.1, 4.5
 */
interface MemoryManager {
    /**
     * Gets the current memory usage of the application in bytes.
     * 
     * @return Current memory usage in bytes
     */
    fun getCurrentMemoryUsage(): Long
    
    /**
     * Gets the available memory for the application in bytes.
     * 
     * @return Available memory in bytes
     */
    fun getAvailableMemory(): Long
    
    /**
     * Gets the maximum memory the application can use in bytes.
     * 
     * @return Maximum memory in bytes
     */
    fun getMaxMemory(): Long
    
    /**
     * Calculates the percentage of memory currently in use.
     * 
     * @return Memory usage percentage (0.0 to 1.0)
     */
    fun getMemoryUsagePercent(): Double
    
    /**
     * Triggers cache cleanup and resource release when memory is low.
     * This method is called by the system when it detects low memory conditions.
     */
    fun onLowMemory()
    
    /**
     * Registers a component to receive memory pressure notifications.
     * 
     * @param listener The listener to register
     */
    fun registerMemoryPressureListener(listener: MemoryPressureListener)
    
    /**
     * Unregisters a previously registered memory pressure listener.
     * 
     * @param listener The listener to unregister
     */
    fun unregisterMemoryPressureListener(listener: MemoryPressureListener)
    
    /**
     * Gets the current memory pressure level based on available memory.
     * 
     * @return Current memory pressure level, or null if memory is not under pressure
     */
    fun getCurrentPressureLevel(): MemoryPressureLevel?
}
