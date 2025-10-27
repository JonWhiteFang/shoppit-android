package com.shoppit.app.domain.validator

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.Meal
import javax.inject.Inject

/**
 * Validator for meal domain objects.
 * Ensures meal data meets business rules before persistence.
 */
class MealValidator @Inject constructor() {
    /**
     * Validates a meal according to business rules.
     *
     * Business rules:
     * - Meal name must contain at least one non-whitespace character
     * - Meal must have at least one ingredient
     * - Each ingredient must have a non-empty name
     *
     * @param meal The meal to validate
     * @return Result indicating success or validation error with message
     */
    fun validate(meal: Meal): Result<Unit> {
        return when {
            meal.name.isBlank() -> 
                Result.failure(ValidationException("Meal name cannot be empty"))
            
            meal.ingredients.isEmpty() -> 
                Result.failure(ValidationException("Meal must have at least one ingredient"))
            
            meal.ingredients.any { it.name.isBlank() } -> 
                Result.failure(ValidationException("Ingredient name cannot be empty"))
            
            else -> Result.success(Unit)
        }
    }
}

/**
 * Exception thrown when meal validation fails.
 * Contains a descriptive message about the validation failure.
 */
class ValidationException(message: String) : Exception(message)
