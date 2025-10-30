package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a meal in the local database.
 * Stores meal information including name, ingredients list, notes, tags, and timestamps.
 *
 * Indices are created on frequently queried columns to optimize performance:
 * - name: For searching and sorting meals by name
 * - created_at: For sorting meals by creation date
 * - updated_at: For sorting meals by last update date
 *
 * @property id Unique identifier for the meal (auto-generated)
 * @property name The name of the meal (required)
 * @property ingredients List of ingredients in the meal (stored as JSON)
 * @property notes Optional notes or instructions for the meal
 * @property tags Comma-separated list of meal tags for categorization
 * @property createdAt Timestamp when the meal was created
 * @property updatedAt Timestamp when the meal was last updated
 * @property serverId Cloud backend ID (null if not yet synced)
 * @property syncStatus Current sync status ("synced", "pending", "conflict", "error")
 */
@Entity(
    tableName = "meals",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "ingredients")
    val ingredients: List<IngredientEntity>,
    
    @ColumnInfo(name = "notes")
    val notes: String,
    
    @ColumnInfo(name = "tags")
    val tags: String = "",
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending"
)
