package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import com.shoppit.app.domain.validator.MealValidator
import javax.inject.Inject

/**
 * Use case for adding a new meal.
 * Validates the meal before persisting to ensure business rules are met.
 *
 * Requirements:
 * - 1.1: Validate meal name contains at least one non-whitespace character
 * - 1.2: Validate at least one ingredient is included
 * - 1.3: Persist meal to database with unique identifier
 * - 1.4: Display confirmation message on success
 */
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    /**
     * Invokes the use case to add a new meal.
     * Validates the meal first, then persists if validation passes.
     *
     * @param meal The meal to add
     * @return Result with the ID of the newly created meal or validation/database error
     */
    suspend operator fun invoke(meal: Meal): Result<Long> {
        // Validate the meal first
        val validationResult = validator.validate(meal)
        
        return if (validationResult.isValid()) {
            repository.addMeal(meal)
        } else {
            // Convert validation errors to failure result with formatted message
            val errors = validationResult.getErrors()
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            Result.failure(com.shoppit.app.domain.validator.ValidationException(message))
        }
    }
}
