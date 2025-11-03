package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Domain model representing a meal plan assignment.
 * Links a meal to a specific date and meal type for weekly planning.
 * Pure Kotlin data class with no Android dependencies.
 *
 * @property id Unique identifier for the meal plan (0 for new plans)
 * @property mealId The ID of the meal being planned
 * @property date The date for which the meal is planned
 * @property mealType The type of meal (breakfast, lunch, dinner, snack)
 * @property createdAt Timestamp when the meal plan was created
 */
@Immutable
data class MealPlan(
    val id: Long = 0,
    val mealId: Long,
    val date: LocalDate,
    val mealType: MealType,
    val createdAt: Long = System.currentTimeMillis()
)
