package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for reordering shopping list items via drag-and-drop.
 * Updates the custom order field to persist user's preferred item arrangement.
 */
class ReorderItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Update the custom order of a shopping list item.
     * @param itemId The ID of the item to reorder
     * @param newPosition The new position/order value
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, newPosition: Int): Result<Unit> {
        return repository.updateItemOrder(itemId, newPosition)
    }
}
