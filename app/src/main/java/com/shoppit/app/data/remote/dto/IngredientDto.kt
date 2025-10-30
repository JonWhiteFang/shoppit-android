package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for Ingredient.
 * Used for network communication with the sync backend.
 */
data class IngredientDto(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("quantity")
    val quantity: String,
    
    @SerializedName("unit")
    val unit: String
)
