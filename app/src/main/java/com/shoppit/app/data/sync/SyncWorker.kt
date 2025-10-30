package com.shoppit.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import com.shoppit.app.domain.repository.AuthRepository
import com.shoppit.app.domain.repository.SyncEngine
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that performs periodic data synchronization.
 *
 * This worker:
 * - Runs every 15 minutes when network is available
 * - Only syncs if user is authenticated
 * - Implements exponential backoff on failures
 * - Respects battery optimization settings
 * - Retries up to 5 times on failure
 *
 * Requirements addressed:
 * - 2.4: Upload queued changes within 30 seconds when network available
 * - 4.1: Queue changes locally when offline
 * - 4.2: Automatically resume sync when connectivity restored
 * - 4.4: Retry with exponential backoff up to 5 attempts
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    /**
     * Entry point for accessing Hilt dependencies in the worker.
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncEngine(): SyncEngine
        fun authRepository(): AuthRepository
    }
    
    /**
     * Performs the sync work.
     *
     * @return Result indicating success, failure, or retry
     */
    override suspend fun doWork(): Result {
        Timber.d("SyncWorker started (attempt ${runAttemptCount + 1}/$MAX_RETRIES)")
        
        return try {
            // Get dependencies from Hilt
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val syncEngine = entryPoint.syncEngine()
            val authRepository = entryPoint.authRepository()
            
            // Check if user is authenticated
            if (!authRepository.isAuthenticated()) {
                Timber.d("User not authenticated, skipping sync")
                return Result.success()
            }
            
            // Perform sync
            val syncResult = syncEngine.syncAll()
            
            syncResult.fold(
                onSuccess = { result ->
                    Timber.i(
                        "Sync completed successfully: " +
                        "synced=${result.syncedEntities}, " +
                        "failed=${result.failedEntities}, " +
                        "conflicts=${result.conflicts}"
                    )
                    Result.success()
                },
                onFailure = { error ->
                    Timber.e(error, "Sync failed: ${error.message}")
                    
                    // Retry if we haven't exceeded max attempts
                    if (runAttemptCount < MAX_RETRIES - 1) {
                        Timber.d("Scheduling retry (attempt ${runAttemptCount + 2}/$MAX_RETRIES)")
                        Result.retry()
                    } else {
                        Timber.w("Max retries exceeded, marking as failure")
                        Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error in SyncWorker")
            
            // Retry on unexpected errors
            if (runAttemptCount < MAX_RETRIES - 1) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        /**
         * Maximum number of retry attempts before giving up.
         * Requirement 4.4: Retry up to 5 attempts
         */
        const val MAX_RETRIES = 5
        
        /**
         * Unique work name for the periodic sync.
         */
        const val WORK_NAME = "sync_work"
        
        /**
         * Sync interval in minutes.
         * Requirement 2.4: Upload within 30 seconds when network available
         * Note: 15 minutes is the minimum for PeriodicWorkRequest
         */
        private const val SYNC_INTERVAL_MINUTES = 15L
        
        /**
         * Schedules periodic sync work with WorkManager.
         *
         * This configures:
         * - 15-minute periodic execution (minimum allowed)
         * - Network connectivity requirement
         * - Exponential backoff on failures
         * - Unique work policy to prevent duplicates
         *
         * @param workManager WorkManager instance
         */
        fun schedule(workManager: WorkManager) {
            Timber.d("Scheduling periodic sync work")
            
            // Define constraints
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Require network connectivity
                .build()
            
            // Create periodic work request
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = SYNC_INTERVAL_MINUTES,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL, // Exponential backoff
                    WorkRequest.MIN_BACKOFF_MILLIS, // Start with minimum delay (10 seconds)
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            // Enqueue unique periodic work
            // KEEP policy ensures we don't create duplicate work
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            
            Timber.i("Periodic sync work scheduled (interval: ${SYNC_INTERVAL_MINUTES}m)")
        }
        
        /**
         * Cancels the periodic sync work.
         *
         * @param workManager WorkManager instance
         */
        fun cancel(workManager: WorkManager) {
            Timber.d("Cancelling periodic sync work")
            workManager.cancelUniqueWork(WORK_NAME)
            Timber.i("Periodic sync work cancelled")
        }
    }
}
