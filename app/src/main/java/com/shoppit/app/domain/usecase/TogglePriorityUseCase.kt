package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for toggling the priority status of a shopping list item.
 * Priority items are displayed at the top of their section to highlight importance.
 */
class TogglePriorityUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Toggle the priority status of a shopping list item.
     * @param itemId The ID of the item to toggle
     * @param isPriority The new priority status
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, isPriority: Boolean): Result<Unit> {
        return repository.toggleItemPriority(itemId, isPriority)
    }
}
