package com.shoppit.app.di

import com.shoppit.app.data.memory.MemoryManager
import com.shoppit.app.data.memory.MemoryManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing memory management dependencies.
 * 
 * Provides singleton instances of MemoryManager for monitoring and managing
 * application memory usage.
 * 
 * Requirements: 4.1
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MemoryModule {
    
    /**
     * Binds MemoryManagerImpl to MemoryManager interface.
     * 
     * @param impl The implementation to bind
     * @return MemoryManager instance
     */
    @Binds
    @Singleton
    abstract fun bindMemoryManager(impl: MemoryManagerImpl): MemoryManager
}
