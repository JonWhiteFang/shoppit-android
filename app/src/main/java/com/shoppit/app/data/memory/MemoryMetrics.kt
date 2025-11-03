package com.shoppit.app.data.memory

/**
 * Data class representing memory usage metrics.
 * 
 * Used for tracking and reporting memory usage over time.
 * 
 * Requirements: 4.5, 10.3
 * 
 * @property currentUsage Current memory usage in bytes
 * @property maxUsage Maximum memory usage observed in bytes
 * @property availableMemory Available memory in bytes
 * @property pressureEvents Number of memory pressure events that occurred
 * @property timestamp Timestamp when metrics were captured
 */
data class MemoryMetrics(
    val currentUsage: Long,
    val maxUsage: Long,
    val availableMemory: Long,
    val pressureEvents: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the memory usage percentage.
     * 
     * @return Memory usage as a percentage (0.0 to 1.0)
     */
    val usagePercent: Double
        get() = if (maxUsage > 0) currentUsage.toDouble() / maxUsage.toDouble() else 0.0
    
    /**
     * Converts memory values to megabytes for easier reading.
     * 
     * @return MemoryMetrics with values in MB
     */
    fun toMegabytes(): MemoryMetricsMB {
        return MemoryMetricsMB(
            currentUsageMB = currentUsage / (1024 * 1024),
            maxUsageMB = maxUsage / (1024 * 1024),
            availableMemoryMB = availableMemory / (1024 * 1024),
            pressureEvents = pressureEvents,
            timestamp = timestamp
        )
    }
}

/**
 * Memory metrics with values in megabytes for easier reading.
 * 
 * @property currentUsageMB Current memory usage in MB
 * @property maxUsageMB Maximum memory usage in MB
 * @property availableMemoryMB Available memory in MB
 * @property pressureEvents Number of memory pressure events
 * @property timestamp Timestamp when metrics were captured
 */
data class MemoryMetricsMB(
    val currentUsageMB: Long,
    val maxUsageMB: Long,
    val availableMemoryMB: Long,
    val pressureEvents: Int,
    val timestamp: Long
) {
    /**
     * Calculates the memory usage percentage.
     * 
     * @return Memory usage as a percentage (0.0 to 1.0)
     */
    val usagePercent: Double
        get() = if (maxUsageMB > 0) currentUsageMB.toDouble() / maxUsageMB.toDouble() else 0.0
    
    override fun toString(): String {
        return "MemoryMetrics(used=${currentUsageMB}MB, available=${availableMemoryMB}MB, " +
                "max=${maxUsageMB}MB, usage=${(usagePercent * 100).toInt()}%, " +
                "pressureEvents=$pressureEvents)"
    }
}
