package com.shoppit.app.domain.model

/**
 * Represents the current state of synchronization.
 */
enum class SyncStatus {
    /** No sync operation in progress */
    IDLE,
    
    /** Sync operation currently in progress */
    SYNCING,
    
    /** Last sync operation completed successfully */
    SUCCESS,
    
    /** Last sync operation failed with an error */
    ERROR,
    
    /** Device is offline, sync not possible */
    OFFLINE
}
