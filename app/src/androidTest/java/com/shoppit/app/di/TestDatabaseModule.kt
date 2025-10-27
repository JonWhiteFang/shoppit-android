package com.shoppit.app.di

import android.content.Context
import androidx.room.Room
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test database module that replaces the production DatabaseModule in tests.
 * 
 * Provides an in-memory Room database for testing purposes.
 * The in-memory database:
 * - Does not persist data between test runs
 * - Allows main thread queries for simpler test code
 * - Is faster than file-based databases
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideTestDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // Simplifies test code
            .build()
    }
    
    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = database.mealDao()
}
