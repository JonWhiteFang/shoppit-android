package com.shoppit.app.presentation.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.shoppit.app.presentation.ui.auth.AuthScreen
import com.shoppit.app.presentation.ui.meal.AddEditMealScreen
import com.shoppit.app.presentation.ui.meal.MealDetailScreen
import com.shoppit.app.presentation.ui.meal.MealListScreen
import com.shoppit.app.presentation.ui.navigation.util.FocusManagementEffect
import com.shoppit.app.presentation.ui.navigation.util.NavigationAnalytics
import com.shoppit.app.presentation.ui.navigation.util.NavigationErrorHandler
import com.shoppit.app.presentation.ui.navigation.util.NavigationLogger
import com.shoppit.app.presentation.ui.navigation.util.NavigationPerformanceAnalytics
import com.shoppit.app.presentation.ui.navigation.util.NavigationPerformanceMonitor
import com.shoppit.app.presentation.ui.navigation.util.NavigationPreloader
import com.shoppit.app.presentation.ui.navigation.util.RecordNavigationForPreloading
import com.shoppit.app.presentation.ui.planner.MealPlannerScreen
import com.shoppit.app.presentation.ui.shopping.ItemHistoryScreen
import com.shoppit.app.presentation.ui.shopping.ShoppingListScreen
import com.shoppit.app.presentation.ui.shopping.ShoppingListViewModel
import com.shoppit.app.presentation.ui.shopping.ShoppingModeScreen
import com.shoppit.app.presentation.ui.shopping.StoreSectionEditor
import com.shoppit.app.presentation.ui.shopping.TemplateManagerScreen

/**
 * Main navigation host for the Shoppit app.
 * Defines all navigation routes and their corresponding composable screens.
 *
 * Requirements:
 * - 2.5: Navigate from meal list to meal detail
 * - 3.3: Provide edit action button on detail screen
 * - 3.4: Provide delete action button on detail screen
 *
 * @param navController The navigation controller for managing navigation
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun ShoppitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // Announce screen transitions for accessibility
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val previousRoute = remember { mutableStateOf<String?>(null) }
    
    // Record navigation for preloading analysis
    RecordNavigationForPreloading(
        currentRoute = currentRoute,
        previousRoute = previousRoute.value
    )
    
    // Manage focus during screen transitions
    FocusManagementEffect(currentRoute)
    
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            // Stop comprehensive performance monitoring
            NavigationPerformanceAnalytics.stopMonitoring(route)
            
            val screenName = getScreenNameFromRoute(route)
            val arguments = currentBackStackEntry?.arguments?.keyValueMap()
            
            NavigationLogger.logNavigationSuccess(
                route = route,
                arguments = arguments
            )
            
            // Track screen view in analytics
            NavigationAnalytics.trackScreenView(route, arguments)
            
            // Track navigation path if there was a previous route
            previousRoute.value?.let { prevRoute ->
                NavigationAnalytics.trackNavigationPath(prevRoute, route, arguments)
            }
            
            // Update previous route for next navigation
            previousRoute.value = route
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.MealList.route,
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
        }
    ) {
        // Authentication screen
        composable(Screen.Auth.route) {
            AuthScreen(
                onAuthenticationComplete = {
                    // Navigate to meal list after successful authentication
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.MealList.route,
                        fallbackRoute = Screen.MealList.route
                    )
                },
                onSkip = {
                    // Navigate to meal list in offline mode
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.MealList.route,
                        fallbackRoute = Screen.MealList.route
                    )
                }
            )
        }
        
        // Meal list screen - starting destination
        composable(Screen.MealList.route) {
            MealListScreen(
                onMealClick = { mealId ->
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.MealDetail.createRoute(mealId),
                        arguments = mapOf("mealId" to mealId),
                        fallbackRoute = Screen.MealList.route
                    )
                },
                onAddMealClick = {
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.AddMeal.route,
                        fallbackRoute = Screen.MealList.route
                    )
                }
            )
        }
        
        // Meal detail screen with mealId argument
        composable(
            route = Screen.MealDetail.route,
            arguments = listOf(
                navArgument("mealId") {
                    type = NavType.LongType
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "shoppit://meal/{mealId}"
                }
            )
        ) { backStackEntry ->
            // Validate arguments
            val arguments = backStackEntry.arguments
            val validationErrors = NavigationErrorHandler.validateArguments(
                arguments = arguments,
                requiredArgs = listOf("mealId")
            )
            
            if (validationErrors.isNotEmpty()) {
                NavigationLogger.logNavigationError(
                    message = "Validation failed for MealDetail screen",
                    route = Screen.MealDetail.route,
                    arguments = arguments?.keyValueMap()
                )
                
                // Handle missing arguments
                NavigationErrorHandler.handleMissingArguments(
                    navController = navController,
                    route = Screen.MealDetail.route,
                    requiredArgs = listOf("mealId"),
                    fallbackRoute = Screen.MealList.route
                )
                return@composable
            }
            
            val mealId = arguments?.getLong("mealId")
            if (mealId == null || mealId <= 0) {
                NavigationLogger.logNavigationError(
                    message = "Invalid mealId argument",
                    route = Screen.MealDetail.route,
                    arguments = arguments?.keyValueMap()
                )
                
                // Handle invalid arguments
                NavigationErrorHandler.handleInvalidArguments(
                    navController = navController,
                    route = Screen.MealDetail.route,
                    arguments = arguments?.keyValueMap() ?: emptyMap(),
                    fallbackRoute = Screen.MealList.route
                )
                return@composable
            }
            
            MealDetailScreen(
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from MealDetail",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onEditClick = { mealId ->
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.EditMeal.createRoute(mealId),
                        arguments = mapOf("mealId" to mealId),
                        fallbackRoute = Screen.MealList.route
                    )
                },
                onDeleteClick = { mealId ->
                    try {
                        // After deletion, navigate back to meal list
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to navigate back after meal deletion",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
        
        // Add meal screen
        composable(Screen.AddMeal.route) {
            AddEditMealScreen(
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from AddMeal",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onMealSaved = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to navigate back after meal save",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
        
        // Edit meal screen with mealId argument
        composable(
            route = Screen.EditMeal.route,
            arguments = listOf(
                navArgument("mealId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            // Validate arguments
            val arguments = backStackEntry.arguments
            val validationErrors = NavigationErrorHandler.validateArguments(
                arguments = arguments,
                requiredArgs = listOf("mealId")
            )
            
            if (validationErrors.isNotEmpty()) {
                NavigationLogger.logNavigationError(
                    message = "Validation failed for EditMeal screen",
                    route = Screen.EditMeal.route,
                    arguments = arguments?.keyValueMap()
                )
                
                // Handle missing arguments
                NavigationErrorHandler.handleMissingArguments(
                    navController = navController,
                    route = Screen.EditMeal.route,
                    requiredArgs = listOf("mealId"),
                    fallbackRoute = Screen.MealList.route
                )
                return@composable
            }
            
            val mealId = arguments?.getLong("mealId")
            if (mealId == null || mealId <= 0) {
                NavigationLogger.logNavigationError(
                    message = "Invalid mealId argument for EditMeal",
                    route = Screen.EditMeal.route,
                    arguments = arguments?.keyValueMap()
                )
                
                // Handle invalid arguments
                NavigationErrorHandler.handleInvalidArguments(
                    navController = navController,
                    route = Screen.EditMeal.route,
                    arguments = arguments?.keyValueMap() ?: emptyMap(),
                    fallbackRoute = Screen.MealList.route
                )
                return@composable
            }
            
            AddEditMealScreen(
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from EditMeal",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onMealSaved = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to navigate back after meal edit save",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
        
        // Meal planner screen
        composable(
            route = Screen.MealPlanner.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "shoppit://planner?date={date}"
                },
                navDeepLink {
                    uriPattern = "shoppit://planner"
                }
            )
        ) {
            MealPlannerScreen(
                onMealDetailClick = { mealId ->
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.MealDetail.createRoute(mealId),
                        arguments = mapOf("mealId" to mealId),
                        fallbackRoute = Screen.MealPlanner.route
                    )
                }
            )
        }
        
        // Shopping list screen
        composable(
            route = Screen.ShoppingList.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "shoppit://shopping"
                }
            )
        ) {
            ShoppingListScreen(
                onMealDetailClick = { mealId ->
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.MealDetail.createRoute(mealId),
                        arguments = mapOf("mealId" to mealId),
                        fallbackRoute = Screen.ShoppingList.route
                    )
                },
                onNavigateToHistory = {
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.ItemHistory.route,
                        fallbackRoute = Screen.ShoppingList.route
                    )
                },
                onNavigateToTemplates = {
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.TemplateManager.route,
                        fallbackRoute = Screen.ShoppingList.route
                    )
                },
                onNavigateToSectionEditor = {
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.StoreSectionEditor.route,
                        fallbackRoute = Screen.ShoppingList.route
                    )
                },
                onNavigateToShoppingMode = {
                    NavigationErrorHandler.safeNavigate(
                        navController = navController,
                        route = Screen.ShoppingMode.route,
                        fallbackRoute = Screen.ShoppingList.route
                    )
                }
            )
        }
        
        // Item history screen
        composable(Screen.ItemHistory.route) {
            val viewModel: ShoppingListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            ItemHistoryScreen(
                historyItems = uiState.frequentItems,
                isLoading = uiState.isLoadingHistory,
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from ItemHistory",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onItemClick = { historyItem ->
                    // Add item from history to shopping list
                    // This would need a method in ViewModel
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to navigate back from ItemHistory item click",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
        
        // Template manager screen
        composable(Screen.TemplateManager.route) {
            val viewModel: ShoppingListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            TemplateManagerScreen(
                templates = uiState.templates,
                isLoading = uiState.isLoadingTemplates,
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from TemplateManager",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onCreateTemplate = { name, description ->
                    // Create template - would need a method in ViewModel
                },
                onLoadTemplate = { templateId ->
                    // Load template - would need a method in ViewModel
                },
                onDeleteTemplate = { templateId ->
                    // Delete template - would need a method in ViewModel
                }
            )
        }
        
        // Store section editor screen
        composable(Screen.StoreSectionEditor.route) {
            val viewModel: ShoppingListViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            
            StoreSectionEditor(
                sections = uiState.storeSections,
                isLoading = uiState.isLoading,
                onNavigateBack = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from StoreSectionEditor",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                },
                onReorderSections = { sections ->
                    // Update section order through repository
                    // This would need a method in ViewModel
                },
                onCreateSection = { name, color ->
                    // Create custom section through repository
                    // This would need a method in ViewModel
                }
            )
        }
        
        // Shopping mode screen
        composable(
            route = Screen.ShoppingMode.route,
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "shoppit://shopping/mode"
                }
            )
        ) {
            ShoppingModeScreen(
                onExitShoppingMode = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from ShoppingMode",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
        
        // Navigation analytics dashboard screen
        composable(Screen.AnalyticsDashboard.route) {
            NavigationAnalyticsDashboard(
                onClose = {
                    try {
                        navController.popBackStack()
                    } catch (e: Exception) {
                        NavigationLogger.logNavigationError(
                            message = "Failed to pop back stack from AnalyticsDashboard",
                            exception = e
                        )
                        NavigationErrorHandler.handleNavigationFailure(navController, e)
                    }
                }
            )
        }
    }
}

/**
 * Extension function to convert Bundle to Map for logging purposes.
 */
private fun Bundle.keyValueMap(): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (key in this.keySet()) {
        map[key] = this.get(key)
    }
    return map
}

/**
 * Helper function to get human-readable screen name from route for accessibility announcements.
 */
private fun getScreenNameFromRoute(route: String): String {
    return when {
        route.startsWith("meal_list") -> "Meal List"
        route.startsWith("meal_detail") -> "Meal Detail"
        route.startsWith("add_meal") -> "Add Meal"
        route.startsWith("edit_meal") -> "Edit Meal"
        route.startsWith("meal_planner") -> "Meal Planner"
        route.startsWith("shopping_list") -> "Shopping List"
        route.startsWith("item_history") -> "Item History"
        route.startsWith("template_manager") -> "Template Manager"
        route.startsWith("store_section_editor") -> "Store Section Editor"
        route.startsWith("shopping_mode") -> "Shopping Mode"
        route.startsWith("analytics_dashboard") -> "Analytics Dashboard"
        else -> "Unknown Screen"
    }
}