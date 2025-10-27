package com.shoppit.app.data.backup

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoppit.app.data.local.database.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks database integrity and handles corruption detection.
 */
@Singleton
class DatabaseIntegrityChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val backupManager: BackupManager
) {
    
    /**
     * Checks if the database is corrupted.
     *
     * @return Result with true if database is healthy, false if corrupted
     */
    suspend fun checkIntegrity(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.writableDatabase
            val isHealthy = performIntegrityCheck(db)
            
            if (isHealthy) {
                Timber.d("Database integrity check passed")
                Result.success(true)
            } else {
                Timber.w("Database integrity check failed - corruption detected")
                Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check database integrity")
            Result.failure(e)
        }
    }
    
    /**
     * Attempts to recover from database corruption.
     * Tries to restore from the most recent valid backup.
     *
     * @return Result indicating success or error
     */
    suspend fun recoverFromCorruption(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Timber.i("Attempting to recover from database corruption")
            
            // Get list of backups
            val backupsResult = backupManager.listBackups()
            if (backupsResult.isFailure) {
                return@withContext Result.failure(
                    Exception("Failed to list backups for recovery")
                )
            }
            
            val backups = backupsResult.getOrNull() ?: emptyList()
            if (backups.isEmpty()) {
                Timber.w("No backups available for recovery")
                return@withContext Result.failure(
                    Exception("No backups available for recovery")
                )
            }
            
            // Try to restore from most recent backup
            for (backup in backups.sortedByDescending { it.timestamp }) {
                Timber.d("Attempting to restore from backup: ${backup.id}")
                
                val restoreResult = backupManager.restoreBackup(backup.id)
                if (restoreResult.isSuccess) {
                    // Verify restored database
                    val integrityResult = checkIntegrity()
                    if (integrityResult.isSuccess && integrityResult.getOrNull() == true) {
                        Timber.i("Successfully recovered from backup: ${backup.id}")
                        return@withContext Result.success(Unit)
                    } else {
                        Timber.w("Restored backup is also corrupted: ${backup.id}")
                    }
                } else {
                    Timber.w("Failed to restore backup: ${backup.id}")
                }
            }
            
            Timber.e("Failed to recover from any available backup")
            Result.failure(Exception("Failed to recover from any available backup"))
        } catch (e: Exception) {
            Timber.e(e, "Error during corruption recovery")
            Result.failure(e)
        }
    }
    
    /**
     * Creates an automatic checkpoint backup.
     * Should be called periodically or after significant operations.
     *
     * @return Result with backup metadata or error
     */
    suspend fun createCheckpoint(): Result<BackupMetadata> {
        Timber.d("Creating automatic checkpoint backup")
        return backupManager.createBackup()
    }
    
    /**
     * Performs SQLite integrity check using PRAGMA integrity_check.
     *
     * @param db The database to check
     * @return true if database is healthy, false if corrupted
     */
    private fun performIntegrityCheck(db: SupportSQLiteDatabase): Boolean {
        return try {
            db.query("PRAGMA integrity_check").use { cursor ->
                if (cursor.moveToFirst()) {
                    val result = cursor.getString(0)
                    result == "ok"
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Integrity check query failed")
            false
        }
    }
    
    /**
     * Performs a quick check to see if the database can be opened.
     *
     * @return true if database can be opened, false otherwise
     */
    suspend fun canOpenDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            database.openHelper.writableDatabase
            true
        } catch (e: Exception) {
            Timber.e(e, "Cannot open database")
            false
        }
    }
}
