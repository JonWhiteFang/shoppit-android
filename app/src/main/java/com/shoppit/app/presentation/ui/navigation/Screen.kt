package com.shoppit.app.presentation.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app.
 * Each screen has a route string used for navigation.
 */
sealed class Screen(val route: String) {
    /**
     * Meal list screen - displays all saved meals
     */
    data object MealList : Screen("meal_list")
    
    /**
     * Meal detail screen - displays detailed information about a specific meal
     * Route includes mealId parameter
     */
    data object MealDetail : Screen("meal_detail/{mealId}") {
        /**
         * Creates a route with the specified meal ID
         * @param mealId The ID of the meal to display
         * @return The complete route string
         */
        fun createRoute(mealId: Long): String = "meal_detail/$mealId"
    }
    
    /**
     * Add meal screen - form for creating a new meal
     */
    data object AddMeal : Screen("add_meal")
    
    /**
     * Edit meal screen - form for editing an existing meal
     * Route includes mealId parameter
     */
    data object EditMeal : Screen("edit_meal/{mealId}") {
        /**
         * Creates a route with the specified meal ID
         * @param mealId The ID of the meal to edit
         * @return The complete route string
         */
        fun createRoute(mealId: Long): String = "edit_meal/$mealId"
    }
    
    /**
     * Future screens will be added here as features are implemented:
     * - Meal planner screen
     * - Shopping list screen
     */
}
