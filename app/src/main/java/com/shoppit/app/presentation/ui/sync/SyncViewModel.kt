package com.shoppit.app.presentation.ui.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.SyncStatus
import com.shoppit.app.domain.repository.AuthRepository
import com.shoppit.app.domain.repository.SyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for sync status monitoring and manual sync triggering.
 * Observes sync status from SyncEngine and provides UI state.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncEngine: SyncEngine,
    private val authRepository: AuthRepository,
    private val errorLogger: ErrorLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        observeSyncStatus()
        observeAuthenticationStatus()
        loadPendingChanges()
    }

    /**
     * Observe sync status from SyncEngine.
     */
    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncEngine.observeSyncStatus().collect { status ->
                _uiState.update { currentState ->
                    currentState.copy(
                        syncStatus = status,
                        lastSyncTime = syncEngine.getLastSyncTime(),
                        isSyncing = status == SyncStatus.SYNCING
                    )
                }
            }
        }
    }

    /**
     * Observe authentication status to determine if sync is available.
     */
    private fun observeAuthenticationStatus() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(isAuthenticated = user != null) }
            }
        }
    }

    /**
     * Load pending changes count.
     */
    private fun loadPendingChanges() {
        viewModelScope.launch {
            try {
                val pendingChanges = syncEngine.getPendingChanges()
                _uiState.update { it.copy(pendingChangesCount = pendingChanges.size) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load pending changes")
            }
        }
    }

    /**
     * Trigger manual sync.
     */
    fun triggerManualSync() {
        if (!_uiState.value.isAuthenticated) {
            _uiState.update {
                it.copy(
                    errorMessage = "Sign in required to sync data",
                    showError = true
                )
            }
            return
        }

        if (_uiState.value.isSyncing) {
            Timber.d("Sync already in progress, ignoring manual trigger")
            return
        }

        viewModelScope.launch {
            try {
                Timber.d("Manual sync triggered")
                _uiState.update { it.copy(isSyncing = true, errorMessage = null, showError = false) }

                syncEngine.forceSyncNow().fold(
                    onSuccess = { result ->
                        Timber.d("Manual sync completed: $result")
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                lastSyncTime = result.timestamp,
                                successMessage = "Synced ${result.syncedEntities} items",
                                showSuccess = true
                            )
                        }
                        // Reload pending changes
                        loadPendingChanges()
                    },
                    onFailure = { error ->
                        errorLogger.logError(error, "SyncViewModel.triggerManualSync")
                        handleSyncError(error)
                    }
                )
            } catch (e: Exception) {
                errorLogger.logError(e, "SyncViewModel.triggerManualSync")
                handleSyncError(e)
            }
        }
    }

    /**
     * Dismiss success message.
     */
    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccess = false, successMessage = null) }
    }

    /**
     * Dismiss error message.
     */
    fun dismissErrorMessage() {
        _uiState.update { it.copy(showError = false, errorMessage = null) }
    }

    /**
     * Handle sync errors with user-friendly messages.
     * Network errors show "Using offline data" message.
     * Other errors show specific error messages.
     */
    private fun handleSyncError(error: Throwable) {
        val (errorMessage, isOfflineMode) = when (error) {
            is AppError.NetworkError -> {
                "Unable to sync. Using offline data." to true
            }
            is AppError.AuthenticationError -> {
                "Authentication failed. Please sign in again." to false
            }
            else -> {
                error.message ?: "Sync failed. Using offline data." to true
            }
        }

        _uiState.update {
            it.copy(
                isSyncing = false,
                errorMessage = errorMessage,
                showError = true,
                isOfflineMode = isOfflineMode
            )
        }
    }
}

/**
 * UI state for sync status display.
 */
data class SyncUiState(
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val isSyncing: Boolean = false,
    val isAuthenticated: Boolean = false,
    val lastSyncTime: Long? = null,
    val pendingChangesCount: Int = 0,
    val errorMessage: String? = null,
    val showError: Boolean = false,
    val successMessage: String? = null,
    val showSuccess: Boolean = false,
    val isOfflineMode: Boolean = false
)
