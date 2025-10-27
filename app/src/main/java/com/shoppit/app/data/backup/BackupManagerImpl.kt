package com.shoppit.app.data.backup

import android.content.Context
import android.net.Uri
import com.shoppit.app.data.local.database.AppDatabase
import com.shoppit.app.domain.error.BackupException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BackupManager for managing database backups.
 * Handles backup creation, restoration, and file management.
 */
@Singleton
class BackupManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) : BackupManager {
    
    companion object {
        private const val BACKUP_DIR = "backups"
        private const val BACKUP_FILE_EXTENSION = ".db"
        private const val METADATA_FILE_EXTENSION = ".meta"
        private const val BUFFER_SIZE = 8192
    }
    
    private val backupDirectory: File
        get() = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }
    
    override suspend fun createBackup(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // Close database connections to ensure consistent backup
            database.close()
            
            val backupId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val version = database.openHelper.readableDatabase.version
            
            // Get database file
            val dbFile = context.getDatabasePath(database.openHelper.databaseName)
            if (!dbFile.exists()) {
                return@withContext Result.failure(BackupException("Database file not found"))
            }
            
            // Create backup file
            val backupFile = File(backupDirectory, "$backupId$BACKUP_FILE_EXTENSION")
            
            // Copy database file
            dbFile.copyTo(backupFile, overwrite = true)
            
            // Calculate checksum
            val checksum = calculateChecksum(backupFile)
            val size = backupFile.length()
            
            // Create metadata
            val metadata = BackupMetadata(
                id = backupId,
                timestamp = timestamp,
                version = version,
                size = size,
                checksum = checksum
            )
            
            // Save metadata
            saveMetadata(metadata)
            
            Timber.i("Backup created successfully: id=$backupId, size=$size bytes")
            Result.success(metadata)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create backup")
            Result.failure(BackupException("Failed to create backup", e))
        }
    }
    
    override suspend fun restoreBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupDirectory, "$backupId$BACKUP_FILE_EXTENSION")
            if (!backupFile.exists()) {
                return@withContext Result.failure(BackupException("Backup not found: $backupId"))
            }
            
            // Verify backup integrity
            val metadata = loadMetadata(backupId)
                ?: return@withContext Result.failure(BackupException("Backup metadata not found"))
            
            val currentChecksum = calculateChecksum(backupFile)
            if (currentChecksum != metadata.checksum) {
                return@withContext Result.failure(BackupException("Backup integrity check failed"))
            }
            
            // Close database connections
            database.close()
            
            // Get database file
            val dbFile = context.getDatabasePath(database.openHelper.databaseName)
            
            // Restore backup
            backupFile.copyTo(dbFile, overwrite = true)
            
            Timber.i("Backup restored successfully: id=$backupId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore backup: id=$backupId")
            Result.failure(BackupException("Failed to restore backup", e))
        }
    }
    
    override suspend fun listBackups(): Result<List<BackupMetadata>> = withContext(Dispatchers.IO) {
        try {
            val backups = backupDirectory.listFiles { file ->
                file.extension == BACKUP_FILE_EXTENSION.removePrefix(".")
            }?.mapNotNull { file ->
                val backupId = file.nameWithoutExtension
                loadMetadata(backupId)
            }?.sortedByDescending { it.timestamp } ?: emptyList()
            
            Timber.d("Found ${backups.size} backups")
            Result.success(backups)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list backups")
            Result.failure(BackupException("Failed to list backups", e))
        }
    }
    
    override suspend fun deleteBackup(backupId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(backupDirectory, "$backupId$BACKUP_FILE_EXTENSION")
            val metadataFile = File(backupDirectory, "$backupId$METADATA_FILE_EXTENSION")
            
            var deleted = false
            if (backupFile.exists()) {
                deleted = backupFile.delete()
            }
            if (metadataFile.exists()) {
                metadataFile.delete()
            }
            
            if (deleted) {
                Timber.i("Backup deleted: id=$backupId")
                Result.success(Unit)
            } else {
                Result.failure(BackupException("Backup not found: $backupId"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete backup: id=$backupId")
            Result.failure(BackupException("Failed to delete backup", e))
        }
    }
    
    override suspend fun exportToFile(destination: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Close database connections
            database.close()
            
            val dbFile = context.getDatabasePath(database.openHelper.databaseName)
            if (!dbFile.exists()) {
                return@withContext Result.failure(BackupException("Database file not found"))
            }
            
            // Export to destination
            context.contentResolver.openOutputStream(destination)?.use { outputStream ->
                FileInputStream(dbFile).use { inputStream ->
                    inputStream.copyTo(outputStream, BUFFER_SIZE)
                }
            } ?: return@withContext Result.failure(BackupException("Failed to open output stream"))
            
            Timber.i("Database exported to: $destination")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to export database")
            Result.failure(BackupException("Failed to export database", e))
        }
    }
    
    override suspend fun importFromFile(source: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Close database connections
            database.close()
            
            val dbFile = context.getDatabasePath(database.openHelper.databaseName)
            
            // Import from source
            context.contentResolver.openInputStream(source)?.use { inputStream ->
                FileOutputStream(dbFile).use { outputStream ->
                    inputStream.copyTo(outputStream, BUFFER_SIZE)
                }
            } ?: return@withContext Result.failure(BackupException("Failed to open input stream"))
            
            Timber.i("Database imported from: $source")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to import database")
            Result.failure(BackupException("Failed to import database", e))
        }
    }
    
    /**
     * Calculates SHA-256 checksum of a file.
     */
    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Saves backup metadata to a file.
     */
    private fun saveMetadata(metadata: BackupMetadata) {
        val metadataFile = File(backupDirectory, "${metadata.id}$METADATA_FILE_EXTENSION")
        metadataFile.writeText(
            """
            id=${metadata.id}
            timestamp=${metadata.timestamp}
            version=${metadata.version}
            size=${metadata.size}
            checksum=${metadata.checksum}
            """.trimIndent()
        )
    }
    
    /**
     * Loads backup metadata from a file.
     */
    private fun loadMetadata(backupId: String): BackupMetadata? {
        val metadataFile = File(backupDirectory, "$backupId$METADATA_FILE_EXTENSION")
        if (!metadataFile.exists()) return null
        
        return try {
            val properties = metadataFile.readLines()
                .associate { line ->
                    val (key, value) = line.split("=", limit = 2)
                    key to value
                }
            
            BackupMetadata(
                id = properties["id"] ?: backupId,
                timestamp = properties["timestamp"]?.toLongOrNull() ?: 0L,
                version = properties["version"]?.toIntOrNull() ?: 0,
                size = properties["size"]?.toLongOrNull() ?: 0L,
                checksum = properties["checksum"] ?: ""
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to load metadata for backup: $backupId")
            null
        }
    }
}
