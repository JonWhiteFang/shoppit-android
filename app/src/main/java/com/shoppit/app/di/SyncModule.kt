package com.shoppit.app.di

import com.shoppit.app.data.sync.SyncEngineImpl
import com.shoppit.app.domain.repository.SyncEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing sync-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    
    /**
     * Binds the SyncEngine implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSyncEngine(
        impl: SyncEngineImpl
    ): SyncEngine
}
