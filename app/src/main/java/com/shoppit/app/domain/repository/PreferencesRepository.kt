package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.ShoppingModePreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user preferences.
 * Provides access to app-wide settings and preferences.
 */
interface PreferencesRepository {
    
    /**
     * Get shopping mode preferences as a reactive Flow.
     * @return Flow of ShoppingModePreferences
     */
    fun getShoppingModePreferences(): Flow<ShoppingModePreferences>
    
    /**
     * Enable or disable shopping mode.
     * @param enabled True to enable shopping mode, false to disable
     * @return Result indicating success or failure
     */
    suspend fun setShoppingModeEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * Update shopping mode preferences.
     * @param preferences The new preferences to save
     * @return Result indicating success or failure
     */
    suspend fun updateShoppingModePreferences(preferences: ShoppingModePreferences): Result<Unit>
}
