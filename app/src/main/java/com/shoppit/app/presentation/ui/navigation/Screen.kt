package com.shoppit.app.presentation.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 * Each screen has a route string used for navigation.
 */
sealed class Screen(val route: String) {
    /**
     * Placeholder home screen - will be replaced with actual feature screens
     */
    data object Home : Screen("home")
    
    /**
     * Future screens will be added here as features are implemented:
     * - Meal list screen
     * - Meal detail screen
     * - Add/Edit meal screen
     * - Meal planner screen
     * - Shopping list screen
     */
}
