package com.shoppit.app.data.local.migration

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shoppit.app.data.local.database.AppDatabase
import com.shoppit.app.data.local.database.migration.MigrationHandlerImpl
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for database migrations.
 * Tests that migrations preserve data integrity when upgrading schema versions.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    
    private val TEST_DB_NAME = "migration_test"
    private val migrationHandler = MigrationHandlerImpl()
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        migrationHandler.getMigrations(),
        FrameworkSQLiteOpenHelperFactory()
    )
    
    @Test
    @Throws(IOException::class)
    fun `database can be created from scratch`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // When
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        ).build()
        
        // Then
        val dbHelper = db.openHelper
        dbHelper.writableDatabase.close()
        assertTrue(dbHelper.databaseName == TEST_DB_NAME)
    }
    
    @Test
    @Throws(IOException::class)
    fun `database maintains data integrity after migration`() {
        // Given - Create database at version 1
        val db = helper.createDatabase(TEST_DB_NAME, 1)
        
        // Insert test data
        db.execSQL(
            """
            INSERT INTO meals (id, name, ingredients, notes, created_at, updated_at)
            VALUES (1, 'Test Meal', '[]', 'Test notes', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to latest version
        val context = ApplicationProvider.getApplicationContext<Context>()
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        ).build()
        
        // Then - Verify data is preserved
        val cursor = migratedDb.openHelper.writableDatabase.query(
            "SELECT * FROM meals WHERE id = 1"
        )
        
        assertTrue(cursor.moveToFirst())
        val nameIndex = cursor.getColumnIndex("name")
        assertTrue(nameIndex >= 0)
        assertEquals("Test Meal", cursor.getString(nameIndex))
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `database schema is valid after creation`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // When
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        ).build()
        
        // Then - Verify tables exist
        val cursor = db.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table'"
        )
        
        val tables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()
        
        assertTrue(tables.contains("meals"))
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `database indices are created correctly`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // When
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        ).build()
        
        // Then - Verify indices exist
        val cursor = db.openHelper.writableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='meals'"
        )
        
        val indices = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indices.add(cursor.getString(0))
        }
        cursor.close()
        
        // Should have at least one index (Room creates some automatically)
        assertTrue(indices.isNotEmpty())
        
        db.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `database handles large data sets during migration`() {
        // Given - Create database with multiple records
        val db = helper.createDatabase(TEST_DB_NAME, 1)
        
        // Insert 100 test records
        for (i in 1..100) {
            db.execSQL(
                """
                INSERT INTO meals (id, name, ingredients, notes, created_at, updated_at)
                VALUES ($i, 'Meal $i', '[]', 'Notes $i', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                """.trimIndent()
            )
        }
        
        db.close()
        
        // When - Migrate to latest version
        val context = ApplicationProvider.getApplicationContext<Context>()
        val migratedDb = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            TEST_DB_NAME
        ).build()
        
        // Then - Verify all records are preserved
        val cursor = migratedDb.openHelper.writableDatabase.query(
            "SELECT COUNT(*) FROM meals"
        )
        
        assertTrue(cursor.moveToFirst())
        assertEquals(100, cursor.getInt(0))
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 6 to 7 adds tags column with default empty string`() {
        // Given - Create database at version 6 with test meal
        val db = helper.createDatabase(TEST_DB_NAME, 6)
        
        // Insert test meal without tags column
        db.execSQL(
            """
            INSERT INTO meals (id, name, ingredients, notes, created_at, updated_at)
            VALUES (1, 'Test Meal', '[]', 'Test notes', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to version 7
        helper.runMigrationsAndValidate(TEST_DB_NAME, 7, true)
        
        // Then - Verify tags column exists with default empty string
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 7, true)
        
        val cursor = migratedDb.query("SELECT id, name, tags FROM meals WHERE id = 1")
        assertTrue(cursor.moveToFirst())
        
        val tagsIndex = cursor.getColumnIndex("tags")
        assertTrue("tags column should exist", tagsIndex >= 0)
        
        val tags = cursor.getString(tagsIndex)
        assertEquals("tags should default to empty string", "", tags)
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 6 to 7 preserves existing meal data`() {
        // Given - Create database at version 6 with multiple meals
        val db = helper.createDatabase(TEST_DB_NAME, 6)
        
        val testMeals = listOf(
            Triple(1L, "Pasta Carbonara", "Delicious pasta"),
            Triple(2L, "Caesar Salad", "Fresh salad"),
            Triple(3L, "Grilled Chicken", "Healthy protein")
        )
        
        testMeals.forEach { (id, name, notes) ->
            db.execSQL(
                """
                INSERT INTO meals (id, name, ingredients, notes, created_at, updated_at)
                VALUES ($id, '$name', '[]', '$notes', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                """.trimIndent()
            )
        }
        
        db.close()
        
        // When - Migrate to version 7
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 7, true)
        
        // Then - Verify all meals are preserved with tags column
        testMeals.forEach { (id, name, notes) ->
            val cursor = migratedDb.query("SELECT id, name, notes, tags FROM meals WHERE id = $id")
            assertTrue("Meal $id should exist", cursor.moveToFirst())
            
            val nameIndex = cursor.getColumnIndex("name")
            val notesIndex = cursor.getColumnIndex("notes")
            val tagsIndex = cursor.getColumnIndex("tags")
            
            assertEquals("Name should be preserved", name, cursor.getString(nameIndex))
            assertEquals("Notes should be preserved", notes, cursor.getString(notesIndex))
            assertEquals("Tags should default to empty string", "", cursor.getString(tagsIndex))
            
            cursor.close()
        }
        
        migratedDb.close()
    }
    
    // ========== Migration 7 to 8 Tests (Sync Fields) ==========
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 creates sync_metadata table`() {
        // Given - Create database at version 7
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify sync_metadata table exists
        val cursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='sync_metadata'"
        )
        assertTrue("sync_metadata table should exist", cursor.moveToFirst())
        cursor.close()
        
        // Verify table structure
        val tableInfoCursor = migratedDb.query("PRAGMA table_info(sync_metadata)")
        val columns = mutableListOf<String>()
        while (tableInfoCursor.moveToNext()) {
            val columnNameIndex = tableInfoCursor.getColumnIndex("name")
            columns.add(tableInfoCursor.getString(columnNameIndex))
        }
        tableInfoCursor.close()
        
        assertTrue("Should have id column", columns.contains("id"))
        assertTrue("Should have entity_type column", columns.contains("entity_type"))
        assertTrue("Should have entity_id column", columns.contains("entity_id"))
        assertTrue("Should have server_id column", columns.contains("server_id"))
        assertTrue("Should have last_synced_at column", columns.contains("last_synced_at"))
        assertTrue("Should have local_updated_at column", columns.contains("local_updated_at"))
        assertTrue("Should have sync_status column", columns.contains("sync_status"))
        assertTrue("Should have retry_count column", columns.contains("retry_count"))
        assertTrue("Should have error_message column", columns.contains("error_message"))
        
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 creates sync_queue table`() {
        // Given - Create database at version 7
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify sync_queue table exists
        val cursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='sync_queue'"
        )
        assertTrue("sync_queue table should exist", cursor.moveToFirst())
        cursor.close()
        
        // Verify table structure
        val tableInfoCursor = migratedDb.query("PRAGMA table_info(sync_queue)")
        val columns = mutableListOf<String>()
        while (tableInfoCursor.moveToNext()) {
            val columnNameIndex = tableInfoCursor.getColumnIndex("name")
            columns.add(tableInfoCursor.getString(columnNameIndex))
        }
        tableInfoCursor.close()
        
        assertTrue("Should have id column", columns.contains("id"))
        assertTrue("Should have entity_type column", columns.contains("entity_type"))
        assertTrue("Should have entity_id column", columns.contains("entity_id"))
        assertTrue("Should have operation column", columns.contains("operation"))
        assertTrue("Should have payload column", columns.contains("payload"))
        assertTrue("Should have created_at column", columns.contains("created_at"))
        assertTrue("Should have retry_count column", columns.contains("retry_count"))
        assertTrue("Should have last_attempt_at column", columns.contains("last_attempt_at"))
        
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 adds sync fields to meals table`() {
        // Given - Create database at version 7 with test meal
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        
        db.execSQL(
            """
            INSERT INTO meals (id, name, ingredients, notes, tags, created_at, updated_at)
            VALUES (1, 'Test Meal', '[]', 'Test notes', '', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify sync fields exist with default values
        val cursor = migratedDb.query("SELECT id, name, server_id, sync_status FROM meals WHERE id = 1")
        assertTrue("Meal should exist", cursor.moveToFirst())
        
        val serverIdIndex = cursor.getColumnIndex("server_id")
        val syncStatusIndex = cursor.getColumnIndex("sync_status")
        
        assertTrue("server_id column should exist", serverIdIndex >= 0)
        assertTrue("sync_status column should exist", syncStatusIndex >= 0)
        
        // server_id should be null by default
        assertTrue("server_id should be null", cursor.isNull(serverIdIndex))
        
        // sync_status should default to 'pending'
        assertEquals("sync_status should default to pending", "pending", cursor.getString(syncStatusIndex))
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 adds sync fields to meal_plans table`() {
        // Given - Create database at version 7 with test meal and meal plan
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        
        // Insert meal first (foreign key constraint)
        db.execSQL(
            """
            INSERT INTO meals (id, name, ingredients, notes, tags, created_at, updated_at)
            VALUES (1, 'Test Meal', '[]', '', '', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        // Insert meal plan
        db.execSQL(
            """
            INSERT INTO meal_plans (id, meal_id, date, meal_type, created_at)
            VALUES (1, 1, ${System.currentTimeMillis()}, 'LUNCH', ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify sync fields exist with default values
        val cursor = migratedDb.query("SELECT id, server_id, sync_status FROM meal_plans WHERE id = 1")
        assertTrue("Meal plan should exist", cursor.moveToFirst())
        
        val serverIdIndex = cursor.getColumnIndex("server_id")
        val syncStatusIndex = cursor.getColumnIndex("sync_status")
        
        assertTrue("server_id column should exist", serverIdIndex >= 0)
        assertTrue("sync_status column should exist", syncStatusIndex >= 0)
        
        assertTrue("server_id should be null", cursor.isNull(serverIdIndex))
        assertEquals("sync_status should default to pending", "pending", cursor.getString(syncStatusIndex))
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 adds sync fields to shopping_list_items table`() {
        // Given - Create database at version 7 with test shopping list item
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        
        db.execSQL(
            """
            INSERT INTO shopping_list_items (
                id, name, quantity, unit, category, is_checked, is_manual, meal_ids, created_at,
                notes, is_priority, custom_order, estimated_price, store_section, last_modified_at
            )
            VALUES (
                1, 'Tomato', '2', 'pcs', 'VEGETABLES', 0, 0, '[]', ${System.currentTimeMillis()},
                '', 0, 0, NULL, 'OTHER', 0
            )
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify sync fields exist with default values
        val cursor = migratedDb.query("SELECT id, name, server_id, sync_status FROM shopping_list_items WHERE id = 1")
        assertTrue("Shopping list item should exist", cursor.moveToFirst())
        
        val serverIdIndex = cursor.getColumnIndex("server_id")
        val syncStatusIndex = cursor.getColumnIndex("sync_status")
        
        assertTrue("server_id column should exist", serverIdIndex >= 0)
        assertTrue("sync_status column should exist", syncStatusIndex >= 0)
        
        assertTrue("server_id should be null", cursor.isNull(serverIdIndex))
        assertEquals("sync_status should default to pending", "pending", cursor.getString(syncStatusIndex))
        
        cursor.close()
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 preserves all existing data`() {
        // Given - Create database at version 7 with comprehensive test data
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        
        // Insert multiple meals
        val testMeals = listOf(
            Triple(1L, "Pasta", "Italian dish"),
            Triple(2L, "Salad", "Healthy greens"),
            Triple(3L, "Soup", "Warm comfort")
        )
        
        testMeals.forEach { (id, name, notes) ->
            db.execSQL(
                """
                INSERT INTO meals (id, name, ingredients, notes, tags, created_at, updated_at)
                VALUES ($id, '$name', '[]', '$notes', '', ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
                """.trimIndent()
            )
        }
        
        // Insert meal plans
        db.execSQL(
            """
            INSERT INTO meal_plans (id, meal_id, date, meal_type, created_at)
            VALUES (1, 1, ${System.currentTimeMillis()}, 'LUNCH', ${System.currentTimeMillis()})
            """.trimIndent()
        )
        
        // Insert shopping list items
        db.execSQL(
            """
            INSERT INTO shopping_list_items (
                id, name, quantity, unit, category, is_checked, is_manual, meal_ids, created_at,
                notes, is_priority, custom_order, estimated_price, store_section, last_modified_at
            )
            VALUES (
                1, 'Tomato', '2', 'pcs', 'VEGETABLES', 0, 0, '[]', ${System.currentTimeMillis()},
                '', 0, 0, NULL, 'OTHER', 0
            )
            """.trimIndent()
        )
        
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify all data is preserved
        
        // Check meals
        val mealsCursor = migratedDb.query("SELECT COUNT(*) FROM meals")
        assertTrue(mealsCursor.moveToFirst())
        assertEquals("All meals should be preserved", 3, mealsCursor.getInt(0))
        mealsCursor.close()
        
        // Check meal plans
        val plansCursor = migratedDb.query("SELECT COUNT(*) FROM meal_plans")
        assertTrue(plansCursor.moveToFirst())
        assertEquals("All meal plans should be preserved", 1, plansCursor.getInt(0))
        plansCursor.close()
        
        // Check shopping list items
        val itemsCursor = migratedDb.query("SELECT COUNT(*) FROM shopping_list_items")
        assertTrue(itemsCursor.moveToFirst())
        assertEquals("All shopping list items should be preserved", 1, itemsCursor.getInt(0))
        itemsCursor.close()
        
        // Verify specific meal data
        testMeals.forEach { (id, name, notes) ->
            val cursor = migratedDb.query("SELECT name, notes, sync_status FROM meals WHERE id = $id")
            assertTrue("Meal $id should exist", cursor.moveToFirst())
            
            val nameIndex = cursor.getColumnIndex("name")
            val notesIndex = cursor.getColumnIndex("notes")
            val syncStatusIndex = cursor.getColumnIndex("sync_status")
            
            assertEquals("Name should be preserved", name, cursor.getString(nameIndex))
            assertEquals("Notes should be preserved", notes, cursor.getString(notesIndex))
            assertEquals("Sync status should default to pending", "pending", cursor.getString(syncStatusIndex))
            
            cursor.close()
        }
        
        migratedDb.close()
    }
    
    @Test
    @Throws(IOException::class)
    fun `migration from 7 to 8 creates proper indices`() {
        // Given - Create database at version 7
        val db = helper.createDatabase(TEST_DB_NAME, 7)
        db.close()
        
        // When - Migrate to version 8
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 8, true)
        
        // Then - Verify indices exist
        val cursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='sync_metadata'"
        )
        
        val indices = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indices.add(cursor.getString(0))
        }
        cursor.close()
        
        // Check for expected indices
        assertTrue(
            "Should have index on entity_type and entity_id",
            indices.any { it.contains("entity_type") && it.contains("entity_id") }
        )
        assertTrue(
            "Should have index on sync_status",
            indices.any { it.contains("sync_status") }
        )
        
        // Check sync_queue indices
        val queueCursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='sync_queue'"
        )
        
        val queueIndices = mutableListOf<String>()
        while (queueCursor.moveToNext()) {
            queueIndices.add(queueCursor.getString(0))
        }
        queueCursor.close()
        
        assertTrue(
            "Should have index on created_at",
            queueIndices.any { it.contains("created_at") }
        )
        assertTrue(
            "Should have index on entity_type and entity_id",
            queueIndices.any { it.contains("entity_type") && it.contains("entity_id") }
        )
        
        migratedDb.close()
    }
}
