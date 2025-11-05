package com.shoppit.app.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Scroll performance benchmark for Shoppit app.
 * 
 * Measures frame timing during list scrolling to validate:
 * - 60 FPS (16.67ms per frame) target
 * - Smooth scrolling in meal lists
 * - Efficient LazyColumn rendering
 * 
 * Requirements: 2.1, 2.2, 5.1, 5.2, 9.1
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun scrollMealListNoCompilation() = scrollMealList(
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun scrollMealListPartialCompilation() = scrollMealList(
        compilationMode = CompilationMode.Partial()
    )
    
    @Test
    fun scrollShoppingListNoCompilation() = scrollShoppingList(
        compilationMode = CompilationMode.None()
    )
    
    @Test
    fun scrollShoppingListPartialCompilation() = scrollShoppingList(
        compilationMode = CompilationMode.Partial()
    )
    
    private fun scrollMealList(compilationMode: CompilationMode) = 
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
            }
        ) {
            // Find the meal list
            val mealList = device.findObject(By.scrollable(true))
            
            // Scroll down and up multiple times
            repeat(3) {
                mealList?.setGestureMargin(device.displayWidth / 5)
                mealList?.fling(Direction.DOWN)
                device.waitForIdle()
                
                mealList?.fling(Direction.UP)
                device.waitForIdle()
            }
        }
    
    private fun scrollShoppingList(compilationMode: CompilationMode) = 
        benchmarkRule.measureRepeated(
            packageName = "com.shoppit.app",
            metrics = listOf(FrameTimingMetric()),
            compilationMode = compilationMode,
            startupMode = StartupMode.WARM,
            iterations = 5,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                
                // Navigate to shopping list
                device.wait(Until.hasObject(By.desc("Shopping tab")), 5000)
                val shoppingTab = device.findObject(By.desc("Shopping tab"))
                shoppingTab?.click()
                device.waitForIdle()
            }
        ) {
            // Find the shopping list
            val shoppingList = device.findObject(By.scrollable(true))
            
            // Scroll down and up multiple times
            repeat(3) {
                shoppingList?.setGestureMargin(device.displayWidth / 5)
                shoppingList?.fling(Direction.DOWN)
                device.waitForIdle()
                
                shoppingList?.fling(Direction.UP)
                device.waitForIdle()
            }
        }
}
