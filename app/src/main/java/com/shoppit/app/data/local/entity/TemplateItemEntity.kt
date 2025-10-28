package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["template_id"])]
)
data class TemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "template_id")
    val templateId: Long,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "notes")
    val notes: String
)
