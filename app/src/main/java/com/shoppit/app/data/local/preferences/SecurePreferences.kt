package com.shoppit.app.data.local.preferences

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper for encrypted SharedPreferences providing type-safe access to secure storage.
 * Uses EncryptedSharedPreferences under the hood for data encryption at rest.
 */
class SecurePreferences(
    private val sharedPreferences: SharedPreferences
) {
    
    /**
     * Store a string value securely
     */
    fun putString(key: String, value: String) {
        sharedPreferences.edit {
            putString(key, value)
        }
    }
    
    /**
     * Retrieve a string value
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }
    
    /**
     * Store a boolean value securely
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit {
            putBoolean(key, value)
        }
    }
    
    /**
     * Retrieve a boolean value
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }
    
    /**
     * Store an integer value securely
     */
    fun putInt(key: String, value: Int) {
        sharedPreferences.edit {
            putInt(key, value)
        }
    }
    
    /**
     * Retrieve an integer value
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }
    
    /**
     * Store a long value securely
     */
    fun putLong(key: String, value: Long) {
        sharedPreferences.edit {
            putLong(key, value)
        }
    }
    
    /**
     * Retrieve a long value
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }
    
    /**
     * Store a float value securely
     */
    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit {
            putFloat(key, value)
        }
    }
    
    /**
     * Retrieve a float value
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }
    
    /**
     * Remove a specific key
     */
    fun remove(key: String) {
        sharedPreferences.edit {
            remove(key)
        }
    }
    
    /**
     * Clear all stored data
     */
    fun clear() {
        sharedPreferences.edit {
            clear()
        }
    }
    
    /**
     * Check if a key exists
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
    
    companion object {
        // Common preference keys
        const val KEY_USER_TOKEN = "user_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_FIRST_LAUNCH = "first_launch"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
