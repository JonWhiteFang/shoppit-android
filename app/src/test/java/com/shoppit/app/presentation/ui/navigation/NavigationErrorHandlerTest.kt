package com.shoppit.app.presentation.ui.navigation.util

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptionsBuilder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class NavigationErrorHandlerTest {
    
    private lateinit var navController: NavController
    private lateinit var navGraph: NavGraph
    
    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        navGraph = mockk(relaxed = true)
        
        every { navController.graph } returns navGraph
        every { navGraph.startDestinationId } returns 1
    }
    
    @Test
    fun `handleInvalidArguments navigates to fallback route`() {
        // Given
        val route = "meal_detail/{mealId}"
        val arguments = mapOf("mealId" to -1L)
        val fallbackRoute = "meal_list"
        val exception = IllegalArgumentException("Invalid meal ID")
        
        every { navController.navigate(fallbackRoute) } just runs
        
        // When
        NavigationErrorHandler.handleInvalidArguments(
            navController = navController,
            route = route,
            arguments = arguments,
            fallbackRoute = fallbackRoute,
            exception = exception
        )
        
        // Then
        verify(exactly = 1) { navController.navigate(fallbackRoute) }
    }
    
    @Test
    fun `handleMissingArguments navigates to fallback route`() {
        // Given
        val route = "meal_detail/{mealId}"
        val requiredArgs = listOf("mealId")
        val fallbackRoute = "meal_list"
        
        every { navController.navigate(fallbackRoute) } just runs
        
        // When
        NavigationErrorHandler.handleMissingArguments(
            navController = navController,
            route = route,
            requiredArgs = requiredArgs,
            fallbackRoute = fallbackRoute
        )
        
        // Then
        verify(exactly = 1) { navController.navigate(fallbackRoute) }
    }
    
    @Test
    fun `handleInvalidRoute navigates to fallback route`() {
        // Given
        val invalidRoute = "invalid_route"
        val fallbackRoute = "meal_list"
        val exception = IllegalArgumentException("Route not found")
        
        every { navController.navigate(fallbackRoute) } just runs
        
        // When
        NavigationErrorHandler.handleInvalidRoute(
            navController = navController,
            invalidRoute = invalidRoute,
            fallbackRoute = fallbackRoute,
            exception = exception
        )
        
        // Then
        verify(exactly = 1) { navController.navigate(fallbackRoute) }
    }
    
    @Test
    fun `validateArguments returns error for missing required argument`() {
        // Given
        val bundle = Bundle().apply {
            putString("name", "Pasta")
        }
        val requiredArgs = listOf("mealId", "name")
        
        // When
        val errors = NavigationErrorHandler.validateArguments(bundle, requiredArgs)
        
        // Then
        assert(errors.isNotEmpty())
        assert(errors.contains("Required argument 'mealId' is missing"))
    }
    
    @Test
    fun `validateArguments returns no errors for valid arguments`() {
        // Given
        val bundle = Bundle().apply {
            putLong("mealId", 1L)
            putString("name", "Pasta")
        }
        val requiredArgs = listOf("mealId", "name")
        
        // When
        val errors = NavigationErrorHandler.validateArguments(bundle, requiredArgs)
        
        // Then
        assert(errors.isEmpty())
    }
    
    @Test
    fun `safeNavigate handles navigation success`() {
        // Given
        val route = "meal_detail/1"
        val fallbackRoute = "meal_list"
        
        every { navController.navigate(route) } just runs
        
        // When
        NavigationErrorHandler.safeNavigate(
            navController = navController,
            route = route,
            fallbackRoute = fallbackRoute
        )
        
        // Then
        verify(exactly = 1) { navController.navigate(route) }
    }
    
    @Test
    fun `safeNavigate handles navigation failure and uses fallback`() {
        // Given
        val route = "invalid_route"
        val fallbackRoute = "meal_list"
        val exception = RuntimeException("Navigation failed")
        
        every { navController.navigate(route) } throws exception
        every { navController.navigate(fallbackRoute) } just runs
        
        // When
        NavigationErrorHandler.safeNavigate(
            navController = navController,
            route = route,
            fallbackRoute = fallbackRoute
        )
        
        // Then
        verify(exactly = 1) { navController.navigate(route) }
        verify(exactly = 1) { navController.navigate(fallbackRoute) }
    }
}