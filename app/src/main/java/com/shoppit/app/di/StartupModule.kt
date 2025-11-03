package com.shoppit.app.di

import com.shoppit.app.data.startup.StartupOptimizer
import com.shoppit.app.data.startup.StartupOptimizerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing startup optimization dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StartupModule {
    
    @Binds
    @Singleton
    abstract fun bindStartupOptimizer(
        impl: StartupOptimizerImpl
    ): StartupOptimizer
}
