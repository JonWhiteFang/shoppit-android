package com.shoppit.app.presentation.ui.navigation.util

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController

/**
 * Keyboard navigation handler for the Shoppit app.
 * Provides keyboard shortcuts for navigation and focus management.
 * 
 * Requirements:
 * - 9.2: Support keyboard navigation
 * - 9.4: Ensure tab order follows logical reading patterns
 * - 9.5: Implement focus management during screen transitions
 */
object KeyboardNavigationHandler {
    
    /**
     * Handles keyboard shortcuts for bottom navigation.
     * 
     * Shortcuts:
     * - Ctrl+1: Navigate to Meals
     * - Ctrl+2: Navigate to Planner
     * - Ctrl+3: Navigate to Shopping
     * 
     * @param keyEvent The keyboard event
     * @param navController The navigation controller
     * @return True if the event was handled, false otherwise
     */
    fun handleBottomNavigationShortcut(
        keyEvent: android.view.KeyEvent,
        navController: NavHostController
    ): Boolean {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        // Check for Ctrl key modifier
        if (!keyEvent.isCtrlPressed) {
            return false
        }
        
        return when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_1 -> {
                navigateToBottomNavItem(navController, 0)
                true
            }
            KeyEvent.KEYCODE_2 -> {
                navigateToBottomNavItem(navController, 1)
                true
            }
            KeyEvent.KEYCODE_3 -> {
                navigateToBottomNavItem(navController, 2)
                true
            }
            else -> false
        }
    }
    
    /**
     * Navigates to a bottom navigation item by index.
     */
    private fun navigateToBottomNavItem(navController: NavHostController, index: Int) {
        val routes = listOf("meal_list", "meal_planner", "shopping_list")
        if (index in routes.indices) {
            try {
                navController.navigate(routes[index]) {
                    popUpTo(navController.graph.startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } catch (e: Exception) {
                NavigationLogger.logNavigationError(
                    message = "Failed to navigate via keyboard shortcut",
                    route = routes[index],
                    exception = e
                )
            }
        }
    }
    
    /**
     * Public wrapper for navigateToBottomNavItem for use in modifier extensions.
     */
    fun navigateToBottomNavItemPublic(navController: NavHostController, index: Int) {
        navigateToBottomNavItem(navController, index)
    }
    
    /**
     * Handles back navigation via Escape key.
     */
    fun handleBackNavigation(
        keyEvent: android.view.KeyEvent,
        navController: NavHostController
    ): Boolean {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        return when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> {
                try {
                    navController.popBackStack()
                    true
                } catch (e: Exception) {
                    NavigationLogger.logNavigationError(
                        message = "Failed to navigate back via Escape key",
                        exception = e
                    )
                    false
                }
            }
            else -> false
        }
    }
}

/**
 * Modifier extension for handling keyboard navigation shortcuts.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.keyboardNavigationShortcuts(
    navController: NavHostController
): Modifier = this.onKeyEvent { keyEvent ->
    if (keyEvent.type == KeyEventType.KeyDown) {
        when {
            // Ctrl+1, Ctrl+2, Ctrl+3 for bottom navigation
            keyEvent.isCtrlPressed && keyEvent.key == Key.One -> {
                KeyboardNavigationHandler.navigateToBottomNavItemPublic(navController, 0)
                true
            }
            keyEvent.isCtrlPressed && keyEvent.key == Key.Two -> {
                KeyboardNavigationHandler.navigateToBottomNavItemPublic(navController, 1)
                true
            }
            keyEvent.isCtrlPressed && keyEvent.key == Key.Three -> {
                KeyboardNavigationHandler.navigateToBottomNavItemPublic(navController, 2)
                true
            }
            else -> false
        }
    } else {
        false
    }
}

/**
 * Composable that sets up keyboard event handling for the entire app.
 */
@Composable
fun SetupKeyboardNavigation(
    navController: NavHostController
) {
    val view = LocalView.current
    
    DisposableEffect(view) {
        val listener = android.view.View.OnKeyListener { _, keyCode, event ->
            KeyboardNavigationHandler.handleBottomNavigationShortcut(event, navController) ||
            KeyboardNavigationHandler.handleBackNavigation(event, navController)
        }
        
        view.setOnKeyListener(listener)
        
        onDispose {
            view.setOnKeyListener(null)
        }
    }
}

/**
 * Modifier for managing focus order in composables.
 * Ensures tab order follows logical reading patterns.
 */
fun Modifier.focusOrder(
    focusManager: FocusManager,
    onNext: () -> Unit = { focusManager.moveFocus(FocusDirection.Next) },
    onPrevious: () -> Unit = { focusManager.moveFocus(FocusDirection.Previous) }
): Modifier = this.onKeyEvent { keyEvent ->
    if (keyEvent.type == KeyEventType.KeyDown) {
        when (keyEvent.key) {
            Key.Tab -> {
                if (keyEvent.isShiftPressed) {
                    onPrevious()
                } else {
                    onNext()
                }
                true
            }
            else -> false
        }
    } else {
        false
    }
}

/**
 * Composable that manages focus during screen transitions.
 */
@Composable
fun FocusManagementEffect(
    currentRoute: String?
) {
    val focusManager = LocalFocusManager.current
    
    DisposableEffect(currentRoute) {
        // Clear focus when navigating to a new screen
        focusManager.clearFocus()
        
        onDispose { }
    }
}
