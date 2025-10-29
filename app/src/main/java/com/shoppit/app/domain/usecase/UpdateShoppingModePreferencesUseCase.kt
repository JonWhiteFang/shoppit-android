package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingModePreferences
import com.shoppit.app.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Use case for updating shopping mode preferences.
 * Handles validation and persistence of preference changes.
 */
class UpdateShoppingModePreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Update shopping mode preferences.
     * @param preferences The new preferences to save
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(preferences: ShoppingModePreferences): Result<Unit> {
        return preferencesRepository.updateShoppingModePreferences(preferences)
    }
}
