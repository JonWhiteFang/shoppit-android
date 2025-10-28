package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents the purchase history of a shopping list item.
 * Used for quick re-adding frequently purchased items.
 */
@Immutable
data class ItemHistory(
    val id: Long = 0,
    val itemName: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val purchaseCount: Int = 1,
    val lastPurchasedAt: Long = System.currentTimeMillis(),
    val averagePrice: Double? = null
)
