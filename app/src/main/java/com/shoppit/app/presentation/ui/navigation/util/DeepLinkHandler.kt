package com.shoppit.app.presentation.ui.navigation.util

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.shoppit.app.presentation.ui.navigation.Screen
import timber.log.Timber

/**
 * Utility object for handling deep links in the application.
 * Validates deep link parameters and constructs proper back stacks.
 *
 * Requirements:
 * - 8.1: Navigate directly to specified screen with appropriate parameters
 * - 8.2: Construct proper back stack for deep link navigation
 * - 8.3: Validate deep link parameters before navigation
 * - 8.4: Handle invalid deep links with fallback navigation
 * - 8.5: Support deep links while app is running
 */
object DeepLinkHandler {
    
    /**
     * Handles incoming deep link intent and navigates to the appropriate screen.
     *
     * @param intent The intent containing the deep link
     * @param navController The navigation controller to use for navigation
     * @return True if deep link was handled successfully, false otherwise
     */
    fun handleDeepLink(intent: Intent?, navController: NavController): Boolean {
        val uri = intent?.data ?: return false
        
        NavigationLogger.logDeepLink(
            uri = uri.toString(),
            action = intent.action
        )
        
        return when {
            uri.scheme != "shoppit" -> {
                NavigationLogger.logNavigationError(
                    message = "Invalid deep link scheme: ${uri.scheme}",
                    route = uri.toString()
                )
                false
            }
            else -> handleShoppitDeepLink(uri, navController)
        }
    }
    
    /**
     * Handles shoppit:// scheme deep links.
     */
    private fun handleShoppitDeepLink(uri: Uri, navController: NavController): Boolean {
        return when (uri.host) {
            "meal" -> handleMealDeepLink(uri, navController)
            "planner" -> handlePlannerDeepLink(uri, navController)
            "shopping" -> handleShoppingDeepLink(uri, navController)
            else -> {
                NavigationLogger.logNavigationError(
                    message = "Unknown deep link host: ${uri.host}",
                    route = uri.toString()
                )
                navigateToFallback(navController)
                false
            }
        }
    }
    
    /**
     * Handles meal detail deep links (shoppit://meal/{mealId}).
     */
    private fun handleMealDeepLink(uri: Uri, navController: NavController): Boolean {
        val mealIdString = uri.pathSegments.firstOrNull()
        
        if (mealIdString.isNullOrBlank()) {
            NavigationLogger.logNavigationError(
                message = "Missing mealId in meal deep link",
                route = uri.toString()
            )
            navigateToFallback(navController, Screen.MealList.route)
            return false
        }
        
        val mealId = mealIdString.toLongOrNull()
        if (mealId == null || mealId <= 0) {
            NavigationLogger.logNavigationError(
                message = "Invalid mealId in meal deep link: $mealIdString",
                route = uri.toString()
            )
            navigateToFallback(navController, Screen.MealList.route)
            return false
        }
        
        // Construct proper back stack: MealList -> MealDetail
        try {
            navController.navigate(Screen.MealList.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
            navController.navigate(Screen.MealDetail.createRoute(mealId))
            
            NavigationLogger.logNavigation(
                from = "DeepLink",
                to = Screen.MealDetail.route,
                arguments = mapOf("mealId" to mealId)
            )
            return true
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to meal detail from deep link",
                exception = e,
                route = uri.toString()
            )
            navigateToFallback(navController, Screen.MealList.route)
            return false
        }
    }
    
    /**
     * Handles planner deep links (shoppit://planner or shoppit://planner?date={date}).
     */
    private fun handlePlannerDeepLink(uri: Uri, navController: NavController): Boolean {
        val dateParam = uri.getQueryParameter("date")
        
        // Validate date parameter if provided
        if (dateParam != null) {
            val dateMillis = dateParam.toLongOrNull()
            if (dateMillis == null || dateMillis < 0) {
                NavigationLogger.logNavigationError(
                    message = "Invalid date parameter in planner deep link: $dateParam",
                    route = uri.toString()
                )
                // Navigate to planner without date parameter
            }
        }
        
        try {
            navController.navigate(Screen.MealPlanner.route) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
            
            NavigationLogger.logNavigation(
                from = "DeepLink",
                to = Screen.MealPlanner.route,
                arguments = dateParam?.let { mapOf("date" to it) } ?: emptyMap()
            )
            return true
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to planner from deep link",
                exception = e,
                route = uri.toString()
            )
            navigateToFallback(navController, Screen.MealPlanner.route)
            return false
        }
    }
    
    /**
     * Handles shopping deep links (shoppit://shopping or shoppit://shopping/mode).
     */
    private fun handleShoppingDeepLink(uri: Uri, navController: NavController): Boolean {
        val isShoppingMode = uri.pathSegments.firstOrNull() == "mode"
        
        try {
            if (isShoppingMode) {
                // Construct back stack: ShoppingList -> ShoppingMode
                navController.navigate(Screen.ShoppingList.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                navController.navigate(Screen.ShoppingMode.route)
                
                NavigationLogger.logNavigation(
                    from = "DeepLink",
                    to = Screen.ShoppingMode.route
                )
            } else {
                navController.navigate(Screen.ShoppingList.route) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }
                
                NavigationLogger.logNavigation(
                    from = "DeepLink",
                    to = Screen.ShoppingList.route
                )
            }
            return true
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to shopping from deep link",
                exception = e,
                route = uri.toString()
            )
            navigateToFallback(navController, Screen.ShoppingList.route)
            return false
        }
    }
    
    /**
     * Navigates to a fallback screen when deep link handling fails.
     */
    private fun navigateToFallback(navController: NavController, fallbackRoute: String = Screen.MealList.route) {
        try {
            navController.navigate(fallbackRoute) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
            NavigationLogger.logNavigation(
                from = "DeepLink",
                to = fallbackRoute,
                arguments = mapOf("reason" to "fallback")
            )
        } catch (e: Exception) {
            NavigationLogger.logNavigationError(
                message = "Failed to navigate to fallback screen",
                exception = e,
                route = fallbackRoute
            )
        }
    }
    
    /**
     * Validates a deep link URI without navigating.
     *
     * @param uri The URI to validate
     * @return True if the URI is a valid deep link, false otherwise
     */
    fun isValidDeepLink(uri: Uri?): Boolean {
        if (uri == null) return false
        
        if (uri.scheme != "shoppit") return false
        
        return when (uri.host) {
            "meal" -> {
                val mealIdString = uri.pathSegments.firstOrNull()
                val mealId = mealIdString?.toLongOrNull()
                mealId != null && mealId > 0
            }
            "planner" -> {
                val dateParam = uri.getQueryParameter("date")
                if (dateParam != null) {
                    val dateMillis = dateParam.toLongOrNull()
                    dateMillis != null && dateMillis >= 0
                } else {
                    true // planner without date is valid
                }
            }
            "shopping" -> {
                val path = uri.pathSegments.firstOrNull()
                path == null || path == "mode"
            }
            else -> false
        }
    }
}
