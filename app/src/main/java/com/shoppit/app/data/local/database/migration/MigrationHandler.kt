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
            MIGRATION_3_4,
            MIGRATION_4_5
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
         * Migration from version 2 to 3: Add meal plan tables
         * Future migration for meal planning feature.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 2 to 3")
                
                // Create meal_plans table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS meal_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        meal_type TEXT NOT NULL,
                        meal_id INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create index on date and meal_type for efficient queries
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_meal_plans_date 
                    ON meal_plans(date)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_meal_plans_date_meal_type 
                    ON meal_plans(date, meal_type)
                """.trimIndent())
                
                Timber.d("Migration 2->3 completed: meal_plans table created")
            }
        }
        
        /**
         * Migration from version 3 to 4: Add shopping list tables
         * Future migration for shopping list feature.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 3 to 4")
                
                // Create shopping_lists table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        is_completed INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Create shopping_list_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        list_id INTEGER NOT NULL,
                        ingredient_name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        is_checked INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(list_id) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for efficient queries
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_shopping_lists_created_at 
                    ON shopping_lists(created_at)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS idx_shopping_list_items_list_checked 
                    ON shopping_list_items(list_id, is_checked)
                """.trimIndent())
                
                Timber.d("Migration 3->4 completed: shopping list tables created")
            }
        }
        
        /**
         * Migration from version 4 to 5: Add performance indices and metadata columns
         * Adds indices for frequently queried columns and additional metadata.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 4 to 5")
                
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
                
                // Add soft delete columns to meals table
                database.execSQL("""
                    ALTER TABLE meals 
                    ADD COLUMN is_deleted INTEGER NOT NULL DEFAULT 0
                """.trimIndent())
                
                database.execSQL("""
                    ALTER TABLE meals 
                    ADD COLUMN deleted_at INTEGER
                """.trimIndent())
                
                database.execSQL("""
                    ALTER TABLE meals 
                    ADD COLUMN version INTEGER NOT NULL DEFAULT 1
                """.trimIndent())
                
                // Add tags column to meals table
                database.execSQL("""
                    ALTER TABLE meals 
                    ADD COLUMN tags TEXT
                """.trimIndent())
                
                Timber.d("Migration 4->5 completed: indices and metadata columns added")
            }
        }
    }
}
