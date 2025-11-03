package com.shoppit.app.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.model.ItemCategory

/**
 * Optimized derived state computations for performance (Task 7.2).
 * Uses derivedStateOf to minimize recompositions when computing values from state.
 * 
 * Requirements: 6.2, 6.5
 */

/**
 * Computes filtered and sorted meals using derivedStateOf.
 * Only recomputes when meals, searchQuery, or selectedTags change.
 * 
 * @param meals List of all meals
 * @param searchQuery Current search query
 * @param selectedTags Currently selected filter tags
 * @return Filtered and sorted list of meals
 */
@Composable
fun rememberFilteredMeals(
    meals: List<Meal>,
    searchQuery: String,
    selectedTags: Set<com.shoppit.app.domain.model.MealTag>
): List<Meal> {
    return remember(meals, searchQuery, selectedTags) {
        derivedStateOf {
            var filtered = meals
            
            // Apply search filter
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter { meal ->
                    meal.name.contains(searchQuery, ignoreCase = true)
                }
            }
            
            // Apply tag filter
            if (selectedTags.isNotEmpty()) {
                filtered = filtered.filter { meal ->
                    meal.tags.any { it in selectedTags }
                }
            }
            
            // Sort alphabetically
            filtered.sortedBy { it.name.lowercase() }
        }
    }.value
}

/**
 * Computes meal count statistics using derivedStateOf.
 * Only recomputes when meals list changes.
 * 
 * @param meals List of meals
 * @return Pair of (total count, filtered count)
 */
@Composable
fun rememberMealCounts(
    meals: List<Meal>,
    filteredMeals: List<Meal>
): Pair<Int, Int> {
    return remember(meals, filteredMeals) {
        derivedStateOf {
            Pair(meals.size, filteredMeals.size)
        }
    }.value
}

/**
 * Computes shopping list statistics using derivedStateOf.
 * Only recomputes when items change.
 * 
 * @param items List of shopping list items
 * @return Triple of (total count, checked count, unchecked count)
 */
@Composable
fun rememberShoppingListStats(
    items: List<ShoppingListItem>
): Triple<Int, Int, Int> {
    return remember(items) {
        derivedStateOf {
            val total = items.size
            val checked = items.count { it.isChecked }
            val unchecked = total - checked
            Triple(total, checked, unchecked)
        }
    }.value
}

/**
 * Computes filtered shopping list items using derivedStateOf.
 * Only recomputes when items or filter state changes.
 * 
 * @param items List of shopping list items
 * @param filterUncheckedOnly Whether to show only unchecked items
 * @param searchQuery Current search query
 * @return Filtered list of items
 */
@Composable
fun rememberFilteredShoppingItems(
    items: List<ShoppingListItem>,
    filterUncheckedOnly: Boolean,
    searchQuery: String
): List<ShoppingListItem> {
    return remember(items, filterUncheckedOnly, searchQuery) {
        derivedStateOf {
            var filtered = items
            
            // Apply checked filter
            if (filterUncheckedOnly) {
                filtered = filtered.filter { !it.isChecked }
            }
            
            // Apply search filter
            if (searchQuery.isNotBlank()) {
                filtered = filtered.filter { item ->
                    item.name.contains(searchQuery, ignoreCase = true)
                }
            }
            
            filtered
        }
    }.value
}

/**
 * Computes items grouped by category using derivedStateOf.
 * Only recomputes when items change.
 * 
 * @param items List of shopping list items
 * @return Map of category to items
 */
@Composable
fun rememberItemsByCategory(
    items: List<ShoppingListItem>
): Map<ItemCategory, List<ShoppingListItem>> {
    return remember(items) {
        derivedStateOf {
            items.groupBy { it.category }
                .toSortedMap(compareBy { it.ordinal })
        }
    }.value
}

/**
 * Computes unchecked count for a category using derivedStateOf.
 * Only recomputes when items change.
 * 
 * @param items List of items in the category
 * @return Count of unchecked items
 */
@Composable
fun rememberUncheckedCount(
    items: List<ShoppingListItem>
): Int {
    return remember(items) {
        derivedStateOf {
            items.count { !it.isChecked }
        }
    }.value
}

/**
 * Computes shopping list progress percentage using derivedStateOf.
 * Only recomputes when total or checked count changes.
 * 
 * @param totalItems Total number of items
 * @param checkedItems Number of checked items
 * @return Progress percentage (0-100)
 */
@Composable
fun rememberProgressPercentage(
    totalItems: Int,
    checkedItems: Int
): Int {
    return remember(totalItems, checkedItems) {
        derivedStateOf {
            if (totalItems > 0) {
                (checkedItems * 100) / totalItems
            } else {
                0
            }
        }
    }.value
}

/**
 * Computes whether filters are active using derivedStateOf.
 * Only recomputes when search query or selected tags change.
 * 
 * @param searchQuery Current search query
 * @param selectedTags Currently selected filter tags
 * @return True if any filters are active
 */
@Composable
fun rememberIsFiltered(
    searchQuery: String,
    selectedTags: Set<com.shoppit.app.domain.model.MealTag>
): Boolean {
    return remember(searchQuery, selectedTags) {
        derivedStateOf {
            searchQuery.isNotBlank() || selectedTags.isNotEmpty()
        }
    }.value
}

/**
 * Computes sorted meal plans using derivedStateOf.
 * Only recomputes when meal plans change.
 * 
 * @param mealPlans List of meal plans
 * @return Sorted list of meal plans by date and meal type
 */
@Composable
fun rememberSortedMealPlans(
    mealPlans: List<com.shoppit.app.domain.model.MealPlan>
): List<com.shoppit.app.domain.model.MealPlan> {
    return remember(mealPlans) {
        derivedStateOf {
            mealPlans.sortedWith(
                compareBy<com.shoppit.app.domain.model.MealPlan> { it.date }
                    .thenBy { it.mealType.ordinal }
            )
        }
    }.value
}

/**
 * Computes meal plans grouped by date using derivedStateOf.
 * Only recomputes when meal plans change.
 * 
 * @param mealPlans List of meal plans
 * @return Map of date to meal plans
 */
@Composable
fun rememberMealPlansByDate(
    mealPlans: List<com.shoppit.app.domain.model.MealPlan>
): Map<java.time.LocalDate, List<com.shoppit.app.domain.model.MealPlan>> {
    return remember(mealPlans) {
        derivedStateOf {
            mealPlans.groupBy { it.date }
                .toSortedMap()
        }
    }.value
}

/**
 * Computes ingredient count for a meal using derivedStateOf.
 * Only recomputes when meal changes.
 * 
 * @param meal The meal
 * @return Number of ingredients
 */
@Composable
fun rememberIngredientCount(
    meal: Meal
): Int {
    return remember(meal) {
        derivedStateOf {
            meal.ingredients.size
        }
    }.value
}

/**
 * Computes total ingredient count for multiple meals using derivedStateOf.
 * Only recomputes when meals change.
 * 
 * @param meals List of meals
 * @return Total number of ingredients across all meals
 */
@Composable
fun rememberTotalIngredientCount(
    meals: List<Meal>
): Int {
    return remember(meals) {
        derivedStateOf {
            meals.sumOf { it.ingredients.size }
        }
    }.value
}
