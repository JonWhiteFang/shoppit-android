package com.shoppit.app.domain.model

/**
 * Enum representing predefined meal tags for categorization and filtering.
 * Tags can be used to classify meals by meal type, dietary preferences, or other characteristics.
 *
 * @property displayName Human-readable name for the tag
 */
enum class MealTag(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    GLUTEN_FREE("Gluten Free"),
    DAIRY_FREE("Dairy Free"),
    QUICK("Quick (<30 min)"),
    HEALTHY("Healthy"),
    COMFORT_FOOD("Comfort Food"),
    DESSERT("Dessert")
}
