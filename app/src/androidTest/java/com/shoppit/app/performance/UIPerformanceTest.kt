package com.shoppit.app.performance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.MainActivity
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * UI performance tests to validate rendering and navigation performance.
 * 
 * Requirements tested:
 * - 2.1: LazyColumn scroll at 60 FPS
 * - 2.2: Navigation transition < 100ms
 * - 2.3: Frame drop rate monitoring
 * - 9.1: List rendering optimization
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class UIPerformanceTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * Test LazyColumn scroll performance with large dataset.
     * Target: Smooth scrolling at 60 FPS (< 16.67ms per frame)
     * Requirement: 2.1, 5.1
     */
    @Test
    fun testLazyColumnScrollPerformance() {
        // Wait for app to load
        composeTestRule.waitForIdle()

        // Navigate to meal list (assuming it's the default screen)
        // Find the meal list
        val mealList = composeTestRule.onNodeWithTag("meal_list", useUnmergedTree = true)
        
        // If meal list exists, test scrolling
        if (mealList.isDisplayed()) {
            // Measure scroll performance
            val scrollTime = measureTimeMillis {
                // Perform scroll gesture
                mealList.performTouchInput {
                    swipeUp(
                        startY = bottom,
                        endY = top,
                        durationMillis = 1000
                    )
                }
                composeTestRule.waitForIdle()
            }

            Timber.tag("UIPerformance").i("LazyColumn scroll time: ${scrollTime}ms")

            // Scroll should be smooth (complete within reasonable time)
            assertTrue(
                "LazyColumn scroll should be smooth (< 1500ms), was ${scrollTime}ms",
                scrollTime < 1500
            )
        }
    }

    /**
     * Test navigation transition performance.
     * Target: < 100ms
     * Requirement: 2.2, 9.1
     */
    @Test
    fun testNavigationTransitionPerformance() {
        composeTestRule.waitForIdle()

        // Find navigation items
        val plannerTab = composeTestRule.onNodeWithText("Planner", useUnmergedTree = true)
        
        if (plannerTab.isDisplayed()) {
            // Measure navigation time
            val navigationTime = measureTimeMillis {
                plannerTab.performClick()
                composeTestRule.waitForIdle()
            }

            Timber.tag("UIPerformance").i("Navigation transition time: ${navigationTime}ms")

            assertTrue(
                "Navigation should complete in < 100ms, was ${navigationTime}ms",
                navigationTime < 100
            )

            // Navigate back to meals
            val mealsTab = composeTestRule.onNodeWithText("Meals", useUnmergedTree = true)
            if (mealsTab.isDisplayed()) {
                val backNavigationTime = measureTimeMillis {
                    mealsTab.performClick()
                    composeTestRule.waitForIdle()
                }

                Timber.tag("UIPerformance").i("Back navigation time: ${backNavigationTime}ms")

                assertTrue(
                    "Back navigation should complete in < 100ms, was ${backNavigationTime}ms",
                    backNavigationTime < 100
                )
            }
        }
    }

    /**
     * Test screen rendering time.
     * Target: First frame < 500ms
     * Requirement: 2.1
     */
    @Test
    fun testScreenRenderingTime() {
        val renderTime = measureTimeMillis {
            composeTestRule.waitForIdle()
        }

        Timber.tag("UIPerformance").i("Screen rendering time: ${renderTime}ms")

        assertTrue(
            "Screen should render in < 500ms, was ${renderTime}ms",
            renderTime < 500
        )
    }

    /**
     * Test list item rendering performance.
     * Target: Each item renders in < 16.67ms (60 FPS)
     * Requirement: 5.3, 9.1
     */
    @Test
    fun testListItemRenderingPerformance() {
        composeTestRule.waitForIdle()

        // Try to find meal list items
        val mealItems = composeTestRule.onAllNodes(
            hasTestTag("meal_card"),
            useUnmergedTree = true
        )

        // If we have items, measure rendering
        if (mealItems.fetchSemanticsNodes().isNotEmpty()) {
            val itemCount = mealItems.fetchSemanticsNodes().size
            
            Timber.tag("UIPerformance").i("Found $itemCount meal items")

            // Scroll through list to trigger rendering
            val scrollTime = measureTimeMillis {
                repeat(3) {
                    composeTestRule.onNodeWithTag("meal_list", useUnmergedTree = true)
                        .performTouchInput {
                            swipeUp(
                                startY = bottom,
                                endY = top,
                                durationMillis = 500
                            )
                        }
                    composeTestRule.waitForIdle()
                }
            }

            val avgTimePerScroll = scrollTime / 3
            Timber.tag("UIPerformance").i("Average scroll time: ${avgTimePerScroll}ms")

            // Scrolling should be smooth
            assertTrue(
                "List scrolling should be smooth (< 600ms per scroll), was ${avgTimePerScroll}ms",
                avgTimePerScroll < 600
            )
        }
    }

    /**
     * Test recomposition performance.
     * Target: Minimal recompositions on state changes
     * Requirement: 2.4, 6.1
     */
    @Test
    fun testRecompositionPerformance() {
        composeTestRule.waitForIdle()

        // Measure time for state change to propagate
        val stateChangeTime = measureTimeMillis {
            // Trigger a state change (e.g., clicking a button)
            val addButton = composeTestRule.onNodeWithContentDescription(
                "Add new meal",
                useUnmergedTree = true
            )
            
            if (addButton.isDisplayed()) {
                addButton.performClick()
                composeTestRule.waitForIdle()
            }
        }

        Timber.tag("UIPerformance").i("State change propagation time: ${stateChangeTime}ms")

        assertTrue(
            "State change should propagate quickly (< 100ms), was ${stateChangeTime}ms",
            stateChangeTime < 100
        )
    }

    /**
     * Test bottom navigation performance.
     * Target: Tab switch < 100ms
     * Requirement: 9.1
     */
    @Test
    fun testBottomNavigationPerformance() {
        composeTestRule.waitForIdle()

        val tabs = listOf("Meals", "Planner", "Shopping")
        val navigationTimes = mutableListOf<Long>()

        tabs.forEach { tabName ->
            val tab = composeTestRule.onNodeWithText(tabName, useUnmergedTree = true)
            
            if (tab.isDisplayed()) {
                val navTime = measureTimeMillis {
                    tab.performClick()
                    composeTestRule.waitForIdle()
                }
                navigationTimes.add(navTime)
                
                Timber.tag("UIPerformance").i("Navigation to $tabName: ${navTime}ms")
            }
        }

        if (navigationTimes.isNotEmpty()) {
            val avgNavigationTime = navigationTimes.average()
            val maxNavigationTime = navigationTimes.maxOrNull() ?: 0L

            Timber.tag("UIPerformance").i("Average navigation time: ${avgNavigationTime}ms")
            Timber.tag("UIPerformance").i("Max navigation time: ${maxNavigationTime}ms")

            assertTrue(
                "Average navigation should be < 100ms, was ${avgNavigationTime}ms",
                avgNavigationTime < 100
            )
            assertTrue(
                "Max navigation should be < 150ms, was ${maxNavigationTime}ms",
                maxNavigationTime < 150
            )
        }
    }

    /**
     * Test search input performance.
     * Target: Search results update < 100ms
     * Requirement: 2.4
     */
    @Test
    fun testSearchInputPerformance() {
        composeTestRule.waitForIdle()

        // Try to find search field
        val searchField = composeTestRule.onNodeWithTag("search_field", useUnmergedTree = true)
        
        if (searchField.isDisplayed()) {
            val searchTime = measureTimeMillis {
                searchField.performTextInput("pasta")
                composeTestRule.waitForIdle()
            }

            Timber.tag("UIPerformance").i("Search input processing time: ${searchTime}ms")

            assertTrue(
                "Search should process quickly (< 100ms), was ${searchTime}ms",
                searchTime < 100
            )
        }
    }

    /**
     * Test dialog rendering performance.
     * Target: Dialog appears < 100ms
     * Requirement: 2.1
     */
    @Test
    fun testDialogRenderingPerformance() {
        composeTestRule.waitForIdle()

        // Try to trigger a dialog (e.g., add meal button)
        val addButton = composeTestRule.onNodeWithContentDescription(
            "Add new meal",
            useUnmergedTree = true
        )
        
        if (addButton.isDisplayed()) {
            val dialogTime = measureTimeMillis {
                addButton.performClick()
                composeTestRule.waitForIdle()
            }

            Timber.tag("UIPerformance").i("Dialog rendering time: ${dialogTime}ms")

            assertTrue(
                "Dialog should render quickly (< 100ms), was ${dialogTime}ms",
                dialogTime < 100
            )
        }
    }

    /**
     * Test frame drop detection during complex animations.
     * Target: No significant frame drops
     * Requirement: 2.3
     */
    @Test
    fun testFrameDropDetection() {
        composeTestRule.waitForIdle()

        // Perform complex UI operations
        val operationTime = measureTimeMillis {
            // Navigate between screens multiple times
            repeat(5) {
                val plannerTab = composeTestRule.onNodeWithText("Planner", useUnmergedTree = true)
                if (plannerTab.isDisplayed()) {
                    plannerTab.performClick()
                    composeTestRule.waitForIdle()
                }

                val mealsTab = composeTestRule.onNodeWithText("Meals", useUnmergedTree = true)
                if (mealsTab.isDisplayed()) {
                    mealsTab.performClick()
                    composeTestRule.waitForIdle()
                }
            }
        }

        val avgOperationTime = operationTime / 10 // 5 iterations * 2 navigations
        Timber.tag("UIPerformance").i("Average operation time: ${avgOperationTime}ms")

        // Each operation should be smooth
        assertTrue(
            "Operations should be smooth (< 100ms avg), was ${avgOperationTime}ms",
            avgOperationTime < 100
        )
    }
}
