package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for syncing shopping list items with the backend.
 */
data class ShoppingListSyncRequest(
    @SerializedName("items")
    val items: List<ShoppingListItemDto>,
    
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long?
)
