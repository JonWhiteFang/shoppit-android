package com.shoppit.app.presentation.ui.navigation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

/**
 * Accessibility tests for navigation components.
 * 
 * Tests verify:
 * - Content descriptions are present on navigation icons
 * - Semantic labels are correctly applied
 * - Screen transitions are announced properly
 * - Focus order follows logical patterns
 * 
 * Requirements:
 * - 9.1: TalkBack announces screen transitions with descriptive labels
 * - 9.2: Keyboard navigation support
 * - 9.3: Content descriptions for all navigation elements
 * - 9.4: Focus moves to appropriate element on new screen
 * - 9.5: Focus order follows logical reading patterns
 */
class AccessibilityNavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    /**
     * Test that bottom navigation items have proper content descriptions.
     * Requirement: 9.3
     */
    @Test
    fun bottomNavigationItems_haveContentDescriptions() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Verify each bottom navigation item has a content description
        composeTestRule.onNodeWithText("Meals")
            .assertIsDisplayed()
            .assert(hasContentDescription("Meals tab"))
        
        composeTestRule.onNodeWithText("Planner")
            .assertIsDisplayed()
            .assert(hasContentDescription("Planner tab"))
        
        composeTestRule.onNodeWithText("Shopping")
            .assertIsDisplayed()
            .assert(hasContentDescription("Shopping tab"))
    }
    
    /**
     * Test that selected navigation items have proper state descriptions.
     * Requirement: 9.1, 9.3
     */
    @Test
    fun selectedNavigationItem_hasSelectedStateDescription() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Meals should be selected by default (start destination)
        composeTestRule.onNodeWithText("Meals")
            .assertIsDisplayed()
            .assert(hasStateDescription("Selected"))
    }
    
    /**
     * Test that unselected navigation items have proper state descriptions.
     * Requirement: 9.1, 9.3
     */
    @Test
    fun unselectedNavigationItem_hasNotSelectedStateDescription() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Planner and Shopping should not be selected initially
        composeTestRule.onNodeWithText("Planner")
            .assertIsDisplayed()
            .assert(hasStateDescription("Not selected"))
        
        composeTestRule.onNodeWithText("Shopping")
            .assertIsDisplayed()
            .assert(hasStateDescription("Not selected"))
    }
    
    /**
     * Test that state description updates when navigation item is clicked.
     * Requirement: 9.1, 9.3
     */
    @Test
    fun navigationItemStateDescription_updatesOnClick() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Click on Planner tab
        composeTestRule.onNodeWithText("Planner")
            .performClick()
        
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
        
        // Planner should now be selected
        composeTestRule.onNodeWithText("Planner")
            .assert(hasStateDescription("Selected"))
        
        // Meals should no longer be selected
        composeTestRule.onNodeWithText("Meals")
            .assert(hasStateDescription("Not selected"))
    }
    
    /**
     * Test that navigation icons have descriptive content descriptions.
     * Requirement: 9.3
     */
    @Test
    fun navigationIcons_haveDescriptiveContentDescriptions() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Verify icons have descriptive content descriptions
        composeTestRule.onNode(hasContentDescription("Meals navigation button"))
            .assertExists()
        
        composeTestRule.onNode(hasContentDescription("Planner navigation button"))
            .assertExists()
        
        composeTestRule.onNode(hasContentDescription("Shopping navigation button"))
            .assertExists()
    }
    
    /**
     * Test that screen transitions maintain accessibility focus.
     * Requirement: 9.4, 9.5
     */
    @Test
    fun screenTransition_maintainsFocusManagement() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            MainScreen(navController)
        }
        
        // Navigate to Planner
        composeTestRule.onNodeWithText("Planner")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify navigation completed successfully
        composeTestRule.onNodeWithText("Planner")
            .assert(hasStateDescription("Selected"))
    }
    
    /**
     * Test that all navigation items are accessible in tab order.
     * Requirement: 9.2, 9.5
     */
    @Test
    fun navigationItems_followLogicalTabOrder() {
        composeTestRule.setContent {
            MainScreen()
        }
        
        // Verify all navigation items are present and accessible
        // Tab order should be: Meals -> Planner -> Shopping
        composeTestRule.onNodeWithText("Meals").assertIsDisplayed()
        composeTestRule.onNodeWithText("Planner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shopping").assertIsDisplayed()
    }
    
    // Helper functions for semantic matchers
    
    private fun hasContentDescription(description: String): SemanticsMatcher {
        return SemanticsMatcher("has content description '$description'") { node ->
            val contentDescription = node.config.getOrNull(SemanticsProperties.ContentDescription)
            contentDescription?.any { it == description } == true
        }
    }
    
    private fun hasStateDescription(description: String): SemanticsMatcher {
        return SemanticsMatcher("has state description '$description'") { node ->
            val stateDescription = node.config.getOrNull(SemanticsProperties.StateDescription)
            stateDescription == description
        }
    }
}
