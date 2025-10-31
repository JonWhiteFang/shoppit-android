package com.shoppit.app.domain.validator

import com.shoppit.app.domain.model.Ingredient
import javax.inject.Inject

/**
 * Validator for ingredient domain objects.
 * Ensures ingredient data meets business rules before persistence.
 */
class IngredientValidator @Inject constructor() : DataValidator<Ingredient> {

    /**
     * Validates an ingredient according to business rules.
     *
     * Business rules:
     * - Ingredient name must contain at least one non-whitespace character
     * - Quantity must be a valid positive number if provided
     * - Unit is optional and has no validation
     *
     * @param data The ingredient to validate
     * @return ValidationResult indicating success or failure with detailed errors
     */
    override fun validate(data: Ingredient): ValidationResult {
        val errors = mutableListOf<ValidationError>()

        // Validate ingredient name
        if (data.name.isBlank()) {
            errors.add(
                ValidationError(
                    field = "name",
                    message = "Ingredient name cannot be empty",
                    code = ValidationError.CODE_REQUIRED
                )
            )
        }

        // Validate quantity if provided
        if (data.quantity.isNotBlank()) {
            val quantityValue = data.quantity.toDoubleOrNull()
            when {
                quantityValue == null -> errors.add(
                    ValidationError(
                        field = "quantity",
                        message = "Quantity must be a valid number",
                        code = ValidationError.CODE_INVALID_FORMAT
                    )
                )
                quantityValue <= 0 -> errors.add(
                    ValidationError(
                        field = "quantity",
                        message = "Quantity must be greater than zero",
                        code = ValidationError.CODE_OUT_OF_RANGE
                    )
                )
            }
        }

        // Unit is optional, no validation needed

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
