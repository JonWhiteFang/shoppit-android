package com.shoppit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Temporary placeholder entity to satisfy Room's requirement for at least one entity.
 * This will be removed once actual entities (Meal, Ingredient, etc.) are implemented.
 * 
 * DO NOT USE THIS ENTITY IN PRODUCTION CODE.
 */
@Entity(tableName = "placeholder")
internal data class PlaceholderEntity(
    @PrimaryKey
    val id: Int = 1
)
