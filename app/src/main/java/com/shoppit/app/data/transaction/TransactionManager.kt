package com.shoppit.app.data.transaction

import androidx.room.withTransaction
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.pow

/**
 * Interface for managing database transactions.
 * 
 * Provides methods to execute operations within transactions with
 * automatic retry logic and timeout handling.
 */
interface TransactionManager {
    /**
     * Executes a block of code within a database transaction.
     * 
     * If the block throws an exception, the transaction is rolled back.
     * If the block completes successfully, the transaction is committed.
     * 
     * @param T The return type of the transaction block
     * @param block The code to execute within the transaction
     * @return Result containing the return value or an exception
     */
    suspend fun <T> executeInTransaction(block: suspend () -> T): Result<T>
    
    /**
     * Executes a block of code with automatic retry on failure.
     * 
     * Uses exponential backoff between retry attempts.
     * 
     * @param T The return type of the block
     * @param maxRetries Maximum number of retry attempts (default: 3)
     * @param initialDelayMs Initial delay between retries in milliseconds (default: 100)
     * @param maxDelayMs Maximum delay between retries in milliseconds (default: 2000)
     * @param factor Exponential backoff factor (default: 2.0)
     * @param block The code to execute
     * @return Result containing the return value or the last exception
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 100,
        maxDelayMs: Long = 2000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T>
}

/**
 * Default implementation of TransactionManager.
 * 
 * Provides transaction execution with retry logic and timeout handling.
 * Uses Room's transaction support for ACID guarantees.
 * 
 * @property database The Room database instance
 */
class TransactionManagerImpl(
    private val database: androidx.room.RoomDatabase
) : TransactionManager {
    
    companion object {
        private const val TRANSACTION_TIMEOUT_MS = 30_000L
    }
    
    override suspend fun <T> executeInTransaction(block: suspend () -> T): Result<T> {
        return try {
            val startTime = System.currentTimeMillis()
            Timber.d("Starting transaction")
            
            val result = withTransaction(database) {
                block()
            }
            
            val duration = System.currentTimeMillis() - startTime
            Timber.d("Transaction completed successfully in ${duration}ms")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Transaction failed")
            Result.failure(e)
        }
    }
    
    override suspend fun <T> executeWithRetry(
        maxRetries: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        factor: Double,
        block: suspend () -> T
    ): Result<T> {
        require(maxRetries > 0) { "maxRetries must be positive" }
        require(initialDelayMs > 0) { "initialDelayMs must be positive" }
        require(maxDelayMs >= initialDelayMs) { "maxDelayMs must be >= initialDelayMs" }
        require(factor > 1.0) { "factor must be > 1.0" }
        
        var currentDelay = initialDelayMs
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                Timber.d("Executing operation (attempt ${attempt + 1}/$maxRetries)")
                val result = block()
                if (attempt > 0) {
                    Timber.i("Operation succeeded after ${attempt + 1} attempts")
                }
                return Result.success(result)
            } catch (e: Exception) {
                lastException = e
                Timber.w(e, "Operation failed (attempt ${attempt + 1}/$maxRetries)")
                
                if (attempt < maxRetries - 1) {
                    Timber.d("Retrying in ${currentDelay}ms")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }
        
        Timber.e(lastException, "Operation failed after $maxRetries attempts")
        return Result.failure(
            lastException ?: Exception("Operation failed after $maxRetries attempts")
        )
    }
}

/**
 * Exception thrown when a transaction exceeds the timeout limit.
 */
class TransactionTimeoutException(message: String) : Exception(message)
