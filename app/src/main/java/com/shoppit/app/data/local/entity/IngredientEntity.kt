package com.shoppit.app.data.local.entity

/**
 * Entity representing an ingredient as part of a meal.
 * This is an embedded data class stored as part of MealEntity.
 *
 * @property name The name of the ingredient (required)
 * @property quantity The quantity amount (optional, e.g., "2", "1.5")
 * @property unit The unit of measurement (optional, e.g., "cups", "grams", "pieces")
 */
data class IngredientEntity(
    val name: String,
    val quantity: String,
    val unit: String
)
