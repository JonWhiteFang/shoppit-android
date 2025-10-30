package com.shoppit.app.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides WorkManager dependencies.
 *
 * This module configures WorkManager to work with Hilt's dependency injection,
 * allowing workers (like SyncWorker) to receive injected dependencies through
 * the HiltWorkerFactory.
 * 
 * Note: The Application class must implement Configuration.Provider and return
 * a Configuration that uses HiltWorkerFactory for this to work properly.
 * 
 * Requirements: 2.4, 4.1, 4.2, 4.4 (WorkManager configuration for background sync)
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    
    /**
     * Provides a singleton WorkManager instance.
     * 
     * WorkManager is used for scheduling and executing background sync operations.
     * It handles:
     * - Periodic sync every 15 minutes
     * - Network connectivity constraints
     * - Exponential backoff on failures
     * - Guaranteed execution even after app restart
     *
     * @param context Application context
     * @return WorkManager instance
     */
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
}
