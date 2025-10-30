package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Meal.
 * Used for network communication with the sync backend.
 */
data class MealDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("local_id")
    val localId: Long?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("ingredients")
    val ingredients: List<IngredientDto>,
    
    @SerializedName("notes")
    val notes: String?,
    
    @SerializedName("tags")
    val tags: List<String>,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long,
    
    @SerializedName("deleted_at")
    val deletedAt: Long?
)
