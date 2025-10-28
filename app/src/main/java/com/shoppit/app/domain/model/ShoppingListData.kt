package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ShoppingListData(
    val itemsByCategory: Map<ItemCategory, List<ShoppingListItem>>,
    val totalItems: Int,
    val checkedItems: Int
)
