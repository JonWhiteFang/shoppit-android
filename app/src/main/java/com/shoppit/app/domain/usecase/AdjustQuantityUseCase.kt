package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for adjusting shopping list item quantities with increment/decrement.
 * Only works with numeric quantities, validates and ensures minimum value of 1.
 */
class AdjustQuantityUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Adjust the quantity of a shopping list item.
     * @param itemId The ID of the item to adjust
     * @param increment True to increment, false to decrement
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, increment: Boolean): Result<Unit> {
        return try {
            repository.getShoppingListItem(itemId).first().fold(
                onSuccess = { item ->
                    val currentQuantity = item.quantity.toIntOrNull()
                        ?: return Result.failure(
                            AppError.ValidationError("Cannot adjust non-numeric quantity")
                        )
                    
                    val newQuantity = if (increment) {
                        currentQuantity + 1
                    } else {
                        maxOf(1, currentQuantity - 1)
                    }
                    
                    val updatedItem = item.copy(
                        quantity = newQuantity.toString(),
                        lastModifiedAt = System.currentTimeMillis()
                    )
                    repository.updateShoppingListItem(updatedItem)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError("Failed to adjust quantity: ${e.message}"))
        }
    }
}
