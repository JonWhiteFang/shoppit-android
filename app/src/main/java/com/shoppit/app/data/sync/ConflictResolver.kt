package com.shoppit.app.data.sync

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves synchronization conflicts using Last-Write-Wins (LWW) strategy.
 *
 * When the same entity is modified on multiple devices, this resolver determines
 * which version should be kept based on modification timestamps.
 *
 * Strategy:
 * - Compare updatedAt timestamps
 * - Keep the version with the most recent timestamp
 * - If timestamps are identical, prefer the server version
 * - Log all conflicts for potential user review
 *
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
@Singleton
class ConflictResolver @Inject constructor() {
    
    /**
     * Resolves a conflict between local and remote versions of an entity.
     *
     * @param local The local version of the entity
     * @param remote The remote (server) version of the entity
     * @return ConflictResolution indicating which version to use
     */
    fun <T : SyncableEntity> resolve(
        local: T,
        remote: T
    ): ConflictResolution<T> {
        // Requirement 5.1: Detect conflict during synchronization
        Timber.d("Detecting conflict for entity: localId=${local.id}, serverId=${local.serverId}")
        
        // Requirement 5.2: Apply Last-Write-Wins resolution based on timestamps
        val resolution = when {
            local.updatedAt > remote.updatedAt -> {
                // Local version is newer
                Timber.i("Conflict resolved: Local version wins (local=${local.updatedAt}, remote=${remote.updatedAt})")
                ConflictResolution.UseLocal(local)
            }
            local.updatedAt < remote.updatedAt -> {
                // Remote version is newer
                Timber.i("Conflict resolved: Remote version wins (local=${local.updatedAt}, remote=${remote.updatedAt})")
                ConflictResolution.UseRemote(remote)
            }
            else -> {
                // Requirement 5.5: Timestamps are identical, prefer server version
                Timber.i("Conflict resolved: Timestamps equal, using server version (timestamp=${local.updatedAt})")
                ConflictResolution.UseRemote(remote)
            }
        }
        
        // Requirement 5.4: Log the conflict for potential user review
        logConflict(local, remote, resolution)
        
        return resolution
    }
    
    /**
     * Logs conflict details for user review and debugging.
     *
     * @param local The local version of the entity
     * @param remote The remote version of the entity
     * @param resolution The resolution decision
     */
    private fun <T : SyncableEntity> logConflict(
        local: T,
        remote: T,
        resolution: ConflictResolution<T>
    ) {
        val winner = when (resolution) {
            is ConflictResolution.UseLocal -> "LOCAL"
            is ConflictResolution.UseRemote -> "REMOTE"
        }
        
        val conflictLog = buildString {
            appendLine("=== SYNC CONFLICT DETECTED ===")
            appendLine("Entity ID: ${local.id}")
            appendLine("Server ID: ${local.serverId}")
            appendLine("Local timestamp: ${local.updatedAt}")
            appendLine("Remote timestamp: ${remote.updatedAt}")
            appendLine("Resolution: $winner version kept")
            appendLine("Discarded: ${if (winner == "LOCAL") "REMOTE" else "LOCAL"} version")
            appendLine("==============================")
        }
        
        // Log at INFO level for visibility
        Timber.i(conflictLog)
        
        // TODO: In production, consider storing conflict logs in database
        // for user review through a conflict history UI
    }
    
    /**
     * Resolves multiple conflicts in a batch operation.
     *
     * @param conflicts List of conflict pairs (local, remote)
     * @return Map of entity IDs to their resolutions
     */
    fun <T : SyncableEntity> resolveBatch(
        conflicts: List<Pair<T, T>>
    ): Map<Long, ConflictResolution<T>> {
        Timber.d("Resolving ${conflicts.size} conflicts in batch")
        
        return conflicts.associate { (local, remote) ->
            local.id to resolve(local, remote)
        }
    }
    
    /**
     * Checks if two entities have a conflict (both modified since last sync).
     *
     * @param local The local version
     * @param remote The remote version
     * @param lastSyncTime The timestamp of the last successful sync
     * @return true if both versions were modified since last sync
     */
    fun hasConflict(
        local: SyncableEntity,
        remote: SyncableEntity,
        lastSyncTime: Long?
    ): Boolean {
        // No conflict if we've never synced before
        if (lastSyncTime == null) {
            return false
        }
        
        // Conflict exists if both versions were modified after last sync
        val localModified = local.updatedAt > lastSyncTime
        val remoteModified = remote.updatedAt > lastSyncTime
        
        val hasConflict = localModified && remoteModified
        
        if (hasConflict) {
            Timber.d("Conflict detected: both versions modified since last sync (lastSync=$lastSyncTime)")
        }
        
        return hasConflict
    }
}

/**
 * Sealed class representing the result of conflict resolution.
 *
 * @param T The type of entity being resolved
 */
sealed class ConflictResolution<out T : SyncableEntity> {
    /**
     * Use the local version of the entity.
     */
    data class UseLocal<T : SyncableEntity>(val entity: T) : ConflictResolution<T>()
    
    /**
     * Use the remote (server) version of the entity.
     */
    data class UseRemote<T : SyncableEntity>(val entity: T) : ConflictResolution<T>()
}

/**
 * Interface for entities that can be synchronized and have conflicts resolved.
 *
 * All syncable entities must provide:
 * - id: Local database ID
 * - serverId: Server-side ID (nullable for new entities)
 * - updatedAt: Timestamp of last modification
 */
interface SyncableEntity {
    val id: Long
    val serverId: String?
    val updatedAt: Long
}
