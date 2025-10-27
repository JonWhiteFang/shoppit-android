package com.shoppit.app.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shoppit.app.presentation.ui.common.HomeScreen

/**
 * Main navigation host for the Shoppit app.
 * Defines all navigation routes and their corresponding composable screens.
 *
 * @param navController The navigation controller for managing navigation
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun ShoppitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }
        
        // Future navigation destinations will be added here:
        // - Meal list screen
        // - Meal detail screen with arguments
        // - Add/Edit meal screen
        // - Meal planner screen
        // - Shopping list screen
    }
}
