package com.shoppit.app.presentation.ui.common

/**
 * Sealed interface representing one-time error or success events.
 * Used for displaying snackbar messages to the user.
 * 
 * Requirements:
 * - 1.1: Display user-friendly error messages
 * - 9.1, 9.2, 9.3, 9.4: Display success messages for user actions
 */
sealed interface ErrorEvent {
    /**
     * Represents an error event with a message to display.
     * 
     * @property message The error message to display to the user
     */
    data class Error(val message: String) : ErrorEvent
    
    /**
     * Represents a success event with a message to display.
     * 
     * @property message The success message to display to the user
     */
    data class Success(val message: String) : ErrorEvent
}
