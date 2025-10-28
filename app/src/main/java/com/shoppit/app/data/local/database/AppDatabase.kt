package com.shoppit.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shoppit.app.data.local.dao.MealDao
import com.shoppit.app.data.local.dao.MealPlanDao
import com.shoppit.app.data.local.dao.ShoppingListDao
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.data.local.entity.MealPlanEntity
import com.shoppit.app.data.local.entity.PlaceholderEntity
import com.shoppit.app.data.local.entity.ShoppingListItemEntity

/**
 * Main Room database for the Shoppit application.
 * 
 * This database serves as the offline-first data persistence layer.
 * 
 * Version 5: Added shopping_list_items table for shopping list generation feature
 * Version 4: Added meal_plans table for meal planning feature
 * Version 3: Added indices on meals table for performance optimization
 * Version 2: Added MealEntity and MealDao for meal management feature
 * Version 1: Initial database setup with placeholder entity
 */
@Database(
    entities = [
        PlaceholderEntity::class,
        MealEntity::class,
        MealPlanEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class, MealConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
}
