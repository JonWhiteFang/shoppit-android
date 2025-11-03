package com.shoppit.app.presentation.ui.navigation.util

import android.os.Bundle
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import timber.log.Timber

/**
 * Utility object for handling navigation errors and providing fallback mechanisms.
 * Implements error recovery strategies for common navigation issues.
 * 
 * Requirements:
 * - 5.5: Prevent circular navigation loops
 * - 7.1: Safe navigation with valid routes
 * - 7.2: Fallback navigation on errors
 * - 7.3: Error logging for debugging
 * - 7.4: Argument validation
 * - 10.5: Reset to default starting screen if back stack becomes corrupted
 */
object NavigationErrorHandler {
    
    /**
     * Safely navigates to a route with error handling and fallback navigation.
     * 
     * Requirements:
     * - 7.1: Navigate to route when valid
     * - 7.2: Catch exceptions and navigate to fallback route
     * - 7.3: Log errors for debugging
     *
     * @param navController The NavHostController to use for navigation
     * @param route The route to navigate to
     * @param fallbackRoute Route to navigate to if this navigation fails
     * @param builder Optional navigation options builder
     */
    fun safeNavigate(
        navController: NavHostController,
        route: String,
        fallbackRoute: String,
        builder: (NavOptionsBuilder.() -> Unit)? = null
    ) {
        try {
            // Requirement 5.5: Check if navigation would create a circular loop (if validator available)
            try {
                if (BackStackValidator.wouldCreateLoop(navController, route)) {
                    Timber.e("Navigation failed for route: $route, using fallback: $fallbackRoute - Prevented circular navigation loop")
                    NavigationLogger.logNavigationError(
                        message = "Prevented circular navigation loop",
                        route = route
                    )
                    NavigationAnalytics.trackNavigationError(
                        route = route,
                        errorType = "CIRCULAR_LOOP_PREVENTED",
                        message = "Navigation would create circular loop"
                    )
                    return
                }
            } catch (e: NoClassDefFoundError) {
                // BackStackValidator not available, continue with navigation
            } catch (e: Exception) {
                // BackStackValidator check failed, continue with navigation
                Timber.d("BackStackValidator check skipped: ${e.message}")
            }
            
            // Start performance monitoring
            try {
                NavigationPerformanceAnalytics.startMonitoring(route)
            } catch (e: Exception) {
                // Performance monitoring not available, continue
            }
            
            // Requirement 7.1: Navigate to route when valid
            if (builder != null) {
                navController.navigate(route, builder)
            } else {
                navController.navigate(route)
            }
            
            try {
                NavigationLogger.logNavigationSuccess(route, null)
            } catch (e: Exception) {
                // Logging not available, continue
            }
        } catch (e: Exception) {
            // Requirement 7.3: Log error for debugging
            Timber.e(e, "Navigation failed for route: $route, using fallback: $fallbackRoute")
            
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to route",
                    route = route,
                    exception = e
                )
                NavigationAnalytics.trackNavigationError(
                    route = route,
                    errorType = "NAVIGATION_EXCEPTION",
                    message = e.message ?: "Failed to navigate to route",
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available, continue
            }
            
            // Requirement 7.2: Try fallback navigation
            try {
                try {
                    NavigationPerformanceAnalytics.startMonitoring(fallbackRoute)
                } catch (perfError: Exception) {
                    // Performance monitoring not available
                }
                navController.navigate(fallbackRoute)
            } catch (fallbackError: Exception) {
                Timber.e(fallbackError, "Fallback navigation also failed")
                try {
                    NavigationLogger.logNavigationError(
                        message = "Failed to navigate to fallback route",
                        route = fallbackRoute,
                        exception = fallbackError
                    )
                } catch (logError: Exception) {
                    // Logging not available
                }
                handleNavigationFailure(navController, fallbackError)
            }
        }
    }
    
    /**
     * Safely navigates to a route with error handling and fallback navigation.
     * Overload that accepts arguments parameter for backward compatibility.
     * 
     * @param navController The NavHostController to use for navigation
     * @param route The route to navigate to
     * @param arguments Optional arguments map (for logging purposes)
     * @param navOptions Optional navigation options builder
     * @param fallbackRoute Route to navigate to if this navigation fails
     */
    fun safeNavigate(
        navController: NavHostController,
        route: String,
        arguments: Map<String, Any?>? = null,
        navOptions: (NavOptionsBuilder.() -> Unit)? = null,
        fallbackRoute: String = "meal_list"
    ) {
        try {
            // Requirement 5.5: Check if navigation would create a circular loop (if validator available)
            try {
                if (BackStackValidator.wouldCreateLoop(navController, route)) {
                    Timber.e("Navigation failed for route: $route, using fallback: $fallbackRoute - Prevented circular navigation loop")
                    NavigationLogger.logNavigationError(
                        message = "Prevented circular navigation loop",
                        route = route,
                        arguments = arguments
                    )
                    NavigationAnalytics.trackNavigationError(
                        route = route,
                        errorType = "CIRCULAR_LOOP_PREVENTED",
                        message = "Navigation would create circular loop"
                    )
                    return
                }
            } catch (e: NoClassDefFoundError) {
                // BackStackValidator not available, continue with navigation
            } catch (e: Exception) {
                // BackStackValidator check failed, continue with navigation
            }
            
            // Start performance monitoring
            try {
                NavigationPerformanceAnalytics.startMonitoring(route)
            } catch (e: Exception) {
                // Performance monitoring not available, continue
            }
            
            if (navOptions != null) {
                navController.navigate(route, navOptions)
            } else {
                navController.navigate(route)
            }
            
            try {
                NavigationLogger.logNavigationSuccess(route, arguments)
            } catch (e: Exception) {
                // Logging not available, continue
            }
        } catch (e: Exception) {
            Timber.e(e, "Navigation failed for route: $route, using fallback: $fallbackRoute")
            
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to route",
                    route = route,
                    arguments = arguments,
                    exception = e
                )
                NavigationAnalytics.trackNavigationError(
                    route = route,
                    errorType = "NAVIGATION_EXCEPTION",
                    message = e.message ?: "Failed to navigate to route",
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available, continue
            }
            
            // Try fallback navigation
            try {
                try {
                    NavigationPerformanceAnalytics.startMonitoring(fallbackRoute)
                } catch (perfError: Exception) {
                    // Performance monitoring not available
                }
                navController.navigate(fallbackRoute)
            } catch (fallbackException: Exception) {
                Timber.e(fallbackException, "Fallback navigation also failed")
                try {
                    NavigationLogger.logNavigationError(
                        message = "Failed to navigate to fallback route",
                        route = fallbackRoute,
                        exception = fallbackException
                    )
                } catch (logError: Exception) {
                    // Logging not available
                }
                handleNavigationFailure(navController, fallbackException)
            }
        }
    }
    
    /**
     * Handles invalid navigation arguments by providing fallback navigation.
     * 
     * Requirement 7.3: Log errors for debugging
     *
     * @param navController The NavHostController to use for navigation
     * @param route The route that failed
     * @param arguments The arguments that were invalid
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred
     */
    fun handleInvalidArguments(
        navController: NavHostController,
        route: String,
        arguments: Map<String, Any?>,
        fallbackRoute: String,
        exception: Exception
    ) {
        // Requirement 7.3: Log error for debugging
        Timber.e(exception, "Invalid arguments for route $route: $arguments")
        
        try {
            NavigationLogger.logNavigationError(
                message = "Invalid navigation arguments detected",
                route = route,
                arguments = arguments,
                exception = exception
            )
            NavigationAnalytics.trackNavigationError(
                route = route,
                errorType = "INVALID_ARGUMENTS",
                message = "Invalid navigation arguments detected",
                exception = exception
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles invalid navigation arguments by providing fallback navigation.
     * Overload that accepts Throwable for backward compatibility.
     *
     * @param navController The NavHostController to use for navigation
     * @param route The route that failed
     * @param arguments The arguments that were invalid
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred (if available)
     */
    fun handleInvalidArguments(
        navController: NavHostController,
        route: String,
        arguments: Map<String, Any?>,
        fallbackRoute: String,
        exception: Throwable? = null
    ) {
        Timber.e(exception, "Invalid arguments for route $route: $arguments")
        
        try {
            NavigationLogger.logNavigationError(
                message = "Invalid navigation arguments detected",
                route = route,
                arguments = arguments,
                exception = exception
            )
            NavigationAnalytics.trackNavigationError(
                route = route,
                errorType = "INVALID_ARGUMENTS",
                message = "Invalid navigation arguments detected",
                exception = exception
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles missing required navigation arguments.
     * 
     * Requirement 7.3: Log errors for debugging
     *
     * @param navController The NavHostController to use for navigation
     * @param route The route that is missing arguments
     * @param requiredArgs The list of required argument names
     * @param fallbackRoute The route to navigate to as fallback
     */
    fun handleMissingArguments(
        navController: NavHostController,
        route: String,
        requiredArgs: List<String>,
        fallbackRoute: String
    ) {
        // Requirement 7.3: Log error for debugging
        Timber.e("Missing required arguments for route $route: $requiredArgs")
        
        try {
            NavigationLogger.logNavigationError(
                message = "Missing required navigation arguments: ${requiredArgs.joinToString(", ")}",
                route = route
            )
            NavigationAnalytics.trackNavigationError(
                route = route,
                errorType = "MISSING_ARGUMENTS",
                message = "Missing required arguments: ${requiredArgs.joinToString(", ")}"
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles navigation to invalid or non-existent routes.
     * 
     * Requirement 7.3: Log errors for debugging
     *
     * @param navController The NavHostController to use for navigation
     * @param invalidRoute The route that doesn't exist
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred
     */
    fun handleInvalidRoute(
        navController: NavHostController,
        invalidRoute: String,
        fallbackRoute: String,
        exception: Exception
    ) {
        // Requirement 7.3: Log error for debugging
        Timber.e(exception, "Invalid route: $invalidRoute")
        
        try {
            NavigationLogger.logNavigationError(
                message = "Attempted to navigate to invalid route",
                route = invalidRoute,
                exception = exception
            )
            NavigationAnalytics.trackNavigationError(
                route = invalidRoute,
                errorType = "INVALID_ROUTE",
                message = "Attempted to navigate to invalid route",
                exception = exception
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Handles navigation to invalid or non-existent routes.
     * Overload that accepts Throwable for backward compatibility.
     *
     * @param navController The NavHostController to use for navigation
     * @param invalidRoute The route that doesn't exist
     * @param fallbackRoute The route to navigate to as fallback
     * @param exception The exception that occurred (if available)
     */
    fun handleInvalidRoute(
        navController: NavHostController,
        invalidRoute: String,
        fallbackRoute: String,
        exception: Throwable? = null
    ) {
        Timber.e(exception, "Invalid route: $invalidRoute")
        
        try {
            NavigationLogger.logNavigationError(
                message = "Attempted to navigate to invalid route",
                route = invalidRoute,
                exception = exception
            )
            NavigationAnalytics.trackNavigationError(
                route = invalidRoute,
                errorType = "INVALID_ROUTE",
                message = "Attempted to navigate to invalid route",
                exception = exception
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        // Navigate to fallback route
        try {
            navController.navigate(fallbackRoute)
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate to fallback route",
                    route = fallbackRoute,
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
    
    /**
     * Validates navigation arguments and returns any validation errors.
     * 
     * Requirement 7.4: Argument validation
     *
     * @param bundle The arguments bundle to validate
     * @param requiredArgs The list of required argument names
     * @return List of validation errors, empty if all arguments are valid
     */
    fun validateArguments(
        bundle: Bundle,
        requiredArgs: List<String>
    ): List<String> {
        val errors = mutableListOf<String>()
        
        for (argName in requiredArgs) {
            if (!bundle.containsKey(argName)) {
                errors.add("Required argument '$argName' is missing")
            }
        }
        
        return errors
    }
    
    /**
     * Validates navigation arguments and returns any validation errors.
     * Overload that accepts nullable Bundle for backward compatibility.
     *
     * @param arguments The arguments to validate
     * @param requiredArgs The list of required argument names
     * @return List of validation errors, empty if all arguments are valid
     */
    @JvmName("validateArgumentsNullable")
    fun validateArguments(
        arguments: Bundle?,
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
     * Handles general navigation failures.
     *
     * @param navController The NavHostController to use for navigation
     * @param exception The exception that occurred
     */
    fun handleNavigationFailure(
        navController: NavHostController,
        exception: Throwable
    ) {
        try {
            NavigationLogger.logNavigationError(
                message = "Navigation failed",
                exception = exception
            )
            NavigationAnalytics.trackNavigationFailure(
                route = "unknown",
                failureType = "GENERAL_FAILURE",
                message = exception.message ?: "Navigation failed"
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
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
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to recover from navigation failure",
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            // Last resort: try to navigate to start destination
            try {
                val graph = navController.graph
                navController.navigate(graph.startDestinationId)
            } catch (finalException: Exception) {
                try {
                    NavigationLogger.logNavigationError(
                        message = "Complete navigation recovery failed - app may be in inconsistent state",
                        exception = finalException
                    )
                } catch (logError: Exception) {
                    // Logging not available
                }
                // At this point, we've exhausted all recovery options
                // The app might be in an inconsistent state
            }
        }
    }
    
    /**
     * Handles corrupted back stack by attempting to recover.
     * 
     * Requirements:
     * - 5.5: Validate back stack to prevent circular references
     * - 10.5: Reset to default starting screen if back stack becomes corrupted
     *
     * @param navController The NavHostController to use for navigation
     * @param exception The exception that indicated back stack corruption
     */
    fun handleCorruptedBackStack(
        navController: NavHostController,
        exception: Throwable
    ) {
        try {
            NavigationLogger.logBackStackRecovery(
                message = "Attempting to recover from corrupted back stack",
                exception = exception
            )
        } catch (e: Exception) {
            // Logging not available
        }
        
        try {
            // Requirement 5.5: Validate and fix back stack issues
            val validationResult = BackStackValidator.validateBackStack(navController)
            
            if (!validationResult.isValid) {
                try {
                    NavigationLogger.logNavigationError(
                        message = "Back stack validation failed",
                        arguments = mapOf("issues" to validationResult.issues.joinToString(", "))
                    )
                } catch (e: Exception) {
                    // Logging not available
                }
                
                // Try to fix the issues
                val fixed = BackStackValidator.fixBackStackIssues(navController)
                
                if (fixed) {
                    try {
                        NavigationLogger.logBackStackRecovery(
                            message = "Successfully fixed back stack issues"
                        )
                    } catch (e: Exception) {
                        // Logging not available
                    }
                    return
                }
            }
            
            // Requirement 10.5: Reset to default starting screen if back stack is corrupted
            val graph = navController.graph
            navController.navigate(graph.startDestinationId)
            
            try {
                NavigationLogger.logBackStackRecovery(
                    message = "Successfully recovered from corrupted back stack by resetting to start"
                )
            } catch (e: Exception) {
                // Logging not available
            }
        } catch (e: Exception) {
            try {
                NavigationLogger.logNavigationError(
                    message = "Failed to recover from corrupted back stack",
                    exception = e
                )
            } catch (logError: Exception) {
                // Logging not available
            }
            handleNavigationFailure(navController, e)
        }
    }
}
