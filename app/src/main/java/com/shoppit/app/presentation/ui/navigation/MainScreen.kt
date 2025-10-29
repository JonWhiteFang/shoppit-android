package com.shoppit.app.presentation.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoppit.app.presentation.ui.navigation.util.NavigationErrorHandler
import com.shoppit.app.presentation.ui.navigation.util.NavigationLogger
import com.shoppit.app.presentation.ui.navigation.util.NavigationPerformanceAnalytics
import com.shoppit.app.presentation.ui.navigation.util.SetupKeyboardNavigation
import com.shoppit.app.presentation.ui.navigation.util.keyboardNavigationShortcuts

/**
 * Main screen with bottom navigation bar.
 * Provides navigation between main app sections: Meals, Planner, and Shopping.
 * 
 * Requirements:
 * - 6.5: Each bottom nav item maintains its own back stack
 * - 1.3: State preservation when switching between tabs
 * - 1.4: Back navigation works correctly within each section
 *
 * @param navController Optional NavController to use. If not provided, creates a new one.
 */
@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // Setup keyboard navigation shortcuts
    SetupKeyboardNavigation(navController)
    
    Scaffold(
        modifier = Modifier.keyboardNavigationShortcuts(navController),
        bottomBar = {
            NavigationBar {
                BottomNavigationItem.items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route == item.route 
                    } == true
                    
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                imageVector = item.icon,
                                contentDescription = "${item.title} navigation button"
                            ) 
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            try {
                                // Start comprehensive performance monitoring
                                NavigationPerformanceAnalytics.startMonitoring(item.route)
                                
                                // Requirement 6.5, 1.3, 1.4: Independent back stacks for bottom navigation
                                // Each bottom nav item maintains its own back stack with state preservation
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up a large stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        // Requirement 6.5: Save state when navigating away from a section
                                        // This preserves the back stack and scroll positions
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Requirement 1.3: Restore state when reselecting a previously selected item
                                    // This brings back the entire back stack and UI state
                                    restoreState = true
                                }
                                
                                NavigationLogger.logNavigationSuccess(
                                    route = item.route,
                                    arguments = null
                                )
                            } catch (e: Exception) {
                                NavigationLogger.logNavigationError(
                                    message = "Failed to navigate to bottom navigation item",
                                    route = item.route,
                                    exception = e
                                )
                                
                                // Handle navigation failure with recovery
                                NavigationErrorHandler.handleNavigationFailure(navController, e)
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "${item.title} tab"
                            stateDescription = if (selected) "Selected" else "Not selected"
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        ShoppitNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}