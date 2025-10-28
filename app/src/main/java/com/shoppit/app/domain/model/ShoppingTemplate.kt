package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a saved shopping list template for recurring shopping trips.
 */
@Immutable
data class ShoppingTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val items: List<TemplateItem>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)

/**
 * Represents an item within a shopping template.
 */
@Immutable
data class TemplateItem(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val notes: String = ""
)
