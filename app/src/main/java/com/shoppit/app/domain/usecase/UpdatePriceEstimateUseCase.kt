package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ItemHistoryRepository
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for updating the estimated price of a shopping list item.
 * Also updates the average price in item history for future reference.
 */
class UpdatePriceEstimateUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val itemHistoryRepository: ItemHistoryRepository
) {
    /**
     * Update the estimated price for a shopping list item.
     * If a price is provided, also updates the average price in history.
     * @param itemId The ID of the item to update
     * @param price The new price estimate (null to remove price)
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, price: Double?): Result<Unit> {
        return try {
            shoppingListRepository.getShoppingListItem(itemId).first().fold(
                onSuccess = { item ->
                    shoppingListRepository.updateItemPrice(itemId, price).also { result ->
                        // Update history average price if successful and price is not null
                        if (result.isSuccess && price != null) {
                            itemHistoryRepository.updateAveragePrice(item.name, price)
                        }
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
