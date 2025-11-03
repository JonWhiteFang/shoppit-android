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
                "Unable to connect. Please check your internet connection and try again."
            
            is AppError.DatabaseError -> 
                "Unable to save your changes. Please try again."
            
            is AppError.AuthenticationError -> 
                "Authentication failed. Please sign in again."
            
            is AppError.ValidationError -> 
                error.message
            
            is AppError.PermissionDenied -> 
                "Permission required. Please enable ${error.permission} in Settings."
            
            is AppError.VoiceParsingError -> 
                "Couldn't understand the voice input. Please try again or type manually."
            
            is AppError.BarcodeScanError -> 
                "Couldn't scan the barcode. Please try again or enter manually."
            
            is AppError.NotFoundError -> 
                "Item not found. It may have been deleted."
            
            is AppError.UnknownError -> 
                "Something went wrong. Please try again."
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
                "Clear app data: Settings > Apps > Shoppit > Storage > Clear Data"
            
            is PersistenceError.MigrationFailed -> 
                "Please reinstall the app to fix this issue"
            
            is PersistenceError.CacheFull -> 
                "Clear checked items or delete old meals to free up space"
            
            is AppError.NetworkError -> 
                "Check your internet connection and try again"
            
            is AppError.PermissionDenied -> 
                "Enable permission: Settings > Apps > Shoppit > Permissions"
            
            is AppError.VoiceParsingError -> 
                "Try typing the item name instead"
            
            is AppError.BarcodeScanError -> 
                "Try entering the item name manually"
            
            else -> null
        }
    }
}
