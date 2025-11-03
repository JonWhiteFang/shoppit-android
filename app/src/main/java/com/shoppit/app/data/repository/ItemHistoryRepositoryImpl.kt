package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.ItemHistoryDao
import com.shoppit.app.data.local.entity.ItemHistoryEntity
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.model.ItemHistory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ItemHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of ItemHistoryRepository that manages purchase history using Room database.
 */
class ItemHistoryRepositoryImpl @Inject constructor(
    private val itemHistoryDao: ItemHistoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ItemHistoryRepository {
    
    override fun getRecentHistory(limit: Int): Flow<Result<List<ItemHistory>>> {
        return itemHistoryDao.getRecentHistory(limit)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                emit(Result.failure(PersistenceError.QueryFailed("getRecentHistory", e)))
            }
    }
    
    override fun getFrequentItems(limit: Int): Flow<Result<List<ItemHistory>>> {
        return itemHistoryDao.getFrequentItems(limit)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                emit(Result.failure(PersistenceError.QueryFailed("getFrequentItems", e)))
            }
    }
    
    override suspend fun addToHistory(item: ShoppingListItem): Result<Unit> {
        return try {
            // Check if item already exists in history
            val existingHistory = itemHistoryDao.getHistoryByName(item.name)
            
            if (existingHistory != null) {
                // Update existing history entry
                val updatedHistory = existingHistory.copy(
                    quantity = item.quantity,
                    unit = item.unit,
                    category = item.category.name,
                    purchaseCount = existingHistory.purchaseCount + 1,
                    lastPurchasedAt = System.currentTimeMillis(),
                    averagePrice = calculateAveragePrice(
                        existingHistory.averagePrice,
                        item.estimatedPrice,
                        existingHistory.purchaseCount
                    )
                )
                itemHistoryDao.updateHistory(updatedHistory)
            } else {
                // Create new history entry
                val newHistory = ItemHistoryEntity(
                    itemName = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    category = item.category.name,
                    purchaseCount = 1,
                    lastPurchasedAt = System.currentTimeMillis(),
                    averagePrice = item.estimatedPrice
                )
                itemHistoryDao.insertHistory(newHistory)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("addToHistory", e))
        }
    }
    
    override suspend fun incrementPurchaseCount(itemName: String): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            itemHistoryDao.incrementPurchaseCount(itemName, timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("incrementPurchaseCount", e))
        }
    }
    
    override suspend fun updateAveragePrice(itemName: String, price: Double): Result<Unit> {
        return try {
            val existingHistory = itemHistoryDao.getHistoryByName(itemName)
            
            if (existingHistory != null) {
                val newAveragePrice = calculateAveragePrice(
                    existingHistory.averagePrice,
                    price,
                    existingHistory.purchaseCount
                )
                if (newAveragePrice != null) {
                    itemHistoryDao.updateAveragePrice(itemName, newAveragePrice)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("updateAveragePrice", e))
        }
    }
    
    override suspend fun clearHistory(): Result<Unit> {
        return try {
            itemHistoryDao.clearHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("clearHistory", e))
        }
    }
    
    /**
     * Calculate the new average price based on existing average and new price.
     * Uses incremental average formula: new_avg = old_avg + (new_value - old_avg) / (count + 1)
     */
    private fun calculateAveragePrice(
        currentAverage: Double?,
        newPrice: Double?,
        currentCount: Int
    ): Double? {
        if (newPrice == null) return currentAverage
        if (currentAverage == null) return newPrice
        
        // Incremental average formula
        return currentAverage + (newPrice - currentAverage) / (currentCount + 1)
    }
}
