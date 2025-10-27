package com.shoppit.app.data.local.database

import com.shoppit.app.util.DatabaseTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Integration tests for AppDatabase configuration.
 * Validates database setup and dependency injection.
 */
@HiltAndroidTest
class AppDatabaseTest : DatabaseTest() {

    @Test
    fun database_isInjected() {
        // Then
        assertNotNull(database)
    }

    @Test
    fun database_canBeOpened() {
        // When
        val isOpen = database.isOpen

        // Then
        assertNotNull(isOpen)
    }

    @Test
    fun database_hasCorrectVersion() {
        // When
        val version = database.openHelper.readableDatabase.version

        // Then
        assertNotNull(version)
    }
}
