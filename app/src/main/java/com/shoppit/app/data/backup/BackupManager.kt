package com.shoppit.app.data.backup

import android.net.Uri

/**
 * Interface for managing database backups and recovery.
 * Provides methods for creating, restoring, and managing backups.
 */
interface BackupManager {
    /**
     * Creates a backup of the current database.
     *
     * @return Result with backup metadata or error
     */
    suspend fun createBackup(): Result<BackupMetadata>
    
    /**
     * Restores the database from a backup.
     *
     * @param backupId The unique identifier of the backup to restore
     * @return Result indicating success or error
     */
    suspend fun restoreBackup(backupId: String): Result<Unit>
    
    /**
     * Lists all available backups.
     *
     * @return Result with list of backup metadata or error
     */
    suspend fun listBackups(): Result<List<BackupMetadata>>
    
    /**
     * Deletes a specific backup.
     *
     * @param backupId The unique identifier of the backup to delete
     * @return Result indicating success or error
     */
    suspend fun deleteBackup(backupId: String): Result<Unit>
    
    /**
     * Exports the database to an external file.
     *
     * @param destination The URI where the backup should be exported
     * @return Result indicating success or error
     */
    suspend fun exportToFile(destination: Uri): Result<Unit>
    
    /**
     * Imports the database from an external file.
     *
     * @param source The URI of the backup file to import
     * @return Result indicating success or error
     */
    suspend fun importFromFile(source: Uri): Result<Unit>
}

/**
 * Metadata for a database backup.
 *
 * @property id Unique identifier for the backup
 * @property timestamp Unix timestamp when the backup was created
 * @property version Database version at the time of backup
 * @property size Size of the backup in bytes
 * @property checksum SHA-256 checksum of the backup for integrity verification
 */
data class BackupMetadata(
    val id: String,
    val timestamp: Long,
    val version: Int,
    val size: Long,
    val checksum: String
)
