package com.shoppit.app.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppResult extension functions.
 * Validates the error handling framework functionality.
 */
class AppResultTest {

    @Test
    fun `onSuccess executes action when result is successful`() {
        // Given
        val result: AppResult<String> = Result.success("test value")
        var capturedValue: String? = null

        // When
        result.onSuccess { capturedValue = it }

        // Then
        assertEquals("test value", capturedValue)
    }

    @Test
    fun `onSuccess does not execute action when result is failure`() {
        // Given
        val result: AppResult<String> = Result.failure(Exception("error"))
        var actionExecuted = false

        // When
        result.onSuccess { actionExecuted = true }

        // Then
        assertTrue(!actionExecuted)
    }

    @Test
    fun `onFailure executes action when result is failure`() {
        // Given
        val error = AppError.ValidationError("Invalid input")
        val result: AppResult<String> = error.toResult()
        var capturedError: AppError? = null

        // When
        result.onFailure { capturedError = it }

        // Then
        assertTrue(capturedError is AppError.ValidationError)
        assertEquals("Invalid input", (capturedError as AppError.ValidationError).message)
    }

    @Test
    fun `onFailure does not execute action when result is successful`() {
        // Given
        val result: AppResult<String> = Result.success("test value")
        var actionExecuted = false

        // When
        result.onFailure { actionExecuted = true }

        // Then
        assertTrue(!actionExecuted)
    }

    @Test
    fun `mapSuccess transforms successful result`() {
        // Given
        val result: AppResult<Int> = Result.success(5)

        // When
        val mapped = result.mapSuccess { it * 2 }

        // Then
        assertTrue(mapped.isSuccess)
        assertEquals(10, mapped.getOrNull())
    }

    @Test
    fun `mapSuccess preserves failure`() {
        // Given
        val result: AppResult<Int> = Result.failure(Exception("error"))

        // When
        val mapped = result.mapSuccess { it * 2 }

        // Then
        assertTrue(mapped.isFailure)
    }

    @Test
    fun `toResult converts AppError to failure result`() {
        // Given
        val error = AppError.NetworkError

        // When
        val result: AppResult<String> = error.toResult()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `chaining onSuccess and onFailure works correctly`() {
        // Given
        val result: AppResult<String> = Result.success("test")
        var successCalled = false
        var failureCalled = false

        // When
        result
            .onSuccess { successCalled = true }
            .onFailure { failureCalled = true }

        // Then
        assertTrue(successCalled)
        assertTrue(!failureCalled)
    }
}
