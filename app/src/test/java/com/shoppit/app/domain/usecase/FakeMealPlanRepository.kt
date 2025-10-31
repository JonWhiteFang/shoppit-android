package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealPlan
import com.shoppit.app.domain.repository.MealPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Fake implementation of MealPlanRepository for testing use cases.
 * Provides in-memory storage and controllable behavior for tests.
 */
class FakeMealPlanRepository : MealPlanRepository {

    private val mealPlans = MutableStateFlow<List<MealPlan>>(emptyList())
    private var nextId = 1L
    private var shouldFail = false
    private var failureException: Exception = Exception("Repository error")

    /**
     * Sets the repository to fail on the next operation.
     */
    fun setShouldFail(shouldFail: Boolean, exception: Exception = Exception("Repository error")) {
        this.shouldFail = shouldFail
        this.failureException = exception
    }

    /**
     * Sets the meal plans in the repository.
     */
    fun setMealPlans(planList: List<MealPlan>) {
        mealPlans.value = planList
        // Update nextId to be greater than any existing ID
        if (planList.isNotEmpty()) {
            nextId = planList.maxOf { it.id } + 1
        }
    }

    /**
     * Gets the current meal plans in the repository.
     */
    fun getMealPlansList(): List<MealPlan> = mealPlans.value

    override fun getMealPlansForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<MealPlan>>> {
        return mealPlans.map { plans ->
            if (shouldFail) {
                Result.failure(failureException)
            } else {
                val filtered = plans.filter { plan ->
                    !plan.date.isBefore(startDate) && !plan.date.isAfter(endDate)
                }
                Result.success(filtered)
            }
        }
    }

    override fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>> {
        return mealPlans.map { plans ->
            if (shouldFail) {
                Result.failure(failureException)
            } else {
                val filtered = plans.filter { it.date == date }
                Result.success(filtered)
            }
        }
    }

    override fun getMealPlanById(id: Long): Flow<Result<MealPlan>> {
        return mealPlans.map { plans ->
            if (shouldFail) {
                Result.failure(failureException)
            } else {
                val plan = plans.find { it.id == id }
                if (plan != null) {
                    Result.success(plan)
                } else {
                    Result.failure(Exception("Meal plan not found"))
                }
            }
        }
    }

    override suspend fun addMealPlan(mealPlan: MealPlan): Result<Long> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            val id = nextId++
            val newPlan = mealPlan.copy(id = id)
            mealPlans.value = mealPlans.value + newPlan
            Result.success(id)
        }
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            val index = mealPlans.value.indexOfFirst { it.id == mealPlan.id }
            if (index != -1) {
                val updatedList = mealPlans.value.toMutableList()
                updatedList[index] = mealPlan
                mealPlans.value = updatedList
                Result.success(Unit)
            } else {
                Result.failure(Exception("Meal plan not found"))
            }
        }
    }

    override suspend fun deleteMealPlan(mealPlanId: Long): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            mealPlans.value = mealPlans.value.filter { it.id != mealPlanId }
            Result.success(Unit)
        }
    }

    override suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            mealPlans.value = mealPlans.value.filter { it.date != date }
            Result.success(Unit)
        }
    }

    override suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<List<Long>> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            val ids = mutableListOf<Long>()
            val newPlans = mealPlans.map { plan ->
                val id = nextId++
                ids.add(id)
                plan.copy(id = id)
            }
            this.mealPlans.value = this.mealPlans.value + newPlans
            Result.success(ids)
        }
    }
}
