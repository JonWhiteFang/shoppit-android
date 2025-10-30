package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for syncing meal plans with the backend.
 */
data class MealPlanSyncRequest(
    @SerializedName("meal_plans")
    val mealPlans: List<MealPlanDto>,
    
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long?
)
