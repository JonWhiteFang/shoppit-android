package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for shopping list sync operations.
 */
data class ShoppingListSyncResponse(
    @SerializedName("synced")
    val synced: List<ShoppingListItemDto>,
    
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
