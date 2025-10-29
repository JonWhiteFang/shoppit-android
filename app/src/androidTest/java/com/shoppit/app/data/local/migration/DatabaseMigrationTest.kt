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
}
