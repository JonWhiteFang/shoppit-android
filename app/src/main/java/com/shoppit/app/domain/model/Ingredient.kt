package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model representing an ingredient in a meal.
 * Pure Kotlin data class with no Android dependencies.
 *
 * @property name The name of the ingredient (required)
 * @property quantity The quantity amount (optional, e.g., "2", "1.5")
 * @property unit The unit of measurement (optional, e.g., "cups", "grams", "pieces")
 */
@Immutable
data class Ingredient(
    val name: String,
    val quantity: String = "",
    val unit: String = ""
)
