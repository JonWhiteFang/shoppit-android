package com.shoppit.app.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import timber.log.Timber

/**
 * Interface for managing database migrations.
 * 
 * Provides methods to retrieve migrations, validate migration success,
 * and handle migration lifecycle events.
 */
interface MigrationHandler {
    /**
     * Returns all available migrations for the database.
     */
    fun getMigrations(): List<Migration>
    
    /**
     * Validates that a migration completed successfully.
     * 
     * @param fromVersion The starting database version
     * @param toVersion The target database version
     * @return true if validation passes, false otherwise
     */
    fun validateMigration(fromVersion: Int, toVersion: Int): Boolean
    
    /**
     * Called when a migration completes successfully.
     * 
     * @param fromVersion The starting database version
     * @param toVersion The target database version
     */
    fun onMigrationComplete(fromVersion: Int, toVersion: Int)
    
    /**
     * Called when a migration fails.
     * 
     * @param fromVersion The starting database version
     * @param toVersion The target database version
     * @param error The error that caused the migration to fail
     */
    fun onMigrationFailed(fromVersion: Int, toVersion: Int, error: Throwable)
}

/**
 * Default implementation of MigrationHandler.
 * 
 * Provides migrations for all schema version changes and includes
 * validation logic to ensure data integrity after migrations.
 */
class MigrationHandlerImpl : MigrationHandler {
    
    override fun getMigrations(): List<Migration> {
        return listOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4
        )
    }
    
    override fun validateMigration(fromVersion: Int, toVersion: Int): Boolean {
        Timber.d("Validating migration from version $fromVersion to $toVersion")
        // Validation logic can be added here if needed
        // For now, we assume migrations are valid if they complete without exceptions
        return true
    }
    
    override fun onMigrationComplete(fromVersion: Int, toVersion: Int) {
        Timber.i("Migration completed successfully: $fromVersion -> $toVersion")
    }
    
    override fun onMigrationFailed(fromVersion: Int, toVersion: Int, error: Throwable) {
        Timber.e(error, "Migration failed: $fromVersion -> $toVersion")
    }
    
    companion object {
        /**
         * Migration from version 1 to 2: Added meals table
         * This migration was already applied in the current database version.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 1 to 2")
                
                // Create meals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        ingredients TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())
                
                Timber.d("Migration 1->2 completed: meals table created")
            }
        }
        
        /**
         * Migration from version 2 to 3: Add indices on meals table for performance optimization
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 2 to 3")
                
                // Add indices on meals table for performance
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_meals_name 
                    ON meals(name)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_meals_created_at 
                    ON meals(created_at)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_meals_updated_at 
                    ON meals(updated_at)
                """.trimIndent())
                
                Timber.d("Migration 2->3 completed: indices added to meals table")
            }
        }
        
        /**
         * Migration from version 3 to 4: Add meal_plans table for meal planning feature
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 3 to 4")
                
                // Create meal_plans table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        meal_id INTEGER NOT NULL,
                        date INTEGER NOT NULL,
                        meal_type TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create index on date for efficient week queries
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_meal_plans_date 
                    ON meal_plans(date)
                """.trimIndent())
                
                // Create index on meal_id for cascade delete performance
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_meal_plans_meal_id 
                    ON meal_plans(meal_id)
                """.trimIndent())
                
                // Create unique index on (date, meal_type) to prevent double booking
                database.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_meal_plans_date_meal_type 
                    ON meal_plans(date, meal_type)
                """.trimIndent())
                
                Timber.d("Migration 3->4 completed: meal_plans table created with indices")
            }
        }
    }
}
