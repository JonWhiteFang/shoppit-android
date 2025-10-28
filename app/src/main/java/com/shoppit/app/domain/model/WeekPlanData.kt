package com.shoppit.app.domain.model

import java.time.LocalDate

/**
 * Domain model representing a week's worth of meal plans.
 * Organizes meal plans by date for weekly calendar view.
 *
 * @property startDate The first day of the week (typically Monday)
 * @property endDate The last day of the week (typically Sunday)
 * @property plansByDate Map of dates to their meal plans with meal details
 */
data class WeekPlanData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val plansByDate: Map<LocalDate, List<MealPlanWithMeal>>
)
