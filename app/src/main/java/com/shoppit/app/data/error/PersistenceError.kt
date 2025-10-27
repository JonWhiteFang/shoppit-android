package com.shoppit.app.data.error

/**
 * Sealed class hierarchy representing all possible persistence layer errors.
 * Each error type includes specific context and recovery strategies.
 */
sealed class PersistenceError : Exception() {
    
    /**
     * Database migration failed during schema version upgrade.
     */
    data class MigrationFailed(
        val fromVersion: Int,
        val toVersion: Int,
        override val cause: Throwable
    ) : PersistenceError() {
        override val message: String
            get() = "Migration from version $fromVersion to $toVersion failed: ${cause.message}"
    }
    
    /**
     * Database transaction failed and was rolled back.
     */
    data class TransactionFailed(
        val operation: String,
        override val cause: Throwable
    ) : PersistenceError() {
        override val message: String
            get() = "Transaction failed for operation '$operation': ${cause.message}"
    }
    
    /**
     * Data validation failed with specific field errors.
     */
    data class ValidationFailed(
        val errors: List<ValidationError>
    ) : PersistenceError() {
        override val message: String
            get() = "Validation failed: ${errors.joinToString(", ") { "${it.field}: ${it.message}" }}"
    }
    
    /**
     * Database corruption detected.
     */
    data class CorruptionDetected(
        val details: String
    ) : PersistenceError() {
        override val message: String
            get() = "Database corruption detected: $details"
    }
    
    /**
     * Backup or restore operation failed.
     */
    data class BackupFailed(
        val reason: String,
        override val cause: Throwable?
    ) : PersistenceError() {
        override val message: String
            get() = "Backup operation failed: $reason${cause?.let { " - ${it.message}" } ?: ""}"
    }
    
    /**
     * Cache is full and cannot accept more entries.
     */
    data class CacheFull(
        val currentSize: Int,
        val maxSize: Int
    ) : PersistenceError() {
        override val message: String
            get() = "Cache is full: $currentSize/$maxSize entries"
    }
    
    /**
     * Database query execution failed.
     */
    data class QueryFailed(
        val query: String,
        override val cause: Throwable
    ) : PersistenceError() {
        override val message: String
            get() = "Query execution failed: ${cause.message}"
    }
    
    /**
     * Database write operation failed.
     */
    data class WriteFailed(
        val operation: String,
        override val cause: Throwable
    ) : PersistenceError() {
        override val message: String
            get() = "Write operation '$operation' failed: ${cause.message}"
    }
    
    /**
     * Database constraint violation (foreign key, unique, etc.).
     */
    data class ConstraintViolation(
        val constraint: String,
        val details: String
    ) : PersistenceError() {
        override val message: String
            get() = "Constraint violation on '$constraint': $details"
    }
    
    /**
     * Concurrent access conflict detected.
     */
    data class ConcurrencyConflict(
        val resource: String,
        val details: String
    ) : PersistenceError() {
        override val message: String
            get() = "Concurrency conflict on '$resource': $details"
    }
}

/**
 * Represents a single validation error for a specific field.
 */
data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)
