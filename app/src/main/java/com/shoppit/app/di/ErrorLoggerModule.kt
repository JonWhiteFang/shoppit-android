package com.shoppit.app.di

import com.shoppit.app.data.error.ErrorLoggerImpl
import com.shoppit.app.domain.error.ErrorLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing error logging dependencies.
 * 
 * This module binds the ErrorLogger interface to its implementation,
 * making it available for injection throughout the application.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorLoggerModule {

    /**
     * Binds ErrorLoggerImpl to ErrorLogger interface.
     * 
     * @param errorLoggerImpl The implementation to bind
     * @return The ErrorLogger interface
     */
    @Binds
    @Singleton
    abstract fun bindErrorLogger(
        errorLoggerImpl: ErrorLoggerImpl
    ): ErrorLogger
}
