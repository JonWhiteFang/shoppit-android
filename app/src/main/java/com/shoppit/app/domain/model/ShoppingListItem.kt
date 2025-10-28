package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ShoppingListItem(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val isChecked: Boolean = false,
    val isManual: Boolean = false,
    val mealIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    
    // Management features
    val notes: String = "",
    val isPriority: Boolean = false,
    val customOrder: Int = 0,
    val estimatedPrice: Double? = null,
    val storeSection: String = category.name,
    val lastModifiedAt: Long = System.currentTimeMillis()
)
