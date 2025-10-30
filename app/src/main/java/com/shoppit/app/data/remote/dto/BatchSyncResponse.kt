package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response DTO for batch sync operations.
 */
data class BatchSyncResponse(
    @SerializedName("meals")
    val meals: MealSyncResponse?,
    
    @SerializedName("meal_plans")
    val mealPlans: MealPlanSyncResponse?,
    
    @SerializedName("shopping_list_items")
    val shoppingListItems: ShoppingListSyncResponse?,
    
    @SerializedName("server_timestamp")
    val serverTimestamp: Long
)
