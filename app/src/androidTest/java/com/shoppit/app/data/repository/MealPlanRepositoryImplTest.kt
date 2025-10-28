package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.entity.IngredientEntity
import com.shoppit.app.data.local.entity.MealEntity
import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.util.DatabaseTest
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import javax.inject.Inject

/**
 * Instrumented tests for MealPlanRepositoryImpl.
 * Tests repository operations with real database to verify entity/model mapping
 * and error handling.
 */
@HiltAndroidTest
class MealPlanRepositoryImplTest : DatabaseTest() {

    @Inject
    lateinit var repository: MealPlanRepositoryImpl

    override fun setUp() {
        super.setUp()
        hiltRule.inject()
    }

    @Test
    fun getMealPlansForWeek_mapsEntitiesToDomainModels() = runTest {
        // Given - Create meals and meal plans
        val meal1 = createTestMealEntity(name = "Breakfast Meal")
        val meal2 = createTestMealEntity(name = "Lunch Meal")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        
        val monday = LocalDate.of(2024, 1, 15)
        val wednesday = LocalDate.of(2024, 1, 17)
        val friday = LocalDate.of(2024, 1, 19)
        val sunday = LocalDate.of(2024, 1, 21)
        
        // Add meal plans using repository
        repository.addMealPlan(createTestMealPlan(mealId1, monday, MealType.BREAKFAST))
        repository.addMealPlan(createTestMealPlan(mealId2, wednesday, MealType.LUNCH))
        repository.addMealPlan(createTestMealPlan(mealId1, friday, MealType.DINNER))

        // When
        val result = repository.getMealPlansForWeek(monday, sunday).first()

        // Then
        assertTrue(result.isSuccess)
        val plans = result.getOrNull()!!
        assertEquals(3, plans.size)
        
        // Verify mapping to domain models
        assertEquals(mealId1, plans[0].mealId)
        assertEquals(monday, plans[0].date)
        assertEquals(MealType.BREAKFAST, plans[0].mealType)
        
        assertEquals(mealId2, plans[1].mealId)
        assertEquals(wednesday, plans[1].date)
        assertEquals(MealType.LUNCH, plans[1].mealType)
        
        assertEquals(mealId1, plans[2].mealId)
        assertEquals(friday, plans[2].date)
        assertEquals(MealType.DINNER, plans[2].mealType)
    }

    @Test
    fun getMealPlansForDate_mapsEntitiesToDomainModels() = runTest {
        // Given
        val meal1 = createTestMealEntity(name = "Breakfast")
        val meal2 = createTestMealEntity(name = "Lunch")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        
        val date = LocalDate.of(2024, 1, 15)
        
        repository.addMealPlan(createTestMealPlan(mealId1, date, MealType.BREAKFAST))
        repository.addMealPlan(createTestMealPlan(mealId2, date, MealType.LUNCH))

        // When
        val result = repository.getMealPlansForDate(date).first()

        // Then
        assertTrue(result.isSuccess)
        val plans = result.getOrNull()!!
        assertEquals(2, plans.size)
        assertEquals(MealType.BREAKFAST, plans[0].mealType)
        assertEquals(MealType.LUNCH, plans[1].mealType)
    }

    @Test
    fun getMealPlanById_mapsEntityToDomainModel() = runTest {
        // Given
        val meal = createTestMealEntity(name = "Test Meal")
        val mealId = database.mealDao().insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val addResult = repository.addMealPlan(
            createTestMealPlan(mealId, date, MealType.DINNER)
        )
        val planId = addResult.getOrNull()!!

        // When
        val result = repository.getMealPlanById(planId).first()

        // Then
        assertTrue(result.isSuccess)
        val plan = result.getOrNull()!!
        assertEquals(planId, plan.id)
        assertEquals(mealId, plan.mealId)
        assertEquals(date, plan.date)
        assertEquals(MealType.DINNER, plan.mealType)
    }

    @Test
    fun addMealPlan_persistsToDatabase() = runTest {
        // Given
        val meal = createTestMealEntity(name = "Pasta")
        val mealId = database.mealDao().insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlan = createTestMealPlan(mealId, date, MealType.LUNCH)

        // When
        val result = repository.addMealPlan(mealPlan)

        // Then
        assertTrue(result.isSuccess)
        val planId = result.getOrNull()!!
        assertTrue(planId > 0)
        
        // Verify persistence
        val retrievedResult = repository.getMealPlanById(planId).first()
        assertTrue(retrievedResult.isSuccess)
        val retrievedPlan = retrievedResult.getOrNull()!!
        assertEquals(mealId, retrievedPlan.mealId)
        assertEquals(date, retrievedPlan.date)
        assertEquals(MealType.LUNCH, retrievedPlan.mealType)
    }

    @Test
    fun addMealPlan_withDuplicateSlot_replacesExisting() = runTest {
        // Given
        val meal1 = createTestMealEntity(name = "First Meal")
        val meal2 = createTestMealEntity(name = "Second Meal")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealType = MealType.BREAKFAST
        
        // Add first plan
        repository.addMealPlan(createTestMealPlan(mealId1, date, mealType))

        // When - Add second plan for same slot
        val result = repository.addMealPlan(createTestMealPlan(mealId2, date, mealType))

        // Then - Should succeed (REPLACE strategy)
        assertTrue(result.isSuccess)
        
        // Verify only one plan exists for the slot
        val plansResult = repository.getMealPlansForDate(date).first()
        val plans = plansResult.getOrNull()!!
        assertEquals(1, plans.size)
        assertEquals(mealId2, plans[0].mealId) // Should be the second meal
    }

    @Test
    fun updateMealPlan_modifiesExistingPlan() = runTest {
        // Given
        val meal1 = createTestMealEntity(name = "Original Meal")
        val meal2 = createTestMealEntity(name = "New Meal")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        
        val date = LocalDate.of(2024, 1, 15)
        val addResult = repository.addMealPlan(
            createTestMealPlan(mealId1, date, MealType.BREAKFAST)
        )
        val planId = addResult.getOrNull()!!
        
        // Create updated plan
        val updatedPlan = MealPlan(
            id = planId,
            mealId = mealId2,
            date = date,
            mealType = MealType.BREAKFAST
        )

        // When
        val updateResult = repository.updateMealPlan(updatedPlan)

        // Then
        assertTrue(updateResult.isSuccess)
        
        // Verify update
        val retrievedResult = repository.getMealPlanById(planId).first()
        val retrievedPlan = retrievedResult.getOrNull()!!
        assertEquals(mealId2, retrievedPlan.mealId)
    }

    @Test
    fun deleteMealPlan_removesFromDatabase() = runTest {
        // Given
        val meal = createTestMealEntity(name = "Test Meal")
        val mealId = database.mealDao().insertMeal(meal)
        
        val date = LocalDate.of(2024, 1, 15)
        val addResult = repository.addMealPlan(
            createTestMealPlan(mealId, date, MealType.DINNER)
        )
        val planId = addResult.getOrNull()!!
        
        // Verify plan exists
        var retrievedResult = repository.getMealPlanById(planId).first()
        assertTrue(retrievedResult.isSuccess)

        // When
        val deleteResult = repository.deleteMealPlan(planId)

        // Then
        assertTrue(deleteResult.isSuccess)
        
        // Verify deletion
        retrievedResult = repository.getMealPlanById(planId).first()
        assertTrue(retrievedResult.isFailure)
        assertTrue(retrievedResult.exceptionOrNull() is PersistenceError.QueryFailed)
    }

    @Test
    fun deleteMealPlansForDate_removesAllPlans() = runTest {
        // Given
        val meal1 = createTestMealEntity(name = "Breakfast")
        val meal2 = createTestMealEntity(name = "Lunch")
        val meal3 = createTestMealEntity(name = "Dinner")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        val mealId3 = database.mealDao().insertMeal(meal3)
        
        val targetDate = LocalDate.of(2024, 1, 15)
        val otherDate = LocalDate.of(2024, 1, 16)
        
        // Add multiple plans for target date
        repository.addMealPlan(createTestMealPlan(mealId1, targetDate, MealType.BREAKFAST))
        repository.addMealPlan(createTestMealPlan(mealId2, targetDate, MealType.LUNCH))
        repository.addMealPlan(createTestMealPlan(mealId3, targetDate, MealType.DINNER))
        
        // Add plan for different date
        repository.addMealPlan(createTestMealPlan(mealId1, otherDate, MealType.BREAKFAST))

        // When
        val deleteResult = repository.deleteMealPlansForDate(targetDate)

        // Then
        assertTrue(deleteResult.isSuccess)
        
        // Verify target date plans are deleted
        val targetDateResult = repository.getMealPlansForDate(targetDate).first()
        val targetDatePlans = targetDateResult.getOrNull()!!
        assertTrue(targetDatePlans.isEmpty())
        
        // Verify other date plans remain
        val otherDateResult = repository.getMealPlansForDate(otherDate).first()
        val otherDatePlans = otherDateResult.getOrNull()!!
        assertEquals(1, otherDatePlans.size)
    }

    @Test
    fun addMealPlans_batchInsertPersistsAllPlans() = runTest {
        // Given
        val meal1 = createTestMealEntity(name = "Meal 1")
        val meal2 = createTestMealEntity(name = "Meal 2")
        val meal3 = createTestMealEntity(name = "Meal 3")
        val mealId1 = database.mealDao().insertMeal(meal1)
        val mealId2 = database.mealDao().insertMeal(meal2)
        val mealId3 = database.mealDao().insertMeal(meal3)
        
        val date = LocalDate.of(2024, 1, 15)
        val mealPlans = listOf(
            createTestMealPlan(mealId1, date, MealType.BREAKFAST),
            createTestMealPlan(mealId2, date, MealType.LUNCH),
            createTestMealPlan(mealId3, date, MealType.DINNER)
        )

        // When
        val result = repository.addMealPlans(mealPlans)

        // Then
        assertTrue(result.isSuccess)
        val ids = result.getOrNull()!!
        assertEquals(3, ids.size)
        
        // Verify all plans were persisted
        val retrievedResult = repository.getMealPlansForDate(date).first()
        val retrievedPlans = retrievedResult.getOrNull()!!
        assertEquals(3, retrievedPlans.size)
    }

    @Test
    fun getMealPlansForWeek_withEmptyDatabase_returnsEmptyList() = runTest {
        // When
        val monday = LocalDate.of(2024, 1, 15)
        val sunday = LocalDate.of(2024, 1, 21)
        val result = repository.getMealPlansForWeek(monday, sunday).first()

        // Then
        assertTrue(result.isSuccess)
        val plans = result.getOrNull()!!
        assertTrue(plans.isEmpty())
    }

    @Test
    fun getMealPlansForDate_withEmptyDatabase_returnsEmptyList() = runTest {
        // When
        val date = LocalDate.of(2024, 1, 15)
        val result = repository.getMealPlansForDate(date).first()

        // Then
        assertTrue(result.isSuccess)
        val plans = result.getOrNull()!!
        assertTrue(plans.isEmpty())
    }

    @Test
    fun getMealPlanById_withNonExistentId_returnsFailure() = runTest {
        // When
        val result = repository.getMealPlanById(999L).first()

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PersistenceError.QueryFailed)
    }

    @Test
    fun foreignKeyCascade_deletesMealPlansWhenMealDeleted() = runTest {
        // Given
        val meal = createTestMealEntity(name = "Test Meal")
        val mealId = database.mealDao().insertMeal(meal)
        
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 1, 16)
        
        // Create multiple meal plans for the same meal
        repository.addMealPlan(createTestMealPlan(mealId, date1, MealType.BREAKFAST))
        repository.addMealPlan(createTestMealPlan(mealId, date2, MealType.LUNCH))
        
        // Verify plans exist
        var plans1 = repository.getMealPlansForDate(date1).first().getOrNull()!!
        var plans2 = repository.getMealPlansForDate(date2).first().getOrNull()!!
        assertEquals(1, plans1.size)
        assertEquals(1, plans2.size)

        // When - Delete the meal
        database.mealDao().deleteMealById(mealId)

        // Then - All meal plans should be cascade deleted
        plans1 = repository.getMealPlansForDate(date1).first().getOrNull()!!
        plans2 = repository.getMealPlansForDate(date2).first().getOrNull()!!
        assertTrue(plans1.isEmpty())
        assertTrue(plans2.isEmpty())
    }

    /**
     * Helper function to create test meal entities.
     */
    private fun createTestMealEntity(
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
     * Helper function to create test domain meal plans.
     */
    private fun createTestMealPlan(
        mealId: Long,
        date: LocalDate,
        mealType: MealType
    ): MealPlan {
        return MealPlan(
            id = 0,
            mealId = mealId,
            date = date,
            mealType = mealType,
            createdAt = System.currentTimeMillis()
        )
    }
}
