package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ItemHistory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for quickly adding an item from purchase history to the shopping list.
 * Adds the item with its historical quantity, unit, and average price.
 */
class QuickAddItemUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {
    /**
     * Add an item from history to the shopping list.
     * @param historyItem The item from purchase history to add
     * @return Result containing the new shopping list item ID
     */
    suspend operator fun invoke(historyItem: ItemHistory): Result<Long> {
        val item = ShoppingListItem(
            name = historyItem.itemName,
            quantity = historyItem.quantity,
            unit = historyItem.unit,
            category = historyItem.category,
            isManual = true,
            estimatedPrice = historyItem.averagePrice,
            storeSection = historyItem.category.name
        )
        
        return shoppingListRepository.addShoppingListItem(item)
    }
}
