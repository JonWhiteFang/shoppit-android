package com.shoppit.app.domain.model

/**
 * Enumeration of meal types for meal planning.
 * Represents the different meal categories that can be planned throughout the day.
 */
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    /**
     * Returns a human-readable display name for the meal type.
     *
     * @return The formatted display name
     */
    fun displayName(): String = when (this) {
        BREAKFAST -> "Breakfast"
        LUNCH -> "Lunch"
        DINNER -> "Dinner"
        SNACK -> "Snack"
    }
}
