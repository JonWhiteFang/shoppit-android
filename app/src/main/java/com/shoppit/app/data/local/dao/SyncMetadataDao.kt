package com.shoppit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.shoppit.app.data.local.entity.SyncMetadataEntity
import com.shoppit.app.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync metadata and queue operations.
 * 
 * Provides methods for tracking synchronization state and managing
 * the queue of pending sync operations.
 */
@Dao
interface SyncMetadataDao {
    
    // ========== Sync Metadata Operations ==========
    
    /**
     * Gets sync metadata for a specific entity.
     * @param type Entity type ("meal", "meal_plan", "shopping_list_item")
     * @param id Local entity ID
     * @return Sync metadata or null if not found
     */
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :type AND entity_id = :id")
    suspend fun getMetadata(type: String, id: Long): SyncMetadataEntity?
    
    /**
     * Inserts or replaces sync metadata.
     * @param metadata Sync metadata to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadataEntity)
    
    /**
     * Updates existing sync metadata.
     * @param metadata Sync metadata to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateMetadata(metadata: SyncMetadataEntity): Int
    
    /**
     * Observes all pending sync operations.
     * @return Flow of sync metadata with "pending" status
     */
    @Query("SELECT * FROM sync_metadata WHERE sync_status = 'pending'")
    fun observePendingSync(): Flow<List<SyncMetadataEntity>>
    
    /**
     * Gets all sync metadata with a specific status.
     * @param status Sync status to filter by
     * @return List of sync metadata
     */
    @Query("SELECT * FROM sync_metadata WHERE sync_status = :status")
    suspend fun getMetadataByStatus(status: String): List<SyncMetadataEntity>
    
    /**
     * Gets all sync metadata for a specific entity type.
     * @param type Entity type to filter by
     * @return List of sync metadata
     */
    @Query("SELECT * FROM sync_metadata WHERE entity_type = :type")
    suspend fun getMetadataByType(type: String): List<SyncMetadataEntity>
    
    /**
     * Deletes sync metadata for a specific entity.
     * @param type Entity type
     * @param id Local entity ID
     */
    @Query("DELETE FROM sync_metadata WHERE entity_type = :type AND entity_id = :id")
    suspend fun deleteMetadata(type: String, id: Long)
    
    /**
     * Deletes all sync metadata.
     */
    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAllMetadata()
    
    // ========== Sync Queue Operations ==========
    
    /**
     * Gets all queued changes ordered by creation time (FIFO).
     * @return List of queued sync operations
     */
    @Query("SELECT * FROM sync_queue ORDER BY created_at ASC")
    suspend fun getAllQueuedChanges(): List<SyncQueueEntity>
    
    /**
     * Gets queued changes for a specific entity type.
     * @param type Entity type to filter by
     * @return List of queued sync operations
     */
    @Query("SELECT * FROM sync_queue WHERE entity_type = :type ORDER BY created_at ASC")
    suspend fun getQueuedChangesByType(type: String): List<SyncQueueEntity>
    
    /**
     * Gets a specific queued change by entity.
     * @param type Entity type
     * @param id Local entity ID
     * @return Queued sync operation or null if not found
     */
    @Query("SELECT * FROM sync_queue WHERE entity_type = :type AND entity_id = :id LIMIT 1")
    suspend fun getQueuedChange(type: String, id: Long): SyncQueueEntity?
    
    /**
     * Inserts a new sync operation into the queue.
     * @param change Sync queue entity to insert
     * @return Row ID of inserted entity
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun queueChange(change: SyncQueueEntity): Long
    
    /**
     * Updates an existing queued change (e.g., to increment retry count).
     * @param change Sync queue entity to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateQueuedChange(change: SyncQueueEntity): Int
    
    /**
     * Removes a change from the sync queue.
     * @param change Sync queue entity to remove
     */
    @Delete
    suspend fun removeFromQueue(change: SyncQueueEntity)
    
    /**
     * Removes a queued change by entity.
     * @param type Entity type
     * @param id Local entity ID
     */
    @Query("DELETE FROM sync_queue WHERE entity_type = :type AND entity_id = :id")
    suspend fun removeQueuedChange(type: String, id: Long)
    
    /**
     * Observes the count of pending changes in the queue.
     * @return Flow of pending change count
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    fun observePendingChangeCount(): Flow<Int>
    
    /**
     * Gets the count of pending changes in the queue.
     * @return Number of pending changes
     */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingChangeCount(): Int
    
    /**
     * Deletes all queued changes.
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearQueue()
}
