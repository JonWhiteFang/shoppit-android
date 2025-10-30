package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for meal sync operations.
 */
data class MealSyncResponse(
    @SerializedName("synced")
    val synced: List<MealDto>,
    
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
