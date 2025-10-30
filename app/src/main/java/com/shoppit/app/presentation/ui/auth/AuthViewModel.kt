package com.shoppit.app.presentation.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.model.AuthState
import com.shoppit.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for authentication screens.
 * Manages sign-in, sign-up, and authentication state.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check if user is already authenticated
        checkAuthenticationStatus()
    }

    /**
     * Check if user is already authenticated on app start.
     */
    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                if (user != null && authRepository.isAuthenticated()) {
                    _uiState.update { it.copy(isAuthenticated = true) }
                }
            }
        }
    }

    /**
     * Sign in with email and password.
     */
    fun signIn(email: String, password: String) {
        // Clear previous errors
        _uiState.update { it.copy(error = null, validationErrors = emptyMap()) }

        // Validate input
        val validationErrors = validateSignInInput(email, password)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = validationErrors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.signIn(email, password).fold(
                onSuccess = { user ->
                    Timber.d("Sign in successful: ${user.email}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Sign in failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Sign in failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    /**
     * Sign up with email, password, and name.
     */
    fun signUp(email: String, password: String, confirmPassword: String, name: String) {
        // Clear previous errors
        _uiState.update { it.copy(error = null, validationErrors = emptyMap()) }

        // Validate input
        val validationErrors = validateSignUpInput(email, password, confirmPassword, name)
        if (validationErrors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = validationErrors) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.signUp(email, password, name).fold(
                onSuccess = { user ->
                    Timber.d("Sign up successful: ${user.email}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    Timber.e(error, "Sign up failed")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Sign up failed. Please try again."
                        )
                    }
                }
            )
        }
    }

    /**
     * Skip authentication and continue in offline-only mode.
     */
    fun skipAuthentication() {
        Timber.d("User skipped authentication")
        _uiState.update {
            it.copy(
                isAuthenticated = false,
                skippedAuth = true
            )
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Validate sign-in input.
     */
    private fun validateSignInInput(email: String, password: String): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (email.isBlank()) {
            errors["email"] = "Email is required"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Invalid email format"
        }

        if (password.isBlank()) {
            errors["password"] = "Password is required"
        }

        return errors
    }

    /**
     * Validate sign-up input.
     */
    private fun validateSignUpInput(
        email: String,
        password: String,
        confirmPassword: String,
        name: String
    ): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (name.isBlank()) {
            errors["name"] = "Name is required"
        }

        if (email.isBlank()) {
            errors["email"] = "Email is required"
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errors["email"] = "Invalid email format"
        }

        if (password.isBlank()) {
            errors["password"] = "Password is required"
        } else if (password.length < 8) {
            errors["password"] = "Password must be at least 8 characters"
        }

        if (confirmPassword.isBlank()) {
            errors["confirmPassword"] = "Please confirm your password"
        } else if (password != confirmPassword) {
            errors["confirmPassword"] = "Passwords do not match"
        }

        return errors
    }
}

/**
 * UI state for authentication screens.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val skippedAuth: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
