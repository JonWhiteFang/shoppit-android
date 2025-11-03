package com.shoppit.app.data.memory

/**
 * Represents different levels of memory pressure in the system.
 * 
 * Memory pressure levels are determined by the percentage of available memory:
 * - LOW: < 25% available memory
 * - MODERATE: < 15% available memory
 * - CRITICAL: < 10% available memory
 * 
 * Requirements: 4.1, 4.5
 */
enum class MemoryPressureLevel {
    /**
     * Low memory pressure - less than 25% of memory available.
     * Consider reducing cache sizes and cleaning up non-essential data.
     */
    LOW,
    
    /**
     * Moderate memory pressure - less than 15% of memory available.
     * Aggressively reduce cache sizes and clear non-critical caches.
     */
    MODERATE,
    
    /**
     * Critical memory pressure - less than 10% of memory available.
     * Clear all possible caches and release all non-essential resources.
     */
    CRITICAL
}
