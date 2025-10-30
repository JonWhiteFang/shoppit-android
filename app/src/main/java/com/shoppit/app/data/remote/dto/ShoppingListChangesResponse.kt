package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for fetching shopping list changes from the backend.
 */
data class ShoppingListChangesResponse(
    @SerializedName("items")
    val items: List<ShoppingListItemDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
