package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ItemHistoryRepository
import javax.inject.Inject

/**
 * Use case for updating item purchase history when items are checked off.
 * Tracks purchase frequency and average prices for quick re-adding.
 */
class UpdateItemHistoryUseCase @Inject constructor(
    private val itemHistoryRepository: ItemHistoryRepository
) {
    /**
     * Add a checked shopping list item to purchase history.
     * If the item already exists in history, increments purchase count and updates timestamp.
     * @param item The shopping list item that was checked off
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(item: ShoppingListItem): Result<Unit> {
        return itemHistoryRepository.addToHistory(item)
    }
}
