package com.shoppit.app.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shoppit.app.presentation.ui.meal.AddEditMealScreen
import com.shoppit.app.presentation.ui.meal.MealDetailScreen
import com.shoppit.app.presentation.ui.meal.MealListScreen
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
    NavHost(
        navController = navController,
        startDestination = Screen.MealList.route,
        modifier = modifier
    ) {
        // Meal list screen - starting destination
        composable(Screen.MealList.route) {
            MealListScreen(
                onMealClick = { mealId ->
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                },
                onAddMealClick = {
                    navController.navigate(Screen.AddMeal.route)
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
            )
        ) {
            MealDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditClick = { mealId ->
                    navController.navigate(Screen.EditMeal.createRoute(mealId))
                },
                onDeleteClick = { mealId ->
                    // After deletion, navigate back to meal list
                    navController.popBackStack()
                }
            )
        }
        
        // Add meal screen
        composable(Screen.AddMeal.route) {
            AddEditMealScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMealSaved = {
                    navController.popBackStack()
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
        ) {
            AddEditMealScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMealSaved = {
                    navController.popBackStack()
                }
            )
        }
        
        // Meal planner screen
        composable(Screen.MealPlanner.route) {
            MealPlannerScreen(
                onMealDetailClick = { mealId ->
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                }
            )
        }
        
        // Shopping list screen
        composable(Screen.ShoppingList.route) {
            ShoppingListScreen(
                onMealDetailClick = { mealId ->
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.ItemHistory.route)
                },
                onNavigateToTemplates = {
                    navController.navigate(Screen.TemplateManager.route)
                },
                onNavigateToSectionEditor = {
                    navController.navigate(Screen.StoreSectionEditor.route)
                },
                onNavigateToShoppingMode = {
                    navController.navigate(Screen.ShoppingMode.route)
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
                    navController.popBackStack()
                },
                onItemClick = { historyItem ->
                    // Add item from history to shopping list
                    // This would need a method in ViewModel
                    navController.popBackStack()
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
                    navController.popBackStack()
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
                    navController.popBackStack()
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
        composable(Screen.ShoppingMode.route) {
            ShoppingModeScreen(
                onExitShoppingMode = {
                    navController.popBackStack()
                }
            )
        }
    }
}
