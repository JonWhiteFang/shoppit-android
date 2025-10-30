package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing queued sync operations for offline changes.
 * 
 * This entity maintains a queue of pending changes that need to be synchronized
 * with the cloud backend when network connectivity is available.
 * 
 * Indices are created for:
 * - created_at: For processing queue in FIFO order
 * - (entity_type, entity_id): For efficient lookup and deduplication
 * 
 * @property id Unique identifier for the queued operation (auto-generated)
 * @property entityType Type of entity being synced ("meal", "meal_plan", "shopping_list_item")
 * @property entityId Local database ID of the entity
 * @property operation Type of operation ("create", "update", "delete")
 * @property payload JSON serialized entity data
 * @property createdAt Timestamp when the operation was queued
 * @property retryCount Number of sync retry attempts
 * @property lastAttemptAt Timestamp of last sync attempt (null if never attempted)
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["entity_type", "entity_id"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long,
    
    @ColumnInfo(name = "operation")
    val operation: String, // "create", "update", "delete"
    
    @ColumnInfo(name = "payload")
    val payload: String, // JSON serialized entity
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null
)
