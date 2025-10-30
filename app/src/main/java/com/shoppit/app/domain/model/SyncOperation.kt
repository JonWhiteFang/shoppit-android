package com.shoppit.app.domain.model

/**
 * Types of sync operations that can be performed on entities.
 */
enum class SyncOperation {
    /** Entity was created locally and needs to be uploaded */
    CREATE,
    
    /** Entity was updated locally and needs to be synchronized */
    UPDATE,
    
    /** Entity was deleted locally and needs to be removed from server */
    DELETE;
    
    /**
     * Converts the enum to its string representation for database storage.
     */
    fun toStorageString(): String = when (this) {
        CREATE -> "create"
        UPDATE -> "update"
        DELETE -> "delete"
    }
    
    companion object {
        /**
         * Converts a storage string back to a SyncOperation enum.
         */
        fun fromStorageString(value: String): SyncOperation = when (value) {
            "create" -> CREATE
            "update" -> UPDATE
            "delete" -> DELETE
            else -> throw IllegalArgumentException("Unknown sync operation: $value")
        }
    }
}
