package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.EntityType
import com.shoppit.app.domain.model.PendingChange
import com.shoppit.app.domain.model.SyncOperation
import com.shoppit.app.domain.model.SyncResult
import com.shoppit.app.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the Sync Engine that orchestrates all synchronization operations
 * between local database and cloud backend.
 *
 * The Sync Engine follows an offline-first architecture where:
 * - Local database is the single source of truth
 * - Sync operations are non-blocking background tasks
 * - Changes are queued when offline and synced when connectivity is restored
 * - Conflicts are resolved automatically using Last-Write-Wins strategy
 */
interface SyncEngine {
    
    // ========== Sync Operations ==========
    
    /**
     * Synchronizes all entity types (meals, meal plans, shopping lists).
     * 
     * This performs a full bidirectional sync:
     * 1. Uploads all pending local changes to the server
     * 2. Downloads remote changes from the server
     * 3. Resolves any conflicts using Last-Write-Wins
     * 
     * @return Result containing SyncResult with sync statistics or an error
     */
    suspend fun syncAll(): Result<SyncResult>
    
    /**
     * Synchronizes only meal entities.
     * 
     * @return Result containing SyncResult with sync statistics or an error
     */
    suspend fun syncMeals(): Result<SyncResult>
    
    /**
     * Synchronizes only meal plan entities.
     * 
     * @return Result containing SyncResult with sync statistics or an error
     */
    suspend fun syncMealPlans(): Result<SyncResult>
    
    /**
     * Synchronizes only shopping list item entities.
     * 
     * @return Result containing SyncResult with sync statistics or an error
     */
    suspend fun syncShoppingLists(): Result<SyncResult>
    
    // ========== Queue Management ==========
    
    /**
     * Queues a change for synchronization.
     * 
     * This is called by repositories when entities are created, updated, or deleted.
     * The change will be synchronized when network connectivity is available.
     * 
     * @param entityType Type of entity that changed
     * @param entityId Local database ID of the entity
     * @param operation Type of operation performed (CREATE, UPDATE, DELETE)
     */
    suspend fun queueChange(
        entityType: EntityType,
        entityId: Long,
        operation: SyncOperation
    )
    
    /**
     * Gets all pending changes that are queued for synchronization.
     * 
     * @return List of pending changes ordered by creation time (FIFO)
     */
    suspend fun getPendingChanges(): List<PendingChange>
    
    // ========== Status Monitoring ==========
    
    /**
     * Observes the current sync status.
     * 
     * Emits status updates as sync operations progress:
     * - IDLE: No sync in progress
     * - SYNCING: Sync operation in progress
     * - SUCCESS: Last sync completed successfully
     * - ERROR: Last sync failed
     * - OFFLINE: Device is offline
     * 
     * @return Flow of SyncStatus updates
     */
    fun observeSyncStatus(): Flow<SyncStatus>
    
    /**
     * Gets the timestamp of the last successful sync operation.
     * 
     * @return Timestamp in milliseconds, or null if never synced
     */
    fun getLastSyncTime(): Long?
    
    // ========== Manual Control ==========
    
    /**
     * Forces an immediate sync operation regardless of schedule.
     * 
     * This is typically triggered by user action (e.g., pull-to-refresh).
     * If a sync is already in progress, this will wait for it to complete
     * and then start a new sync.
     * 
     * @return Result containing SyncResult with sync statistics or an error
     */
    suspend fun forceSyncNow(): Result<SyncResult>
    
    /**
     * Cancels any ongoing sync operation.
     * 
     * This is a best-effort cancellation. Some operations may complete
     * before cancellation takes effect.
     */
    suspend fun cancelSync()
}
