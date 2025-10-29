package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import javax.inject.Inject

/**
 * Use case for filtering meals by selected tags.
 * Supports multiple tag selection with AND logic (meal must have all selected tags).
 *
 * Requirements:
 * - 3.1: Display only meals with corresponding tag when filter selected
 * - 3.2: Remove filter and update results when filter deselected
 * - 3.3: Support multiple simultaneous active filters
 * - 3.4: Display meals matching all selected filters (AND logic)
 * - 3.5: Combine with search to display meals matching both criteria
 */
class FilterMealsByTagsUseCase @Inject constructor() {
    /**
     * Filters meals based on selected tags.
     * Returns all meals if no tags are selected.
     * Uses AND logic: meal must have ALL selected tags to be included.
     *
     * @param meals List of meals to filter
     * @param tags Set of tags to filter by
     * @return Filtered list of meals containing all selected tags
     */
    operator fun invoke(meals: List<Meal>, tags: Set<MealTag>): List<Meal> {
        // Return all meals if no tags are selected
        if (tags.isEmpty()) return meals
        
        return meals.filter { meal ->
            // Meal must have ALL selected tags (AND logic)
            tags.all { tag -> meal.tags.contains(tag) }
        }
    }
}
