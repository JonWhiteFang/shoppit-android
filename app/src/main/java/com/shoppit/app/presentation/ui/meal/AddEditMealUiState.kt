package com.shoppit.app.presentation.ui.meal

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal

/**
 * UI state for the add/edit meal screen.
 * Contains the meal being edited, loading flags, and error information.
 *
 * @property meal The meal being created or edited
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
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
