package com.shoppit.app.data.local.dao

import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.data.local.entity.MealPlanEntity
import com.shoppit.app.util.DatabaseTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Instrumented tests for MealPlanDao.
 * Tests CRUD operations, unique constraints, and foreign key cascades with in-memory database.
 */
@HiltAndroidTest
class MealPlanDaoTest : DatabaseTest() {

    private lateinit var mealPlanDao: MealPlanDao
    private lateinit var mealDao: MealDao

    override fun setUp() {
        super.setUp()
        mealPlanDao = database.mealPlanDao()
        mealDao = database.mealDao()
    }

    @Test
    fun insertMealPlan_andGetMealPlansForDate_returnsInsertedPlan() = runTest {
        // Given - Create a meal first
        val meal = createTestMeal(name = "Pasta")
        val mealId = mealDao.insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlan = createTestMealPlan(
            mealId = mealId,
            date = date,
            mealType = "BREAKFAST"
        )

        // When
        mealPlanDao.insertMealPlan(mealPlan)
        val plans = mealPlanDao.getMealPlansForDate(date.toEpochDay()).first()

        // Then
        assertEquals(1, plans.size)
        assertEquals(mealId, plans[0].mealId)
        assertEquals(date.toEpochDay(), plans[0].date)
        assertEquals("BREAKFAST", plans[0].mealType)
    }

    @Test
    fun getMealPlansForWeek_returnsPlansInDateRange() = runTest {
        // Given - Create meals
        val meal1 = createTestMeal(name = "Breakfast Meal")
        val meal2 = createTestMeal(name = "Lunch Meal")
        val meal3 = createTestMeal(name = "Dinner Meal")
        val mealId1 = mealDao.insertMeal(meal1)
        val mealId2 = mealDao.insertMeal(meal2)
        val mealId3 = mealDao.insertMeal(meal3)
        
        // Create meal plans for different dates
        val monday = LocalDate.of(2024, 1, 15)
        val wednesday = LocalDate.of(2024, 1, 17)
        val friday = LocalDate.of(2024, 1, 19)
        val nextMonday = LocalDate.of(2024, 1, 22)
        
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId1, monday, "BREAKFAST"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId2, wednesday, "LUNCH"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId3, friday, "DINNER"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId1, nextMonday, "BREAKFAST"))

        // When - Query for week (Monday to Sunday)
        val sunday = LocalDate.of(2024, 1, 21)
        val plans = mealPlanDao.getMealPlansForWeek(
            monday.toEpochDay(),
            sunday.toEpochDay()
        ).first()

        // Then - Should return only plans within the week
        assertEquals(3, plans.size)
        assertEquals(monday.toEpochDay(), plans[0].date)
        assertEquals(wednesday.toEpochDay(), plans[1].date)
        assertEquals(friday.toEpochDay(), plans[2].date)
    }

    @Test
    fun getMealPlanById_returnsCorrectPlan() = runTest {
        // Given
        val meal = createTestMeal(name = "Test Meal")
        val mealId = mealDao.insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlan = createTestMealPlan(mealId, date, "LUNCH")
        val planId = mealPlanDao.insertMealPlan(mealPlan)

        // When
        val retrievedPlan = mealPlanDao.getMealPlanById(planId).first()

        // Then
        assertEquals(planId, retrievedPlan?.id)
        assertEquals(mealId, retrievedPlan?.mealId)
        assertEquals(date.toEpochDay(), retrievedPlan?.date)
        assertEquals("LUNCH", retrievedPlan?.mealType)
    }

    @Test
    fun updateMealPlan_modifiesExistingPlan() = runTest {
        // Given
        val meal1 = createTestMeal(name = "Original Meal")
        val meal2 = createTestMeal(name = "New Meal")
        val mealId1 = mealDao.insertMeal(meal1)
        val mealId2 = mealDao.insertMeal(meal2)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlan = createTestMealPlan(mealId1, date, "BREAKFAST")
        val planId = mealPlanDao.insertMealPlan(mealPlan)
        
        // Update to different meal
        val updatedPlan = mealPlan.copy(
            id = planId,
            mealId = mealId2
        )

        // When
        mealPlanDao.updateMealPlan(updatedPlan)
        val retrievedPlan = mealPlanDao.getMealPlanById(planId).first()

        // Then
        assertEquals(mealId2, retrievedPlan?.mealId)
        assertEquals(date.toEpochDay(), retrievedPlan?.date)
        assertEquals("BREAKFAST", retrievedPlan?.mealType)
    }

    @Test
    fun deleteMealPlanById_removesPlan() = runTest {
        // Given
        val meal = createTestMeal(name = "Test Meal")
        val mealId = mealDao.insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlan = createTestMealPlan(mealId, date, "DINNER")
        val planId = mealPlanDao.insertMealPlan(mealPlan)
        
        // Verify plan exists
        var retrievedPlan = mealPlanDao.getMealPlanById(planId).first()
        assertEquals(planId, retrievedPlan?.id)

        // When
        mealPlanDao.deleteMealPlanById(planId)
        retrievedPlan = mealPlanDao.getMealPlanById(planId).first()

        // Then
        assertNull(retrievedPlan)
    }

    @Test
    fun deleteMealPlansForDate_removesAllPlansForDate() = runTest {
        // Given
        val meal1 = createTestMeal(name = "Breakfast")
        val meal2 = createTestMeal(name = "Lunch")
        val meal3 = createTestMeal(name = "Dinner")
        val mealId1 = mealDao.insertMeal(meal1)
        val mealId2 = mealDao.insertMeal(meal2)
        val mealId3 = mealDao.insertMeal(meal3)
        
        val targetDate = LocalDate.of(2024, 1, 15)
        val otherDate = LocalDate.of(2024, 1, 16)
        
        // Create multiple plans for target date
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId1, targetDate, "BREAKFAST"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId2, targetDate, "LUNCH"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId3, targetDate, "DINNER"))
        
        // Create plan for different date
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId1, otherDate, "BREAKFAST"))

        // When
        mealPlanDao.deleteMealPlansForDate(targetDate.toEpochDay())
        val targetDatePlans = mealPlanDao.getMealPlansForDate(targetDate.toEpochDay()).first()
        val otherDatePlans = mealPlanDao.getMealPlansForDate(otherDate.toEpochDay()).first()

        // Then
        assertTrue(targetDatePlans.isEmpty())
        assertEquals(1, otherDatePlans.size)
    }

    @Test
    fun uniqueConstraint_preventsDoubleBookingSameSlot() = runTest {
        // Given
        val meal1 = createTestMeal(name = "First Meal")
        val meal2 = createTestMeal(name = "Second Meal")
        val mealId1 = mealDao.insertMeal(meal1)
        val mealId2 = mealDao.insertMeal(meal2)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealType = "BREAKFAST"
        
        // Insert first plan
        val plan1 = createTestMealPlan(mealId1, date, mealType)
        val planId1 = mealPlanDao.insertMealPlan(plan1)

        // When - Try to insert another plan for same date and meal type
        // Using REPLACE strategy, this should replace the existing plan
        val plan2 = createTestMealPlan(mealId2, date, mealType)
        val planId2 = mealPlanDao.insertMealPlan(plan2)

        // Then - Should have replaced the first plan
        val plans = mealPlanDao.getMealPlansForDate(date.toEpochDay()).first()
        assertEquals(1, plans.size)
        assertEquals(mealId2, plans[0].mealId) // Should be the second meal
        
        // First plan should no longer exist
        val plan1Retrieved = mealPlanDao.getMealPlanById(planId1).first()
        assertNull(plan1Retrieved)
    }

    @Test
    fun foreignKeyCascade_deletesMealPlansWhenMealDeleted() = runTest {
        // Given
        val meal = createTestMeal(name = "Test Meal")
        val mealId = mealDao.insertMeal(meal)
        
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 1, 16)
        
        // Create multiple meal plans for the same meal
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, date1, "BREAKFAST"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, date2, "LUNCH"))
        
        // Verify plans exist
        var plans1 = mealPlanDao.getMealPlansForDate(date1.toEpochDay()).first()
        var plans2 = mealPlanDao.getMealPlansForDate(date2.toEpochDay()).first()
        assertEquals(1, plans1.size)
        assertEquals(1, plans2.size)

        // When - Delete the meal
        mealDao.deleteMealById(mealId)

        // Then - All meal plans should be cascade deleted
        plans1 = mealPlanDao.getMealPlansForDate(date1.toEpochDay()).first()
        plans2 = mealPlanDao.getMealPlansForDate(date2.toEpochDay()).first()
        assertTrue(plans1.isEmpty())
        assertTrue(plans2.isEmpty())
    }

    @Test
    fun insertMealPlans_batchInsertWorksCorrectly() = runTest {
        // Given
        val meal1 = createTestMeal(name = "Meal 1")
        val meal2 = createTestMeal(name = "Meal 2")
        val meal3 = createTestMeal(name = "Meal 3")
        val mealId1 = mealDao.insertMeal(meal1)
        val mealId2 = mealDao.insertMeal(meal2)
        val mealId3 = mealDao.insertMeal(meal3)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlans = listOf(
            createTestMealPlan(mealId1, date, "BREAKFAST"),
            createTestMealPlan(mealId2, date, "LUNCH"),
            createTestMealPlan(mealId3, date, "DINNER")
        )

        // When
        val ids = mealPlanDao.insertMealPlans(mealPlans)
        val retrievedPlans = mealPlanDao.getMealPlansForDate(date.toEpochDay()).first()

        // Then
        assertEquals(3, ids.size)
        assertEquals(3, retrievedPlans.size)
        
        // Verify all meal types are present
        val mealTypes = retrievedPlans.map { it.mealType }.toSet()
        assertTrue(mealTypes.contains("BREAKFAST"))
        assertTrue(mealTypes.contains("LUNCH"))
        assertTrue(mealTypes.contains("DINNER"))
    }

    @Test
    fun getMealPlansForWeek_ordersResultsByDateAndMealType() = runTest {
        // Given
        val meal = createTestMeal(name = "Test Meal")
        val mealId = mealDao.insertMeal(meal)
        
        val monday = LocalDate.of(2024, 1, 15)
        val tuesday = LocalDate.of(2024, 1, 16)
        
        // Insert in random order
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, tuesday, "LUNCH"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, monday, "DINNER"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, tuesday, "BREAKFAST"))
        mealPlanDao.insertMealPlan(createTestMealPlan(mealId, monday, "BREAKFAST"))

        // When
        val sunday = LocalDate.of(2024, 1, 21)
        val plans = mealPlanDao.getMealPlansForWeek(
            monday.toEpochDay(),
            sunday.toEpochDay()
        ).first()

        // Then - Should be ordered by date first, then meal type
        assertEquals(4, plans.size)
        assertEquals(monday.toEpochDay(), plans[0].date)
        assertEquals("BREAKFAST", plans[0].mealType)
        assertEquals(monday.toEpochDay(), plans[1].date)
        assertEquals("DINNER", plans[1].mealType)
        assertEquals(tuesday.toEpochDay(), plans[2].date)
        assertEquals("BREAKFAST", plans[2].mealType)
        assertEquals(tuesday.toEpochDay(), plans[3].date)
        assertEquals("LUNCH", plans[3].mealType)
    }

    /**
     * Helper function to create test meal entities.
     */
    private fun createTestMeal(
        name: String,
        ingredients: List<IngredientEntity> = listOf(
            IngredientEntity("Test Ingredient", "1", "unit")
        ),
        notes: String = ""
    ): MealEntity {
        val currentTime = System.currentTimeMillis()
        return MealEntity(
            id = 0,
            name = name,
            ingredients = ingredients,
            notes = notes,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Helper function to create test meal plan entities.
     */
    private fun createTestMealPlan(
        mealId: Long,
        date: LocalDate,
        mealType: String
    ): MealPlanEntity {
        return MealPlanEntity(
            id = 0,
            mealId = mealId,
            date = date.toEpochDay(),
            mealType = mealType,
            createdAt = System.currentTimeMillis()
        )
    }
}
