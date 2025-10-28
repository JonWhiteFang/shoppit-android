package com.shoppit.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shoppit.app.data.backup.BackupManager
import com.shoppit.app.data.backup.BackupManagerImpl
import com.shoppit.app.data.backup.CheckpointManager
import com.shoppit.app.data.backup.DatabaseIntegrityChecker
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.dao.MealPlanDao
import com.shoppit.app.data.local.database.AppDatabase
import com.shoppit.app.data.local.database.migration.MigrationHandler
import com.shoppit.app.data.local.database.migration.MigrationHandlerImpl
import com.shoppit.app.data.maintenance.MaintenanceManager
import com.shoppit.app.data.maintenance.MaintenanceManagerImpl
import com.shoppit.app.data.performance.PerformanceMonitor
import com.shoppit.app.data.performance.PerformanceMonitorImpl
import com.shoppit.app.data.transaction.TransactionManager
import com.shoppit.app.data.transaction.TransactionManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies.
 * 
 * This module provides the Room database instance as a singleton.
 * The database is configured with:
 * - Proper migration strategy using MigrationHandler
 * - Write-Ahead Logging (WAL) for better concurrency
 * - Type converters for complex data types
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "shoppit_database"

    @Provides
    @Singleton
    fun provideMigrationHandler(): MigrationHandler {
        return MigrationHandlerImpl()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        migrationHandler: MigrationHandler
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*migrationHandler.getMigrations().toTypedArray())
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Timber.i("Database created: version ${db.version}")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Enable foreign key constraints
                    db.execSQL("PRAGMA foreign_keys=ON")
                    Timber.d("Database opened: version ${db.version}")
                }
            })
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration() // Fallback for development
            .build()
    }
    
    @Provides
    fun provideMealDao(database: AppDatabase) = database.mealDao()
    
    @Provides
    fun provideMealPlanDao(database: AppDatabase) = database.mealPlanDao()
    
    @Provides
    fun provideShoppingListDao(database: AppDatabase) = database.shoppingListDao()
    
    @Provides
    fun provideItemHistoryDao(database: AppDatabase) = database.itemHistoryDao()
    
    @Provides
    fun provideTemplateDao(database: AppDatabase) = database.templateDao()
    
    @Provides
    fun provideStoreSectionDao(database: AppDatabase) = database.storeSectionDao()
    
    @Provides
    @Singleton
    fun provideTransactionManager(database: AppDatabase): TransactionManager {
        return TransactionManagerImpl(database)
    }
    
    @Provides
    @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitorImpl()
    }
    
    @Provides
    @Singleton
    fun provideBackupManager(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): BackupManager {
        return BackupManagerImpl(context, database)
    }
    
    @Provides
    @Singleton
    fun provideDatabaseIntegrityChecker(
        @ApplicationContext context: Context,
        database: AppDatabase,
        backupManager: BackupManager
    ): DatabaseIntegrityChecker {
        return DatabaseIntegrityChecker(context, database, backupManager)
    }
    
    @Provides
    @Singleton
    fun provideCheckpointManager(
        @ApplicationContext context: Context,
        backupManager: BackupManager,
        integrityChecker: DatabaseIntegrityChecker
    ): CheckpointManager {
        return CheckpointManager(context, backupManager, integrityChecker)
    }
    
    @Provides
    @Singleton
    fun provideMaintenanceManager(
        @ApplicationContext context: Context,
        database: AppDatabase
    ): MaintenanceManager {
        return MaintenanceManagerImpl(context, database)
    }
}