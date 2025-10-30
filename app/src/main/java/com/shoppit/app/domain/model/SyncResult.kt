package com.shoppit.app.domain.model

/**
 * Result of a synchronization operation.
 *
 * @property success Whether the sync operation completed successfully
 * @property syncedEntities Number of entities successfully synchronized
 * @property failedEntities Number of entities that failed to sync
 * @property conflicts Number of conflicts detected and resolved
 * @property timestamp Timestamp when the sync operation completed
 */
data class SyncResult(
    val success: Boolean,
    val syncedEntities: Int,
    val failedEntities: Int,
    val conflicts: Int,
    val timestamp: Long
)
