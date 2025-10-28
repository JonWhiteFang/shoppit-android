package com.shoppit.app.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shoppit.app.presentation.ui.meal.AddEditMealScreen
import com.shoppit.app.presentation.ui.meal.MealDetailScreen
import com.shoppit.app.presentation.ui.meal.MealListScreen
import com.shoppit.app.presentation.ui.planner.MealPlannerScreen

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
        
        // Future navigation destinations will be added here:
        // - Shopping list screen
    }
}
