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
 * Unit tests for CopyDayPlansUseCase.
 * Tests copying meal plans from one day to another.
 */
@ExperimentalCoroutinesApi
class CopyDayPlansUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealPlanRepository
    private lateinit var useCase: CopyDayPlansUseCase

    @Before
    fun setUp() {
        repository = FakeMealPlanRepository()
        useCase = CopyDayPlansUseCase(repository)
    }

    @Test
    fun `copies all plans to target date`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = sourceDate, mealType = MealType.LUNCH)
        repository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = false)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(4, plans.size) // 2 original + 2 copied
        
        val targetPlans = plans.filter { it.date == targetDate }
        assertEquals(2, targetPlans.size)
        assertEquals(1L, targetPlans.find { it.mealType == MealType.BREAKFAST }?.mealId)
        assertEquals(2L, targetPlans.find { it.mealType == MealType.LUNCH }?.mealId)
    }

    @Test
    fun `clears target when replaceExisting is true`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        
        val sourcePlan = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.BREAKFAST)
        val existingTargetPlan = MealPlan(id = 2, mealId = 99, date = targetDate, mealType = MealType.LUNCH)
        repository.setMealPlans(listOf(sourcePlan, existingTargetPlan))

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = true)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        
        val targetPlans = plans.filter { it.date == targetDate }
        assertEquals(1, targetPlans.size) // Only copied plan, existing removed
        assertEquals(MealType.BREAKFAST, targetPlans[0].mealType)
        assertEquals(1L, targetPlans[0].mealId)
    }

    @Test
    fun `preserves meal types when copying`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = sourceDate, mealType = MealType.DINNER)
        repository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = false)

        // Then
        assertTrue(result.isSuccess)
        val targetPlans = repository.getMealPlansList().filter { it.date == targetDate }
        
        assertTrue(targetPlans.any { it.mealType == MealType.BREAKFAST })
        assertTrue(targetPlans.any { it.mealType == MealType.DINNER })
    }

    @Test
    fun `handles empty source date`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        repository.setMealPlans(emptyList())

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = false)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.getMealPlansList().isEmpty())
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        
        val plan = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.LUNCH)
        repository.setMealPlans(listOf(plan))
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = false)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `creates new plan instances with new IDs`() = runTest {
        // Given
        val sourceDate = LocalDate.now()
        val targetDate = sourceDate.plusDays(1)
        
        val plan = MealPlan(id = 1, mealId = 1, date = sourceDate, mealType = MealType.BREAKFAST)
        repository.setMealPlans(listOf(plan))

        // When
        val result = useCase(sourceDate, targetDate, replaceExisting = false)

        // Then
        assertTrue(result.isSuccess)
        val plans = repository.getMealPlansList()
        assertEquals(2, plans.size)
        
        val targetPlan = plans.find { it.date == targetDate }!!
        assertTrue(targetPlan.id != plan.id) // New ID assigned
    }
}
