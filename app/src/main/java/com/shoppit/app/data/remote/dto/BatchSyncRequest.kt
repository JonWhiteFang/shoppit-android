package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request DTO for batch syncing all entities with the backend.
 */
data class BatchSyncRequest(
    @SerializedName("meals")
    val meals: List<MealDto>?,
    
    @SerializedName("meal_plans")
    val mealPlans: List<MealPlanDto>?,
    
    @SerializedName("shopping_list_items")
    val shoppingListItems: List<ShoppingListItemDto>?,
    
    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long?
)
