package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["is_manual"]),
        Index(value = ["is_checked"]),
        Index(value = ["is_priority"]),
        Index(value = ["store_section"])
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,
    
    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,
    
    @ColumnInfo(name = "meal_ids")
    val mealIds: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    // Management features
    @ColumnInfo(name = "notes")
    val notes: String = "",
    
    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,
    
    @ColumnInfo(name = "custom_order")
    val customOrder: Int = 0,
    
    @ColumnInfo(name = "estimated_price")
    val estimatedPrice: Double? = null,
    
    @ColumnInfo(name = "store_section")
    val storeSection: String,
    
    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long
)
