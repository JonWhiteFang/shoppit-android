package com.shoppit.app.presentation.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.shoppit.app.presentation.ui.navigation.util.DeepLinkHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DeepLinkHandler.
 * Tests deep link validation and navigation handling.
 */
class DeepLinkHandlerTest {
    
    private lateinit var navController: NavController
    
    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        every { navController.graph } returns mockk(relaxed = true)
        every { navController.graph.startDestinationId } returns 1
    }
    
    @Test
    fun `isValidDeepLink returns false for null URI`() {
        val result = DeepLinkHandler.isValidDeepLink(null)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for invalid scheme`() {
        val uri = Uri.parse("https://example.com/meal/123")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for unknown host`() {
        val uri = Uri.parse("shoppit://unknown/123")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns true for valid meal deep link`() {
        val uri = Uri.parse("shoppit://meal/123")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertTrue(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for meal deep link with invalid ID`() {
        val uri = Uri.parse("shoppit://meal/invalid")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for meal deep link with negative ID`() {
        val uri = Uri.parse("shoppit://meal/-1")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns true for planner deep link without date`() {
        val uri = Uri.parse("shoppit://planner")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertTrue(result)
    }
    
    @Test
    fun `isValidDeepLink returns true for planner deep link with valid date`() {
        val uri = Uri.parse("shoppit://planner?date=1234567890")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertTrue(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for planner deep link with invalid date`() {
        val uri = Uri.parse("shoppit://planner?date=invalid")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for planner deep link with negative date`() {
        val uri = Uri.parse("shoppit://planner?date=-1")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `isValidDeepLink returns true for shopping deep link`() {
        val uri = Uri.parse("shoppit://shopping")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertTrue(result)
    }
    
    @Test
    fun `isValidDeepLink returns true for shopping mode deep link`() {
        val uri = Uri.parse("shoppit://shopping/mode")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertTrue(result)
    }
    
    @Test
    fun `isValidDeepLink returns false for shopping deep link with invalid path`() {
        val uri = Uri.parse("shoppit://shopping/invalid")
        val result = DeepLinkHandler.isValidDeepLink(uri)
        assertFalse(result)
    }
    
    @Test
    fun `handleDeepLink returns false for null intent`() {
        val result = DeepLinkHandler.handleDeepLink(null, navController)
        assertFalse(result)
    }
    
    @Test
    fun `handleDeepLink returns false for intent without data`() {
        val intent = Intent()
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        assertFalse(result)
    }
    
    @Test
    fun `handleDeepLink navigates to meal detail for valid meal deep link`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://meal/123")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertTrue(result)
        verify { navController.navigate(Screen.MealList.route, any<NavOptions>()) }
        verify { navController.navigate(Screen.MealDetail.createRoute(123)) }
    }
    
    @Test
    fun `handleDeepLink navigates to planner for valid planner deep link`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://planner")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertTrue(result)
        verify { navController.navigate(Screen.MealPlanner.route, any<NavOptions>()) }
    }
    
    @Test
    fun `handleDeepLink navigates to shopping list for valid shopping deep link`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://shopping")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertTrue(result)
        verify { navController.navigate(Screen.ShoppingList.route, any<NavOptions>()) }
    }
    
    @Test
    fun `handleDeepLink navigates to shopping mode for valid shopping mode deep link`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://shopping/mode")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertTrue(result)
        verify { navController.navigate(Screen.ShoppingList.route, any<NavOptions>()) }
        verify { navController.navigate(Screen.ShoppingMode.route) }
    }
    
    @Test
    fun `handleDeepLink navigates to fallback for invalid meal ID`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://meal/invalid")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertFalse(result)
        verify { navController.navigate(Screen.MealList.route, any<NavOptions>()) }
    }
    
    @Test
    fun `handleDeepLink navigates to fallback for unknown host`() {
        val intent = Intent().apply {
            data = Uri.parse("shoppit://unknown/123")
        }
        
        val result = DeepLinkHandler.handleDeepLink(intent, navController)
        
        assertFalse(result)
        verify { navController.navigate(Screen.MealList.route, any<NavOptions>()) }
    }
}
