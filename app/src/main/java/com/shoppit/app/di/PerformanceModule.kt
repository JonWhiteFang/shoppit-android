package com.shoppit.app.di

import com.shoppit.app.data.performance.EnhancedPerformanceMonitor
import com.shoppit.app.data.performance.EnhancedPerformanceMonitorImpl
import com.shoppit.app.data.performance.PerformanceLogger
import com.shoppit.app.data.performance.PerformanceMonitor
import com.shoppit.app.data.performance.PerformanceMonitorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing performance monitoring dependencies.
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitorImpl {
        return PerformanceMonitorImpl()
    }
    
    @Provides
    @Singleton
    fun provideEnhancedPerformanceMonitor(
        baseMonitor: PerformanceMonitorImpl
    ): EnhancedPerformanceMonitor {
        return EnhancedPerformanceMonitorImpl(baseMonitor, PerformanceLogger())
    }
}

/**
 * Bindings module for performance monitoring interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PerformanceBindingsModule {
    
    @Binds
    @Singleton
    abstract fun bindPerformanceMonitor(
        impl: PerformanceMonitorImpl
    ): PerformanceMonitor
}
