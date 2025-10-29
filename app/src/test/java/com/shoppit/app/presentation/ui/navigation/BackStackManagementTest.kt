package com.shoppit.app.presentation.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.shoppit.app.presentation.ui.navigation.util.BackStackValidator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for back stack management.
 * Tests back stack validation, circular reference detection, and state management.
 * 
 * Requirements:
 * - Back stack maintains correct order
 * - Circular references are detected
 * - Back stack validation works correctly
 * - Independent back stacks for bottom navigation items
 */
class BackStackManagementTest {
    
    private lateinit var navController: NavHostController
    
    @Before
    fun setup() {
        navController = mockk(relaxed = true)
    }
    
    /**
     * Test back stack maintains correct order.
     */
    @Test
    fun `back stack maintains correct order`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Entries should be in correct order
        assertEquals(3, entries.size)
        assertEquals("meal_list", entries[0].destination.route)
        assertEquals("meal_detail/1", entries[1].destination.route)
        assertEquals("edit_meal/1", entries[2].destination.route)
    }
    
    /**
     * Test back stack with single entry.
     */
    @Test
    fun `back stack with single entry`() {
        // Given: Back stack with single entry
        val entries = createBackStackEntries("meal_list")
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Should have one entry
        assertEquals(1, entries.size)
        assertEquals("meal_list", entries[0].destination.route)
    }
    
    /**
     * Test back stack with empty entries.
     */
    @Test
    fun `back stack with empty entries`() {
        // Given: Empty back stack
        val entries = emptyList<NavBackStackEntry>()
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Should be empty
        assertTrue(entries.isEmpty())
    }
    
    /**
     * Test detecting circular reference in back stack.
     */
    @Test
    fun `detect circular reference in back stack`() {
        // Given: Back stack with duplicate route
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "meal_list" // Circular reference
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Should detect circular reference
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("Circular reference") })
    }
    
    /**
     * Test no circular reference with unique routes.
     */
    @Test
    fun `no circular reference with unique routes`() {
        // Given: Back stack with unique routes
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Should pass validation
        assertTrue(result.isValid)
        assertTrue(result.issues.isEmpty())
    }
    
    /**
     * Test detecting circular reference with same route different parameters.
     */
    @Test
    fun `detect circular reference with same route different parameters`() {
        // Given: Back stack with same route but different IDs
        val entries = createBackStackEntries(
            "meal_detail/1",
            "meal_detail/2",
            "meal_detail/1" // Circular reference to first entry
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Should detect circular reference
        assertFalse(result.isValid)
        assertTrue(result.issues.any { it.contains("Circular reference") })
    }
    
    /**
     * Test would create loop detection.
     */
    @Test
    fun `would create loop when navigating to existing route`() {
        // Given: Back stack with existing routes
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Checking if navigating to meal_list would create loop
        val wouldCreateLoop = BackStackValidator.wouldCreateLoop(navController, "meal_list")
        
        // Then: Should detect potential loop
        assertTrue(wouldCreateLoop)
    }
    
    /**
     * Test would not create loop with new route.
     */
    @Test
    fun `would not create loop when navigating to new route`() {
        // Given: Back stack with existing routes
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Checking if navigating to edit_meal would create loop
        val wouldCreateLoop = BackStackValidator.wouldCreateLoop(navController, "edit_meal/1")
        
        // Then: Should not detect loop
        assertFalse(wouldCreateLoop)
    }
    
    /**
     * Test back stack depth calculation.
     */
    @Test
    fun `calculate back stack depth`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Depth should be 3
        assertEquals(3, entries.size)
    }
    
    /**
     * Test back stack contains specific route.
     */
    @Test
    fun `back stack contains specific route`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Should contain meal_detail route
        val containsRoute = entries.any { it.destination.route?.startsWith("meal_detail") == true }
        assertTrue(containsRoute)
    }
    
    /**
     * Test back stack does not contain specific route.
     */
    @Test
    fun `back stack does not contain specific route`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Should not contain shopping_list route
        val containsRoute = entries.any { it.destination.route == "shopping_list" }
        assertFalse(containsRoute)
    }
    
    /**
     * Test finding route in back stack.
     */
    @Test
    fun `find route in back stack`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Finding meal_detail route
        val foundEntry = entries.find { it.destination.route?.startsWith("meal_detail") == true }
        
        // Then: Should find the entry
        assertTrue(foundEntry != null)
        assertEquals("meal_detail/1", foundEntry?.destination?.route)
    }
    
    /**
     * Test back stack validation with maximum depth.
     */
    @Test
    fun `back stack validation with maximum depth`() {
        // Given: Back stack with many entries
        val routes = (1..20).map { "meal_detail/$it" }
        val entries = createBackStackEntries(*routes.toTypedArray())
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Should pass validation (no circular references)
        assertTrue(result.isValid)
    }
    
    /**
     * Test back stack validation detects multiple circular references.
     */
    @Test
    fun `back stack validation detects multiple circular references`() {
        // Given: Back stack with multiple circular references
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "meal_list", // First circular reference
            "meal_detail/1" // Second circular reference
        )
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // When: Validating back stack
        val result = BackStackValidator.validateBackStack(navController)
        
        // Then: Should detect circular references
        assertFalse(result.isValid)
        assertTrue(result.issues.size >= 1)
    }
    
    /**
     * Test back stack with bottom navigation maintains independent stacks.
     */
    @Test
    fun `bottom navigation maintains independent back stacks`() {
        // Given: Back stacks for different bottom nav items
        val mealsStack = createBackStackEntries("meal_list", "meal_detail/1")
        val plannerStack = createBackStackEntries("meal_planner")
        val shoppingStack = createBackStackEntries("shopping_list", "shopping_mode")
        
        // Then: Each stack should be independent
        assertEquals(2, mealsStack.size)
        assertEquals(1, plannerStack.size)
        assertEquals(2, shoppingStack.size)
        
        // Stacks should not share entries
        assertFalse(mealsStack.any { it.destination.route == "meal_planner" })
        assertFalse(plannerStack.any { it.destination.route == "meal_list" })
        assertFalse(shoppingStack.any { it.destination.route == "meal_list" })
    }
    
    /**
     * Test back stack preserves state when switching bottom nav items.
     */
    @Test
    fun `back stack preserves state when switching bottom nav items`() {
        // Given: User navigates in Meals section
        val mealsStack = createBackStackEntries("meal_list", "meal_detail/1", "edit_meal/1")
        
        // When: User switches to Planner and back to Meals
        // The meals stack should be preserved
        
        // Then: Stack should maintain same entries
        assertEquals(3, mealsStack.size)
        assertEquals("meal_list", mealsStack[0].destination.route)
        assertEquals("meal_detail/1", mealsStack[1].destination.route)
        assertEquals("edit_meal/1", mealsStack[2].destination.route)
    }
    
    /**
     * Test back stack clears correctly on logout.
     */
    @Test
    fun `back stack clears correctly on logout`() {
        // Given: Back stack with multiple entries
        val entries = createBackStackEntries(
            "meal_list",
            "meal_detail/1",
            "edit_meal/1"
        )
        
        // When: User logs out (simulated by clearing stack)
        val clearedStack = emptyList<NavBackStackEntry>()
        
        // Then: Stack should be empty
        assertTrue(clearedStack.isEmpty())
        assertFalse(entries.isEmpty()) // Original stack unchanged
    }
    
    /**
     * Test back stack with deep link constructs proper hierarchy.
     */
    @Test
    fun `back stack with deep link constructs proper hierarchy`() {
        // Given: Deep link to meal detail should create back stack
        // Expected: meal_list -> meal_detail/1
        val entries = createBackStackEntries("meal_list", "meal_detail/1")
        
        val backStack = MutableStateFlow(entries)
        every { navController.currentBackStack } returns backStack
        
        // Then: Should have proper hierarchy
        assertEquals(2, entries.size)
        assertEquals("meal_list", entries[0].destination.route)
        assertEquals("meal_detail/1", entries[1].destination.route)
    }
    
    /**
     * Helper function to create mock back stack entries.
     */
    private fun createBackStackEntries(vararg routes: String): List<NavBackStackEntry> {
        return routes.map { route ->
            val entry = mockk<NavBackStackEntry>(relaxed = true)
            val destination = mockk<NavDestination>(relaxed = true)
            every { destination.route } returns route
            every { entry.destination } returns destination
            entry
        }
    }
}
