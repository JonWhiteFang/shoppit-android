package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for ClearDayPlansUseCase.
 * Tests clearing all meal plans for a specific date.
 */
@ExperimentalCoroutinesApi
class ClearDayPlansUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var useCase: ClearDayPlansUseCase

    @Before
    fun setUp() {
        repository = FakeMealPlanRepository()
        useCase = ClearDayPlansUseCase(repository)
    }

    @Test
    fun `calls repository correctly`() = runTest {
        // Given
        val date = LocalDate.now()
        val plan1 = MealPlan(id = 1, mealId = 1, date = date, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = date, mealType = MealType.LUNCH)
        repository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(date)

        // Then
        assertTrue(result.isSuccess)
        val remainingPlans = repository.getMealPlansList()
        assertTrue(remainingPlans.isEmpty())
    }

    @Test
    fun `removes all plans for specified date`() = runTest {
        // Given
        val targetDate = LocalDate.now()
        val otherDate = targetDate.plusDays(1)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = targetDate, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = targetDate, mealType = MealType.LUNCH)
        val plan3 = MealPlan(id = 3, mealId = 3, date = otherDate, mealType = MealType.DINNER)
        repository.setMealPlans(listOf(plan1, plan2, plan3))

        // When
        val result = useCase(targetDate)

        // Then
        assertTrue(result.isSuccess)
        val remainingPlans = repository.getMealPlansList()
        assertEquals(1, remainingPlans.size)
        assertEquals(otherDate, remainingPlans[0].date)
    }

    @Test
    fun `handles date with no plans`() = runTest {
        // Given
        val date = LocalDate.now()
        repository.setMealPlans(emptyList())

        // When
        val result = useCase(date)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val date = LocalDate.now()
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(date)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `removes multiple meal types for same date`() = runTest {
        // Given
        val date = LocalDate.now()
        val plan1 = MealPlan(id = 1, mealId = 1, date = date, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = date, mealType = MealType.LUNCH)
        val plan3 = MealPlan(id = 3, mealId = 3, date = date, mealType = MealType.DINNER)
        val plan4 = MealPlan(id = 4, mealId = 4, date = date, mealType = MealType.SNACK)
        repository.setMealPlans(listOf(plan1, plan2, plan3, plan4))

        // When
        val result = useCase(date)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.getMealPlansList().isEmpty())
    }
}
