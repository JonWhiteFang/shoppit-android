package com.shoppit.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for ShoppingListItem.
 * Used for network communication with the sync backend.
 */
data class ShoppingListItemDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("local_id")
    val localId: Long?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("quantity")
    val quantity: String,
    
    @SerializedName("unit")
    val unit: String,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("is_checked")
    val isChecked: Boolean,
    
    @SerializedName("is_manual")
    val isManual: Boolean,
    
    @SerializedName("meal_ids")
    val mealIds: List<String>,
    
    @SerializedName("notes")
    val notes: String,
    
    @SerializedName("is_priority")
    val isPriority: Boolean,
    
    @SerializedName("custom_order")
    val customOrder: Int,
    
    @SerializedName("estimated_price")
    val estimatedPrice: Double?,
    
    @SerializedName("store_section")
    val storeSection: String,
    
    @SerializedName("created_at")
    val createdAt: Long,
    
    @SerializedName("updated_at")
    val updatedAt: Long,
    
    @SerializedName("deleted_at")
    val deletedAt: Long?
)
