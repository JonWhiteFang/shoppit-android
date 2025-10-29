package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag

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
        tags = tags.toTagSet(),
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
        tags = tags.toTagString(),
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

/**
 * Extension function to convert a comma-separated string to a set of MealTag enums.
 * Invalid tag names are ignored.
 *
 * @return Set of MealTag enums, or empty set if string is blank
 */
fun String.toTagSet(): Set<MealTag> {
    if (this.isBlank()) return emptySet()
    return this.split(",")
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
 * Extension function to convert a set of MealTag enums to a comma-separated string.
 *
 * @return Comma-separated string of tag names
 */
fun Set<MealTag>.toTagString(): String {
    return this.joinToString(",") { it.name }
}
