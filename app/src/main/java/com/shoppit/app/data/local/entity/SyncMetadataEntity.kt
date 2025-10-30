package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing sync metadata for tracking synchronization state of entities.
 * 
 * This entity tracks the sync status of meals, meal plans, and shopping list items,
 * enabling offline-first architecture with cloud synchronization.
 * 
 * Indices are created for:
 * - (entity_type, entity_id): For efficient lookup of sync metadata by entity
 * - sync_status: For querying pending/failed syncs
 * 
 * @property id Unique identifier for the sync metadata (auto-generated)
 * @property entityType Type of entity being tracked ("meal", "meal_plan", "shopping_list_item")
 * @property entityId Local database ID of the entity
 * @property serverId Cloud backend ID (null if not yet synced)
 * @property lastSyncedAt Timestamp of last successful sync (null if never synced)
 * @property localUpdatedAt Timestamp of last local modification
 * @property syncStatus Current sync status ("synced", "pending", "conflict", "error")
 * @property retryCount Number of sync retry attempts
 * @property errorMessage Error message from last failed sync attempt
 */
@Entity(
    tableName = "sync_metadata",
    indices = [
        Index(value = ["entity_type", "entity_id"], unique = true),
        Index(value = ["sync_status"])
    ]
)
data class SyncMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    
    @ColumnInfo(name = "entity_id")
    val entityId: Long,
    
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null,
    
    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: Long,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String, // "synced", "pending", "conflict", "error"
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)
