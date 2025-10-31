package com.shoppit.app.domain.usecase

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for GetMealPlanHistoryUseCase.
 * Tests history calculation from meal plans over the past 30 days.
 */
@ExperimentalCoroutinesApi
class GetMealPlanHistoryUseCaseTest : RepositoryTest() {

    private lateinit var mealPlanRepository: FakeMealPlanRepository
    private lateinit var useCase: GetMealPlanHistoryUseCase

    @Before
    fun setUp() {
        mealPlanRepository = FakeMealPlanRepository()
        useCase = GetMealPlanHistoryUseCase(mealPlanRepository)
    }

    @Test
    fun `calculates history with various plan patterns`() = runTest {
        // Given - meal 1 planned 3 times, meal 2 planned once
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(5), mealType = MealType.LUNCH),
            MealPlan(id = 2, mealId = 1, date = today.minusDays(15), mealType = MealType.DINNER),
            MealPlan(id = 3, mealId = 1, date = today.minusDays(25), mealType = MealType.LUNCH),
            MealPlan(id = 4, mealId = 2, date = today.minusDays(10), mealType = MealType.BREAKFAST)
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        // Meal 1 history
        val meal1History = historyMap[1]!!
        assertEquals(1L, meal1History.mealId)
        assertEquals(3, meal1History.planCount)
        assertEquals(today.minusDays(5), meal1History.lastPlannedDate)
        assertEquals(3, meal1History.planDates.size)
        assertEquals(today.minusDays(25), meal1History.planDates[0]) // Sorted ascending
        assertEquals(today.minusDays(15), meal1History.planDates[1])
        assertEquals(today.minusDays(5), meal1History.planDates[2])
        
        // Meal 2 history
        val meal2History = historyMap[2]!!
        assertEquals(2L, meal2History.mealId)
        assertEquals(1, meal2History.planCount)
        assertEquals(today.minusDays(10), meal2History.lastPlannedDate)
        assertEquals(1, meal2History.planDates.size)
    }

    @Test
    fun `filters to 30 day date range`() = runTest {
        // Given - plans within and outside 30 day window
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(5), mealType = MealType.LUNCH),
            MealPlan(id = 2, mealId = 1, date = today.minusDays(29), mealType = MealType.DINNER),
            MealPlan(id = 3, mealId = 1, date = today.minusDays(31), mealType = MealType.LUNCH), // Outside window
            MealPlan(id = 4, mealId = 1, date = today.minusDays(40), mealType = MealType.BREAKFAST) // Outside window
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        val meal1History = historyMap[1]!!
        assertEquals(2, meal1History.planCount) // Only 2 plans within 30 days
        assertEquals(today.minusDays(5), meal1History.lastPlannedDate)
    }

    @Test
    fun `groups plans by meal ID correctly`() = runTest {
        // Given - multiple meals with different plan counts
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(5), mealType = MealType.LUNCH),
            MealPlan(id = 2, mealId = 2, date = today.minusDays(10), mealType = MealType.DINNER),
            MealPlan(id = 3, mealId = 3, date = today.minusDays(15), mealType = MealType.BREAKFAST),
            MealPlan(id = 4, mealId = 1, date = today.minusDays(20), mealType = MealType.LUNCH)
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        assertEquals(3, historyMap.size) // 3 unique meals
        assertEquals(2, historyMap[1]!!.planCount)
        assertEquals(1, historyMap[2]!!.planCount)
        assertEquals(1, historyMap[3]!!.planCount)
    }

    @Test
    fun `handles empty history`() = runTest {
        // Given - no meal plans
        mealPlanRepository.setMealPlans(emptyList())

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        assertTrue(historyMap.isEmpty())
    }

    @Test
    fun `handles meal never planned`() = runTest {
        // Given - only one meal planned
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(5), mealType = MealType.LUNCH)
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        // Meal 1 exists in history
        assertTrue(historyMap.containsKey(1))
        
        // Meal 2 does not exist in history (never planned)
        assertTrue(!historyMap.containsKey(2))
    }

    @Test
    fun `determines most recent plan date correctly`() = runTest {
        // Given - multiple plans for same meal on different dates
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(20), mealType = MealType.LUNCH),
            MealPlan(id = 2, mealId = 1, date = today.minusDays(5), mealType = MealType.DINNER), // Most recent
            MealPlan(id = 3, mealId = 1, date = today.minusDays(15), mealType = MealType.BREAKFAST)
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        val meal1History = historyMap[1]!!
        assertEquals(today.minusDays(5), meal1History.lastPlannedDate)
    }

    @Test
    fun `handles repository error`() = runTest {
        // Given - repository configured to fail
        val exception = Exception("Database error")
        mealPlanRepository.setShouldFail(true, exception)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `sorts plan dates in ascending order`() = runTest {
        // Given - plans in random order
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today.minusDays(10), mealType = MealType.LUNCH),
            MealPlan(id = 2, mealId = 1, date = today.minusDays(5), mealType = MealType.DINNER),
            MealPlan(id = 3, mealId = 1, date = today.minusDays(20), mealType = MealType.BREAKFAST)
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        val meal1History = historyMap[1]!!
        val dates = meal1History.planDates
        
        // Verify dates are sorted ascending
        assertEquals(today.minusDays(20), dates[0])
        assertEquals(today.minusDays(10), dates[1])
        assertEquals(today.minusDays(5), dates[2])
    }

    @Test
    fun `handles meal planned on boundary dates`() = runTest {
        // Given - plans exactly at 30 day boundary
        val today = LocalDate.now()
        val plans = listOf(
            MealPlan(id = 1, mealId = 1, date = today, mealType = MealType.LUNCH), // Today
            MealPlan(id = 2, mealId = 1, date = today.minusDays(30), mealType = MealType.DINNER) // Exactly 30 days ago
        )
        mealPlanRepository.setMealPlans(plans)

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        val meal1History = historyMap[1]!!
        assertEquals(2, meal1History.planCount) // Both should be included
        assertEquals(today, meal1History.lastPlannedDate)
    }

    @Test
    fun `handles null last planned date when no plans exist`() = runTest {
        // Given - empty meal plans
        mealPlanRepository.setMealPlans(emptyList())

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        val historyMap = result.getOrNull()!!
        
        // For a meal that was never planned, it won't be in the map
        // This test verifies the map is empty
        assertTrue(historyMap.isEmpty())
    }
}
