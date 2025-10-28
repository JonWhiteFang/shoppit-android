package com.shoppit.app.domain.error

/**
 * Exception thrown when a database operation fails.
 * Wraps the underlying cause for debugging purposes.
 *
 * @property message Descriptive error message
 * @property cause The underlying exception that caused the database error
 */
class DatabaseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a requested entity is not found.
 *
 * @property message Descriptive error message indicating what was not found
 */
class NotFoundException(
    message: String
) : Exception(message)

/**
 * Exception thrown when a backup or restore operation fails.
 * Wraps the underlying cause for debugging purposes.
 *
 * @property message Descriptive error message
 * @property cause The underlying exception that caused the backup error
 */
class BackupException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a meal plan conflict occurs.
 * Typically used when attempting to assign a meal to a slot that is already occupied.
 *
 * @property message Descriptive error message indicating the conflict
 */
class ConflictException(
    message: String
) : Exception(message)
