package com.shoppit.app.presentation.ui.navigation.util

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

/**
 * Utility for handling back button presses consistently across the app.
 * 
 * Requirements:
 * - 5.1: Back button pops navigation stack correctly on detail screens
 * - 5.2: Exit app when back is pressed on main screens
 */
object BackPressHandler {
    
    /**
     * Determines if the current route is a main section screen.
     * Main screens are: MealList, MealPlanner, ShoppingList
     * 
     * @param route The current route
     * @return true if the route is a main section screen
     */
    fun isMainScreen(route: String?): Boolean {
        return when (route) {
            "meal_list",
            "meal_planner",
            "shopping_list" -> true
            else -> false
        }
    }
    
    /**
     * Determines if the current route is a detail or edit screen.
     * 
     * @param route The current route
     * @return true if the route is a detail or edit screen
     */
    fun isDetailOrEditScreen(route: String?): Boolean {
        return when {
            route == null -> false
            route.startsWith("meal_detail") -> true
            route.startsWith("edit_meal") -> true
            route == "add_meal" -> true
            route == "item_history" -> true
            route == "template_manager" -> true
            route == "store_section_editor" -> true
            route == "shopping_mode" -> true
            route == "analytics_dashboard" -> true
            else -> false
        }
    }
    
    /**
     * Handles back navigation based on the current route.
     * 
     * Requirements:
     * - 5.1: Pop back stack on detail screens
     * - 5.2: Exit app on main screens
     * 
     * @param navController The navigation controller
     * @param currentRoute The current route
     * @param onExitApp Callback to exit the app (should finish the activity)
     * @return true if back press was handled, false to allow default behavior
     */
    fun handleBackPress(
        navController: NavHostController,
        currentRoute: String?,
        onExitApp: () -> Unit
    ): Boolean {
        return try {
            when {
                // On main screens, exit the app
                isMainScreen(currentRoute) -> {
                    onExitApp()
                    true
                }
                // On detail/edit screens, pop back stack
                isDetailOrEditScreen(currentRoute) -> {
                    navController.popBackStack()
                    true
                }
                // Default: allow normal back navigation
                else -> {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                        true
                    } else {
                        // No previous entry, exit app
                        onExitApp()
                        true
                    }
                }
            }
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Error handling back press",
                route = currentRoute,
                exception = e
            )
            false
        }
    }
}

/**
 * Composable that handles back button presses for dialogs and bottom sheets.
 * 
 * Requirements:
 * - 5.1: Handle back navigation from dialogs and bottom sheets
 * 
 * @param enabled Whether the back handler is enabled
 * @param onBack Callback when back is pressed
 */
@Composable
fun DialogBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
) {
    BackHandler(enabled = enabled) {
        onBack()
    }
}

/**
 * Composable that provides a confirmation dialog before allowing back navigation.
 * Used for screens with unsaved changes.
 * 
 * Requirements:
 * - 5.3: Show confirmation when navigating back from forms with unsaved data
 * - 5.3: Allow users to save, discard, or cancel navigation
 * - 5.3: Preserve form state if user cancels back navigation
 * 
 * @param hasUnsavedChanges Whether there are unsaved changes
 * @param onSave Optional callback when user chooses to save (if null, save button is hidden)
 * @param onConfirmBack Callback when user confirms they want to go back (discard changes)
 */
@Composable
fun UnsavedChangesBackHandler(
    hasUnsavedChanges: Boolean,
    onSave: (() -> Unit)? = null,
    onConfirmBack: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    BackHandler(enabled = hasUnsavedChanges) {
        showConfirmDialog = true
    }
    
    if (showConfirmDialog) {
        UnsavedChangesDialog(
            showSaveButton = onSave != null,
            onSave = {
                showConfirmDialog = false
                onSave?.invoke()
            },
            onDiscard = {
                showConfirmDialog = false
                onConfirmBack()
            },
            onCancel = {
                showConfirmDialog = false
            }
        )
    }
}

/**
 * Dialog shown when user tries to navigate back with unsaved changes.
 * 
 * @param showSaveButton Whether to show the save button
 * @param onSave Callback when user chooses to save changes
 * @param onDiscard Callback when user chooses to discard changes
 * @param onCancel Callback when user cancels the navigation
 */
@Composable
private fun UnsavedChangesDialog(
    showSaveButton: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { androidx.compose.material3.Text("Unsaved Changes") },
        text = { 
            androidx.compose.material3.Text(
                "You have unsaved changes. What would you like to do?"
            ) 
        },
        confirmButton = {
            if (showSaveButton) {
                androidx.compose.material3.TextButton(onClick = onSave) {
                    androidx.compose.material3.Text("Save")
                }
            }
        },
        dismissButton = {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.TextButton(onClick = onDiscard) {
                    androidx.compose.material3.Text("Discard")
                }
                androidx.compose.material3.TextButton(onClick = onCancel) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        }
    )
}
