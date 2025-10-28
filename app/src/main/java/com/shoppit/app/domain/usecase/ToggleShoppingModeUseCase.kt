package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingModePreferences
import com.shoppit.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for toggling shopping mode on/off.
 * Shopping mode provides a simplified interface optimized for active shopping.
 */
class ToggleShoppingModeUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Enable or disable shopping mode.
     * @param enabled True to enable shopping mode, false to disable
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return preferencesRepository.setShoppingModeEnabled(enabled)
    }
    
    /**
     * Get the current shopping mode preferences as a reactive Flow.
     * @return Flow of ShoppingModePreferences
     */
    fun getShoppingModeState(): Flow<ShoppingModePreferences> {
        return preferencesRepository.getShoppingModePreferences()
    }
}
