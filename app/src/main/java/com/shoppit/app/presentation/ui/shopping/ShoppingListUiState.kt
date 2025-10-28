package com.shoppit.app.presentation.ui.shopping

import com.shoppit.app.domain.model.IngredientSource
import com.shoppit.app.domain.model.ShoppingListData
import com.shoppit.app.domain.model.ShoppingListItem

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
    val searchQuery: String = ""
)
