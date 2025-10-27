package com.shoppit.app.data.error

import timber.log.Timber

/**
 * Defines recovery strategies for different types of persistence errors.
 */
interface ErrorRecoveryStrategy {
    suspend fun recover(error: PersistenceError): RecoveryResult
}

/**
 * Result of an error recovery attempt.
 */
sealed class RecoveryResult {
    data object Success : RecoveryResult()
    data class Retry(val delayMs: Long = 0) : RecoveryResult()
    data class Fallback(val action: String) : RecoveryResult()
    data class Failed(val reason: String) : RecoveryResult()
}

/**
 * Default implementation of error recovery strategies.
 */
class DefaultErrorRecoveryStrategy : ErrorRecoveryStrategy {
    
    override suspend fun recover(error: PersistenceError): RecoveryResult {
        return when (error) {
            is PersistenceError.MigrationFailed -> recoverFromMigrationFailure(error)
            is PersistenceError.TransactionFailed -> recoverFromTransactionFailure(error)
            is PersistenceError.ValidationFailed -> recoverFromValidationFailure(error)
            is PersistenceError.CorruptionDetected -> recoverFromCorruption(error)
            is PersistenceError.BackupFailed -> recoverFromBackupFailure(error)
            is PersistenceError.CacheFull -> recoverFromCacheFull(error)
            is PersistenceError.QueryFailed -> recoverFromQueryFailure(error)
            is PersistenceError.WriteFailed -> recoverFromWriteFailure(error)
            is PersistenceError.ConstraintViolation -> recoverFromConstraintViolation(error)
            is PersistenceError.ConcurrencyConflict -> recoverFromConcurrencyConflict(error)
        }
    }
    
    private fun recoverFromMigrationFailure(error: PersistenceError.MigrationFailed): RecoveryResult {
        Timber.e(error, "Migration failed from v${error.fromVersion} to v${error.toVersion}")
        return RecoveryResult.Fallback("destructive_migration")
    }
    
    private fun recoverFromTransactionFailure(error: PersistenceError.TransactionFailed): RecoveryResult {
        Timber.w(error, "Transaction failed for operation: ${error.operation}")
        return RecoveryResult.Retry(delayMs = calculateBackoff(1))
    }
    
    private fun recoverFromValidationFailure(error: PersistenceError.ValidationFailed): RecoveryResult {
        Timber.d("Validation failed: ${error.errors.size} errors")
        return RecoveryResult.Failed("Validation errors must be fixed by user")
    }
    
    private fun recoverFromCorruption(error: PersistenceError.CorruptionDetected): RecoveryResult {
        Timber.e(error, "Database corruption detected: ${error.details}")
        return RecoveryResult.Fallback("restore_from_backup")
    }
    
    private fun recoverFromBackupFailure(error: PersistenceError.BackupFailed): RecoveryResult {
        Timber.e(error, "Backup operation failed: ${error.reason}")
        return RecoveryResult.Failed("Continue without backup")
    }
    
    private fun recoverFromCacheFull(error: PersistenceError.CacheFull): RecoveryResult {
        Timber.w("Cache full: ${error.currentSize}/${error.maxSize}")
        return RecoveryResult.Fallback("evict_lru_entries")
    }
    
    private fun recoverFromQueryFailure(error: PersistenceError.QueryFailed): RecoveryResult {
        Timber.e(error, "Query execution failed")
        return RecoveryResult.Retry(delayMs = calculateBackoff(1))
    }
    
    private fun recoverFromWriteFailure(error: PersistenceError.WriteFailed): RecoveryResult {
        Timber.e(error, "Write operation failed: ${error.operation}")
        return RecoveryResult.Retry(delayMs = calculateBackoff(1))
    }
    
    private fun recoverFromConstraintViolation(error: PersistenceError.ConstraintViolation): RecoveryResult {
        Timber.w("Constraint violation on ${error.constraint}: ${error.details}")
        return RecoveryResult.Failed("Constraint violation cannot be automatically recovered")
    }
    
    private fun recoverFromConcurrencyConflict(error: PersistenceError.ConcurrencyConflict): RecoveryResult {
        Timber.w("Concurrency conflict on ${error.resource}: ${error.details}")
        return RecoveryResult.Retry(delayMs = calculateBackoff(1))
    }
    
    /**
     * Calculate exponential backoff delay.
     */
    private fun calculateBackoff(attempt: Int): Long {
        return minOf(1000L * (1 shl (attempt - 1)), 10000L)
    }
}
