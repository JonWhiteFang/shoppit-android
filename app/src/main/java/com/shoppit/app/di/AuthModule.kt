package com.shoppit.app.di

import com.shoppit.app.data.auth.AuthRepositoryImpl
import com.shoppit.app.data.auth.TokenStorage
import com.shoppit.app.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication dependencies.
 * 
 * This module provides:
 * - AuthRepository: Manages user authentication and session
 * - TokenStorage: Securely stores authentication tokens
 * 
 * Requirements:
 * - 1.1, 1.2: Authentication service for sign-in/sign-up
 * - 8.1, 8.2, 8.3: Secure token storage and management
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    /**
     * Binds AuthRepositoryImpl to AuthRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}
