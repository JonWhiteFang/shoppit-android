package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import javax.inject.Inject

/**
 * Use case for searching meals by name or ingredient names.
 * Performs case-insensitive search and returns filtered list of meals.
 *
 * Requirements:
 * - 1.1: Filter meals by name containing search text
 * - 1.2: Filter meals by ingredient names containing search text
 * - 1.3: Perform case-insensitive matching
 * - 1.4: Return empty list when no matches found
 * - 1.5: Update results in real-time as query changes
 * - 2.1: Match search query against ingredient names
 * - 2.2: Match against all ingredients within each meal
 * - 2.3: Perform case-insensitive matching for ingredients
 * - 2.4: Display meal once even if multiple ingredients match
 * - 2.5: Display meals matching by name or ingredient
 */
class SearchMealsUseCase @Inject constructor() {
    /**
     * Filters meals based on search query.
     * Returns all meals if query is blank.
     * Searches both meal names and ingredient names (case-insensitive).
     *
     * @param meals List of meals to search through
     * @param query Search query text
     * @return Filtered list of meals matching the query
     */
    operator fun invoke(meals: List<Meal>, query: String): List<Meal> {
        // Return all meals if query is empty or blank
        if (query.isBlank()) return meals
        
        val normalizedQuery = query.trim().lowercase()
        
        return meals.filter { meal ->
            // Search by meal name (case-insensitive)
            meal.name.lowercase().contains(normalizedQuery) ||
            // Search by ingredient names (case-insensitive)
            meal.ingredients.any { ingredient ->
                ingredient.name.lowercase().contains(normalizedQuery)
            }
        }
    }
}
