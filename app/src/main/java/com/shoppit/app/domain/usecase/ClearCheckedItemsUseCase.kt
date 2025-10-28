package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for clearing all checked items from the shopping list.
 * Useful for removing items that have been purchased.
 */
class ClearCheckedItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Deletes all checked items from the shopping list.
     * 
     * @return Result indicating success or error
     */
    suspend operator fun invoke(): Result<Unit> {
        return repository.deleteCheckedItems()
    }
}
