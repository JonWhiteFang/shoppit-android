package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * Primary use case for generating meal suggestions.
 * Orchestrates the suggestion generation process by:
 * 1. Fetching all meals and meal plans
 * 2. Filtering meals based on context (tags, search)
 * 3. Calculating scores for each meal
 * 4. Ranking and limiting results
 *
 * Requirements: 1.1, 1.2, 1.3, 2.1-2.5, 3.1-3.5, 4.1-4.5, 5.1-5.5, 6.1-6.5
 */
class GetMealSuggestionsUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val calculateScoreUseCase: CalculateSuggestionScoreUseCase,
    private val getHistoryUseCase: GetMealPlanHistoryUseCase,
    private val filterByTagsUseCase: FilterMealsByTagsUseCase
) {
    /**
     * Generates meal suggestions based on the provided context.
     * Returns a Flow that emits updated suggestions whenever meals or plans change.
     *
     * @param context The suggestion context with filters and target information
     * @return Flow emitting Result with list of meal suggestions or error
     */
    operator fun invoke(context: SuggestionContext): Flow<Result<List<MealSuggestion>>> {
        // Get the start and end of the week for the target date
        val weekStart = context.targetDate.with(DayOfWeek.MONDAY)
        val weekEnd = weekStart.plusDays(6)
        
        // Combine meals and meal plans flows
        return combine(
            mealRepository.getMeals(),
            mealPlanRepository.getMealPlansForWeek(weekStart, weekEnd)
        ) { mealsResult, plansResult ->
            // Handle repository errors
            if (mealsResult.isFailure) {
                return@combine Result.failure<List<MealSuggestion>>(
                    mealsResult.exceptionOrNull() ?: Exception("Failed to fetch meals")
                )
            }
            
            if (plansResult.isFailure) {
                return@combine Result.failure<List<MealSuggestion>>(
                    plansResult.exceptionOrNull() ?: Exception("Failed to fetch meal plans")
                )
            }
            
            val meals = mealsResult.getOrNull() ?: emptyList()
            val plans = plansResult.getOrNull() ?: emptyList()
            
            // Get IDs of meals already planned this week
            val plannedMealIds = plans.map { it.mealId }.toSet()
            
            // Apply filters
            var filteredMeals = meals
            
            // Filter by tags if any are selected
            if (context.selectedTags.isNotEmpty()) {
                filteredMeals = filterByTagsUseCase(filteredMeals, context.selectedTags)
            }
            
            // Filter by search query if provided
            if (context.searchQuery.isNotBlank()) {
                filteredMeals = filteredMeals.filter { meal ->
                    meal.name.contains(context.searchQuery, ignoreCase = true)
                }
            }
            
            // Exclude meals already planned this week (unless explicitly included in context)
            filteredMeals = filteredMeals.filter { meal ->
                !plannedMealIds.contains(meal.id) || context.existingPlanIds.contains(meal.id)
            }
            
            // Get meal plan history for scoring
            val historyResult = getHistoryUseCase.invoke()
            if (historyResult.isFailure) {
                return@combine Result.failure<List<MealSuggestion>>(
                    historyResult.exceptionOrNull() ?: Exception("Failed to fetch meal history")
                )
            }
            
            val historyMap = historyResult.getOrNull() ?: emptyMap()
            
            // Calculate scores and create suggestions
            val suggestions = filteredMeals.map { meal ->
                // Get history for this meal (or create empty history if never planned)
                val history = historyMap[meal.id] ?: MealPlanHistory(
                    mealId = meal.id,
                    lastPlannedDate = null,
                    planCount = 0,
                    planDates = emptyList()
                )
                
                // Calculate score
                val scoreBreakdown = calculateScoreUseCase(meal, context, history)
                
                // Generate reasons for the suggestion
                val reasons = generateReasons(meal, context, history, scoreBreakdown.totalScore)
                
                MealSuggestion(
                    meal = meal,
                    score = scoreBreakdown.totalScore,
                    reasons = reasons,
                    lastPlannedDate = history.lastPlannedDate,
                    planCount = history.planCount
                )
            }
            
            // Sort by score descending, then alphabetically by name
            val sortedSuggestions = suggestions
                .sortedWith(
                    compareByDescending<MealSuggestion> { it.score }
                        .thenBy { it.meal.name }
                )
                .take(MAX_SUGGESTIONS) // Limit to top 10
            
            Result.success(sortedSuggestions)
        }.catch { e ->
            // Catch any unexpected errors
            emit(Result.failure(e))
        }
    }
    
    /**
     * Generates human-readable reasons for why a meal was suggested.
     *
     * @param meal The suggested meal
     * @param context The suggestion context
     * @param history The meal's planning history
     * @param score The calculated score
     * @return List of reason strings
     */
    private fun generateReasons(
        meal: Meal,
        context: SuggestionContext,
        history: MealPlanHistory,
        score: Double
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        // Check for meal type match
        val matchingTag = when (context.targetMealType) {
            com.shoppit.app.domain.model.MealType.BREAKFAST -> 
                com.shoppit.app.domain.model.MealTag.BREAKFAST
            com.shoppit.app.domain.model.MealType.LUNCH -> 
                com.shoppit.app.domain.model.MealTag.LUNCH
            com.shoppit.app.domain.model.MealType.DINNER -> 
                com.shoppit.app.domain.model.MealTag.DINNER
            com.shoppit.app.domain.model.MealType.SNACK -> 
                com.shoppit.app.domain.model.MealTag.SNACK
        }
        
        if (meal.tags.contains(matchingTag)) {
            reasons.add("Perfect for ${context.targetMealType.name.lowercase()}")
        }
        
        // Check for variety (not planned recently)
        if (history.planCount == 0) {
            reasons.add("Haven't tried this in a while")
        } else if (history.lastPlannedDate != null) {
            val daysSince = java.time.temporal.ChronoUnit.DAYS.between(
                history.lastPlannedDate,
                context.targetDate
            )
            if (daysSince >= 30) {
                reasons.add("Haven't had this in over a month")
            } else if (daysSince >= 14) {
                reasons.add("Good variety choice")
            }
        }
        
        // Check for matching selected tags
        if (context.selectedTags.isNotEmpty()) {
            val matchingTags = meal.tags.intersect(context.selectedTags)
            if (matchingTags.isNotEmpty()) {
                reasons.add("Matches your filters")
            }
        }
        
        // High score indicator
        if (score >= 150) {
            reasons.add("Highly recommended")
        }
        
        return reasons
    }
    
    companion object {
        private const val MAX_SUGGESTIONS = 10
    }
}
