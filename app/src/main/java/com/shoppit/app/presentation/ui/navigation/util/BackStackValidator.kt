package com.shoppit.app.presentation.ui.navigation.util

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController

/**
 * Utility for validating and managing the navigation back stack.
 * Prevents circular navigation loops and duplicate destinations.
 * 
 * Requirements:
 * - 5.5: Validate back stack to prevent circular references
 * - 5.5: Clear duplicate destinations from back stack
 */
object BackStackValidator {
    
    /**
     * Checks if adding a destination would create a circular loop in the back stack.
     * A circular loop occurs when the same destination appears multiple times in the stack.
     * 
     * Requirement 5.5: Validate back stack to prevent circular references
     * 
     * @param navController The navigation controller
     * @param destinationRoute The route to check
     * @return true if adding this destination would create a loop
     */
    fun wouldCreateLoop(
        navController: NavHostController,
        destinationRoute: String
    ): Boolean {
        val backStack = navController.currentBackStack.value
        
        // Count occurrences of this route in the back stack
        val occurrences = backStack.count { entry ->
            entry.destination.route == destinationRoute
        }
        
        // If the route already appears in the back stack, it would create a loop
        return occurrences > 0
    }
    
    /**
     * Checks if the back stack contains duplicate consecutive destinations.
     * 
     * @param navController The navigation controller
     * @return true if there are duplicate consecutive destinations
     */
    fun hasDuplicateConsecutiveDestinations(
        navController: NavHostController
    ): Boolean {
        val backStack = navController.currentBackStack.value
        
        if (backStack.size < 2) return false
        
        for (i in 0 until backStack.size - 1) {
            val current = backStack[i].destination.route
            val next = backStack[i + 1].destination.route
            
            if (current == next) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Removes duplicate destinations from the back stack.
     * Keeps only the most recent occurrence of each destination.
     * 
     * Requirement 5.5: Clear duplicate destinations from back stack
     * 
     * @param navController The navigation controller
     */
    fun removeDuplicates(navController: NavHostController) {
        try {
            val backStack = navController.currentBackStack.value
            val seenRoutes = mutableSetOf<String>()
            val entriesToRemove = mutableListOf<NavBackStackEntry>()
            
            // Iterate from most recent to oldest
            for (i in backStack.size - 1 downTo 0) {
                val entry = backStack[i]
                val route = entry.destination.route ?: continue
                
                if (seenRoutes.contains(route)) {
                    // This is a duplicate, mark for removal
                    entriesToRemove.add(entry)
                } else {
                    seenRoutes.add(route)
                }
            }
            
            // Remove duplicates (oldest occurrences)
            entriesToRemove.forEach { entry ->
                try {
                    navController.popBackStack(
                        entry.destination.id,
                        inclusive = true,
                        saveState = false
                    )
                } catch (e: Exception) {
                    NavigationLogger.logNavigationError(
                        message = "Failed to remove duplicate entry from back stack",
                        route = entry.destination.route,
                        exception = e
                    )
                }
            }
            
            if (entriesToRemove.isNotEmpty()) {
                NavigationLogger.logNavigationSuccess(
                    route = "back_stack_cleanup",
                    arguments = mapOf("removed_count" to entriesToRemove.size)
                )
            }
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to remove duplicates from back stack",
                exception = e
            )
        }
    }
    
    /**
     * Validates the entire back stack for consistency.
     * Checks for circular references, duplicates, and invalid states.
     * 
     * Requirement 5.5: Validate back stack to prevent circular references
     * 
     * @param navController The navigation controller
     * @return ValidationResult with any issues found
     */
    fun validateBackStack(navController: NavHostController): ValidationResult {
        val issues = mutableListOf<String>()
        val backStack = navController.currentBackStack.value
        
        // Check for empty back stack
        if (backStack.isEmpty()) {
            issues.add("Back stack is empty")
            return ValidationResult(isValid = false, issues = issues)
        }
        
        // Check for circular references (same route appearing multiple times)
        val routeCounts = mutableMapOf<String, Int>()
        backStack.forEach { entry ->
            val route = entry.destination.route ?: "unknown"
            routeCounts[route] = (routeCounts[route] ?: 0) + 1
        }
        
        routeCounts.forEach { (route, count) ->
            if (count > 1) {
                issues.add("Circular reference detected: '$route' appears $count times")
            }
        }
        
        // Check for duplicate consecutive destinations
        if (hasDuplicateConsecutiveDestinations(navController)) {
            issues.add("Duplicate consecutive destinations found")
        }
        
        // Check for orphaned entries (entries with no valid destination)
        val orphanedCount = backStack.count { entry ->
            entry.destination.route == null
        }
        if (orphanedCount > 0) {
            issues.add("Found $orphanedCount orphaned entries with no route")
        }
        
        return ValidationResult(
            isValid = issues.isEmpty(),
            issues = issues
        )
    }
    
    /**
     * Attempts to fix common back stack issues.
     * Removes duplicates and orphaned entries.
     * 
     * @param navController The navigation controller
     * @return true if fixes were applied successfully
     */
    fun fixBackStackIssues(navController: NavHostController): Boolean {
        return try {
            val validationResult = validateBackStack(navController)
            
            if (!validationResult.isValid) {
                NavigationLogger.logNavigationError(
                    message = "Back stack validation failed",
                    arguments = mapOf("issues" to validationResult.issues.joinToString(", "))
                )
                
                // Remove duplicates
                removeDuplicates(navController)
                
                // Validate again
                val newValidationResult = validateBackStack(navController)
                
                if (newValidationResult.isValid) {
                    NavigationLogger.logNavigationSuccess(
                        route = "back_stack_fix",
                        arguments = mapOf("fixed" to true)
                    )
                    true
                } else {
                    NavigationLogger.logNavigationError(
                        message = "Failed to fix all back stack issues",
                        arguments = mapOf("remaining_issues" to newValidationResult.issues.joinToString(", "))
                    )
                    false
                }
            } else {
                true
            }
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Error fixing back stack issues",
                exception = e
            )
            false
        }
    }
    
    /**
     * Gets a summary of the current back stack for debugging.
     * 
     * @param navController The navigation controller
     * @return String representation of the back stack
     */
    fun getBackStackSummary(navController: NavHostController): String {
        val backStack = navController.currentBackStack.value
        return buildString {
            appendLine("Back Stack Summary:")
            appendLine("  Size: ${backStack.size}")
            appendLine("  Entries:")
            backStack.forEachIndexed { index, entry ->
                val route = entry.destination.route ?: "unknown"
                val isCurrent = index == backStack.size - 1
                appendLine("    ${if (isCurrent) "→" else " "} [$index] $route")
            }
        }
    }
}

/**
 * Result of back stack validation.
 * 
 * @property isValid Whether the back stack is valid
 * @property issues List of issues found (empty if valid)
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>
)
