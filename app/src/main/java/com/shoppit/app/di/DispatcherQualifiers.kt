package com.shoppit.app.di

import javax.inject.Qualifier

/**
 * Qualifier annotation for IO dispatcher.
 * Use for I/O operations like database queries, network requests, and file operations.
 * 
 * Requirements: 7.1, 7.4
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier annotation for Default dispatcher.
 * Use for CPU-intensive work like data processing and computations.
 * 
 * Requirements: 7.1, 7.4
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Qualifier annotation for Main dispatcher.
 * Use for UI updates and operations that must run on the main thread.
 * 
 * Requirements: 7.1, 7.4
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
