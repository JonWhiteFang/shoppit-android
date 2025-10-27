package com.shoppit.app.util

import com.shoppit.app.data.local.database.AppDatabase
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base class for database-related instrumented tests.
 * 
 * This class provides:
 * - Hilt dependency injection setup
 * - In-memory database instance (via TestDatabaseModule)
 * - Automatic database cleanup after each test
 * 
 * Usage:
 * ```
 * @HiltAndroidTest
 * class MealDaoTest : DatabaseTest() {
 *     
 *     @Test
 *     fun testInsertMeal() = runTest {
 *         // Use database instance
 *         val dao = database.mealDao()
 *         // ... test code
 *     }
 * }
 * ```
 */
@HiltAndroidTest
abstract class DatabaseTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var database: AppDatabase

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    @After
    open fun tearDown() {
        database.close()
    }
}
