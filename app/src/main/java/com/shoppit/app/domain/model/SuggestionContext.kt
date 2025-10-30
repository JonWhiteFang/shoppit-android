package com.shoppit.app.domain.model

import java.time.LocalDate

/**
 * Context information for generating meal suggestions.
 * Provides all necessary information to filter and rank meal suggestions appropriately.
 *
 * @property targetDate The date for which suggestions are requested
 * @property targetMealType The meal type (breakfast, lunch, dinner, snack)
 * @property selectedTags User-selected tag filters
 * @property searchQuery Optional search text to filter by meal name
 * @property existingPlanIds IDs of meals already planned for the week (to avoid duplicates)
 */
data class SuggestionContext(
    val targetDate: LocalDate,
    val targetMealType: MealType,
    val selectedTags: Set<MealTag> = emptySet(),
    val searchQuery: String = "",
    val existingPlanIds: Set<Long> = emptySet()
)
