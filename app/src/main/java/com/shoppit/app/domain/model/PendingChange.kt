package com.shoppit.app.domain.model

/**
 * Represents a pending change that needs to be synchronized.
 *
 * @property id Unique identifier for the pending change
 * @property entityType Type of entity that changed
 * @property entityId Local database ID of the entity
 * @property operation Type of operation performed
 * @property timestamp When the change was queued
 * @property retryCount Number of times sync has been attempted
 */
data class PendingChange(
    val id: Long,
    val entityType: EntityType,
    val entityId: Long,
    val operation: SyncOperation,
    val timestamp: Long,
    val retryCount: Int
)
