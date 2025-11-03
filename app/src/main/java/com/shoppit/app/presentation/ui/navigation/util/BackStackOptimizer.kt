package com.shoppit.app.presentation.ui.navigation.util

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import timber.log.Timber

/**
 * Utility for optimizing back stack management in navigation.
 * Ensures efficient back stack handling and state preservation.
 * 
 * Requirements:
 * - 9.3: Clear unnecessary back stack entries on main screen navigation
 * - 9.3: Use saveState and restoreState for bottom navigation
 * - 9.3: Implement proper launchSingleTop for main destinations
 */
object BackStackOptimizer {
    
    /**
     * Navigates to a main destination with optimized back stack management.
     * Clears unnecessary entries and preserves state.
     * 
     * @param navController The navigation controller
     * @param route The destination route
     * 
     * Requirements: 9.3
     */
    fun navigateToMainDestination(navController: NavController, route: String) {
        try {
            navController.navigate(route) {
                // Pop up to the start destination to avoid building up a large stack
                popUpTo(navController.graph.findStartDestination().id) {
                    // Save state when navigating away from a section (Requirement 9.3)
                    // This preserves the back stack and scroll positions
                    saveState = true
                }
                // Avoid multiple copies of the same destination (Requirement 9.3)
                launchSingleTop = true
                // Restore state when reselecting a previously selected item (Requirement 9.3)
                // This brings back the entire back stack and UI state
                restoreState = true
            }
            
            Timber.d("Navigated to main destination: $route with optimized back stack")
        } catch (e: Exception) {
            Timber.e(e, "Failed to navigate to main destination: $route")
            throw e
        }
    }
    
    /**
     * Clears the back stack for a fresh start.
     * Useful when navigating to a completely new flow.
     * 
     * @param navController The navigation controller
     * @param route The destination route
     */
    fun navigateWithClearedBackStack(navController: NavController, route: String) {
        try {
            navController.navigate(route) {
                // Clear entire back stack
                popUpTo(0) {
                    inclusive = true
                }
                launchSingleTop = true
            }
            
            Timber.d("Navigated to $route with cleared back stack")
        } catch (e: Exception) {
            Timber.e(e, "Failed to navigate with cleared back stack: $route")
            throw e
        }
    }
    
    /**
     * Gets the current back stack size for monitoring.
     * 
     * @param navController The navigation controller
     * @return The number of entries in the back stack
     */
    fun getBackStackSize(navController: NavController): Int {
        return navController.currentBackStack.value.size
    }
    
    /**
     * Logs the current back stack for debugging.
     * 
     * @param navController The navigation controller
     */
    fun logBackStack(navController: NavController) {
        val backStack = navController.currentBackStack.value
        Timber.d("Back stack size: ${backStack.size}")
        backStack.forEachIndexed { index, entry ->
            Timber.d("  [$index] ${entry.destination.route}")
        }
    }
    
    /**
     * Checks if the back stack is getting too large and logs a warning.
     * 
     * @param navController The navigation controller
     * @param threshold The maximum acceptable back stack size (default: 10)
     */
    fun checkBackStackSize(navController: NavController, threshold: Int = 10) {
        val size = getBackStackSize(navController)
        if (size > threshold) {
            Timber.w("Back stack size ($size) exceeds threshold ($threshold)")
            logBackStack(navController)
        }
    }
    
    /**
     * Validates that state is being properly saved and restored.
     * Returns true if the navigation options are optimal.
     * 
     * @param saveState Whether state is being saved
     * @param restoreState Whether state is being restored
     * @param launchSingleTop Whether launchSingleTop is enabled
     * @return True if configuration is optimal
     */
    fun validateNavigationOptions(
        saveState: Boolean,
        restoreState: Boolean,
        launchSingleTop: Boolean
    ): Boolean {
        val isOptimal = saveState && restoreState && launchSingleTop
        
        if (!isOptimal) {
            Timber.w("Navigation options not optimal: saveState=$saveState, restoreState=$restoreState, launchSingleTop=$launchSingleTop")
        }
        
        return isOptimal
    }
    
    /**
     * Gets back stack statistics for performance monitoring.
     * 
     * @param navController The navigation controller
     * @return Statistics about the back stack
     */
    fun getBackStackStats(navController: NavController): BackStackStats {
        val backStack = navController.currentBackStack.value
        val routes = backStack.mapNotNull { it.destination.route }
        val uniqueRoutes = routes.toSet()
        
        return BackStackStats(
            totalEntries = backStack.size,
            uniqueRoutes = uniqueRoutes.size,
            duplicateEntries = backStack.size - uniqueRoutes.size,
            routes = routes
        )
    }
}

/**
 * Statistics about the navigation back stack.
 * 
 * @property totalEntries Total number of entries in the back stack
 * @property uniqueRoutes Number of unique routes in the back stack
 * @property duplicateEntries Number of duplicate entries (indicates potential issues)
 * @property routes List of all routes in the back stack
 */
data class BackStackStats(
    val totalEntries: Int,
    val uniqueRoutes: Int,
    val duplicateEntries: Int,
    val routes: List<String>
) {
    /**
     * Checks if the back stack is healthy (no duplicates, reasonable size).
     */
    fun isHealthy(): Boolean {
        return duplicateEntries == 0 && totalEntries <= 10
    }
    
    override fun toString(): String {
        return "BackStackStats(total=$totalEntries, unique=$uniqueRoutes, duplicates=$duplicateEntries)"
    }
}
