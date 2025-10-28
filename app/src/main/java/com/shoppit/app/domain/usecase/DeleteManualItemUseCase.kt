package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for deleting a manual shopping list item.
 * Only manual items can be deleted; auto-generated items cannot be removed individually.
 */
class DeleteManualItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Deletes a manual shopping list item.
     * 
     * @param itemId The ID of the item to delete
     * @return Result indicating success or error
     */
    suspend operator fun invoke(itemId: Long): Result<Unit> {
        return try {
            val result = repository.getShoppingListItem(itemId).first()
            if (result.isFailure) {
                return result.map { }
            }
            val item = result.getOrThrow()
            
            if (!item.isManual) {
                return Result.failure(
                    IllegalStateException("Cannot delete auto-generated items")
                )
            }
            
            repository.deleteShoppingListItem(itemId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
