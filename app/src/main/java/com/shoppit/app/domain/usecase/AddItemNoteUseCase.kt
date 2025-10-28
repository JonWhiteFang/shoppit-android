package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for adding or updating notes on a shopping list item.
 * Notes allow users to remember specific preferences or details about items.
 */
class AddItemNoteUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Add or update notes for a shopping list item.
     * @param itemId The ID of the item to add notes to
     * @param notes The note text (will be trimmed)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, notes: String): Result<Unit> {
        return repository.updateItemNotes(itemId, notes.trim())
    }
}
