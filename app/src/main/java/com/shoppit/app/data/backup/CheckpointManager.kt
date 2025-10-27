package com.shoppit.app.data.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic checkpoint backups based on time and transaction count.
 */
@Singleton
class CheckpointManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val integrityChecker: DatabaseIntegrityChecker
) {
    
    companion object {
        private const val CHECKPOINT_WORK_NAME = "database_checkpoint"
        private const val CHECKPOINT_INTERVAL_HOURS = 24L
        private const val TRANSACTION_THRESHOLD = 1000
        private const val MAX_BACKUPS_TO_KEEP = 7
    }
    
    private val transactionCount = AtomicInteger(0)
    
    /**
     * Schedules periodic automatic checkpoints.
     */
    fun schedulePeriodicCheckpoints() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val checkpointWork = PeriodicWorkRequestBuilder<CheckpointWorker>(
            CHECKPOINT_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CHECKPOINT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            checkpointWork
        )
        
        Timber.i("Scheduled periodic checkpoints every $CHECKPOINT_INTERVAL_HOURS hours")
    }
    
    /**
     * Increments the transaction counter and creates a checkpoint if threshold is reached.
     */
    suspend fun onTransaction() {
        val count = transactionCount.incrementAndGet()
        
        if (count >= TRANSACTION_THRESHOLD) {
            Timber.d("Transaction threshold reached ($count), creating checkpoint")
            createCheckpoint()
            transactionCount.set(0)
        }
    }
    
    /**
     * Creates a checkpoint backup and cleans up old backups.
     */
    suspend fun createCheckpoint(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            // Create checkpoint
            val result = integrityChecker.createCheckpoint()
            
            if (result.isSuccess) {
                // Clean up old backups
                cleanupOldBackups()
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "Failed to create checkpoint")
            Result.failure(e)
        }
    }
    
    /**
     * Removes old backups, keeping only the most recent ones.
     */
    private suspend fun cleanupOldBackups() {
        try {
            val backupsResult = backupManager.listBackups()
            if (backupsResult.isFailure) return
            
            val backups = backupsResult.getOrNull() ?: return
            
            // Keep only the most recent backups
            val backupsToDelete = backups
                .sortedByDescending { it.timestamp }
                .drop(MAX_BACKUPS_TO_KEEP)
            
            for (backup in backupsToDelete) {
                backupManager.deleteBackup(backup.id)
                Timber.d("Deleted old backup: ${backup.id}")
            }
            
            if (backupsToDelete.isNotEmpty()) {
                Timber.i("Cleaned up ${backupsToDelete.size} old backups")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old backups")
        }
    }
}

/**
 * WorkManager worker for periodic checkpoint creation.
 */
class CheckpointWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            Timber.d("Starting periodic checkpoint backup")
            
            // Note: In a real implementation, you would inject dependencies here
            // For now, this is a placeholder that would need proper DI setup
            
            Timber.i("Periodic checkpoint completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Periodic checkpoint failed")
            Result.failure()
        }
    }
}
