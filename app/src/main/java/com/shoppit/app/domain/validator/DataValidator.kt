package com.shoppit.app.domain.validator

/**
 * Generic interface for data validation.
 * 
 * Provides methods to validate single items or batches of items
 * according to business rules.
 * 
 * @param T The type of data to validate
 */
interface DataValidator<T> {
    /**
     * Validates a single data item.
     * 
     * @param data The data to validate
     * @return ValidationResult indicating success or failure with errors
     */
    fun validate(data: T): ValidationResult
    
    /**
     * Validates a batch of data items.
     * 
     * @param data The list of data items to validate
     * @return List of ValidationResult for each item
     */
    fun validateBatch(data: List<T>): List<ValidationResult> {
        return data.map { validate(it) }
    }
}

/**
 * Sealed class representing the result of a validation operation.
 */
sealed class ValidationResult {
    /**
     * Indicates that validation passed successfully.
     */
    data object Valid : ValidationResult()
    
    /**
     * Indicates that validation failed with one or more errors.
     * 
     * @property errors List of validation errors
     */
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
    
    /**
     * Checks if this result is valid.
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * Checks if this result is invalid.
     */
    fun isInvalid(): Boolean = this is Invalid
    
    /**
     * Returns the list of errors if invalid, or empty list if valid.
     */
    fun getErrors(): List<ValidationError> = when (this) {
        is Valid -> emptyList()
        is Invalid -> errors
    }
}

/**
 * Represents a single validation error.
 * 
 * @property field The field that failed validation
 * @property message Human-readable error message
 * @property code Machine-readable error code for programmatic handling
 */
data class ValidationError(
    val field: String,
    val message: String,
    val code: String
) {
    companion object {
        // Common error codes
        const val CODE_REQUIRED = "REQUIRED"
        const val CODE_INVALID_FORMAT = "INVALID_FORMAT"
        const val CODE_OUT_OF_RANGE = "OUT_OF_RANGE"
        const val CODE_TOO_SHORT = "TOO_SHORT"
        const val CODE_TOO_LONG = "TOO_LONG"
        const val CODE_DUPLICATE = "DUPLICATE"
        const val CODE_NOT_FOUND = "NOT_FOUND"
        const val CODE_INVALID_VALUE = "INVALID_VALUE"
    }
}

/**
 * Extension function to convert ValidationResult to Result<Unit>.
 * Useful for compatibility with existing code using Result.
 */
fun ValidationResult.toResult(): Result<Unit> {
    return when (this) {
        is ValidationResult.Valid -> Result.success(Unit)
        is ValidationResult.Invalid -> {
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            Result.failure(ValidationException(message))
        }
    }
}

/**
 * Exception thrown when validation fails.
 */
class ValidationException(message: String) : Exception(message)
