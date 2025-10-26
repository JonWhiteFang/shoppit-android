package com.shoppit.app.domain.usecase

import android.database.sqlite.SQLiteException
import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.error.AppResult
import java.io.IOException

/**
 * Abstract base class for all use cases in the domain layer.
 * Provides consistent error handling and exception mapping.
 *
 * @param P The type of parameters the use case accepts
 * @param R The type of result the use case returns
 */
abstract class UseCase<in P, R> {
    /**
     * Invokes the use case with the given parameters.
     * Automatically handles exceptions and maps them to AppError types.
     *
     * @param parameters The input parameters for the use case
     * @return AppResult containing either the success value or an AppError
     */
    suspend operator fun invoke(parameters: P): AppResult<R> {
        return try {
            execute(parameters)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    /**
     * Executes the use case logic.
     * Implementations should focus on business logic without worrying about error handling.
     *
     * @param parameters The input parameters for the use case
     * @return AppResult containing the result of the operation
     * @throws RuntimeException if an error occurs during execution
     */
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): AppResult<R>

    /**
     * Maps exceptions to appropriate AppError types.
     *
     * @param exception The exception to map
     * @return The corresponding AppError
     */
    private fun mapException(exception: Exception): Throwable {
        val error = when (exception) {
            is SQLiteException -> AppError.DatabaseError
            is IOException -> AppError.NetworkError
            is IllegalArgumentException -> AppError.ValidationError(
                exception.message ?: "Validation failed"
            )
            else -> AppError.UnknownError(exception)
        }
        return Exception(error.toString())
    }
}

/**
 * Abstract base class for use cases that don't require parameters.
 *
 * @param R The type of result the use case returns
 */
abstract class NoParamUseCase<R> : UseCase<Unit, R>()
