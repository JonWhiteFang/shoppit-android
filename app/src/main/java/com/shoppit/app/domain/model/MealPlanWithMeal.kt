package com.shoppit.app.domain.model

/**
 * Domain model combining a meal plan with its associated meal details.
 * Used for displaying meal plans with full meal information in the UI.
 *
 * @property mealPlan The meal plan assignment
 * @property meal The full meal details including ingredients
 */
data class MealPlanWithMeal(
    val mealPlan: MealPlan,
    val meal: Meal
)
