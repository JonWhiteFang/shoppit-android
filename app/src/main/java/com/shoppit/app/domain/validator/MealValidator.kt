package com.shoppit.app.domain.validator

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.Meal
import javax.inject.Inject

/**
 * Validator for meal domain objects.
 * Ensures meal data meets business rules before persistence.
 */
class MealValidator @Inject constructor() : DataValidator<Meal> {
    
    /**
     * Validates a meal according to business rules.
     *
     * Business rules:
     * - Meal name must contain at least one non-whitespace character
     * - Meal name must not exceed 100 characters
     * - Meal must have at least one ingredient
     * - Each ingredient must have a non-empty name
     * - Quantity and unit are optional fields
     *
     * @param data The meal to validate
     * @return ValidationResult indicating success or failure with detailed errors
     */
    override fun validate(data: Meal): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Validate meal name
        when {
            data.name.isBlank() -> errors.add(
                ValidationError(
                    field = "name",
                    message = "Meal name cannot be empty",
                    code = ValidationError.CODE_REQUIRED
                )
            )
            data.name.length > 100 -> errors.add(
                ValidationError(
                    field = "name",
                    message = "Meal name cannot exceed 100 characters",
                    code = ValidationError.CODE_TOO_LONG
                )
            )
        }
        
        // Validate ingredients list
        if (data.ingredients.isEmpty()) {
            errors.add(
                ValidationError(
                    field = "ingredients",
                    message = "Meal must have at least one ingredient",
                    code = ValidationError.CODE_REQUIRED
                )
            )
        } else {
            // Validate each ingredient
            data.ingredients.forEachIndexed { index, ingredient ->
                if (ingredient.name.isBlank()) {
                    errors.add(
                        ValidationError(
                            field = "ingredients[$index].name",
                            message = "Ingredient name cannot be empty",
                            code = ValidationError.CODE_REQUIRED
                        )
                    )
                }
                
                // Quantity and unit are optional, no validation needed
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    /**
     * Legacy validation method for backward compatibility.
     * Converts ValidationResult to Result<Unit>.
     *
     * @param meal The meal to validate
     * @return Result indicating success or validation error with message
     */
    fun validateLegacy(meal: Meal): Result<Unit> {
        return validate(meal).toResult()
    }
}
