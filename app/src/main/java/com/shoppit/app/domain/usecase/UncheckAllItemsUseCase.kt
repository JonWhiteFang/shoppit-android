package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for unchecking all items in the shopping list.
 * Useful for resetting the list for a new shopping trip.
 */
class UncheckAllItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Unchecks all items in the shopping list.
     * 
     * @return Result indicating success or error
     */
    suspend operator fun invoke(): Result<Unit> {
        return repository.uncheckAllItems()
    }
}
