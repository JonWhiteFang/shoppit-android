package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case for generating suggested shopping list items based on meal plans.
 * Analyzes the current week's meal plans and suggests complementary items.
 */
class GetSuggestedItemsUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    /**
     * Get suggested items based on the current week's meal plans.
     * Suggests complementary items that aren't already in the shopping list.
     * @return Result containing list of suggested item names (max 10)
     */
    suspend operator fun invoke(): Result<List<String>> {
        return try {
            val today = LocalDate.now()
            val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
            val endOfWeek = startOfWeek.plusDays(6)
            
            mealPlanRepository.getMealPlansForWeek(startOfWeek, endOfWeek).first().fold(
                onSuccess = { mealPlans ->
                    val suggestions = mutableSetOf<String>()
                    
                    // Get current shopping list items (lowercase for comparison)
                    val currentItems = shoppingListRepository.getShoppingList().first()
                        .getOrNull()?.map { it.name.lowercase() } ?: emptyList()
                    
                    // Analyze meal plans and generate suggestions
                    mealPlans.forEach { plan ->
                        when (plan.mealType) {
                            MealType.BREAKFAST -> {
                                // Breakfast staples
                                if ("eggs" !in currentItems && "egg" !in currentItems) {
                                    suggestions.add("Eggs")
                                }
                                if ("bread" !in currentItems) {
                                    suggestions.add("Bread")
                                }
                                if ("milk" !in currentItems) {
                                    suggestions.add("Milk")
                                }
                                if ("butter" !in currentItems) {
                                    suggestions.add("Butter")
                                }
                            }
                            MealType.LUNCH -> {
                                // Lunch staples
                                if ("lettuce" !in currentItems) {
                                    suggestions.add("Lettuce")
                                }
                                if ("tomato" !in currentItems && "tomatoes" !in currentItems) {
                                    suggestions.add("Tomatoes")
                                }
                            }
                            MealType.DINNER -> {
                                // Dinner staples
                                if ("salt" !in currentItems) {
                                    suggestions.add("Salt")
                                }
                                if ("pepper" !in currentItems) {
                                    suggestions.add("Pepper")
                                }
                                if ("olive oil" !in currentItems && "oil" !in currentItems) {
                                    suggestions.add("Olive Oil")
                                }
                                if ("garlic" !in currentItems) {
                                    suggestions.add("Garlic")
                                }
                                if ("onion" !in currentItems && "onions" !in currentItems) {
                                    suggestions.add("Onions")
                                }
                            }
                            MealType.SNACK -> {
                                // Snack staples
                                if ("fruit" !in currentItems && "fruits" !in currentItems) {
                                    suggestions.add("Fresh Fruit")
                                }
                                if ("nuts" !in currentItems) {
                                    suggestions.add("Nuts")
                                }
                            }
                        }
                    }
                    
                    // Limit to 10 suggestions
                    Result.success(suggestions.take(10).toList())
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
