package com.shoppit.app.di

import com.google.gson.Gson
import com.shoppit.app.data.sync.ConflictResolver
import com.shoppit.app.data.sync.RetryPolicy
import com.shoppit.app.data.sync.SyncEngineImpl
import com.shoppit.app.domain.repository.SyncEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing sync-related dependencies.
 * 
 * This module provides:
 * - SyncEngine: Core synchronization orchestrator
 * - ConflictResolver: Handles sync conflicts with Last-Write-Wins strategy
 * - RetryPolicy: Implements exponential backoff for failed operations
 * - Gson: JSON serialization for sync payloads
 * 
 * Requirements: All requirements (dependency injection setup)
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    
    companion object {
        /**
         * Provides a Gson instance for JSON serialization/deserialization.
         * Used for serializing sync payloads in the queue.
         */
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()
        
        /**
         * Provides ConflictResolver for handling sync conflicts.
         * Uses Last-Write-Wins strategy based on timestamps.
         */
        @Provides
        @Singleton
        fun provideConflictResolver(): ConflictResolver = ConflictResolver()
        
        /**
         * Provides RetryPolicy for handling failed sync operations.
         * Implements exponential backoff with up to 5 retry attempts.
         */
        @Provides
        @Singleton
        fun provideRetryPolicy(): RetryPolicy = RetryPolicy()
    }
    
    /**
     * Binds the SyncEngine implementation.
     * SyncEngineImpl orchestrates all synchronization operations.
     */
    @Binds
    @Singleton
    abstract fun bindSyncEngine(
        impl: SyncEngineImpl
    ): SyncEngine
}
