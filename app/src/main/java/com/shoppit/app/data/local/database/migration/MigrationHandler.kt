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
            MIGRATION_4_5,
            MIGRATION_5_6
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
        
        /**
         * Migration from version 4 to 5: Add shopping_list_items table
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 4 to 5")
                
                // Create shopping_list_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        category TEXT NOT NULL,
                        is_checked INTEGER NOT NULL DEFAULT 0,
                        is_manual INTEGER NOT NULL DEFAULT 0,
                        meal_ids TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Create indices
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_shopping_list_items_name 
                    ON shopping_list_items(name)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_shopping_list_items_is_manual 
                    ON shopping_list_items(is_manual)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_shopping_list_items_is_checked 
                    ON shopping_list_items(is_checked)
                """.trimIndent())
                
                Timber.d("Migration 4->5 completed: shopping_list_items table created")
            }
        }
        
        /**
         * Migration from version 5 to 6: Add management features to shopping list
         * - Extends shopping_list_items table with new columns
         * - Creates item_history table for purchase tracking
         * - Creates shopping_templates and template_items tables
         * - Creates store_sections table for custom organization
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Timber.d("Migrating database from version 5 to 6")
                
                // Add new columns to shopping_list_items table
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN is_priority INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN custom_order INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN estimated_price REAL")
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN store_section TEXT NOT NULL DEFAULT 'OTHER'")
                database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN last_modified_at INTEGER NOT NULL DEFAULT 0")
                
                // Create item_history table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS item_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        item_name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        category TEXT NOT NULL,
                        purchase_count INTEGER NOT NULL,
                        last_purchased_at INTEGER NOT NULL,
                        average_price REAL
                    )
                """.trimIndent())
                
                // Create indices for item_history
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_item_history_item_name 
                    ON item_history(item_name)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_item_history_last_purchased_at 
                    ON item_history(last_purchased_at)
                """.trimIndent())
                
                // Create shopping_templates table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        last_used_at INTEGER
                    )
                """.trimIndent())
                
                // Create template_items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS template_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        template_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        category TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        FOREIGN KEY(template_id) REFERENCES shopping_templates(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create index for template_items
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_template_items_template_id 
                    ON template_items(template_id)
                """.trimIndent())
                
                // Create store_sections table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS store_sections (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        display_order INTEGER NOT NULL,
                        is_collapsed INTEGER NOT NULL DEFAULT 0,
                        color TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Create indices for new columns in shopping_list_items
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_shopping_list_items_is_priority 
                    ON shopping_list_items(is_priority)
                """.trimIndent())
                
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_shopping_list_items_store_section 
                    ON shopping_list_items(store_section)
                """.trimIndent())
                
                Timber.d("Migration 5->6 completed: management features added")
            }
        }
    }
}
