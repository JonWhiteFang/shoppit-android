package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.validator.MealPlanValidator
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for assigning a meal to a specific date and meal type.
 * Creates a new meal plan linking the meal to the specified slot.
 *
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 7.1, 7.2, 7.3, 7.4
 */
class AssignMealToPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository,
    private val validator: MealPlanValidator
) {
    /**
     * Invokes the use case to assign a meal to a plan.
     * Validates the meal plan before creating it and persisting to the repository.
     *
     * @param mealId The ID of the meal to assign
     * @param date The date for the meal plan
     * @param mealType The type of meal (breakfast, lunch, dinner, snack)
     * @return Result with the ID of the newly created meal plan or validation/repository error
     */
    suspend operator fun invoke(
        mealId: Long,
        date: LocalDate,
        mealType: MealType
    ): Result<Long> {
        val mealPlan = MealPlan(
            mealId = mealId,
            date = date,
            mealType = mealType
        )
        
        // Validate meal plan before repository call
        val validationResult = validator.validate(mealPlan)
        if (validationResult.isInvalid()) {
            val errors = validationResult.getErrors()
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            return Result.failure(AppError.ValidationError(message))
        }
        
        // Proceed with repository call if validation passes
        return repository.addMealPlan(mealPlan)
    }
}
