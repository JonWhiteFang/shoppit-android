package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal

/**
 * Extension function to convert MealEntity to domain Meal model.
 *
 * @return Domain Meal object
 */
fun MealEntity.toDomainModel(): Meal {
    return Meal(
        id = id,
        name = name,
        ingredients = ingredients.map { it.toDomainModel() },
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Extension function to convert domain Meal model to MealEntity.
 *
 * @return MealEntity for database storage
 */
fun Meal.toEntity(): MealEntity {
    return MealEntity(
        id = id,
        name = name,
        ingredients = ingredients.map { it.toEntity() },
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Extension function to convert IngredientEntity to domain Ingredient model.
 *
 * @return Domain Ingredient object
 */
fun IngredientEntity.toDomainModel(): Ingredient {
    return Ingredient(
        name = name,
        quantity = quantity,
        unit = unit
    )
}

/**
 * Extension function to convert domain Ingredient model to IngredientEntity.
 *
 * @return IngredientEntity for database storage
 */
fun Ingredient.toEntity(): IngredientEntity {
    return IngredientEntity(
        name = name,
        quantity = quantity,
        unit = unit
    )
}
