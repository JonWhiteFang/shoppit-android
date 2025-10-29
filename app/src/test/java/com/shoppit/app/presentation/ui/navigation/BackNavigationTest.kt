package com.shoppit.app.presentation.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.shoppit.app.presentation.ui.navigation.util.BackPressHandler
import com.shoppit.app.presentation.ui.navigation.util.BackStackValidator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for back navigation behavior.
 * 
 * Requirements:
 * - 5.1: Back button pops navigation stack correctly on detail screens
 * - 5.2: Exit app when back is pressed on main screens
 * - 5.3: Handle back navigation from forms with unsaved data
 * - 5.4: Back navigation after saving changes reflects updated data
 * - 5.5: Prevent circular navigation loops
 */
class BackNavigationTest {
    
    private lateinit var navController: NavHostController
    private lateinit var currentBackStackEntry: NavBackStackEntry
    private lateinit var previousBackStackEntry: NavBackStackEntry
    private lateinit var currentDestination: NavDestination
    private lateinit var previousDestination: NavDestination
    
    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        currentBackStackEntry = mockk(relaxed = true)
        previousBackStackEntry = mockk(relaxed = true)
        currentDestination = mockk(relaxed = true)
        previousDestination = mockk(relaxed = true)
        
        every { currentBackStackEntry.destination } returns currentDestination
        every { previousBackStackEntry.destination } returns previousDestination
        every { navController.currentBackStackEntry } returns currentBackStackEntry
        every { navController.previousBackStackEntry } returns previousBackStackEntry
    }
    
    /**
     * Test: Requirement 5.1 - Back button pops navigation stack on detail screens
     */
    @Test
    fun `back press on detail screen pops navigation stack`() {
        // Given: User is on meal detail screen
        every { currentDestination.route } returns "meal_detail/1"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "meal_detail/1",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: Navigation stack is popped
        assertTrue(handled)
        verify { navController.popBackStack() }
        assertFalse(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.2 - Exit app when back is pressed on main screens
     */
    @Test
    fun `back press on main screen exits app`() {
        // Given: User is on meal list screen (main screen)
        every { currentDestination.route } returns "meal_list"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "meal_list",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: App exits
        assertTrue(handled)
        assertTrue(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.2 - Exit app when back is pressed on planner screen
     */
    @Test
    fun `back press on planner screen exits app`() {
        // Given: User is on meal planner screen (main screen)
        every { currentDestination.route } returns "meal_planner"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "meal_planner",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: App exits
        assertTrue(handled)
        assertTrue(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.2 - Exit app when back is pressed on shopping list screen
     */
    @Test
    fun `back press on shopping list screen exits app`() {
        // Given: User is on shopping list screen (main screen)
        every { currentDestination.route } returns "shopping_list"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "shopping_list",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: App exits
        assertTrue(handled)
        assertTrue(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.1 - Back button pops navigation stack on edit screens
     */
    @Test
    fun `back press on edit meal screen pops navigation stack`() {
        // Given: User is on edit meal screen
        every { currentDestination.route } returns "edit_meal/1"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "edit_meal/1",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: Navigation stack is popped
        assertTrue(handled)
        verify { navController.popBackStack() }
        assertFalse(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.1 - Back button pops navigation stack on add screens
     */
    @Test
    fun `back press on add meal screen pops navigation stack`() {
        // Given: User is on add meal screen
        every { currentDestination.route } returns "add_meal"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "add_meal",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: Navigation stack is popped
        assertTrue(handled)
        verify { navController.popBackStack() }
        assertFalse(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.1 - Back button pops navigation stack on shopping mode
     */
    @Test
    fun `back press on shopping mode screen pops navigation stack`() {
        // Given: User is on shopping mode screen
        every { currentDestination.route } returns "shopping_mode"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "shopping_mode",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: Navigation stack is popped
        assertTrue(handled)
        verify { navController.popBackStack() }
        assertFalse(exitAppCalled)
    }
    
    /**
     * Test: Requirement 5.5 - Detect circular navigation loops
     */
    @Test
    fun `detects circular navigation loop when same route appears multiple times`() {
        // Given: Back stack with duplicate route
        val entry1 = mockk<NavBackStackEntry>(relaxed = true)
        val entry2 = mockk<NavBackStackEntry>(relaxed = true)
        val entry3 = mockk<NavBackStackEntry>(relaxed = true)
        
        val dest1 = mockk<NavDestination>(relaxed = true)
        val dest2 = mockk<NavDestination>(relaxed = true)
        val dest3 = mockk<NavDestination>(relaxed = true)
        
        every { entry1.destination } returns dest1
        every { entry2.destination } returns dest2
        every { entry3.destination } returns dest3
        
        every { dest1.route } returns "meal_list"
        every { dest2.route } returns "meal_detail/1"
        every { dest3.route } returns "meal_list" // Duplicate!
        
        val backStack = MutableStateFlow(listOf(entry1, entry2, entry3))
        every { navController.currentBackStack } returns backStack
        
        // When: Checking if navigating to meal_list would create a loop
        val wouldCreateLoop = BackStackValidator.wouldCreateLoop(navController, "meal_list")
        
        // Then: Loop is detected
        assertTrue(wouldCreateLoop)
    }
    
    /**
     * Test: Requirement 5.5 - No loop detected for unique routes
     */
    @Test
    fun `does not detect loop when routes are unique`() {
        // Given: Back stack with unique routes
        val entry1 = mockk<NavBackStackEntry>(relaxed = true)
        val entry2 = mockk<NavBackStackEntry>(relaxed = true)
        
        val dest1 = mockk<NavDestination>(relaxed = true)
        val dest2 = mockk<NavDestination>(relaxed = true)
        
        every { entry1.destination } returns dest1
        every { entry2.destination } returns dest2
        
        every { dest1.route } returns "meal_list"
        every { dest2.route } returns "meal_detail/1"
        
        val backStack = MutableStateFlow(listOf(entry1, entry2))
        every { navController.currentBackStack } returns backStack
        
        // When: Checking if navigating to meal_planner would create a loop
        val wouldCreateLoop = BackStackValidator.wouldCreateLoop(navController, "meal_planner")
        
        // Then: No loop is detected
        assertFalse(wouldCreateLoop)
    }
    
    /**
     * Test: Requirement 5.5 - Validate back stack detects circular references
     */
    @Test
    fun `validates back stack and detects circular references`() {
        // Given: Back stack with circular reference
        val entry1 = mockk<NavBackStackEntry>(relaxed = true)
        val entry2 = mockk<NavBackStackEntry>(relaxed = true)
        val entry3 = mockk<NavBackStackEntry>(relaxed = true)
        
        val dest1 = mockk<NavDestination>(relaxed = true)
        val dest2 = mockk<NavDestination>(relaxed = true)
        val dest3 = mockk<NavDestination>(relaxed = true)
        
        every { entry1.destination } returns dest1
        every { entry2.destination } returns dest2
        every { entry3.destination } returns dest3
        
        every { dest1.route } returns "meal_list"
        every { dest2.route } returns "meal_detail/1"
        every { dest3.route } returns "meal_list" // Circular reference
        
        val backStack = MutableStateFlow(listOf(entry1, entry2, entry3))
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Validation fails with circular reference issue
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("Circular reference") })
    }
    
    /**
     * Test: Requirement 5.5 - Validate back stack passes for valid stack
     */
    @Test
    fun `validates back stack and passes for valid stack`() {
        // Given: Valid back stack with unique routes
        val entry1 = mockk<NavBackStackEntry>(relaxed = true)
        val entry2 = mockk<NavBackStackEntry>(relaxed = true)
        
        val dest1 = mockk<NavDestination>(relaxed = true)
        val dest2 = mockk<NavDestination>(relaxed = true)
        
        every { entry1.destination } returns dest1
        every { entry2.destination } returns dest2
        
        every { dest1.route } returns "meal_list"
        every { dest2.route } returns "meal_detail/1"
        
        val backStack = MutableStateFlow(listOf(entry1, entry2))
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Validation passes
        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }
    
    /**
     * Test: Helper method to identify main screens
     */
    @Test
    fun `identifies main screens correctly`() {
        assertTrue(BackPressHandler.isMainScreen("meal_list"))
        assertTrue(BackPressHandler.isMainScreen("meal_planner"))
        assertTrue(BackPressHandler.isMainScreen("shopping_list"))
        
        assertFalse(BackPressHandler.isMainScreen("meal_detail/1"))
        assertFalse(BackPressHandler.isMainScreen("add_meal"))
        assertFalse(BackPressHandler.isMainScreen("edit_meal/1"))
        assertFalse(BackPressHandler.isMainScreen("shopping_mode"))
    }
    
    /**
     * Test: Helper method to identify detail/edit screens
     */
    @Test
    fun `identifies detail and edit screens correctly`() {
        assertTrue(BackPressHandler.isDetailOrEditScreen("meal_detail/1"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("edit_meal/1"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("add_meal"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("shopping_mode"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("item_history"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("template_manager"))
        assertTrue(BackPressHandler.isDetailOrEditScreen("store_section_editor"))
        
        assertFalse(BackPressHandler.isDetailOrEditScreen("meal_list"))
        assertFalse(BackPressHandler.isDetailOrEditScreen("meal_planner"))
        assertFalse(BackPressHandler.isDetailOrEditScreen("shopping_list"))
    }
    
    /**
     * Test: Back press with no previous entry exits app
     */
    @Test
    fun `back press with no previous entry exits app`() {
        // Given: No previous back stack entry
        every { navController.previousBackStackEntry } returns null
        every { currentDestination.route } returns "some_screen"
        
        var exitAppCalled = false
        
        // When: User presses back button
        val handled = BackPressHandler.handleBackPress(
            navController = navController,
            currentRoute = "some_screen",
            onExitApp = { exitAppCalled = true }
        )
        
        // Then: App exits
        assertTrue(handled)
        assertTrue(exitAppCalled)
    }
}
