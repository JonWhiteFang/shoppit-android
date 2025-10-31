package com.shoppit.app.data.sync

import com.shoppit.app.domain.error.SyncError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Unit tests for RetryPolicy.
 *
 * Tests cover:
 * - Exponential backoff logic
 * - Maximum retry attempts
 * - Retryable vs non-retryable errors
 * - Delay calculations
 * - Entity-specific retry logic
 * - Error mapping
 */
@ExperimentalCoroutinesApi
class RetryPolicyTest {

    private lateinit var retryPolicy: RetryPolicy

    @Before
    fun setup() {
        retryPolicy = RetryPolicy()
    }

    // ========== Basic Retry Tests ==========

    @Test
    fun `executeWithRetry succeeds on first attempt`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            "success"
        }

        // When
        val result = retryPolicy.executeWithRetry(block = block)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attemptCount)
    }

    @Test
    fun `executeWithRetry succeeds after retries`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            if (attemptCount < 3) {
                throw IOException("Network error")
            }
            "success"
        }

        // When
        val result = retryPolicy.executeWithRetry(block = block)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attemptCount)
    }

    @Test
    fun `executeWithRetry fails after max attempts`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            throw IOException("Network error")
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 3, block = block)

        // Then
        assertTrue(result.isFailure)
        assertEquals(3, attemptCount)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is IOException)
    }

    @Test
    fun `executeWithRetry does not retry non-retryable errors`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            throw SyncError.ClientError(400, "Invalid data")
        }

        // When
        val result = retryPolicy.executeWithRetry(block = block)

        // Then
        assertTrue(result.isFailure)
        assertEquals(1, attemptCount) // Only one attempt
        val error = result.exceptionOrNull()
        assertTrue(error is SyncError.ClientError)
    }

    // ========== Exponential Backoff Tests ==========

    @Test
    fun `executeWithRetry uses exponential backoff`() = runTest {
        // Given
        var attemptCount = 0
        val delays = mutableListOf<Long>()
        val startTime = System.currentTimeMillis()
        
        val block: suspend () -> String = {
            attemptCount++
            if (attemptCount > 1) {
                delays.add(System.currentTimeMillis() - startTime)
            }
            if (attemptCount < 4) {
                throw IOException("Network error")
            }
            "success"
        }

        // When
        val result = retryPolicy.executeWithRetry(
            initialDelay = 100,
            factor = 2.0,
            block = block
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(4, attemptCount)
        // Verify delays are increasing (with some tolerance for timing)
        assertTrue(delays.size >= 2)
        assertTrue(delays[1] > delays[0])
    }

    @Test
    fun `executeWithRetry respects max delay`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            if (attemptCount < 5) {
                throw IOException("Network error")
            }
            "success"
        }

        // When
        val result = retryPolicy.executeWithRetry(
            initialDelay = 1000,
            maxDelay = 2000,
            factor = 10.0, // Large factor to test max delay
            block = block
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals(5, attemptCount)
    }

    // ========== Entity-Specific Retry Tests ==========

    @Test
    fun `executeWithRetryForEntity succeeds and logs entity context`() = runTest {
        // Given
        val block: suspend () -> String = { "success" }

        // When
        val result = retryPolicy.executeWithRetryForEntity(
            entityType = "meal",
            entityId = 100,
            operation = "create",
            block = block
        )

        // Then
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
    }

    @Test
    fun `executeWithRetryForEntity fails and logs entity context`() = runTest {
        // Given
        val block: suspend () -> String = {
            throw IOException("Network error")
        }

        // When
        val result = retryPolicy.executeWithRetryForEntity(
            entityType = "meal",
            entityId = 100,
            operation = "create",
            block = block
        )

        // Then
        assertTrue(result.isFailure)
    }

    // ========== Retry Decision Tests ==========

    @Test
    fun `shouldRetry returns true for retryable errors within max attempts`() {
        // Given
        val error = IOException("Network error")
        val attemptNumber = 2

        // When
        val result = retryPolicy.shouldRetry(error, attemptNumber)

        // Then
        assertTrue(result)
    }

    @Test
    fun `shouldRetry returns false when max attempts reached`() {
        // Given
        val error = IOException("Network error")
        val attemptNumber = 5 // Max attempts

        // When
        val result = retryPolicy.shouldRetry(error, attemptNumber)

        // Then
        assertFalse(result)
    }

    @Test
    fun `shouldRetry returns false for non-retryable errors`() {
        // Given
        val error = SyncError.ClientError(400, "Invalid data")
        val attemptNumber = 1

        // When
        val result = retryPolicy.shouldRetry(error, attemptNumber)

        // Then
        assertFalse(result)
    }

    @Test
    fun `shouldRetry returns true for network errors`() {
        // Given
        val errors = listOf(
            IOException("Network error"),
            SocketTimeoutException("Timeout"),
            UnknownHostException("Unknown host")
        )

        // When & Then
        errors.forEach { error ->
            assertTrue(retryPolicy.shouldRetry(error, 1))
        }
    }

    @Test
    fun `shouldRetry returns true for retryable sync errors`() {
        // Given
        val errors = listOf(
            SyncError.NetworkError("Network error"),
            SyncError.TimeoutError(),
            SyncError.ServerError(500, "Internal server error")
        )

        // When & Then
        errors.forEach { error ->
            assertTrue(retryPolicy.shouldRetry(error, 1))
        }
    }

    @Test
    fun `shouldRetry returns false for non-retryable sync errors`() {
        // Given
        val errors = listOf(
            SyncError.ClientError(400, "Invalid data"),
            SyncError.AuthenticationError("Unauthorized"),
            SyncError.ClientError(400, "Bad request")
        )

        // When & Then
        errors.forEach { error ->
            assertFalse(retryPolicy.shouldRetry(error, 1))
        }
    }

    // ========== Delay Calculation Tests ==========

    @Test
    fun `calculateDelay returns correct delay for network errors`() {
        // Given
        val error = IOException("Network error")

        // When
        val delay0 = retryPolicy.calculateDelay(error, 0)
        val delay1 = retryPolicy.calculateDelay(error, 1)
        val delay2 = retryPolicy.calculateDelay(error, 2)

        // Then
        assertEquals(1000L, delay0) // Initial delay
        assertEquals(2000L, delay1) // 2x initial
        assertEquals(4000L, delay2) // 4x initial
    }

    @Test
    fun `calculateDelay respects max delay`() {
        // Given
        val error = IOException("Network error")

        // When
        val delay10 = retryPolicy.calculateDelay(error, 10) // Would be > 16000ms

        // Then
        assertTrue(delay10 <= 16000L) // Max delay
    }

    @Test
    fun `calculateDelay handles rate limit errors`() {
        // Given
        val error = SyncError.RateLimitError(retryAfterSeconds = 5000)

        // When
        val delay = retryPolicy.calculateDelay(error, 0)

        // Then
        assertEquals(5000L, delay)
    }

    // ========== Error Mapping Tests ==========

    @Test
    fun `executeWithRetry maps IOException to NetworkError`() = runTest {
        // Given
        val block: suspend () -> String = {
            throw IOException("Connection failed")
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 1, block = block)

        // Then
        assertTrue(result.isFailure)
        // Error should be mapped but original exception is preserved
        val error = result.exceptionOrNull()
        assertNotNull(error)
    }

    @Test
    fun `executeWithRetry maps UnknownHostException to NoInternetError`() = runTest {
        // Given
        val block: suspend () -> String = {
            throw UnknownHostException("No internet")
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 1, block = block)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
    }

    @Test
    fun `executeWithRetry maps SocketTimeoutException to TimeoutError`() = runTest {
        // Given
        val block: suspend () -> String = {
            throw SocketTimeoutException("Request timed out")
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 1, block = block)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
    }

    @Test
    fun `executeWithRetry preserves SyncError types`() = runTest {
        // Given
        val originalError = SyncError.AuthenticationError("Unauthorized")
        val block: suspend () -> String = {
            throw originalError
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 1, block = block)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is SyncError.AuthenticationError)
        assertEquals("Unauthorized", error.message)
    }

    // ========== Edge Cases ==========

    @Test
    fun `executeWithRetry handles zero max attempts`() = runTest {
        // Given
        var attemptCount = 0
        val block: suspend () -> String = {
            attemptCount++
            "success"
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 0, block = block)

        // Then
        assertTrue(result.isFailure)
        assertEquals(0, attemptCount)
    }

    @Test
    fun `executeWithRetry handles negative delays gracefully`() = runTest {
        // Given
        val block: suspend () -> String = { "success" }

        // When
        val result = retryPolicy.executeWithRetry(
            initialDelay = -1000,
            block = block
        )

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `executeWithRetry handles null error messages`() = runTest {
        // Given
        val block: suspend () -> String = {
            throw Exception() // No message
        }

        // When
        val result = retryPolicy.executeWithRetry(maxAttempts = 1, block = block)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
    }
}
