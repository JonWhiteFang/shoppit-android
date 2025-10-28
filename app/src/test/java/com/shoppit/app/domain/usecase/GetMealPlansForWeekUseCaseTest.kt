package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for GetMealPlansForWeekUseCase.
 * Tests combining meal plans with meal details and handling deleted meals.
 */
@ExperimentalCoroutinesApi
class GetMealPlansForWeekUseCaseTest : RepositoryTest() {

    private lateinit var mealPlanRepository: FakeMealPlanRepository
    private lateinit var mealRepository: FakeMealRepository
    private lateinit var useCase: GetMealPlansForWeekUseCase

    @Before
    fun setUp() {
        mealPlanRepository = FakeMealPlanRepository()
        mealRepository = FakeMealRepository()
        useCase = GetMealPlansForWeekUseCase(mealPlanRepository, mealRepository)
    }

    @Test
    fun `combines plans with meals correctly`() = runTest {
        // Given
        val meal1 = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val meal2 = Meal(id = 2, name = "Salad", ingredients = listOf(Ingredient(name = "Lettuce")))
        mealRepository.setMeals(listOf(meal1, meal2))

        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        val tuesday = monday.plusDays(1)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = monday, mealType = MealType.LUNCH)
        val plan2 = MealPlan(id = 2, mealId = 2, date = tuesday, mealType = MealType.DINNER)
        mealPlanRepository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(monday).first()

        // Then
        assertTrue(result.isSuccess)
        val weekData = result.getOrNull()!!
        assertEquals(monday, weekData.startDate)
        assertEquals(monday.plusDays(6), weekData.endDate)
        assertEquals(2, weekData.plansByDate.size)
        
        val mondayPlans = weekData.plansByDate[monday]!!
        assertEquals(1, mondayPlans.size)
        assertEquals("Pasta", mondayPlans[0].meal.name)
        
        val tuesdayPlans = weekData.plansByDate[tuesday]!!
        assertEquals(1, tuesdayPlans.size)
        assertEquals("Salad", tuesdayPlans[0].meal.name)
    }

    @Test
    fun `handles deleted meals gracefully`() = runTest {
        // Given - meal plan exists but meal is deleted
        val meal1 = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        mealRepository.setMeals(listOf(meal1))

        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = monday, mealType = MealType.LUNCH)
        val plan2 = MealPlan(id = 2, mealId = 999, date = monday, mealType = MealType.DINNER) // Deleted meal
        mealPlanRepository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(monday).first()

        // Then
        assertTrue(result.isSuccess)
        val weekData = result.getOrNull()!!
        
        val mondayPlans = weekData.plansByDate[monday]!!
        assertEquals(1, mondayPlans.size) // Only plan with existing meal
        assertEquals("Pasta", mondayPlans[0].meal.name)
    }

    @Test
    fun `adjusts start date to Monday`() = runTest {
        // Given
        val wednesday = LocalDate.now().with(DayOfWeek.WEDNESDAY)
        val expectedMonday = wednesday.with(DayOfWeek.MONDAY)
        
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())

        // When
        val result = useCase(wednesday).first()

        // Then
        assertTrue(result.isSuccess)
        val weekData = result.getOrNull()!!
        assertEquals(expectedMonday, weekData.startDate)
        assertEquals(expectedMonday.plusDays(6), weekData.endDate)
    }

    @Test
    fun `returns empty week data when no plans exist`() = runTest {
        // Given
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        mealRepository.setMeals(emptyList())
        mealPlanRepository.setMealPlans(emptyList())

        // When
        val result = useCase(monday).first()

        // Then
        assertTrue(result.isSuccess)
        val weekData = result.getOrNull()!!
        assertTrue(weekData.plansByDate.isEmpty())
    }

    @Test
    fun `groups multiple plans by date`() = runTest {
        // Given
        val meal1 = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient(name = "Pasta")))
        val meal2 = Meal(id = 2, name = "Salad", ingredients = listOf(Ingredient(name = "Lettuce")))
        mealRepository.setMeals(listOf(meal1, meal2))

        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        
        val plan1 = MealPlan(id = 1, mealId = 1, date = monday, mealType = MealType.BREAKFAST)
        val plan2 = MealPlan(id = 2, mealId = 2, date = monday, mealType = MealType.LUNCH)
        mealPlanRepository.setMealPlans(listOf(plan1, plan2))

        // When
        val result = useCase(monday).first()

        // Then
        assertTrue(result.isSuccess)
        val weekData = result.getOrNull()!!
        
        val mondayPlans = weekData.plansByDate[monday]!!
        assertEquals(2, mondayPlans.size)
    }
}
