package com.shoppit.app.domain.model

import java.time.LocalDate

/**
 * Historical data about a meal's planning patterns.
 * Used to calculate frequency and recency penalties in suggestion scoring.
 *
 * @property mealId The meal ID
 * @property lastPlannedDate The most recent date this meal was planned
 * @property planCount Number of times planned in the analysis period (typically 30 days)
 * @property planDates List of dates when this meal was planned
 */
data class MealPlanHistory(
    val mealId: Long,
    val lastPlannedDate: LocalDate?,
    val planCount: Int,
    val planDates: List<LocalDate>
)
