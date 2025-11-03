package com.shoppit.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.model.ShoppingModePreferences
import com.shoppit.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PreferencesRepository using DataStore.
 * Provides persistent storage for user preferences.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PreferencesRepository {
    
    private object PreferencesKeys {
        val SHOPPING_MODE_ENABLED = booleanPreferencesKey("shopping_mode_enabled")
        val HIDE_CHECKED_ITEMS = booleanPreferencesKey("hide_checked_items")
        val INCREASED_TEXT_SIZE = booleanPreferencesKey("increased_text_size")
        val SHOW_ONLY_ESSENTIALS = booleanPreferencesKey("show_only_essentials")
    }
    
    override fun getShoppingModePreferences(): Flow<ShoppingModePreferences> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Timber.e(exception, "Error reading preferences")
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                ShoppingModePreferences(
                    isEnabled = preferences[PreferencesKeys.SHOPPING_MODE_ENABLED] ?: false,
                    hideCheckedItems = preferences[PreferencesKeys.HIDE_CHECKED_ITEMS] ?: true,
                    increasedTextSize = preferences[PreferencesKeys.INCREASED_TEXT_SIZE] ?: true,
                    showOnlyEssentials = preferences[PreferencesKeys.SHOW_ONLY_ESSENTIALS] ?: true
                )
            }
    }
    
    override suspend fun setShoppingModeEnabled(enabled: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOPPING_MODE_ENABLED] = enabled
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "Error setting shopping mode enabled")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error setting shopping mode enabled")
            Result.failure(e)
        }
    }
    
    override suspend fun updateShoppingModePreferences(
        preferences: ShoppingModePreferences
    ): Result<Unit> {
        return try {
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.SHOPPING_MODE_ENABLED] = preferences.isEnabled
                prefs[PreferencesKeys.HIDE_CHECKED_ITEMS] = preferences.hideCheckedItems
                prefs[PreferencesKeys.INCREASED_TEXT_SIZE] = preferences.increasedTextSize
                prefs[PreferencesKeys.SHOW_ONLY_ESSENTIALS] = preferences.showOnlyEssentials
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Timber.e(e, "Error updating shopping mode preferences")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error updating shopping mode preferences")
            Result.failure(e)
        }
    }
}
