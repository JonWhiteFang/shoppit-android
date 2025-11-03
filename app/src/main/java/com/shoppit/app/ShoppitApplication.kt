package com.shoppit.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.shoppit.app.data.startup.StartupOptimizer
import com.shoppit.app.data.startup.StartupPhase
import com.shoppit.app.util.ReleaseTree
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Shoppit.
 * 
 * Responsibilities:
 * - Initialize Hilt for dependency injection
 * - Configure Timber for logging
 * - Configure WorkManager with HiltWorkerFactory for dependency injection in workers
 * - Optimize startup performance with deferred initialization
 *
 * Requirements:
 * - 1.1: Display first screen within 2000ms on cold start
 * - 1.2: Display first screen within 1000ms on warm start
 * - 1.3: Display first screen within 500ms on hot start
 * - 1.4: Defer non-critical initialization tasks to background threads
 */
@HiltAndroidApp
class ShoppitApplication : Application(), Configuration.Provider {
    
    /**
     * HiltWorkerFactory for injecting dependencies into Workers.
     * This allows SyncWorker to receive dependencies like SyncEngine and AuthRepository.
     * Lazily initialized to defer WorkManager setup.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    /**
     * StartupOptimizer for managing application startup phases.
     */
    @Inject
    lateinit var startupOptimizer: StartupOptimizer
    
    /**
     * Application-scoped coroutine scope for background initialization.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onCreate() {
        val appStartTime = System.currentTimeMillis()
        super.onCreate()
        
        // Track app creation phase
        val appCreationDuration = System.currentTimeMillis() - appStartTime
        startupOptimizer.trackStartupPhase(StartupPhase.APP_CREATION, appCreationDuration)
        
        // Initialize critical components synchronously
        initializeCritical()
        
        // Initialize non-critical components asynchronously
        initializeDeferred()
    }
    
    /**
     * Initializes critical components that must be ready before app is usable.
     * Runs synchronously on main thread but should complete quickly (<100ms).
     */
    private fun initializeCritical() {
        val startTime = System.currentTimeMillis()
        
        // Initialize Timber for logging (critical for debugging)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        
        Timber.d("Shoppit Application initialized")
        
        // Track critical services initialization
        val duration = System.currentTimeMillis() - startTime
        startupOptimizer.trackStartupPhase(StartupPhase.CRITICAL_SERVICES, duration)
    }
    
    /**
     * Initializes non-critical components in background.
     * These components don't block app startup and can be initialized later.
     */
    private fun initializeDeferred() {
        applicationScope.launch {
            val startTime = System.currentTimeMillis()
            
            try {
                // WorkManager initialization is deferred
                // It will be initialized lazily when first accessed via workManagerConfiguration
                
                // Other deferred initialization can be added here:
                // - Analytics initialization
                // - Crash reporting setup
                // - Background sync scheduling
                
                startupOptimizer.initializeDeferred()
                
                val duration = System.currentTimeMillis() - startTime
                Timber.d("Deferred initialization completed in ${duration}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error during deferred initialization")
            }
        }
    }
    
    /**
     * Provides WorkManager configuration with HiltWorkerFactory.
     * This enables dependency injection in Workers (e.g., SyncWorker).
     * Lazily initialized to defer WorkManager setup until first use.
     * 
     * @return WorkManager configuration with Hilt support
     */
    override val workManagerConfiguration: Configuration
        get() {
            val startTime = System.currentTimeMillis()
            
            val config = Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
            
            val duration = System.currentTimeMillis() - startTime
            Timber.d("WorkManager initialized in ${duration}ms")
            
            return config
        }
}