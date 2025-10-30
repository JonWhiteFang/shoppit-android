package com.shoppit.app.domain.error

import com.shoppit.app.data.error.PersistenceError

/**
 * Maps technical errors to user-friendly error messages.
 * Provides consistent error messaging across the application.
 */
object ErrorMapper {
    
    /**
     * Converts a Throwable to a user-friendly error message.
     * @param error The error to convert
     * @return A user-friendly error message
     */
    fun toUserMessage(error: Throwable): String {
        return when (error) {
            is PersistenceError -> mapPersistenceError(error)
            is AppError -> mapAppError(error)
            else -> "An unexpected error occurred. Please try again."
        }
    }
    
    /**
     * Maps PersistenceError to user-friendly messages.
     */
    private fun mapPersistenceError(error: PersistenceError): String {
        return when (error) {
            is PersistenceError.QueryFailed -> 
                "Failed to load data. Please check your connection and try again."
            
            is PersistenceError.WriteFailed -> 
                "Failed to save changes. Please try again."
            
            is PersistenceError.ValidationFailed -> {
                val firstError = error.errors.firstOrNull()
                firstError?.message ?: "Invalid data. Please check your input."
            }
            
            is PersistenceError.ConstraintViolation -> 
                "This item already exists. Please use a different name."
            
            is PersistenceError.TransactionFailed -> 
                "Failed to complete operation. Please try again."
            
            is PersistenceError.MigrationFailed -> 
                "Database upgrade failed. Please reinstall the app."
            
            is PersistenceError.CorruptionDetected -> 
                "Database corruption detected. Please clear app data or reinstall."
            
            is PersistenceError.BackupFailed -> 
                "Failed to backup data. Please check storage permissions."
            
            is PersistenceError.CacheFull -> 
                "Storage is full. Please clear some data."
            
            is PersistenceError.ConcurrencyConflict -> 
                "Data was modified by another process. Please refresh and try again."
        }
    }
    
    /**
     * Maps AppError to user-friendly messages.
     */
    private fun mapAppError(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> 
                error.message
            
            is AppError.DatabaseError -> 
                "Database error occurred. Please try again."
            
            is AppError.AuthenticationError -> 
                error.message
            
            is AppError.ValidationError -> 
                error.message
            
            is AppError.PermissionDenied -> 
                "Permission denied: ${error.permission}. Please enable it in Settings."
            
            is AppError.VoiceParsingError -> 
                error.message
            
            is AppError.BarcodeScanError -> 
                error.message
            
            is AppError.NotFoundError -> 
                error.message
            
            is AppError.UnknownError -> 
                "An unexpected error occurred: ${error.message}"
        }
    }
    
    /**
     * Checks if an error is recoverable (user can retry).
     */
    fun isRecoverable(error: Throwable): Boolean {
        return when (error) {
            is PersistenceError.QueryFailed,
            is PersistenceError.WriteFailed,
            is PersistenceError.TransactionFailed,
            is PersistenceError.ConcurrencyConflict,
            is AppError.NetworkError -> true
            
            is PersistenceError.CorruptionDetected,
            is PersistenceError.MigrationFailed -> false
            
            else -> true
        }
    }
    
    /**
     * Gets a suggested action for the user based on the error.
     */
    fun getSuggestedAction(error: Throwable): String? {
        return when (error) {
            is PersistenceError.CorruptionDetected -> 
                "Clear app data in Settings > Apps > Shoppit > Storage"
            
            is PersistenceError.MigrationFailed -> 
                "Reinstall the app to fix database issues"
            
            is PersistenceError.CacheFull -> 
                "Clear checked items or delete old templates"
            
            is AppError.NetworkError -> 
                "Check your internet connection and try again"
            
            is AppError.PermissionDenied -> 
                "Go to Settings > Apps > Shoppit > Permissions to enable"
            
            is AppError.VoiceParsingError -> 
                "Try typing the item name manually instead"
            
            is AppError.BarcodeScanError -> 
                "Try entering the item name manually"
            
            else -> null
        }
    }
}
