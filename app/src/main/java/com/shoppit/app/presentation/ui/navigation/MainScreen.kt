package com.shoppit.app.presentation.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.shoppit.app.presentation.ui.sync.SyncStatusIndicator
import com.shoppit.app.presentation.ui.sync.SyncStatusSnackbar
import com.shoppit.app.presentation.ui.sync.SyncViewModel

/**
 * Main screen with bottom navigation bar.
 * Provides navigation between main app sections: Meals, Planner, and Shopping.
 * 
 * Requirements:
 * - 1.1: Bottom navigation bar displays three items
 * - 1.5: Bottom bar visibility controlled based on current screen with visual feedback
 * - 6.5: Each bottom nav item maintains its own back stack
 * - 1.3: State preservation when switching between tabs
 * - 1.4: Back navigation works correctly within each section
 *
 * @param navController Optional NavController to use. If not provided, creates a new one.
 * @param getBadgeCount Optional function to get badge count for a navigation item route.
 *                      Returns null if no badge should be shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    getBadgeCount: (String) -> Int? = { null },
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    // Sync state
    val syncState by syncViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Sync status snackbar
    SyncStatusSnackbar(
        syncState = syncState,
        onDismissSuccess = syncViewModel::dismissSuccessMessage,
        onDismissError = syncViewModel::dismissErrorMessage,
        snackbarHostState = snackbarHostState
    )
    
    // Requirement 1.5: Determine if bottom bar should be visible
    // Show on main section screens, hide on detail/edit screens
    val shouldShowBottomBar by remember(currentRoute) {
        derivedStateOf {
            when {
                currentRoute == null -> true
                // Main section screens - show bottom bar
                currentRoute == Screen.MealList.route -> true
                currentRoute == Screen.MealPlanner.route -> true
                currentRoute == Screen.ShoppingList.route -> true
                // Detail and edit screens - hide bottom bar for more content space
                currentRoute.startsWith("meal_detail") -> false
                currentRoute.startsWith("edit_meal") -> false
                currentRoute == Screen.AddMeal.route -> false
                currentRoute == Screen.ItemHistory.route -> false
                currentRoute == Screen.TemplateManager.route -> false
                currentRoute == Screen.StoreSectionEditor.route -> false
                currentRoute == Screen.ShoppingMode.route -> false
                currentRoute == Screen.AnalyticsDashboard.route -> false
                else -> true
            }
        }
    }
    
    // Setup keyboard navigation shortcuts
    SetupKeyboardNavigation(navController)
    
    Scaffold(
        modifier = Modifier.keyboardNavigationShortcuts(navController),
        topBar = {
            // Show sync status in top bar on main screens
            if (shouldShowBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentRoute) {
                                Screen.MealList.route -> "Meals"
                                Screen.MealPlanner.route -> "Planner"
                                Screen.ShoppingList.route -> "Shopping"
                                else -> "Shoppit"
                            }
                        )
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SyncStatusIndicator(
                                syncState = syncState,
                                onManualSync = syncViewModel::triggerManualSync,
                                showText = false
                            )
                            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Requirement 1.5: Smooth show/hide animations for bottom bar
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    BottomNavigationItem.items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == item.route 
                        } == true
                        
                        // Requirement 1.5: Get badge count for visual feedback
                        val badgeCount = getBadgeCount(item.route)
                        
                        NavigationBarItem(
                            icon = { 
                                // Requirement 1.5: Badge support for notifications
                                if (badgeCount != null && badgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(badgeCount.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = "${item.title} navigation button"
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = "${item.title} navigation button"
                                    )
                                }
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
        }
    ) { innerPadding ->
        ShoppitNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}