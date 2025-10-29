package com.shoppit.app.presentation.ui.navigation.util

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.shoppit.app.domain.error.AppError
import timber.log.Timber

/**
 * Utility object for handling navigation errors and providing fallback mechanisms.
 * Implements error recovery strategies for common navigation issues.
 */
object NavigationErrorHandler {
    
    /**
     * Handles invalid navigation arguments by providing fallback navigation.
     *
     * @param navController The NavController to use for navigation
     * @param route The route that failed
     * @param arguments The arguments that were invalid
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred (if available)
     */
    fun handleInvalidArguments(
        navController: NavController,
        route: String,
        arguments: Map<String, Any?>,
        fallbackRoute: String,
        exception: Throwable? = null
    ) {
        NavigationLogger.logNavigationError(
            message = "Invalid navigation arguments detected",
            route = route,
            arguments = arguments,
            exception = exception
        )
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to fallback route",
                route = fallbackRoute,
                exception = e
            )
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles missing required navigation arguments.
     *
     * @param navController The NavController to use for navigation
     * @param route The route that is missing arguments
     * @param requiredArgs The list of required argument names
     * @param fallbackRoute The route to navigate to as fallback
     */
    fun handleMissingArguments(
        navController: NavController,
        route: String,
        requiredArgs: List<String>,
        fallbackRoute: String
    ) {
        NavigationLogger.logNavigationError(
            message = "Missing required navigation arguments: ${requiredArgs.joinToString(", ")}",
            route = route
        )
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to fallback route",
                route = fallbackRoute,
                exception = e
            )
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles navigation to invalid or non-existent routes.
     *
     * @param navController The NavController to use for navigation
     * @param invalidRoute The route that doesn't exist
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred (if available)
     */
    fun handleInvalidRoute(
        navController: NavController,
        invalidRoute: String,
        fallbackRoute: String,
        exception: Throwable? = null
    ) {
        NavigationLogger.logNavigationError(
            message = "Attempted to navigate to invalid route",
            route = invalidRoute,
            exception = exception
        )
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to fallback route",
                route = fallbackRoute,
                exception = e
            )
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles general navigation failures.
     *
     * @param navController The NavController to use for navigation
     * @param exception The exception that occurred
     */
    fun handleNavigationFailure(
        navController: NavController,
        exception: Throwable
    ) {
        NavigationLogger.logNavigationError(
            message = "Navigation failed",
            exception = exception
        )
        
        // Try to navigate to a safe fallback route
        try {
            // Check if we can pop back stack
            if (navController.currentBackStackEntry != null && navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                // If we can't pop, try to go to the start destination
                val graph = navController.graph
                navController.navigate(graph.startDestinationId)
            }
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to recover from navigation failure",
                exception = e
            )
            // Last resort: try to navigate to start destination
            try {
                val graph = navController.graph
                navController.navigate(graph.startDestinationId)
            } catch (finalException: Exception) {
                NavigationLogger.logNavigationError(
                    message = "Complete navigation recovery failed - app may be in inconsistent state",
                    exception = finalException
                )
                // At this point, we've exhausted all recovery options
                // The app might be in an inconsistent state
            }
        }
    }
    
    /**
     * Handles corrupted back stack by attempting to recover.
     *
     * @param navController The NavController to use for navigation
     * @param exception The exception that indicated back stack corruption
     */
    fun handleCorruptedBackStack(
        navController: NavController,
        exception: Throwable
    ) {
        NavigationLogger.logBackStackRecovery(
            message = "Attempting to recover from corrupted back stack",
            exception = exception
        )
        
        try {
            // Try to reset to a known good state
            val graph = navController.graph
            navController.navigate(graph.startDestinationId)
            
            NavigationLogger.logBackStackRecovery(
                message = "Successfully recovered from corrupted back stack"
            )
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to recover from corrupted back stack",
                exception = e
            )
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Validates navigation arguments and returns any validation errors.
     *
     * @param arguments The arguments to validate
     * @param requiredArgs The list of required argument names
     * @return List of validation errors, empty if all arguments are valid
     */
    fun validateArguments(
        arguments: android.os.Bundle?,
        requiredArgs: List<String>
    ): List<String> {
        val errors = mutableListOf<String>()
        
        if (arguments == null) {
            if (requiredArgs.isNotEmpty()) {
                errors.add("Arguments bundle is null but required arguments exist")
            }
            return errors
        }
        
        for (argName in requiredArgs) {
            if (!arguments.containsKey(argName)) {
                errors.add("Required argument '$argName' is missing")
            } else {
                val value = arguments.get(argName)
                if (value == null) {
                    errors.add("Required argument '$argName' is null")
                }
            }
        }
        
        return errors
    }
    
    /**
     * Safely navigates to a route with error handling and performance monitoring.
     *
     * @param navController The NavController to use for navigation
     * @param route The route to navigate to
     * @param arguments The arguments to pass (if any)
     * @param navOptions Configuration for the navigation
     * @param fallbackRoute Route to navigate to if this navigation fails
     */
    fun safeNavigate(
        navController: NavController,
        route: String,
        arguments: Map<String, Any?>? = null,
        navOptions: (NavOptionsBuilder.() -> Unit)? = null,
        fallbackRoute: String = "meal_list" // Default fallback to meal list
    ) {
        try {
            // Start comprehensive performance monitoring
            NavigationPerformanceAnalytics.startMonitoring(route)
            
            if (navOptions != null) {
                navController.navigate(route, navOptions)
            } else {
                navController.navigate(route)
            }
            
            NavigationLogger.logNavigationSuccess(route, arguments)
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to route",
                route = route,
                arguments = arguments,
                exception = e
            )
            
            // Try fallback navigation
            try {
                NavigationPerformanceAnalytics.startMonitoring(fallbackRoute)
                navController.navigate(fallbackRoute)
            } catch (fallbackException: Exception) {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = fallbackException
                )
                handleNavigationFailure(navController, fallbackException)
            }
        }
    }
}