package com.shoppit.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.shoppit.app.data.local.preferences.SecurePreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing security-related dependencies including encrypted SharedPreferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    
    private const val ENCRYPTED_PREFS_FILE_NAME = "shoppit_secure_prefs"
    
    /**
     * Provides MasterKey for encryption.
     * Uses AES256_GCM scheme for strong encryption.
     */
    @Provides
    @Singleton
    fun provideMasterKey(
        @ApplicationContext context: Context
    ): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    /**
     * Provides EncryptedSharedPreferences instance.
     * Encrypts both keys and values using AES256_SIV and AES256_GCM respectively.
     */
    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context,
        masterKey: MasterKey
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Provides SecurePreferences wrapper for type-safe access to encrypted storage.
     */
    @Provides
    @Singleton
    fun provideSecurePreferences(
        sharedPreferences: SharedPreferences
    ): SecurePreferences {
        return SecurePreferences(sharedPreferences)
    }
}
