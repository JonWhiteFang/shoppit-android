package com.shoppit.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shoppit.app.data.local.entity.PlaceholderEntity

/**
 * Main Room database for the Shoppit application.
 * 
 * This database serves as the offline-first data persistence layer.
 * Entities will be added in future feature implementations.
 * 
 * Version 1: Initial database setup with placeholder entity
 * Note: PlaceholderEntity is temporary and will be removed when real entities are added
 */
@Database(
    entities = [
        PlaceholderEntity::class // Temporary - will be replaced with Meal, Ingredient, MealPlan, ShoppingListItem
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // DAOs will be added in future specs as features are implemented
    // Example: abstract fun mealDao(): MealDao
}
