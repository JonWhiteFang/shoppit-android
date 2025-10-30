package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for fetching meal plan changes from the backend.
 */
data class MealPlanChangesResponse(
    @SerializedName("meal_plans")
    val mealPlans: List<MealPlanDto>,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
