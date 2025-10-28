package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for copying all meal plans from one day to another.
 * Supports optionally replacing existing plans on the target date.
 * Uses batch insert for efficiency.
 *
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
class CopyDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    /**
     * Invokes the use case to copy meal plans from source date to target date.
     * If replaceExisting is true, clears target date before copying.
     * Creates new MealPlan instances with target date and uses batch insert.
     *
     * @param sourceDate The date to copy meal plans from
     * @param targetDate The date to copy meal plans to
     * @param replaceExisting If true, clears existing plans on target date first
     * @return Result indicating success or error
     */
    suspend operator fun invoke(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        replaceExisting: Boolean = false
    ): Result<Unit> {
        return repository.getMealPlansForDate(sourceDate).first().flatMap { sourcePlans ->
            if (replaceExisting) {
                repository.deleteMealPlansForDate(targetDate).flatMap {
                    copyPlans(sourcePlans, targetDate)
                }
            } else {
                copyPlans(sourcePlans, targetDate)
            }
        }
    }
    
    /**
     * Helper function to copy meal plans to a new date.
     * Creates new MealPlan instances with id=0 and the target date.
     *
     * @param sourcePlans The meal plans to copy
     * @param targetDate The date to assign to the copied plans
     * @return Result indicating success or error
     */
    private suspend fun copyPlans(
        sourcePlans: List<MealPlan>,
        targetDate: LocalDate
    ): Result<Unit> {
        val newPlans = sourcePlans.map { plan ->
            plan.copy(id = 0, date = targetDate)
        }
        return repository.addMealPlans(newPlans).map { Unit }
    }
}
