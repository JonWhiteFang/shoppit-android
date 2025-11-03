package com.shoppit.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt module for providing coroutine dispatchers.
 * 
 * Provides singleton instances of different dispatchers for different types of work:
 * - IO: For I/O operations (database, network, file system)
 * - Default: For CPU-intensive work
 * - Main: For UI updates
 * 
 * Requirements: 7.1, 7.4
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    /**
     * Provides the IO dispatcher for I/O operations.
     * Use for database queries, network requests, and file operations.
     * 
     * @return IO dispatcher
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    /**
     * Provides the Default dispatcher for CPU-intensive work.
     * Use for data processing, computations, and other CPU-bound operations.
     * 
     * @return Default dispatcher
     */
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    /**
     * Provides the Main dispatcher for UI updates.
     * Use for operations that must run on the main thread.
     * 
     * @return Main dispatcher
     */
    @Provides
    @Singleton
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
