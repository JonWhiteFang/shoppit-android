package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_sections")
data class StoreSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    
    @ColumnInfo(name = "is_collapsed")
    val isCollapsed: Boolean = false,
    
    @ColumnInfo(name = "color")
    val color: String
)
