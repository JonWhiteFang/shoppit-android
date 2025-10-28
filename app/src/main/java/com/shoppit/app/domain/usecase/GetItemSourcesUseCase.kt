package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.IngredientSource
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for retrieving the source meals for a shopping list ingredient.
 * Shows which meals use a particular ingredient and in what quantities.
 */
class GetItemSourcesUseCase @Inject constructor(
    private val mealRepository: MealRepository
) {
    /**
     * Gets the list of meals that use a specific ingredient.
     * 
     * @param ingredientName The name of the ingredient
     * @param mealIds The IDs of meals that use this ingredient
     * @return Result with list of IngredientSource objects or error
     */
    suspend operator fun invoke(
        ingredientName: String,
        mealIds: List<Long>
    ): Result<List<IngredientSource>> {
        return try {
            mealRepository.getMeals().first().map { meals ->
                meals.filter { it.id in mealIds }
                    .mapNotNull { meal ->
                        meal.ingredients.find { 
                            it.name.equals(ingredientName, ignoreCase = true) 
                        }?.let { ingredient ->
                            IngredientSource(
                                mealId = meal.id,
                                mealName = meal.name,
                                quantity = ingredient.quantity,
                                unit = ingredient.unit
                            )
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
