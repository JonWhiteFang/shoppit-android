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
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
