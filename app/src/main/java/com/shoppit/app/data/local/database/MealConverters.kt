package com.shoppit.app.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shoppit.app.data.local.entity.IngredientEntity

/**
 * Type converters for meal-related data types in Room database.
 * Handles serialization and deserialization of ingredient lists using Gson.
 */
class MealConverters {
    
    private val gson = Gson()
    
    /**
     * Converts a JSON string to a list of IngredientEntity objects.
     * @param value JSON string representation of ingredient list
     * @return List of IngredientEntity objects, or empty list if value is null
     */
    @TypeConverter
    fun fromIngredientList(value: String?): List<IngredientEntity> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<IngredientEntity>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    /**
     * Converts a list of IngredientEntity objects to a JSON string.
     * @param list List of IngredientEntity objects to serialize
     * @return JSON string representation of the ingredient list
     */
    @TypeConverter
    fun toIngredientList(list: List<IngredientEntity>): String {
        return gson.toJson(list)
    }
}
