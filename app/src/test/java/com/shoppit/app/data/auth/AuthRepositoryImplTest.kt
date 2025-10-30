package com.shoppit.app.data.auth

import com.shoppit.app.domain.error.AppError
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AuthRepositoryImpl.
 *
 * Tests cover:
 * - Sign in functionality
 * - Sign up functionality
 * - Sign out functionality
 * - Token management (get, refresh)
 * - Authentication state
 * - User profile updates
 * - Input validation
 * - Error handling
 */
@ExperimentalCoroutinesApi
class AuthRepositoryImplTest {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        tokenStorage = mockk(relaxed = true)
        
        // Default token storage behavior
        every { tokenStorage.hasTokens() } returns false
        every { tokenStorage.isTokenExpired() } returns false
        every { tokenStorage.getUserId() } returns null
        every { tokenStorage.getUserEmail() } returns null
        every { tokenStorage.getUserName() } returns null
        every { tokenStorage.getUserCreatedAt() } returns 0L
        
        authRepository = AuthRepositoryImpl(tokenStorage)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========== Sign In Tests ==========

    @Test
    fun `signIn succeeds with valid credentials`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"

        // When
        val result = authRepository.signIn(email, password)

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(email, user.email)
        assertEquals("test", user.name) // Name extracted from email
        
        // Verify tokens were saved
        verify { tokenStorage.saveAccessToken(any()) }
        verify { tokenStorage.saveRefreshToken(any()) }
        verify { tokenStorage.saveUserId(any()) }
        verify { tokenStorage.saveUserEmail(email) }
    }

    @Test
    fun `signIn fails with blank email`() = runTest {
        // Given
        val email = ""
        val password = "password123"

        // When
        val result = authRepository.signIn(email, password)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("Email and password are required", error.message)
    }

    @Test
    fun `signIn fails with blank password`() = runTest {
        // Given
        val email = "test@example.com"
        val password = ""

        // When
        val result = authRepository.signIn(email, password)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
    }

    @Test
    fun `signIn fails with invalid email format`() = runTest {
        // Given
        val email = "invalid-email"
        val password = "password123"

        // When
        val result = authRepository.signIn(email, password)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("Invalid email format", error.message)
    }

    @Test
    fun `signIn updates current user flow`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"

        // When
        authRepository.signIn(email, password)
        val currentUser = authRepository.getCurrentUser().first()

        // Then
        assertNotNull(currentUser)
        assertEquals(email, currentUser.email)
    }

    // ========== Sign Up Tests ==========

    @Test
    fun `signUp succeeds with valid data`() = runTest {
        // Given
        val email = "newuser@example.com"
        val password = "password123"
        val name = "New User"

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(email, user.email)
        assertEquals(name, user.name)
        
        // Verify tokens were saved
        verify { tokenStorage.saveAccessToken(any()) }
        verify { tokenStorage.saveRefreshToken(any()) }
    }

    @Test
    fun `signUp fails with blank email`() = runTest {
        // Given
        val email = ""
        val password = "password123"
        val name = "Test User"

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("All fields are required", error.message)
    }

    @Test
    fun `signUp fails with blank password`() = runTest {
        // Given
        val email = "test@example.com"
        val password = ""
        val name = "Test User"

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
    }

    @Test
    fun `signUp fails with blank name`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "password123"
        val name = ""

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
    }

    @Test
    fun `signUp fails with invalid email format`() = runTest {
        // Given
        val email = "invalid-email"
        val password = "password123"
        val name = "Test User"

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("Invalid email format", error.message)
    }

    @Test
    fun `signUp fails with short password`() = runTest {
        // Given
        val email = "test@example.com"
        val password = "short"
        val name = "Test User"

        // When
        val result = authRepository.signUp(email, password, name)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("Password must be at least 8 characters", error.message)
    }

    // ========== Sign Out Tests ==========

    @Test
    fun `signOut clears all stored data`() = runTest {
        // Given - user is signed in
        authRepository.signIn("test@example.com", "password123")

        // When
        val result = authRepository.signOut()

        // Then
        assertTrue(result.isSuccess)
        verify { tokenStorage.clearAll() }
        
        // Verify current user is cleared
        val currentUser = authRepository.getCurrentUser().first()
        assertNull(currentUser)
    }

    @Test
    fun `signOut succeeds even when not authenticated`() = runTest {
        // When
        val result = authRepository.signOut()

        // Then
        assertTrue(result.isSuccess)
        verify { tokenStorage.clearAll() }
    }

    // ========== Token Management Tests ==========

    @Test
    fun `getAccessToken returns stored token when not expired`() = runTest {
        // Given
        val token = "valid_token"
        every { tokenStorage.getAccessToken() } returns token
        every { tokenStorage.isTokenExpired() } returns false

        // When
        val result = authRepository.getAccessToken()

        // Then
        assertEquals(token, result)
    }

    @Test
    fun `getAccessToken returns null when no token stored`() = runTest {
        // Given
        every { tokenStorage.getAccessToken() } returns null

        // When
        val result = authRepository.getAccessToken()

        // Then
        assertNull(result)
    }

    @Test
    fun `getAccessToken refreshes token when expired`() = runTest {
        // Given
        val oldToken = "expired_token"
        val refreshToken = "refresh_token"
        every { tokenStorage.getAccessToken() } returns oldToken
        every { tokenStorage.isTokenExpired() } returns true
        every { tokenStorage.getRefreshToken() } returns refreshToken
        every { tokenStorage.saveAccessToken(any()) } just Runs
        every { tokenStorage.saveTokenExpiration(any()) } just Runs

        // When
        val result = authRepository.getAccessToken()

        // Then
        assertNotNull(result)
        verify { tokenStorage.saveAccessToken(any()) }
        verify { tokenStorage.saveTokenExpiration(any()) }
    }

    @Test
    fun `refreshAccessToken succeeds with valid refresh token`() = runTest {
        // Given
        val refreshToken = "valid_refresh_token"
        every { tokenStorage.getRefreshToken() } returns refreshToken
        every { tokenStorage.saveAccessToken(any()) } just Runs
        every { tokenStorage.saveTokenExpiration(any()) } just Runs

        // When
        val result = authRepository.refreshAccessToken()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        verify { tokenStorage.saveAccessToken(any()) }
        verify { tokenStorage.saveTokenExpiration(any()) }
    }

    @Test
    fun `refreshAccessToken fails when no refresh token available`() = runTest {
        // Given
        every { tokenStorage.getRefreshToken() } returns null

        // When
        val result = authRepository.refreshAccessToken()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.AuthenticationError)
        assertEquals("No refresh token available", error.message)
    }

    @Test
    fun `refreshAccessToken clears tokens on failure`() = runTest {
        // Given
        every { tokenStorage.getRefreshToken() } returns "invalid_token"
        every { tokenStorage.saveAccessToken(any()) } throws Exception("Network error")

        // When
        val result = authRepository.refreshAccessToken()

        // Then
        assertTrue(result.isFailure)
        verify { tokenStorage.clearAll() }
        
        // Verify current user is cleared
        val currentUser = authRepository.getCurrentUser().first()
        assertNull(currentUser)
    }

    // ========== Authentication State Tests ==========

    @Test
    fun `isAuthenticated returns true when tokens exist and not expired`() {
        // Given
        every { tokenStorage.hasTokens() } returns true
        every { tokenStorage.isTokenExpired() } returns false

        // When
        val result = authRepository.isAuthenticated()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isAuthenticated returns false when no tokens`() {
        // Given
        every { tokenStorage.hasTokens() } returns false

        // When
        val result = authRepository.isAuthenticated()

        // Then
        assertFalse(result)
    }

    @Test
    fun `isAuthenticated returns false when tokens expired`() {
        // Given
        every { tokenStorage.hasTokens() } returns true
        every { tokenStorage.isTokenExpired() } returns true

        // When
        val result = authRepository.isAuthenticated()

        // Then
        assertFalse(result)
    }

    @Test
    fun `getCurrentUser returns null initially`() = runTest {
        // When
        val user = authRepository.getCurrentUser().first()

        // Then
        assertNull(user)
    }

    @Test
    fun `getCurrentUser loads user from storage on init`() = runTest {
        // Given
        every { tokenStorage.hasTokens() } returns true
        every { tokenStorage.isTokenExpired() } returns false
        every { tokenStorage.getUserId() } returns "user_123"
        every { tokenStorage.getUserEmail() } returns "test@example.com"
        every { tokenStorage.getUserName() } returns "Test User"
        every { tokenStorage.getUserCreatedAt() } returns 1234567890L

        // When - create new instance to trigger init
        val newAuthRepository = AuthRepositoryImpl(tokenStorage)
        val user = newAuthRepository.getCurrentUser().first()

        // Then
        assertNotNull(user)
        assertEquals("user_123", user.id)
        assertEquals("test@example.com", user.email)
        assertEquals("Test User", user.name)
    }

    // ========== Profile Update Tests ==========

    @Test
    fun `updateUserProfile succeeds with valid name`() = runTest {
        // Given - user is signed in
        authRepository.signIn("test@example.com", "password123")
        val newName = "Updated Name"

        // When
        val result = authRepository.updateUserProfile(newName)

        // Then
        assertTrue(result.isSuccess)
        val updatedUser = result.getOrNull()
        assertNotNull(updatedUser)
        assertEquals(newName, updatedUser.name)
        verify { tokenStorage.saveUserName(newName) }
    }

    @Test
    fun `updateUserProfile fails when not authenticated`() = runTest {
        // Given - no user signed in
        val newName = "Updated Name"

        // When
        val result = authRepository.updateUserProfile(newName)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.AuthenticationError)
        assertEquals("No user logged in", error.message)
    }

    @Test
    fun `updateUserProfile fails with blank name`() = runTest {
        // Given - user is signed in
        authRepository.signIn("test@example.com", "password123")
        val newName = ""

        // When
        val result = authRepository.updateUserProfile(newName)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.ValidationError)
        assertEquals("Name cannot be empty", error.message)
    }

    @Test
    fun `updateUserProfile updates current user flow`() = runTest {
        // Given - user is signed in
        authRepository.signIn("test@example.com", "password123")
        val newName = "Updated Name"

        // When
        authRepository.updateUserProfile(newName)
        val currentUser = authRepository.getCurrentUser().first()

        // Then
        assertNotNull(currentUser)
        assertEquals(newName, currentUser.name)
    }
}
