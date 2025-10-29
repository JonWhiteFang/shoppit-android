package com.shoppit.app.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.domain.model.MealTag

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
    
    /**
     * Converts a comma-separated string to a set of MealTag enums.
     * Invalid tag names are ignored.
     * @param value Comma-separated string of tag names
     * @return Set of MealTag enums, or empty set if value is blank
     */
    @TypeConverter
    fun fromTagString(value: String?): Set<MealTag> {
        if (value.isNullOrBlank()) return emptySet()
        return value.split(",")
            .mapNotNull { tagName ->
                try {
                    MealTag.valueOf(tagName.trim())
                } catch (e: IllegalArgumentException) {
                    null // Ignore invalid tags
                }
            }
            .toSet()
    }
    
    /**
     * Converts a set of MealTag enums to a comma-separated string.
     * @param tags Set of MealTag enums to serialize
     * @return Comma-separated string of tag names
     */
    @TypeConverter
    fun toTagString(tags: Set<MealTag>): String {
        return tags.joinToString(",") { it.name }
    }
}
