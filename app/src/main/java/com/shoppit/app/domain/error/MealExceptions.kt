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
