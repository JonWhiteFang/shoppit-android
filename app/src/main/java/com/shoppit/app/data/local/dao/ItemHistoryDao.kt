package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppit.app.data.local.entity.ItemHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemHistoryDao {
    
    @Query("SELECT * FROM item_history ORDER BY last_purchased_at DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<ItemHistoryEntity>>
    
    @Query("SELECT * FROM item_history ORDER BY purchase_count DESC LIMIT :limit")
    fun getFrequentItems(limit: Int = 20): Flow<List<ItemHistoryEntity>>
    
    @Query("SELECT * FROM item_history WHERE item_name = :itemName")
    suspend fun getHistoryByName(itemName: String): ItemHistoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ItemHistoryEntity): Long
    
    @Update
    suspend fun updateHistory(history: ItemHistoryEntity)
    
    @Query("UPDATE item_history SET purchase_count = purchase_count + 1, last_purchased_at = :timestamp WHERE item_name = :itemName")
    suspend fun incrementPurchaseCount(itemName: String, timestamp: Long)
    
    @Query("UPDATE item_history SET average_price = :price WHERE item_name = :itemName")
    suspend fun updateAveragePrice(itemName: String, price: Double)
    
    @Query("DELETE FROM item_history")
    suspend fun clearHistory()
    
    @Query("DELETE FROM item_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)
}
