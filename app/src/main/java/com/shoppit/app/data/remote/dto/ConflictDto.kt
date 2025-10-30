package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for conflict resolution data.
 * Contains information about conflicts between local and server versions.
 */
data class ConflictDto(
    @SerializedName("entity_type")
    val entityType: String, // "meal", "meal_plan", "shopping_list_item"
    
    @SerializedName("local_version")
    val localVersion: Any, // The local version of the entity (can be MealDto, MealPlanDto, etc.)
    
    @SerializedName("server_version")
    val serverVersion: Any, // The server version of the entity
    
    @SerializedName("resolution")
    val resolution: String // "server_wins", "local_wins"
)
