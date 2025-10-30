package com.shoppit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a meal plan assignment in the local database.
 * Links a meal to a specific date and meal type for weekly planning.
 *
 * Indices are created for:
 * - date: For efficient week and day queries
 * - meal_id: For cascade delete performance
 * - (date, meal_type): Unique constraint to prevent double booking
 *
 * Foreign key cascade delete ensures meal plans are automatically removed
 * when the associated meal is deleted.
 *
 * @property id Unique identifier for the meal plan (auto-generated)
 * @property mealId The ID of the meal being planned (foreign key to meals table)
 * @property date The date for which the meal is planned (stored as days since epoch)
 * @property mealType The type of meal (BREAKFAST, LUNCH, DINNER, SNACK)
 * @property createdAt Timestamp when the meal plan was created
 * @property serverId Cloud backend ID (null if not yet synced)
 * @property syncStatus Current sync status ("synced", "pending", "conflict", "error")
 */
@Entity(
    tableName = "meal_plans",
    indices = [
        Index(value = ["date"]),
        Index(value = ["meal_id"]),
        Index(value = ["date", "meal_type"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    
    @ColumnInfo(name = "date")
    val date: Long, // Days since epoch
    
    @ColumnInfo(name = "meal_type")
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "server_id")
    val serverId: String? = null,
    
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "pending"
)
