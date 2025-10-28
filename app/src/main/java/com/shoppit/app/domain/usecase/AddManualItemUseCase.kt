package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for adding a manual shopping list item.
 * Manual items are user-created and not auto-generated from meal plans.
 */
class AddManualItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Adds a manual shopping list item.
     * 
     * @param name The item name (required)
     * @param quantity The quantity amount
     * @param unit The unit of measurement
     * @return Result with the ID of the newly created item or error
     */
    suspend operator fun invoke(
        name: String,
        quantity: String,
        unit: String
    ): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Item name cannot be empty"))
        }
        
        val item = ShoppingListItem(
            name = name.trim(),
            quantity = quantity.trim(),
            unit = unit.trim(),
            category = ItemCategory.OTHER,
            isManual = true
        )
        
        return repository.addShoppingListItem(item)
    }
}
