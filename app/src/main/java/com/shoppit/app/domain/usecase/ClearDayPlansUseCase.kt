package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.MealPlanRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for clearing all meal plans for a specific date.
 * Removes all meal plan assignments for the given day.
 *
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
class ClearDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    /**
     * Invokes the use case to clear all meal plans for a date.
     * Deletes all meal plan assignments for the specified day.
     *
     * @param date The date to clear meal plans for
     * @return Result indicating success or error
     */
    suspend operator fun invoke(date: LocalDate): Result<Unit> {
        return repository.deleteMealPlansForDate(date)
    }
}
