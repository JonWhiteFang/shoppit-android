package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_history",
    indices = [
        Index(value = ["item_name"]),
        Index(value = ["last_purchased_at"])
    ]
)
data class ItemHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "item_name")
    val itemName: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "purchase_count")
    val purchaseCount: Int,
    
    @ColumnInfo(name = "last_purchased_at")
    val lastPurchasedAt: Long,
    
    @ColumnInfo(name = "average_price")
    val averagePrice: Double? = null
)
