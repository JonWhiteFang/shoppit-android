package com.shoppit.app.domain.error

/**
 * Type alias for Result with AppError as the failure type.
 * Provides consistent result wrapping across the application.
 */
typealias AppResult<T> = Result<T>

/**
 * Extension function to handle success cases.
 * Executes the provided action if the result is successful.
 *
 * @param action The action to execute with the success value
 * @return The original AppResult for chaining
 */
fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (isSuccess) {
        action(getOrNull()!!)
    }
    return this
}

/**
 * Extension function to handle failure cases.
 * Executes the provided action if the result is a failure.
 *
 * @param action The action to execute with the AppError
 * @return The original AppResult for chaining
 */
fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (isFailure) {
        val exception = exceptionOrNull()
        val error = when (exception) {
            is AppErrorException -> exception.error
            else -> AppError.UnknownError(exception ?: Throwable("Unknown error"))
        }
        action(error)
    }
    return this
}

/**
 * Maps a successful result to a new type.
 *
 * @param transform The transformation function
 * @return A new AppResult with the transformed value
 */
fun <T, R> AppResult<T>.mapSuccess(transform: (T) -> R): AppResult<R> {
    return map(transform)
}

/**
 * Converts an AppError to an AppResult failure.
 *
 * @return An AppResult failure containing the AppError
 */
fun <T> AppError.toResult(): AppResult<T> {
    return Result.failure(AppErrorException(this))
}

/**
 * Exception wrapper for AppError to work with Kotlin's Result type.
 */
internal class AppErrorException(val error: AppError) : Exception(error.toString())
