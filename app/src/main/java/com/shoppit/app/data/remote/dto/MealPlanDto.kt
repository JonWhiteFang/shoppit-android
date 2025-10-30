package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for MealPlan.
 * Used for network communication with the sync backend.
 */
data class MealPlanDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("local_id")
    val localId: Long?,
    
    @SerializedName("meal_id")
    val mealId: String,
    
    @SerializedName("local_meal_id")
    val localMealId: Long,
    
    @SerializedName("date")
    val date: String, // ISO 8601 format: "2025-10-30"
    
    @SerializedName("meal_type")
    val mealType: String, // "BREAKFAST", "LUNCH", "DINNER", "SNACK"
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long,
    
    @SerializedName("deleted_at")
    val deletedAt: Long?
)
