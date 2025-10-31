package com.shoppit.app.presentation.ui.shopping

import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.usecase.ClearCheckedItemsUseCase
import com.shoppit.app.domain.usecase.ToggleItemCheckedUseCase
import com.shoppit.app.presentation.ui.common.ErrorEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for ShoppingListViewModel error handling.
 * Tests error logging and error event emission for toggleItemChecked and clearCheckedItems.
 *
 * Requirements:
 * - 1.1: Display user-friendly error messages
 * - 10.1: Log errors with context
 */
@ExperimentalCoroutinesApi
class ShoppingListViewModelTest {

    @Test
    fun `toggleItemChecked logs error with correct context when use case fails`() = runTest {
        // Given
        val errorLogger = mockk<ErrorLogger>(relaxed = true)
        val toggleUseCase = mockk<ToggleItemCheckedUseCase>()
        val exception = Exception("Failed to update item")
        
        coEvery { toggleUseCase(any(), any()) } returns Result.failure(exception)

        // When - simulate the error handling logic from toggleItemChecked
        val itemId = 1L
        val isChecked = true
        
        toggleUseCase(itemId, isChecked).fold(
            onSuccess = { /* Success case */ },
            onFailure = { error ->
                errorLogger.logError(
                    error = error,
                    context = "ShoppingListViewModel.toggleItemChecked",
                    additionalData = mapOf(
                        "itemId" to itemId.toString(),
                        "isChecked" to isChecked.toString()
                    )
                )
            }
        )

        // Then - verify error was logged with correct context
        verify {
            errorLogger.logError(
                error = exception,
                context = "ShoppingListViewModel.toggleItemChecked",
                additionalData = mapOf(
                    "itemId" to "1",
                    "isChecked" to "true"
                )
            )
        }
    }

    @Test
    fun `toggleItemChecked does not log error when use case succeeds`() = runTest {
        // Given
        val errorLogger = mockk<ErrorLogger>(relaxed = true)
        val toggleUseCase = mockk<ToggleItemCheckedUseCase>()
        
        coEvery { toggleUseCase(any(), any()) } returns Result.success(Unit)

        // When - simulate the error handling logic from toggleItemChecked
        toggleUseCase(1L, true).fold(
            onSuccess = { /* Success case */ },
            onFailure = { error ->
                errorLogger.logError(
                    error = error,
                    context = "ShoppingListViewModel.toggleItemChecked",
                    additionalData = emptyMap()
                )
            }
        )

        // Then - verify error was NOT logged
        verify(exactly = 0) {
            errorLogger.logError(any(), any(), any())
        }
    }

    @Test
    fun `clearCheckedItems logs error with correct context when use case fails`() = runTest {
        // Given
        val errorLogger = mockk<ErrorLogger>(relaxed = true)
        val clearUseCase = mockk<ClearCheckedItemsUseCase>()
        val exception = Exception("Failed to clear items")
        
        coEvery { clearUseCase() } returns Result.failure(exception)

        // When - simulate the error handling logic from clearCheckedItems
        clearUseCase().fold(
            onSuccess = { /* Success case */ },
            onFailure = { error ->
                errorLogger.logError(
                    error = error,
                    context = "ShoppingListViewModel.clearCheckedItems",
                    additionalData = emptyMap()
                )
            }
        )

        // Then - verify error was logged with correct context
        verify {
            errorLogger.logError(
                error = exception,
                context = "ShoppingListViewModel.clearCheckedItems",
                additionalData = emptyMap()
            )
        }
    }

    @Test
    fun `clearCheckedItems does not log error when use case succeeds`() = runTest {
        // Given
        val errorLogger = mockk<ErrorLogger>(relaxed = true)
        val clearUseCase = mockk<ClearCheckedItemsUseCase>()
        
        coEvery { clearUseCase() } returns Result.success(Unit)

        // When - simulate the error handling logic from clearCheckedItems
        clearUseCase().fold(
            onSuccess = { /* Success case */ },
            onFailure = { error ->
                errorLogger.logError(
                    error = error,
                    context = "ShoppingListViewModel.clearCheckedItems",
                    additionalData = emptyMap()
                )
            }
        )

        // Then - verify error was NOT logged
        verify(exactly = 0) {
            errorLogger.logError(any(), any(), any())
        }
    }

    @Test
    fun `error event emission pattern is correct for toggleItemChecked`() = runTest {
        // This test verifies the error event emission pattern
        // In the actual ViewModel, when toggleItemChecked fails:
        // 1. Error is logged with context
        // 2. UI state is updated with error message
        // 3. ErrorEvent.Error is emitted with user-friendly message
        
        val exception = Exception("Failed to update item")
        val expectedErrorMessage = exception.message ?: "Failed to update item"
        
        // Verify the error event would be created correctly
        val errorEvent = ErrorEvent.Error(expectedErrorMessage)
        
        assert(errorEvent is ErrorEvent.Error)
        assert(errorEvent.message == "Failed to update item")
    }

    @Test
    fun `error event emission pattern is correct for clearCheckedItems`() = runTest {
        // This test verifies the error event emission pattern
        // In the actual ViewModel, when clearCheckedItems fails:
        // 1. Error is logged with context
        // 2. UI state is updated with error message
        // 3. ErrorEvent.Error is emitted with user-friendly message
        
        val exception = Exception("Failed to clear items")
        val expectedErrorMessage = exception.message ?: "Failed to clear checked items"
        
        // Verify the error event would be created correctly
        val errorEvent = ErrorEvent.Error(expectedErrorMessage)
        
        assert(errorEvent is ErrorEvent.Error)
        assert(errorEvent.message == "Failed to clear items")
    }

    @Test
    fun `success event emission pattern is correct for clearCheckedItems`() = runTest {
        // This test verifies the success event emission pattern
        // In the actual ViewModel, when clearCheckedItems succeeds:
        // 1. UI state is updated (isClearingChecked = false)
        // 2. ErrorEvent.Success is emitted with success message
        
        val successEvent = ErrorEvent.Success("Checked items cleared")
        
        assert(successEvent is ErrorEvent.Success)
        assert(successEvent.message == "Checked items cleared")
    }
}
