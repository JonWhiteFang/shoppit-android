package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for fetching meal changes from the backend.
 */
data class MealChangesResponse(
    @SerializedName("meals")
    val meals: List<MealDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
