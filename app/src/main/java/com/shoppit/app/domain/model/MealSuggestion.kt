package com.shoppit.app.domain.model

import java.time.LocalDate

/**
 * Represents a meal suggestion with its relevance score and metadata.
 * Used to provide intelligent meal recommendations based on context and history.
 *
 * @property meal The suggested meal
 * @property score The calculated relevance score (0-300)
 * @property reasons List of human-readable reasons why this meal was suggested
 * @property lastPlannedDate The date this meal was last planned (null if never)
 * @property planCount Number of times this meal was planned in the past 30 days
 */
data class MealSuggestion(
    val meal: Meal,
    val score: Double,
    val reasons: List<String>,
    val lastPlannedDate: LocalDate?,
    val planCount: Int
)
