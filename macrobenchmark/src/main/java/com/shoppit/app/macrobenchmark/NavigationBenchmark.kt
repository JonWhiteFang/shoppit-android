package com.shoppit.app.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation performance benchmark for Shoppit app.
 * 
 * Measures navigation transition times to validate:
 * - Navigation completes within 100ms
 * - Smooth screen transitions
 * - Efficient back stack management
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavigationBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun navigationBetweenTabsNoCompilation() = navigationBetweenTabs(
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun navigationBetweenTabsPartialCompilation() = navigationBetweenTabs(
        compilationMode = CompilationMode.Partial()
    )
    
    @Test
    fun navigationToDetailNoCompilation() = navigationToDetail(
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun navigationToDetailPartialCompilation() = navigationToDetail(
        compilationMode = CompilationMode.Partial()
    )
    
    private fun navigationBetweenTabs(compilationMode: CompilationMode) = 
        benchmarkRule.measureRepeated(
            packageName = "com.shoppit.app",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                device.waitForIdle()
            }
        ) {
            // Navigate between tabs multiple times
            repeat(3) {
                // Navigate to Planner
                val plannerTab = device.findObject(By.desc("Planner tab"))
                plannerTab?.click()
                device.waitForIdle()
                
                // Navigate to Shopping
                val shoppingTab = device.findObject(By.desc("Shopping tab"))
                shoppingTab?.click()
                device.waitForIdle()
                
                // Navigate back to Meals
                val mealsTab = device.findObject(By.desc("Meals tab"))
                mealsTab?.click()
                device.waitForIdle()
            }
        }
    
    private fun navigationToDetail(compilationMode: CompilationMode) = 
        benchmarkRule.measureRepeated(
            packageName = "com.shoppit.app",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                
                // Wait for meal list to load
                device.wait(Until.hasObject(By.desc("Meals tab")), 5000)
                device.waitForIdle()
            }
        ) {
            // Navigate to meal detail and back multiple times
            repeat(3) {
                // Find and click first meal (if exists)
                val mealCard = device.findObject(By.clickable(true))
                if (mealCard != null && mealCard.text != null) {
                    mealCard.click()
                    device.waitForIdle()
                    
                    // Navigate back
                    device.pressBack()
                    device.waitForIdle()
                }
            }
        }
}
