package com.shoppit.app.macrobenchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Startup benchmark for Shoppit app.
 * 
 * Measures cold, warm, and hot start times to validate performance targets:
 * - Cold start: < 2000ms
 * - Warm start: < 1000ms
 * - Hot start: < 500ms
 * 
 * Requirements: 1.1, 1.2, 1.3
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startupColdNoCompilation() = startup(
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun startupColdPartialCompilation() = startup(
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3
        )
    )
    
    @Test
    fun startupWarmNoCompilation() = startup(
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun startupWarmPartialCompilation() = startup(
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3
        )
    )
    
    @Test
    fun startupHotNoCompilation() = startup(
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.None()
    )
    
    private fun startup(
        startupMode: StartupMode,
        compilationMode: CompilationMode
    ) = benchmarkRule.measureRepeated(
        packageName = "com.shoppit.app",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = startupMode,
        iterations = 5,
        setupBlock = {
            pressHome()
        }
    ) {
        startActivityAndWait()
    }
}
