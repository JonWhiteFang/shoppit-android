package com.shoppit.app.presentation.ui.meal

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal

/**
 * UI state for the add/edit meal screen.
 * Contains the meal being edited, loading flags, and error information.
 *
 * Requirements:
 * - 5.3: Track unsaved changes for confirmation dialog
 *
 * @property meal The meal being created or edited
 * @property originalMeal The original meal data (for tracking changes)
 * @property isLoading True when initial meal data is being loaded (edit mode)
 * @property isSaving True when the meal is being saved
 * @property error General error message for the form
 * @property validationErrors Map of field names to validation error messages
 */
data class AddEditMealUiState(
    val meal: Meal = Meal(
        name = "",
        ingredients = emptyList()
    ),
    val originalMeal: Meal? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
) {
    /**
     * Determines if there are unsaved changes.
     * Requirement 5.3: Track unsaved changes for confirmation dialog
     *
     * @return true if the current meal differs from the original meal
     */
    fun hasUnsavedChanges(): Boolean {
        // No unsaved changes if we're loading or saving
        if (isLoading || isSaving) return false
        
        // For new meals, check if any data has been entered
        if (originalMeal == null) {
            return meal.name.isNotBlank() || meal.ingredients.isNotEmpty() || meal.notes.isNotBlank()
        }
        
        // For existing meals, compare with original
        return meal != originalMeal
    }
}
