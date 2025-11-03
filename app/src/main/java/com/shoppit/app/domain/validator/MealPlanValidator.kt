package com.shoppit.app.domain.validator

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validator for meal plan domain objects.
 * Ensures meal plan data meets business rules before persistence.
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4
 */
class MealPlanValidator @Inject constructor() : DataValidator<MealPlan> {

    /**
     * Validates a meal plan according to business rules.
     *
     * Business rules:
     * - Date must not be in the past (optional business rule)
     * - Meal type must be one of the allowed values (BREAKFAST, LUNCH, DINNER, SNACK)
     * - Meal ID must be positive
     *
     * @param data The meal plan to validate
     * @return ValidationResult indicating success or failure with detailed errors
     */
    override fun validate(data: MealPlan): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate date is not in the past
        val today = LocalDate.now()
        if (data.date.isBefore(today)) {
            errors.add(
                ValidationError(
                    field = "date",
                    message = "Date cannot be in the past",
                    code = ValidationError.CODE_INVALID_VALUE
                )
            )
        }

        // Validate meal type is one of allowed values
        // This is implicitly validated by the enum type, but we check for completeness
        val validMealTypes = MealType.entries
        if (data.mealType !in validMealTypes) {
            errors.add(
                ValidationError(
                    field = "mealType",
                    message = "Meal type must be one of: ${validMealTypes.joinToString(", ")}",
                    code = ValidationError.CODE_INVALID_VALUE
                )
            )
        }

        // Validate meal ID is positive
        if (data.mealId <= 0) {
            errors.add(
                ValidationError(
                    field = "mealId",
                    message = "Meal ID must be positive",
                    code = ValidationError.CODE_INVALID_VALUE
                )
            )
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
