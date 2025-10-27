package com.shoppit.app.util

import androidx.compose.ui.test.junit4.createComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule

/**
 * Base class for Compose UI instrumented tests.
 * 
 * This class provides:
 * - Hilt dependency injection setup
 * - Compose test rule for UI testing
 * - Consistent test setup for all Compose UI tests
 * 
 * Usage:
 * ```
 * @HiltAndroidTest
 * class MealListScreenTest : ComposeTest() {
 *     
 *     @Test
 *     fun testMealListDisplayed() {
 *         composeTestRule.setContent {
 *             MealListScreen()
 *         }
 *         
 *         composeTestRule.onNodeWithText("Meals").assertIsDisplayed()
 *     }
 * }
 * ```
 */
@HiltAndroidTest
abstract class ComposeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    open fun setUp() {
        hiltRule.inject()
    }
}
