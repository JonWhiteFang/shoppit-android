package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a custom store section configuration for organizing shopping list items.
 * Allows users to customize section order and appearance to match their preferred store layout.
 */
@Immutable
data class StoreSection(
    val id: Long = 0,
    val name: String,
    val displayOrder: Int,
    val isCollapsed: Boolean = false,
    val color: String = "#000000"
)
