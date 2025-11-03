package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.validator.MealPlanValidator
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for AssignMealToPlanUseCase.
 * Tests meal plan creation and assignment.
 */
@ExperimentalCoroutinesApi
class AssignMealToPlanUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var validator: MealPlanValidator
    private lateinit var useCase: AssignMealToPlanUseCase

    @Before
    fun setUp() {
        repository = FakeMealPlanRepository()
        validator = MealPlanValidator()
        useCase = AssignMealToPlanUseCase(repository, validator)
    }

    @Test
    fun `creates correct MealPlan with provided parameters`() = runTest {
        // Given
        val mealId = 1L
        val date = LocalDate.now()
        val mealType = MealType.LUNCH

        // When
        val result = useCase(mealId, date, mealType)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        
        val plans = repository.getMealPlansList()
        assertEquals(1, plans.size)
        assertEquals(mealId, plans[0].mealId)
        assertEquals(date, plans[0].date)
        assertEquals(mealType, plans[0].mealType)
    }

    @Test
    fun `assigns meal to BREAKFAST slot`() = runTest {
        // Given
        val mealId = 1L
        val date = LocalDate.now()

        // When
        val result = useCase(mealId, date, MealType.BREAKFAST)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(MealType.BREAKFAST, plans[0].mealType)
    }

    @Test
    fun `assigns meal to DINNER slot`() = runTest {
        // Given
        val mealId = 1L
        val date = LocalDate.now()

        // When
        val result = useCase(mealId, date, MealType.DINNER)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(MealType.DINNER, plans[0].mealType)
    }

    @Test
    fun `assigns meal to SNACK slot`() = runTest {
        // Given
        val mealId = 1L
        val date = LocalDate.now()

        // When
        val result = useCase(mealId, date, MealType.SNACK)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(MealType.SNACK, plans[0].mealType)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(1L, LocalDate.now(), MealType.LUNCH)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `allows same meal to be assigned to multiple slots`() = runTest {
        // Given
        val mealId = 1L
        val date = LocalDate.now()

        // When
        useCase(mealId, date, MealType.BREAKFAST)
        useCase(mealId, date, MealType.LUNCH)

        // Then
        val plans = repository.getMealPlansList()
        assertEquals(2, plans.size)
        assertTrue(plans.all { it.mealId == mealId })
    }
}
