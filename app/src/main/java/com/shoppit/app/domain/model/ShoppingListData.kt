package com.shoppit.app.domain.model

data class ShoppingListData(
    val itemsByCategory: Map<ItemCategory, List<ShoppingListItem>>,
    val totalItems: Int,
    val checkedItems: Int
)
