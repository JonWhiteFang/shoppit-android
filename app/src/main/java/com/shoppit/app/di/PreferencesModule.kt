package com.shoppit.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.shoppit.app.data.repository.PreferencesRepositoryImpl
import com.shoppit.app.domain.repository.PreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing preferences-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PreferencesModule {
    
    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl
    ): PreferencesRepository
    
    companion object {
        private const val PREFERENCES_NAME = "shoppit_preferences"
        
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = PREFERENCES_NAME
        )
        
        @Provides
        @Singleton
        fun provideDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return context.dataStore
        }
    }
}
