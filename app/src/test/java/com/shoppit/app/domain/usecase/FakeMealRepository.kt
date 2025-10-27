package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of MealRepository for testing use cases.
 * Provides in-memory storage and controllable behavior for tests.
 */
class FakeMealRepository : MealRepository {

    private val meals = MutableStateFlow<List<Meal>>(emptyList())
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
     * Sets the meals in the repository.
     */
    fun setMeals(mealList: List<Meal>) {
        meals.value = mealList
    }

    /**
     * Gets the current meals in the repository.
     */
    fun getMealsList(): List<Meal> = meals.value

    override fun getMeals(): Flow<Result<List<Meal>>> {
        return meals.map { 
            if (shouldFail) {
                Result.failure(failureException)
            } else {
                Result.success(it)
            }
        }
    }

    override fun getMealById(id: Long): Flow<Result<Meal>> {
        return meals.map { mealList ->
            if (shouldFail) {
                Result.failure(failureException)
            } else {
                val meal = mealList.find { it.id == id }
                if (meal != null) {
                    Result.success(meal)
                } else {
                    Result.failure(Exception("Meal not found"))
                }
            }
        }
    }

    override suspend fun addMeal(meal: Meal): Result<Long> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            val id = nextId++
            val newMeal = meal.copy(id = id)
            meals.value = meals.value + newMeal
            Result.success(id)
        }
    }

    override suspend fun updateMeal(meal: Meal): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            val index = meals.value.indexOfFirst { it.id == meal.id }
            if (index != -1) {
                val updatedList = meals.value.toMutableList()
                updatedList[index] = meal
                meals.value = updatedList
                Result.success(Unit)
            } else {
                Result.failure(Exception("Meal not found"))
            }
        }
    }

    override suspend fun deleteMeal(mealId: Long): Result<Unit> {
        return if (shouldFail) {
            Result.failure(failureException)
        } else {
            meals.value = meals.value.filter { it.id != mealId }
            Result.success(Unit)
        }
    }
}
