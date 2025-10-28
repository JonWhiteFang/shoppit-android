package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.MealPlanEntity
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import java.time.LocalDate

/**
 * Extension function to convert MealPlanEntity to domain MealPlan model.
 * Handles conversion of date from epoch day to LocalDate and meal type from String to enum.
 *
 * @return Domain model representation of the meal plan
 */
fun MealPlanEntity.toDomainModel(): MealPlan {
    return MealPlan(
        id = id,
        mealId = mealId,
        date = LocalDate.ofEpochDay(date),
        mealType = MealType.valueOf(mealType),
        createdAt = createdAt
    )
}

/**
 * Extension function to convert domain MealPlan model to MealPlanEntity.
 * Handles conversion of date from LocalDate to epoch day and meal type from enum to String.
 *
 * @return Room entity representation of the meal plan
 */
fun MealPlan.toEntity(): MealPlanEntity {
    return MealPlanEntity(
        id = id,
        mealId = mealId,
        date = date.toEpochDay(),
        mealType = mealType.name,
        createdAt = createdAt
    )
}
