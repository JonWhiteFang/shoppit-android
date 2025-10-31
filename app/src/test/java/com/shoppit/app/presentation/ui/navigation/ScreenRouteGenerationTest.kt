package com.shoppit.app.presentation.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Screen route generation.
 * Tests that all screen routes are correctly defined and parameterized routes
 * generate valid navigation paths.
 * 
 * Requirements:
 * - All screens have unique, non-empty routes
 * - Parameterized routes generate correct paths with arguments
 * - Route patterns match expected format
 */
class ScreenRouteGenerationTest {
    
    /**
     * Test that all screen routes are non-empty strings.
     */
    @Test
    fun `all screen routes are non-empty`() {
        assertTrue(Screen.MealList.route.isNotEmpty())
        assertTrue(Screen.MealDetail.route.isNotEmpty())
        assertTrue(Screen.AddMeal.route.isNotEmpty())
        assertTrue(Screen.EditMeal.route.isNotEmpty())
        assertTrue(Screen.MealPlanner.route.isNotEmpty())
        assertTrue(Screen.ShoppingList.route.isNotEmpty())
        assertTrue(Screen.ItemHistory.route.isNotEmpty())
        assertTrue(Screen.TemplateManager.route.isNotEmpty())
        assertTrue(Screen.StoreSectionEditor.route.isNotEmpty())
        assertTrue(Screen.ShoppingMode.route.isNotEmpty())
        assertTrue(Screen.AnalyticsDashboard.route.isNotEmpty())
    }
    
    /**
     * Test that all screen routes are unique.
     */
    @Test
    fun `all screen routes are unique`() {
        val routes = listOf(
            Screen.MealList.route,
            Screen.MealDetail.route,
            Screen.AddMeal.route,
            Screen.EditMeal.route,
            Screen.MealPlanner.route,
            Screen.ShoppingList.route,
            Screen.ItemHistory.route,
            Screen.TemplateManager.route,
            Screen.StoreSectionEditor.route,
            Screen.ShoppingMode.route,
            Screen.AnalyticsDashboard.route
        )
        
        // All routes should be unique
        assertEquals(routes.size, routes.distinct().size)
    }
    
    /**
     * Test that simple screen routes follow naming convention.
     */
    @Test
    fun `simple screen routes follow snake_case convention`() {
        val simpleRoutes = listOf(
            Screen.MealList.route,
            Screen.AddMeal.route,
            Screen.MealPlanner.route,
            Screen.ShoppingList.route,
            Screen.ItemHistory.route,
            Screen.TemplateManager.route,
            Screen.StoreSectionEditor.route,
            Screen.ShoppingMode.route,
            Screen.AnalyticsDashboard.route
        )
        
        simpleRoutes.forEach { route ->
            // Should be lowercase with underscores
            assertTrue("Route $route should be lowercase", route == route.lowercase())
            assertTrue("Route $route should not contain spaces", !route.contains(" "))
        }
    }
    
    /**
     * Test that parameterized routes contain parameter placeholders.
     */
    @Test
    fun `parameterized routes contain parameter placeholders`() {
        // MealDetail route should contain {mealId} placeholder
        assertTrue(Screen.MealDetail.route.contains("{mealId}"))
        assertEquals("meal_detail/{mealId}", Screen.MealDetail.route)
        
        // EditMeal route should contain {mealId} placeholder
        assertTrue(Screen.EditMeal.route.contains("{mealId}"))
        assertEquals("edit_meal/{mealId}", Screen.EditMeal.route)
    }
    
    /**
     * Test MealDetail.createRoute generates correct path with valid ID.
     */
    @Test
    fun `MealDetail createRoute generates correct path with valid ID`() {
        val mealId = 123L
        val route = Screen.MealDetail.createRoute(mealId)
        
        assertEquals("meal_detail/123", route)
        assertTrue(route.startsWith("meal_detail/"))
        assertTrue(route.contains("123"))
    }
    
    /**
     * Test MealDetail.createRoute with different IDs.
     */
    @Test
    fun `MealDetail createRoute works with different IDs`() {
        assertEquals("meal_detail/1", Screen.MealDetail.createRoute(1))
        assertEquals("meal_detail/999", Screen.MealDetail.createRoute(999))
        assertEquals("meal_detail/1234567890", Screen.MealDetail.createRoute(1234567890))
    }
    
    /**
     * Test MealDetail.createRoute with zero ID.
     */
    @Test
    fun `MealDetail createRoute handles zero ID`() {
        val route = Screen.MealDetail.createRoute(0)
        assertEquals("meal_detail/0", route)
    }
    
    /**
     * Test MealDetail.createRoute with negative ID.
     * Note: Validation should happen at ViewModel/UseCase level, not route generation.
     */
    @Test
    fun `MealDetail createRoute handles negative ID`() {
        val route = Screen.MealDetail.createRoute(-1)
        assertEquals("meal_detail/-1", route)
    }
    
    /**
     * Test EditMeal.createRoute generates correct path with valid ID.
     */
    @Test
    fun `EditMeal createRoute generates correct path with valid ID`() {
        val mealId = 456L
        val route = Screen.EditMeal.createRoute(mealId)
        
        assertEquals("edit_meal/456", route)
        assertTrue(route.startsWith("edit_meal/"))
        assertTrue(route.contains("456"))
    }
    
    /**
     * Test EditMeal.createRoute with different IDs.
     */
    @Test
    fun `EditMeal createRoute works with different IDs`() {
        assertEquals("edit_meal/1", Screen.EditMeal.createRoute(1))
        assertEquals("edit_meal/999", Screen.EditMeal.createRoute(999))
        assertEquals("edit_meal/1234567890", Screen.EditMeal.createRoute(1234567890))
    }
    
    /**
     * Test that createRoute methods generate different routes for different IDs.
     */
    @Test
    fun `createRoute methods generate different routes for different IDs`() {
        val route1 = Screen.MealDetail.createRoute(1)
        val route2 = Screen.MealDetail.createRoute(2)
        
        assertNotEquals(route1, route2)
    }
    
    /**
     * Test that createRoute methods generate same route for same ID.
     */
    @Test
    fun `createRoute methods are deterministic`() {
        val mealId = 123L
        val route1 = Screen.MealDetail.createRoute(mealId)
        val route2 = Screen.MealDetail.createRoute(mealId)
        
        assertEquals(route1, route2)
    }
    
    /**
     * Test that MealDetail and EditMeal routes are different for same ID.
     */
    @Test
    fun `MealDetail and EditMeal routes are different for same ID`() {
        val mealId = 123L
        val detailRoute = Screen.MealDetail.createRoute(mealId)
        val editRoute = Screen.EditMeal.createRoute(mealId)
        
        assertNotEquals(detailRoute, editRoute)
        assertTrue(detailRoute.startsWith("meal_detail/"))
        assertTrue(editRoute.startsWith("edit_meal/"))
    }
    
    /**
     * Test that route patterns can be used for navigation argument extraction.
     */
    @Test
    fun `route patterns support argument extraction`() {
        // The route pattern should allow extracting the mealId
        val pattern = Screen.MealDetail.route // "meal_detail/{mealId}"
        val actualRoute = Screen.MealDetail.createRoute(123) // "meal_detail/123"
        
        // Pattern should match the structure
        assertTrue(pattern.contains("{mealId}"))
        assertTrue(actualRoute.matches(Regex("meal_detail/\\d+")))
    }
    
    /**
     * Test that all bottom navigation routes map to valid screens.
     */
    @Test
    fun `bottom navigation routes map to valid screens`() {
        assertEquals(Screen.MealList.route, BottomNavigationItem.Meals.route)
        assertEquals(Screen.MealPlanner.route, BottomNavigationItem.Planner.route)
        assertEquals(Screen.ShoppingList.route, BottomNavigationItem.Shopping.route)
    }
    
    /**
     * Test that bottom navigation items have unique routes.
     */
    @Test
    fun `bottom navigation items have unique routes`() {
        val items = BottomNavigationItem.items
        val routes = items.map { it.route }
        
        assertEquals(routes.size, routes.distinct().size)
    }
    
    /**
     * Test route string format consistency.
     */
    @Test
    fun `routes use consistent format`() {
        // All routes should use snake_case, not camelCase or kebab-case
        val routes = listOf(
            Screen.MealList.route,
            Screen.AddMeal.route,
            Screen.MealPlanner.route,
            Screen.ShoppingList.route,
            Screen.ItemHistory.route,
            Screen.TemplateManager.route,
            Screen.StoreSectionEditor.route,
            Screen.ShoppingMode.route
        )
        
        routes.forEach { route ->
            // Should not contain hyphens (kebab-case)
            assertTrue("Route $route should not use kebab-case", !route.contains("-"))
            // Should use underscores for multi-word routes
            if (route.split("_").size > 1) {
                assertTrue("Route $route should use underscores", route.contains("_"))
            }
        }
    }
}
