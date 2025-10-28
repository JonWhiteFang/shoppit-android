package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.repository.MealPlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for assigning a meal to a specific date and meal type.
 * Creates a new meal plan linking the meal to the specified slot.
 *
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 */
class AssignMealToPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    /**
     * Invokes the use case to assign a meal to a plan.
     * Creates a new MealPlan and persists it to the repository.
     *
     * @param mealId The ID of the meal to assign
     * @param date The date for the meal plan
     * @param mealType The type of meal (breakfast, lunch, dinner, snack)
     * @return Result with the ID of the newly created meal plan or error
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
        return repository.addMealPlan(mealPlan)
    }
}
