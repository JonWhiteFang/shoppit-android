package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
 * Unit tests for UpdateMealPlanUseCase.
 * Tests updating meal assignments.
 */
@ExperimentalCoroutinesApi
class UpdateMealPlanUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var useCase: UpdateMealPlanUseCase

    @Before
    fun setUp() {
        repository = FakeMealPlanRepository()
        useCase = UpdateMealPlanUseCase(repository)
    }

    @Test
    fun `updates mealId correctly`() = runTest {
        // Given
        val existingPlan = MealPlan(
            id = 1,
            mealId = 1,
            date = LocalDate.now(),
            mealType = MealType.LUNCH
        )
        repository.setMealPlans(listOf(existingPlan))

        // When
        val result = useCase(mealPlanId = 1, newMealId = 2)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(2L, plans[0].mealId)
        assertEquals(existingPlan.date, plans[0].date)
        assertEquals(existingPlan.mealType, plans[0].mealType)
    }

    @Test
    fun `preserves date and meal type when updating`() = runTest {
        // Given
        val date = LocalDate.now()
        val existingPlan = MealPlan(
            id = 1,
            mealId = 1,
            date = date,
            mealType = MealType.BREAKFAST
        )
        repository.setMealPlans(listOf(existingPlan))

        // When
        val result = useCase(mealPlanId = 1, newMealId = 5)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(date, plans[0].date)
        assertEquals(MealType.BREAKFAST, plans[0].mealType)
    }

    @Test
    fun `returns error when meal plan not found`() = runTest {
        // Given - empty repository

        // When
        val result = useCase(mealPlanId = 999, newMealId = 2)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Meal plan not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val existingPlan = MealPlan(
            id = 1,
            mealId = 1,
            date = LocalDate.now(),
            mealType = MealType.LUNCH
        )
        repository.setMealPlans(listOf(existingPlan))
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(mealPlanId = 1, newMealId = 2)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
