package com.shoppit.app.presentation.ui.shopping

import com.shoppit.app.domain.model.IngredientSource
import com.shoppit.app.domain.model.ShoppingListData
import com.shoppit.app.domain.model.ShoppingListItem

/**
 * Sealed class representing different confirmation actions.
 */
sealed class ConfirmationAction {
    data object ClearChecked : ConfirmationAction()
    data object UncheckAll : ConfirmationAction()
    data class DeleteManualItem(val itemId: Long) : ConfirmationAction()
}

/**
 * UI state for the shopping list screen.
 * Contains all state needed to render the shopping list UI.
 */
data class ShoppingListUiState(
    val shoppingListData: ShoppingListData? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val showAddItemDialog: Boolean = false,
    val showItemDetail: ShoppingListItem? = null,
    val itemSources: List<IngredientSource> = emptyList(),
    val filterUncheckedOnly: Boolean = false,
    val searchQuery: String = "",
    val shareText: String? = null,
    val confirmationAction: ConfirmationAction? = null
)
