package com.shoppit.app.data.sync

import androidx.work.WorkManager
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage sync worker scheduling.
 *
 * This class provides a simple interface to start and stop
 * the periodic sync worker based on authentication state.
 */
@Singleton
class SyncScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    
    /**
     * Starts the periodic sync worker.
     *
     * This should be called when:
     * - User successfully signs in
     * - User successfully signs up
     * - App starts and user is already authenticated
     */
    fun startPeriodicSync() {
        Timber.d("Starting periodic sync")
        SyncWorker.schedule(workManager)
    }
    
    /**
     * Stops the periodic sync worker.
     *
     * This should be called when:
     * - User signs out
     * - User deletes their account
     */
    fun stopPeriodicSync() {
        Timber.d("Stopping periodic sync")
        SyncWorker.cancel(workManager)
    }
}
