package com.shoppit.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.data.local.entity.PlaceholderEntity

/**
 * Main Room database for the Shoppit application.
 * 
 * This database serves as the offline-first data persistence layer.
 * 
 * Version 3: Added indices on meals table for performance optimization
 * Version 2: Added MealEntity and MealDao for meal management feature
 * Version 1: Initial database setup with placeholder entity
 */
@Database(
    entities = [
        PlaceholderEntity::class,
        MealEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class, MealConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
}
