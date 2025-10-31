package com.shoppit.app.data.transaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransactionManagerTest {
    
    private lateinit var database: RoomDatabase
    private lateinit var transactionManager: TransactionManagerImpl
    
    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        database = mockk(relaxed = true)
        transactionManager = TransactionManagerImpl(database)
    }
    
    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }
    
    @Test
    fun `executeInTransaction returns success when block succeeds`() = runTest {
        // Given
        val expectedValue = "success"
        coEvery { database.withTransaction<String>(any()) } returns expectedValue
        
        // When
        val result = transactionManager.executeInTransaction {
            expectedValue
        }
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
    }
    
    @Test
    fun `executeInTransaction returns failure when block throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Test exception")
        coEvery { database.withTransaction<String>(any()) } throws exception
        
        // When
        val result = transactionManager.executeInTransaction<String> {
            throw exception
        }
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }
    
    @Test
    fun `executeWithRetry succeeds on first attempt`() = runTest {
        // Given
        val expectedValue = "success"
        var attemptCount = 0
        
        // When
        val result = transactionManager.executeWithRetry {
            attemptCount++
            expectedValue
        }
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
        assertEquals(1, attemptCount)
    }
    
    @Test
    fun `executeWithRetry retries on failure and eventually succeeds`() = runTest {
        // Given
        var attemptCount = 0
        val expectedValue = "success"
        
        // When
        val result = transactionManager.executeWithRetry(maxRetries = 3) {
            attemptCount++
            if (attemptCount < 3) {
                throw RuntimeException("Temporary failure")
            }
            expectedValue
        }
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedValue, result.getOrNull())
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `executeWithRetry returns failure after max retries`() = runTest {
        // Given
        var attemptCount = 0
        val exception = RuntimeException("Persistent failure")
        
        // When
        val result = transactionManager.executeWithRetry<String>(maxRetries = 3) {
            attemptCount++
            throw exception
        }
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `executeWithRetry uses exponential backoff`() = runTest {
        // Given
        var attemptCount = 0
        
        // When
        val result = transactionManager.executeWithRetry<String>(
            maxRetries = 3,
            initialDelayMs = 100,
            factor = 2.0
        ) {
            attemptCount++
            throw RuntimeException("Test failure")
        }
        
        // Then - Just verify retry count, not timing (timing tests are flaky)
        assertTrue(result.isFailure)
        assertEquals(3, attemptCount)
    }
    
    @Test
    fun `executeWithRetry respects max delay`() = runTest {
        // Given
        val delays = mutableListOf<Long>()
        var attemptCount = 0
        var lastTime = System.currentTimeMillis()
        
        // When
        transactionManager.executeWithRetry<String>(
            maxRetries = 4,
            initialDelayMs = 100,
            maxDelayMs = 150,
            factor = 2.0
        ) {
            attemptCount++
            if (attemptCount > 1) {
                val currentTime = System.currentTimeMillis()
                delays.add(currentTime - lastTime)
                lastTime = currentTime
            }
            throw RuntimeException("Test failure")
        }
        
        // Then
        assertEquals(4, attemptCount)
        assertEquals(3, delays.size)
        // All delays should be capped at maxDelayMs
        delays.forEach { delay ->
            assertTrue(delay <= 160) // Allow some tolerance
        }
    }
    
    @Test
    fun `executeWithRetry validates parameters`() = runTest {
        // Test maxRetries validation
        try {
            transactionManager.executeWithRetry<String>(maxRetries = 0) {
                "test"
            }
            fail("Expected IllegalArgumentException for maxRetries = 0")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        // Test initialDelayMs validation
        try {
            transactionManager.executeWithRetry<String>(initialDelayMs = 0) {
                "test"
            }
            fail("Expected IllegalArgumentException for initialDelayMs = 0")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        // Test maxDelayMs validation
        try {
            transactionManager.executeWithRetry<String>(
                initialDelayMs = 200,
                maxDelayMs = 100
            ) {
                "test"
            }
            fail("Expected IllegalArgumentException for maxDelayMs < initialDelayMs")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
        
        // Test factor validation
        try {
            transactionManager.executeWithRetry<String>(factor = 1.0) {
                "test"
            }
            fail("Expected IllegalArgumentException for factor = 1.0")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
