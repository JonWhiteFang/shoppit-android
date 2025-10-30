package com.shoppit.app.domain.model

/**
 * Represents the current authentication state of the application.
 *
 * @property isAuthenticated Whether a user is currently authenticated
 * @property user The authenticated user, or null if not authenticated
 * @property error Error message if authentication failed
 */
data class AuthState(
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val error: String? = null
)
