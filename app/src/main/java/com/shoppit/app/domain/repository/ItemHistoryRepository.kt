package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.ItemHistory
import com.shoppit.app.domain.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing shopping item purchase history.
 * Provides access to historical purchase data for quick re-adding and analytics.
 */
interface ItemHistoryRepository {
    
    /**
     * Get recent purchase history ordered by last purchased date.
     * @param limit Maximum number of items to return (default 100)
     * @return Flow of Result containing list of ItemHistory
     */
    fun getRecentHistory(limit: Int = 100): Flow<Result<List<ItemHistory>>>
    
    /**
     * Get frequently purchased items ordered by purchase count.
     * @param limit Maximum number of items to return (default 20)
     * @return Flow of Result containing list of ItemHistory
     */
    fun getFrequentItems(limit: Int = 20): Flow<Result<List<ItemHistory>>>
    
    /**
     * Add a shopping list item to purchase history.
     * If item already exists in history, updates the purchase count and timestamp.
     * @param item The shopping list item to add to history
     * @return Result containing the history entry ID
     */
    suspend fun addToHistory(item: ShoppingListItem): Result<Unit>
    
    /**
     * Increment the purchase count for an item by name.
     * @param itemName Name of the item to increment
     * @return Result indicating success or failure
     */
    suspend fun incrementPurchaseCount(itemName: String): Result<Unit>
    
    /**
     * Update the average price for an item in history.
     * @param itemName Name of the item
     * @param price New price to factor into average
     * @return Result indicating success or failure
     */
    suspend fun updateAveragePrice(itemName: String, price: Double): Result<Unit>
    
    /**
     * Clear all purchase history.
     * @return Result indicating success or failure
     */
    suspend fun clearHistory(): Result<Unit>
}
