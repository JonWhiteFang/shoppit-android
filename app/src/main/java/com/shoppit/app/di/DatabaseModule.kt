package com.shoppit.app.di

import android.content.Context
import androidx.room.Room
import com.shoppit.app.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies.
 * 
 * This module provides the Room database instance as a singleton.
 * The database is configured with:
 * - Fallback to destructive migration for development (will be replaced with proper migrations in production)
 * - Type converters for LocalDateTime and LocalDate
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "shoppit_database"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            // For development: fallback to destructive migration
            // TODO: Replace with proper migration strategy before production release
            .fallbackToDestructiveMigration()
            .build()
    }
    
    // Future DAO providers will be added here as features are implemented
    // Example:
    // @Provides
    // fun provideMealDao(database: AppDatabase): MealDao = database.mealDao()
}