package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents user preferences for shopping mode.
 * Shopping mode provides a simplified interface optimized for use while actively shopping.
 */
@Immutable
data class ShoppingModePreferences(
    val isEnabled: Boolean = false,
    val hideCheckedItems: Boolean = true,
    val increasedTextSize: Boolean = true,
    val showOnlyEssentials: Boolean = true
)
