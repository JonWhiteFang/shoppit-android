package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for updating a manual shopping list item.
 * Only manual items can be edited; auto-generated items cannot be modified.
 */
class UpdateManualItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Updates a manual shopping list item.
     * 
     * @param itemId The ID of the item to update
     * @param name The new item name (required)
     * @param quantity The new quantity amount
     * @param unit The new unit of measurement
     * @return Result indicating success or error
     */
    suspend operator fun invoke(
        itemId: Long,
        name: String,
        quantity: String,
        unit: String
    ): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Item name cannot be empty"))
        }
        
        return try {
            val result = repository.getShoppingListItem(itemId).first()
            if (result.isFailure) {
                return result.map { }
            }
            val item = result.getOrThrow()
            
            if (!item.isManual) {
                return Result.failure(
                    IllegalStateException("Cannot edit auto-generated items")
                )
            }
            
            val updatedItem = item.copy(
                name = name.trim(),
                quantity = quantity.trim(),
                unit = unit.trim()
            )
            repository.updateShoppingListItem(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
