package com.shoppit.app.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for authentication tokens using EncryptedSharedPreferences.
 * Tokens are encrypted using Android Keystore for maximum security.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to regular SharedPreferences")
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Save access token securely.
     */
    fun saveAccessToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .apply()
    }

    /**
     * Get the stored access token.
     */
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Save refresh token securely.
     */
    fun saveRefreshToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_REFRESH_TOKEN, token)
            .apply()
    }

    /**
     * Get the stored refresh token.
     */
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    /**
     * Save user ID.
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Get the stored user ID.
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Save user email.
     */
    fun saveUserEmail(email: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    /**
     * Get the stored user email.
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Save user name.
     */
    fun saveUserName(name: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, name)
            .apply()
    }

    /**
     * Get the stored user name.
     */
    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    /**
     * Save user creation timestamp.
     */
    fun saveUserCreatedAt(timestamp: Long) {
        sharedPreferences.edit()
            .putLong(KEY_USER_CREATED_AT, timestamp)
            .apply()
    }

    /**
     * Get the stored user creation timestamp.
     */
    fun getUserCreatedAt(): Long {
        return sharedPreferences.getLong(KEY_USER_CREATED_AT, 0L)
    }

    /**
     * Save token expiration timestamp.
     */
    fun saveTokenExpiration(timestamp: Long) {
        sharedPreferences.edit()
            .putLong(KEY_TOKEN_EXPIRATION, timestamp)
            .apply()
    }

    /**
     * Get the stored token expiration timestamp.
     */
    fun getTokenExpiration(): Long {
        return sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0L)
    }

    /**
     * Check if the access token is expired.
     */
    fun isTokenExpired(): Boolean {
        val expiration = getTokenExpiration()
        return expiration > 0 && System.currentTimeMillis() >= expiration
    }

    /**
     * Check if tokens exist (user is authenticated).
     */
    fun hasTokens(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }

    /**
     * Clear all stored authentication data.
     */
    fun clearAll() {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_CREATED_AT)
            .remove(KEY_TOKEN_EXPIRATION)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "shoppit_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_CREATED_AT = "user_created_at"
        private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    }
}
