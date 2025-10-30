package com.shoppit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shoppit.app.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Shoppit.
 * 
 * Responsibilities:
 * - Initialize Hilt for dependency injection
 * - Configure Timber for logging
 * - Configure WorkManager with HiltWorkerFactory for dependency injection in workers
 */
@HiltAndroidApp
class ShoppitApplication : Application(), Configuration.Provider {
    
    /**
     * HiltWorkerFactory for injecting dependencies into Workers.
     * This allows SyncWorker to receive dependencies like SyncEngine and AuthRepository.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("Shoppit Application initialized")
    }
    
    /**
     * Provides WorkManager configuration with HiltWorkerFactory.
     * This enables dependency injection in Workers (e.g., SyncWorker).
     * 
     * @return WorkManager configuration with Hilt support
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}