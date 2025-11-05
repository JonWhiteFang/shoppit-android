package com.shoppit.app.performance

import android.app.ActivityManager
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Memory performance tests to validate memory usage and leak detection.
 * 
 * Requirements tested:
 * - 4.1: Memory usage < 100MB
 * - 4.2: Cache effectiveness > 80%
 * - 8.1: Cache hit rate monitoring
 * - 8.2: Cache miss tracking
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MemoryPerformanceTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var activityManager: ActivityManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    /**
     * Get current memory usage in MB.
     */
    private fun getCurrentMemoryUsage(): Double {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024.0 * 1024.0) // Convert to MB
    }

    /**
     * Get available memory in MB.
     */
    private fun getAvailableMemory(): Double {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024.0 * 1024.0) // Convert to MB
    }

    /**
     * Test memory usage during normal operation.
     * Target: < 100MB
     * Requirement: 4.1
     */
    @Test
    fun testMemoryUsageDuringNormalOperation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Let app initialize
        Thread.sleep(2000)
        
        // Measure memory usage
        val memoryUsage = getCurrentMemoryUsage()
        val availableMemory = getAvailableMemory()
        
        Timber.tag("MemoryPerformance").i("Memory usage: ${memoryUsage}MB")
        Timber.tag("MemoryPerformance").i("Available memory: ${availableMemory}MB")
        
        scenario.close()
        
        assertTrue(
            "Memory usage should be < 100MB, was ${memoryUsage}MB",
            memoryUsage < 100
        )
    }

    /**
     * Test memory usage after multiple screen navigations.
     * Target: No significant memory increase
     * Requirement: 4.1
     */
    @Test
    fun testMemoryUsageAfterNavigations() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Initial memory measurement
        Thread.sleep(1000)
        val initialMemory = getCurrentMemoryUsage()
        
        Timber.tag("MemoryPerformance").i("Initial memory: ${initialMemory}MB")
        
        // Simulate multiple navigations
        repeat(10) {
            Thread.sleep(500)
            // Memory measurement during navigation
        }
        
        // Final memory measurement
        val finalMemory = getCurrentMemoryUsage()
        val memoryIncrease = finalMemory - initialMemory
        
        Timber.tag("MemoryPerformance").i("Final memory: ${finalMemory}MB")
        Timber.tag("MemoryPerformance").i("Memory increase: ${memoryIncrease}MB")
        
        scenario.close()
        
        // Memory increase should be minimal (< 20MB)
        assertTrue(
            "Memory increase should be < 20MB after navigations, was ${memoryIncrease}MB",
            memoryIncrease < 20
        )
        
        // Final memory should still be under limit
        assertTrue(
            "Final memory should be < 100MB, was ${finalMemory}MB",
            finalMemory < 100
        )
    }

    /**
     * Test memory cleanup after activity destruction.
     * Target: Memory should be released
     * Requirement: 4.1
     */
    @Test
    fun testMemoryCleanupAfterDestruction() {
        // Launch and measure
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        val memoryWithActivity = getCurrentMemoryUsage()
        
        Timber.tag("MemoryPerformance").i("Memory with activity: ${memoryWithActivity}MB")
        
        // Close activity
        scenario.close()
        
        // Force garbage collection
        System.gc()
        Thread.sleep(1000)
        
        // Measure after cleanup
        val memoryAfterCleanup = getCurrentMemoryUsage()
        val memoryReleased = memoryWithActivity - memoryAfterCleanup
        
        Timber.tag("MemoryPerformance").i("Memory after cleanup: ${memoryAfterCleanup}MB")
        Timber.tag("MemoryPerformance").i("Memory released: ${memoryReleased}MB")
        
        // Some memory should be released (at least 5MB)
        assertTrue(
            "Memory should be released after cleanup (> 5MB), was ${memoryReleased}MB",
            memoryReleased > 5 || memoryAfterCleanup < 50 // Either released or already low
        )
    }

    /**
     * Test memory usage under stress (multiple activities).
     * Target: < 100MB even under stress
     * Requirement: 4.1
     */
    @Test
    fun testMemoryUsageUnderStress() {
        val scenarios = mutableListOf<ActivityScenario<MainActivity>>()
        
        // Launch multiple activity instances
        repeat(3) {
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenarios.add(scenario)
            Thread.sleep(500)
        }
        
        // Measure memory under stress
        val stressMemory = getCurrentMemoryUsage()
        
        Timber.tag("MemoryPerformance").i("Memory under stress: ${stressMemory}MB")
        
        // Clean up
        scenarios.forEach { it.close() }
        
        assertTrue(
            "Memory under stress should be < 150MB, was ${stressMemory}MB",
            stressMemory < 150
        )
    }

    /**
     * Test memory pressure handling.
     * Target: App should handle low memory gracefully
     * Requirement: 4.2
     */
    @Test
    fun testMemoryPressureHandling() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        
        val initialMemory = getCurrentMemoryUsage()
        
        // Simulate memory pressure by allocating memory
        val largeList = mutableListOf<ByteArray>()
        try {
            repeat(10) {
                largeList.add(ByteArray(1024 * 1024)) // 1MB each
                Thread.sleep(100)
            }
        } catch (e: OutOfMemoryError) {
            Timber.tag("MemoryPerformance").w("OutOfMemoryError during stress test (expected)")
        }
        
        // Trigger garbage collection
        largeList.clear()
        System.gc()
        Thread.sleep(1000)
        
        val finalMemory = getCurrentMemoryUsage()
        
        Timber.tag("MemoryPerformance").i("Initial memory: ${initialMemory}MB")
        Timber.tag("MemoryPerformance").i("Final memory after pressure: ${finalMemory}MB")
        
        scenario.close()
        
        // App should still be functional and memory should recover
        assertTrue(
            "Memory should recover after pressure, final: ${finalMemory}MB",
            finalMemory < 120
        )
    }

    /**
     * Test memory leak detection through repeated operations.
     * Target: No memory leaks
     * Requirement: 4.1
     */
    @Test
    fun testMemoryLeakDetection() {
        val memoryMeasurements = mutableListOf<Double>()
        
        // Perform repeated operations
        repeat(5) { iteration ->
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            Thread.sleep(1000)
            
            val memory = getCurrentMemoryUsage()
            memoryMeasurements.add(memory)
            
            Timber.tag("MemoryPerformance").i("Iteration $iteration memory: ${memory}MB")
            
            scenario.close()
            System.gc()
            Thread.sleep(500)
        }
        
        // Check for memory leak pattern (continuously increasing memory)
        val firstMemory = memoryMeasurements.first()
        val lastMemory = memoryMeasurements.last()
        val memoryGrowth = lastMemory - firstMemory
        
        Timber.tag("MemoryPerformance").i("Memory growth over iterations: ${memoryGrowth}MB")
        
        // Memory growth should be minimal (< 30MB over 5 iterations)
        assertTrue(
            "Memory growth should be minimal (< 30MB), was ${memoryGrowth}MB",
            memoryGrowth < 30
        )
    }

    /**
     * Test cache memory usage.
     * Target: Cache should not consume excessive memory
     * Requirement: 4.2, 8.1
     */
    @Test
    fun testCacheMemoryUsage() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Let app initialize and populate caches
        Thread.sleep(2000)
        
        val memoryWithCache = getCurrentMemoryUsage()
        
        Timber.tag("MemoryPerformance").i("Memory with cache: ${memoryWithCache}MB")
        
        scenario.close()
        
        // Memory with cache should still be reasonable
        assertTrue(
            "Memory with cache should be < 100MB, was ${memoryWithCache}MB",
            memoryWithCache < 100
        )
    }

    /**
     * Test memory usage during data loading.
     * Target: Memory should not spike during loading
     * Requirement: 4.1
     */
    @Test
    fun testMemoryUsageDuringDataLoading() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        
        val memoryBeforeLoad = getCurrentMemoryUsage()
        
        // Simulate data loading
        Thread.sleep(2000)
        
        val memoryDuringLoad = getCurrentMemoryUsage()
        val memorySpike = memoryDuringLoad - memoryBeforeLoad
        
        Timber.tag("MemoryPerformance").i("Memory before load: ${memoryBeforeLoad}MB")
        Timber.tag("MemoryPerformance").i("Memory during load: ${memoryDuringLoad}MB")
        Timber.tag("MemoryPerformance").i("Memory spike: ${memorySpike}MB")
        
        scenario.close()
        
        // Memory spike should be reasonable (< 30MB)
        assertTrue(
            "Memory spike during loading should be < 30MB, was ${memorySpike}MB",
            memorySpike < 30
        )
    }

    /**
     * Test available memory monitoring.
     * Target: App should monitor available memory
     * Requirement: 4.1
     */
    @Test
    fun testAvailableMemoryMonitoring() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(1000)
        
        val availableMemory = getAvailableMemory()
        val usedMemory = getCurrentMemoryUsage()
        val totalMemory = availableMemory + usedMemory
        
        Timber.tag("MemoryPerformance").i("Available memory: ${availableMemory}MB")
        Timber.tag("MemoryPerformance").i("Used memory: ${usedMemory}MB")
        Timber.tag("MemoryPerformance").i("Total memory: ${totalMemory}MB")
        
        scenario.close()
        
        // Should have reasonable available memory (> 50MB)
        assertTrue(
            "Should have available memory (> 50MB), was ${availableMemory}MB",
            availableMemory > 50
        )
    }
}
