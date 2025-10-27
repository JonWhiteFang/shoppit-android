package com.shoppit.app.domain.model

/**
 * Domain model representing a meal with its ingredients.
 * Pure Kotlin data class with no Android dependencies.
 *
 * @property id Unique identifier for the meal (0 for new meals)
 * @property name The name of the meal (required)
 * @property ingredients List of ingredients in the meal
 * @property notes Optional notes or instructions for the meal
 * @property createdAt Timestamp when the meal was created
 * @property updatedAt Timestamp when the meal was last updated
 */
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
