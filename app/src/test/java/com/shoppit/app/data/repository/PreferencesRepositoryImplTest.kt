package com.shoppit.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.shoppit.app.domain.model.ShoppingModePreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class PreferencesRepositoryImplTest {
    
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PreferencesRepositoryImpl
    
    private val shoppingModeEnabledKey = booleanPreferencesKey("shopping_mode_enabled")
    private val hideCheckedItemsKey = booleanPreferencesKey("hide_checked_items")
    private val increasedTextSizeKey = booleanPreferencesKey("increased_text_size")
    private val showOnlyEssentialsKey = booleanPreferencesKey("show_only_essentials")
    
    @Before
    fun setup() {
        mockkStatic("androidx.datastore.preferences.core.PreferencesKt")
        dataStore = mockk()
        repository = PreferencesRepositoryImpl(dataStore)
    }
    
    @After
    fun tearDown() {
        unmockkStatic("androidx.datastore.preferences.core.PreferencesKt")
    }
    
    @Test
    fun `getShoppingModePreferences returns default values when preferences are empty`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(emptyPreferences())
        
        // When
        val result = repository.getShoppingModePreferences().first()
        
        // Then
        assertEquals(ShoppingModePreferences(), result)
        assertFalse(result.isEnabled)
        assertTrue(result.hideCheckedItems)
        assertTrue(result.increasedTextSize)
        assertTrue(result.showOnlyEssentials)
    }
    
    @Test
    fun `getShoppingModePreferences returns stored values`() = runTest {
        // Given
        val preferences = mutablePreferencesOf(
            shoppingModeEnabledKey to true,
            hideCheckedItemsKey to false,
            increasedTextSizeKey to false,
            showOnlyEssentialsKey to false
        )
        every { dataStore.data } returns flowOf(preferences)
        
        // When
        val result = repository.getShoppingModePreferences().first()
        
        // Then
        assertTrue(result.isEnabled)
        assertFalse(result.hideCheckedItems)
        assertFalse(result.increasedTextSize)
        assertFalse(result.showOnlyEssentials)
    }
    
    @Test
    fun `getShoppingModePreferences handles IOException gracefully`() = runTest {
        // Given
        every { dataStore.data } returns flowOf(emptyPreferences())
        
        // When
        val result = repository.getShoppingModePreferences().first()
        
        // Then - should return default values
        assertEquals(ShoppingModePreferences(), result)
    }
    
    @Test
    fun `setShoppingModeEnabled updates preference successfully`() = runTest {
        // Given
        coEvery { 
            dataStore.edit(any())
        } coAnswers {
            mutablePreferencesOf()
        }
        
        // When
        val result = repository.setShoppingModeEnabled(true)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dataStore.edit(any()) }
    }
    
    @Test
    fun `setShoppingModeEnabled returns failure on IOException`() = runTest {
        // Given
        coEvery { dataStore.edit(any()) } throws IOException("Write failed")
        
        // When
        val result = repository.setShoppingModeEnabled(true)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
    
    @Test
    fun `updateShoppingModePreferences updates all preferences successfully`() = runTest {
        // Given
        val preferences = ShoppingModePreferences(
            isEnabled = true,
            hideCheckedItems = false,
            increasedTextSize = false,
            showOnlyEssentials = false
        )
        coEvery { 
            dataStore.edit(any())
        } coAnswers {
            mutablePreferencesOf()
        }
        
        // When
        val result = repository.updateShoppingModePreferences(preferences)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dataStore.edit(any()) }
    }
    
    @Test
    fun `updateShoppingModePreferences returns failure on IOException`() = runTest {
        // Given
        val preferences = ShoppingModePreferences(isEnabled = true)
        coEvery { dataStore.edit(any()) } throws IOException("Write failed")
        
        // When
        val result = repository.updateShoppingModePreferences(preferences)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
    
    @Test
    fun `updateShoppingModePreferences handles unexpected exceptions`() = runTest {
        // Given
        val preferences = ShoppingModePreferences(isEnabled = true)
        coEvery { dataStore.edit(any()) } throws RuntimeException("Unexpected error")
        
        // When
        val result = repository.updateShoppingModePreferences(preferences)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }
}
