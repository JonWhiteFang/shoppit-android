package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for toggling the checked status of a shopping list item.
 */
class ToggleItemCheckedUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Toggles the checked status of a shopping list item.
     * 
     * @param itemId The ID of the item to update
     * @param isChecked The new checked status
     * @return Result indicating success or error
     */
    suspend operator fun invoke(itemId: Long, isChecked: Boolean): Result<Unit> {
        return try {
            val result = repository.getShoppingListItem(itemId).first()
            if (result.isFailure) {
                return result.map { }
            }
            val item = result.getOrThrow()
            val updatedItem = item.copy(isChecked = isChecked)
            repository.updateShoppingListItem(updatedItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
