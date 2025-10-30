package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for syncing meals with the backend.
 */
data class MealSyncRequest(
    @SerializedName("meals")
    val meals: List<MealDto>,
    
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long?
)
