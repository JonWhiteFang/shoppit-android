package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a meal in the local database.
 * Stores meal information including name, ingredients list, notes, and timestamps.
 *
 * @property id Unique identifier for the meal (auto-generated)
 * @property name The name of the meal (required)
 * @property ingredients List of ingredients in the meal (stored as JSON)
 * @property notes Optional notes or instructions for the meal
 * @property createdAt Timestamp when the meal was created
 * @property updatedAt Timestamp when the meal was last updated
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "ingredients")
    val ingredients: List<IngredientEntity>,
    
    @ColumnInfo(name = "notes")
    val notes: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
