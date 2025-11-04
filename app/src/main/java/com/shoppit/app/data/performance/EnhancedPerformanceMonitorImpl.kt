package com.shoppit.app.data.performance

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced implementation of PerformanceMonitor that provides comprehensive
 * performance reporting including startup, query, UI, and memory metrics.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Singleton
class EnhancedPerformanceMonitorImpl @Inject constructor(
    private val baseMonitor: PerformanceMonitorImpl,
    private val logger: PerformanceLogger
) : EnhancedPerformanceMonitor, PerformanceMonitor by baseMonitor {
    
    companion object {
        private const val SLOW_QUERY_THRESHOLD = 100L // milliseconds
        private const val SLOW_NAVIGATION_THRESHOLD = 100L // milliseconds
        private const val SLOW_FRAME_THRESHOLD = 16.67 // milliseconds (60 FPS)
    }
    
    override fun getEnhancedSummary(): EnhancedPerformanceSummary {
        val baseSummary = baseMonitor.getSummary()
        
        // Frame statistics
        val frameStats = try {
            baseMonitor.getFrameDropStats()
        } catch (e: Exception) {
            Timber.e(e, "Error getting frame drop stats")
            null
        }
        
        // Navigation statistics
        val navigationMetrics = baseMonitor.getNavigationMetrics()
        val totalNavigations = navigationMetrics.sumOf { it.transitionCount }
        val avgNavigationTime = if (navigationMetrics.isNotEmpty()) {
            navigationMetrics.map { it.avgDuration }.average().toLong()
        } else {
            0L
        }
        val slowNavigationCount = baseMonitor.getSlowNavigations(SLOW_NAVIGATION_THRESHOLD).size
        
        val navigationStats = NavigationStats(
            totalNavigations = totalNavigations,
            avgNavigationTime = avgNavigationTime,
            slowNavigationCount = slowNavigationCount
        )
        
        // Memory statistics
        val memoryMetrics = baseMonitor.getMemoryMetrics()
        val memoryStats = memoryMetrics?.let {
            val currentUsageMB = it.currentUsage / (1024 * 1024)
            val maxUsageMB = it.maxUsage / (1024 * 1024)
            val availableMemoryMB = it.availableMemory / (1024 * 1024)
            val usagePercentage = if (it.maxUsage > 0) {
                it.currentUsage.toDouble() / it.maxUsage.toDouble()
            } else {
                0.0
            }
            
            MemoryStats(
                currentUsageMB = currentUsageMB,
                maxUsageMB = maxUsageMB,
                availableMemoryMB = availableMemoryMB,
                pressureEvents = it.pressureEvents,
                usagePercentage = usagePercentage
            )
        }
        
        // Slow frame count
        val slowFrameCount = baseMonitor.getSlowFrames(SLOW_FRAME_THRESHOLD).size
        
        val summary = EnhancedPerformanceSummary(
            baseSummary = baseSummary,
            frameStats = frameStats,
            navigationStats = navigationStats,
            memoryStats = memoryStats,
            slowFrameCount = slowFrameCount,
            slowNavigationCount = slowNavigationCount
        )
        
        // Log the summary
        logger.logSummary(summary)
        
        // Log slow queries if any
        val slowQueries = baseMonitor.getSlowQueries(SLOW_QUERY_THRESHOLD)
        if (slowQueries.isNotEmpty()) {
            logger.logSlowQueries(slowQueries, SLOW_QUERY_THRESHOLD)
        }
        
        // Log slow navigations if any
        val slowNavigations = baseMonitor.getSlowNavigations(SLOW_NAVIGATION_THRESHOLD)
        if (slowNavigations.isNotEmpty()) {
            logger.logSlowNavigations(slowNavigations, SLOW_NAVIGATION_THRESHOLD)
        }
        
        // Log slow frames if any
        val slowFrames = baseMonitor.getSlowFrames(SLOW_FRAME_THRESHOLD)
        if (slowFrames.isNotEmpty()) {
            logger.logSlowFrames(slowFrames, SLOW_FRAME_THRESHOLD)
        }
        
        return summary
    }
    
    override fun exportMetrics(): Map<String, Any> {
        val summary = getEnhancedSummary()
        
        val metrics = mutableMapOf<String, Any>()
        
        // Base metrics
        metrics["database"] = mapOf(
            "totalQueries" to summary.baseSummary.totalQueries,
            "totalTransactions" to summary.baseSummary.totalTransactions,
            "avgQueryDuration" to summary.baseSummary.avgQueryDuration,
            "avgTransactionDuration" to summary.baseSummary.avgTransactionDuration,
            "slowQueryCount" to summary.baseSummary.slowQueryCount,
            "slowQueries" to baseMonitor.getSlowQueries(SLOW_QUERY_THRESHOLD).map { query ->
                mapOf(
                    "query" to query.query,
                    "avgDuration" to query.avgDuration,
                    "executionCount" to query.executionCount,
                    "maxDuration" to query.maxDuration
                )
            }
        )
        
        // Cache metrics
        metrics["cache"] = mapOf(
            "hitRate" to summary.baseSummary.cacheHitRate,
            "hitRatePercentage" to (summary.baseSummary.cacheHitRate * 100).toInt()
        )
        
        // Frame metrics
        summary.frameStats?.let { frameStats ->
            metrics["ui"] = mapOf(
                "totalFrames" to frameStats.totalFrames,
                "droppedFrames" to frameStats.droppedFrames,
                "avgFrameTime" to frameStats.avgFrameTime,
                "maxFrameTime" to frameStats.maxFrameTime,
                "frameDropRate" to frameStats.frameDropRate,
                "frameDropRatePercentage" to (frameStats.frameDropRate * 100).toInt(),
                "slowFrameCount" to summary.slowFrameCount,
                "slowFrames" to baseMonitor.getSlowFrames(SLOW_FRAME_THRESHOLD).take(10).map { frame ->
                    mapOf(
                        "screenName" to frame.screenName,
                        "frameTime" to frame.frameTime,
                        "timestamp" to frame.timestamp
                    )
                }
            )
        }
        
        // Navigation metrics
        metrics["navigation"] = mapOf(
            "totalNavigations" to summary.navigationStats.totalNavigations,
            "avgNavigationTime" to summary.navigationStats.avgNavigationTime,
            "slowNavigationCount" to summary.navigationStats.slowNavigationCount,
            "slowNavigations" to baseMonitor.getSlowNavigations(SLOW_NAVIGATION_THRESHOLD).map { nav ->
                mapOf(
                    "from" to nav.from,
                    "to" to nav.to,
                    "avgDuration" to nav.avgDuration,
                    "transitionCount" to nav.transitionCount,
                    "maxDuration" to nav.maxDuration
                )
            }
        )
        
        // Memory metrics
        summary.memoryStats?.let { memoryStats ->
            metrics["memory"] = mapOf(
                "currentUsageMB" to memoryStats.currentUsageMB,
                "maxUsageMB" to memoryStats.maxUsageMB,
                "availableMemoryMB" to memoryStats.availableMemoryMB,
                "usagePercentage" to memoryStats.usagePercentage,
                "usagePercentageInt" to (memoryStats.usagePercentage * 100).toInt(),
                "pressureEvents" to memoryStats.pressureEvents
            )
        }
        
        // Log the export
        logger.logMetricsExport(metrics)
        
        return metrics
    }
}
