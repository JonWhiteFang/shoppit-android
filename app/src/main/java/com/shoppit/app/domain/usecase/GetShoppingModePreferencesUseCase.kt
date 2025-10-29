package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingModePreferences
import com.shoppit.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving shopping mode preferences.
 * Provides a reactive stream of preference updates.
 */
class GetShoppingModePreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    /**
     * Get shopping mode preferences as a Flow.
     * @return Flow of ShoppingModePreferences that emits whenever preferences change
     */
    operator fun invoke(): Flow<ShoppingModePreferences> {
        return preferencesRepository.getShoppingModePreferences()
    }
}
