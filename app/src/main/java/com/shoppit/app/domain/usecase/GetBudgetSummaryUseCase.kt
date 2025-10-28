package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.BudgetSummary
import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for calculating and retrieving the shopping list budget summary.
 * Provides total estimated cost, checked items total, and remaining budget.
 */
class GetBudgetSummaryUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Get the budget summary for the current shopping list.
     * @return Result containing BudgetSummary with cost calculations
     */
    suspend operator fun invoke(): Result<BudgetSummary> {
        return repository.getBudgetSummary()
    }
}
