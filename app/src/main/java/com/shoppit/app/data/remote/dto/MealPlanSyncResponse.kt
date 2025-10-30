package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for meal plan sync operations.
 */
data class MealPlanSyncResponse(
    @SerializedName("synced")
    val synced: List<MealPlanDto>,
    
    @SerializedName("conflicts")
    val conflicts: List<ConflictDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
