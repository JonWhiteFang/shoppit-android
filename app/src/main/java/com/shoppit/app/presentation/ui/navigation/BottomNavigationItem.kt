package com.shoppit.app.presentation.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing bottom navigation items.
 * Each item has a route, title, and icon.
 */
sealed class BottomNavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Meals : BottomNavigationItem(
        route = Screen.MealList.route,
        title = "Meals",
        icon = Icons.Default.Restaurant
    )
    
    data object Planner : BottomNavigationItem(
        route = Screen.MealPlanner.route,
        title = "Planner",
        icon = Icons.Default.CalendarMonth
    )
    
    data object Shopping : BottomNavigationItem(
        route = Screen.ShoppingList.route,
        title = "Shopping",
        icon = Icons.Default.ShoppingCart
    )
    
    companion object {
        val items = listOf(Meals, Planner, Shopping)
    }
}
