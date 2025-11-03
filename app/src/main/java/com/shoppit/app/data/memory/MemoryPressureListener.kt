package com.shoppit.app.data.memory

/**
 * Interface for components that need to respond to memory pressure events.
 * 
 * Components that cache data or hold references to large objects should implement
 * this interface to receive notifications when memory is running low.
 * 
 * Requirements: 4.1, 4.4
 */
interface MemoryPressureListener {
    /**
     * Called when memory pressure level changes.
     * 
     * Implementations should respond appropriately based on the pressure level:
     * - LOW: Consider reducing cache sizes
     * - MODERATE: Aggressively reduce caches
     * - CRITICAL: Clear all non-essential caches
     * 
     * @param level The current memory pressure level
     */
    fun onMemoryPressure(level: MemoryPressureLevel)
}
