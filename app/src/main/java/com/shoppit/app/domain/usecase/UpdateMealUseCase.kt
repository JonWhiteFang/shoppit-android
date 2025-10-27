package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import com.shoppit.app.domain.validator.MealValidator
import javax.inject.Inject

/**
 * Use case for updating an existing meal.
 * Validates the meal before persisting to ensure business rules are met.
 *
 * Requirements:
 * - 4.2: Validate updated meal data
 * - 4.3: Update meal in database preserving original identifier
 * - 4.4: Display confirmation message on success
 */
class UpdateMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    /**
     * Invokes the use case to update an existing meal.
     * Validates the meal first, then persists if validation passes.
     *
     * @param meal The meal with updated data (must have valid ID)
     * @return Result indicating success or validation/database error
     */
    suspend operator fun invoke(meal: Meal): Result<Unit> {
        return validator.validate(meal).fold(
            onSuccess = { repository.updateMeal(meal) },
            onFailure = { Result.failure(it) }
        )
    }
}
