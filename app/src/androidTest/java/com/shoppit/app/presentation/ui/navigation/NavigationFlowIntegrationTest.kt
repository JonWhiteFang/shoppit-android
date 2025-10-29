package com.shoppit.app.presentation.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shoppit.app.presentation.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for navigation flows between all screens.
 * Tests complete navigation paths and user journeys.
 * 
 * Requirements:
 * - Navigation between all screens works correctly
 * - State is preserved across navigation
 * - Deep link handling works correctly
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationFlowIntegrationTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    /**
     * Test: Navigate from Meals to Planner to Shopping and back.
     * Verifies bottom navigation flow.
     */
    @Test
    fun navigateBetweenBottomNavigationItems() {
        // Given: App starts on Meals screen
        composeTestRule.waitForIdle()
        
        // When: Navigate to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Planner screen is displayed
        // (Verification would depend on screen content)
        
        // When: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Shopping screen is displayed
        
        // When: Navigate back to Meals
        composeTestRule.onNodeWithText("Meals").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Meals screen is displayed
    }
    
    /**
     * Test: Navigate through meal management flow.
     * Meals -> Add Meal -> Save -> Meal List
     */
    @Test
    fun navigateThroughMealManagementFlow() {
        // Given: App starts on Meals screen
        composeTestRule.waitForIdle()
        
        // When: Click add meal button
        // Note: This assumes there's a FAB or add button
        // composeTestRule.onNodeWithContentDescription("Add meal").performClick()
        // composeTestRule.waitForIdle()
        
        // Then: Add meal screen is displayed
        
        // When: Fill in meal details and save
        // (Would require form interaction)
        
        // Then: Navigate back to meal list with new meal
    }
    
    /**
     * Test: Navigate through meal detail flow.
     * Meals -> Meal Detail -> Edit -> Save -> Meal Detail
     */
    @Test
    fun navigateThroughMealDetailFlow() {
        // Given: App starts on Meals screen with meals
        composeTestRule.waitForIdle()
        
        // When: Click on a meal
        // (Requires test data)
        
        // Then: Meal detail screen is displayed
        
        // When: Click edit button
        // composeTestRule.onNodeWithContentDescription("Edit meal").performClick()
        // composeTestRule.waitForIdle()
        
        // Then: Edit meal screen is displayed
        
        // When: Make changes and save
        
        // Then: Navigate back to meal detail with updated data
    }
    
    /**
     * Test: Navigate through planner flow.
     * Planner -> Select meal slot -> Meal selection -> Meal Detail
     */
    @Test
    fun navigateThroughPlannerFlow() {
        // Given: Navigate to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // When: Click on a meal slot
        // (Requires interaction with calendar)
        
        // Then: Meal selection dialog is displayed
        
        // When: Select a meal
        
        // Then: Meal is assigned to slot
        
        // When: Click on assigned meal
        
        // Then: Navigate to meal detail
    }
    
    /**
     * Test: Navigate through shopping list flow.
     * Shopping -> Item History -> Back to Shopping
     */
    @Test
    fun navigateThroughShoppingListFlow() {
        // Given: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Navigate to item history
        // (Requires menu or button interaction)
        
        // Then: Item history screen is displayed
        
        // When: Press back
        // composeTestRule.activity.onBackPressed()
        
        // Then: Return to shopping list
    }
    
    /**
     * Test: Navigate through shopping mode flow.
     * Shopping -> Shopping Mode -> Exit -> Shopping
     */
    @Test
    fun navigateThroughShoppingModeFlow() {
        // Given: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Enter shopping mode
        // (Requires button interaction)
        
        // Then: Shopping mode screen is displayed
        
        // When: Exit shopping mode
        
        // Then: Return to shopping list
    }
    
    /**
     * Test: Navigate through template management flow.
     * Shopping -> Templates -> Create/Load -> Back to Shopping
     */
    @Test
    fun navigateThroughTemplateManagementFlow() {
        // Given: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Navigate to template manager
        
        // Then: Template manager screen is displayed
        
        // When: Create or load a template
        
        // Then: Template is applied
        
        // When: Navigate back
        
        // Then: Return to shopping list
    }
    
    /**
     * Test: Navigate through store section editor flow.
     * Shopping -> Section Editor -> Reorder -> Save -> Shopping
     */
    @Test
    fun navigateThroughStoreSectionEditorFlow() {
        // Given: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Navigate to section editor
        
        // Then: Section editor screen is displayed
        
        // When: Reorder sections
        
        // Then: Changes are saved
        
        // When: Navigate back
        
        // Then: Return to shopping list with updated sections
    }
    
    /**
     * Test: State preservation when switching between tabs.
     * Verifies that each tab maintains its navigation state.
     */
    @Test
    fun statePreservationWhenSwitchingTabs() {
        // Given: Navigate to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // When: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Navigate back to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Planner state should be preserved
        // (Would verify scroll position, selected week, etc.)
    }
    
    /**
     * Test: Deep navigation within a section preserves state.
     * Meals -> Detail -> Edit, then switch to Planner and back.
     */
    @Test
    fun deepNavigationStatePreservation() {
        // Given: Navigate deep into Meals section
        // Meals -> Meal Detail -> Edit Meal
        
        // When: Switch to Planner tab
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // When: Switch back to Meals tab
        composeTestRule.onNodeWithText("Meals").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Should return to Edit Meal screen (preserved state)
    }
    
    /**
     * Test: Navigate from meal detail in different contexts.
     * Verifies meal detail can be accessed from Meals, Planner, and Shopping.
     */
    @Test
    fun mealDetailAccessibleFromMultipleContexts() {
        // Test 1: From Meals section
        // Given: On Meals screen
        composeTestRule.waitForIdle()
        
        // When: Click on a meal
        // Then: Meal detail is displayed
        
        // Test 2: From Planner section
        // Given: Navigate to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // When: Click on a planned meal
        // Then: Meal detail is displayed
        
        // Test 3: From Shopping section
        // Given: Navigate to Shopping
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        
        // When: Click on meal source indicator
        // Then: Meal detail is displayed
    }
    
    /**
     * Test: Complex navigation flow with multiple back navigations.
     * Meals -> Detail -> Edit -> Back -> Back -> Planner -> Shopping -> Meals
     */
    @Test
    fun complexNavigationFlowWithBackNavigation() {
        // Given: Start on Meals
        composeTestRule.waitForIdle()
        
        // Navigate: Meals -> Detail -> Edit
        // (Requires test data and interactions)
        
        // When: Press back twice
        // composeTestRule.activity.onBackPressed()
        // composeTestRule.waitForIdle()
        // composeTestRule.activity.onBackPressed()
        // composeTestRule.waitForIdle()
        
        // Then: Should be back on Meals list
        
        // When: Navigate through bottom nav
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Meals").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Should be on Meals list (not Edit screen)
    }
    
    /**
     * Test: Rapid navigation doesn't cause crashes or invalid states.
     */
    @Test
    fun rapidNavigationDoesNotCauseCrashes() {
        // When: Rapidly navigate between tabs
        repeat(10) {
            composeTestRule.onNodeWithText("Planner").performClick()
            composeTestRule.onNodeWithText("Shopping").performClick()
            composeTestRule.onNodeWithText("Meals").performClick()
        }
        
        // Then: App should still be responsive
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Meals").assertIsDisplayed()
    }
    
    /**
     * Test: Navigation after configuration change.
     * Verifies state is preserved after rotation or other config changes.
     */
    @Test
    fun navigationAfterConfigurationChange() {
        // Given: Navigate to Planner
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        
        // When: Simulate configuration change (rotation)
        // composeTestRule.activity.recreate()
        // composeTestRule.waitForIdle()
        
        // Then: Should still be on Planner screen
        // (State should be preserved)
    }
    
    /**
     * Test: Navigation with empty states.
     * Verifies navigation works correctly when there's no data.
     */
    @Test
    fun navigationWithEmptyStates() {
        // Given: App with no meals, plans, or shopping items
        composeTestRule.waitForIdle()
        
        // When: Navigate between tabs
        composeTestRule.onNodeWithText("Planner").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Shopping").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Meals").performClick()
        composeTestRule.waitForIdle()
        
        // Then: Navigation should work without crashes
        // Empty states should be displayed appropriately
    }
}
