package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a summary of budget tracking for the shopping list.
 * Provides estimated costs and remaining budget information.
 */
@Immutable
data class BudgetSummary(
    val totalEstimated: Double,
    val checkedTotal: Double,
    val remainingBudget: Double,
    val itemsWithPrices: Int,
    val totalItems: Int
)
