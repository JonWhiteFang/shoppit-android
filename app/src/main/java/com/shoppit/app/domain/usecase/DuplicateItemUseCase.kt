package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for duplicating a shopping list item.
 * Creates a copy of an item with "(copy)" appended to the name for easy editing.
 */
class DuplicateItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Duplicate a shopping list item.
     * The duplicated item will have "(copy)" appended to its name and be marked as manual.
     * @param itemId The ID of the item to duplicate
     * @return Result containing the new item's ID
     */
    suspend operator fun invoke(itemId: Long): Result<Long> {
        return repository.duplicateItem(itemId)
    }
}
