package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations.
 * Manages user authentication, token storage, and session management.
 */
interface AuthRepository {
    /**
     * Sign in with email and password.
     *
     * @param email User's email address
     * @param password User's password
     * @return Result containing the authenticated User or an error
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Create a new account with email, password, and name.
     *
     * @param email User's email address
     * @param password User's password
     * @param name User's display name
     * @return Result containing the newly created User or an error
     */
    suspend fun signUp(email: String, password: String, name: String): Result<User>

    /**
     * Sign out the current user and clear all authentication tokens.
     *
     * @return Result indicating success or failure
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Get the current access token for API requests.
     *
     * @return The access token, or null if not authenticated
     */
    suspend fun getAccessToken(): String?

    /**
     * Refresh the access token using the refresh token.
     *
     * @return Result containing the new access token or an error
     */
    suspend fun refreshAccessToken(): Result<String>

    /**
     * Check if a user is currently authenticated.
     *
     * @return True if authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean

    /**
     * Observe the current user state.
     *
     * @return Flow emitting the current User or null if not authenticated
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Update the current user's profile information.
     *
     * @param name New display name
     * @return Result containing the updated User or an error
     */
    suspend fun updateUserProfile(name: String): Result<User>
}
