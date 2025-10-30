package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealPlanHistory
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.domain.model.SuggestionScore
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Calculates the relevance score for a meal suggestion.
 * Pure function with no side effects for easy testing.
 *
 * Scoring Formula:
 * - Base Score: 100
 * - Meal Type Match: +100 (if meal has matching meal type tag)
 * - Frequency Penalty: -0 to -50 (based on plan count in past 30 days)
 *   - 0 times: 0 penalty
 *   - 1-2 times: -10 penalty
 *   - 3-4 times: -30 penalty
 *   - 5+ times: -50 penalty
 * - Recency Penalty: -50 (if planned within past 7 days)
 *
 * Requirements: 3.1-3.5, 4.1-4.3, 6.1-6.4
 */
class CalculateSuggestionScoreUseCase @Inject constructor() {
    
    /**
     * Calculates the suggestion score for a meal based on context and history.
     *
     * @param meal The meal to score
     * @param context The suggestion context (target date, meal type, etc.)
     * @param history The meal's planning history
     * @return SuggestionScore with detailed breakdown
     */
    operator fun invoke(
        meal: Meal,
        context: SuggestionContext,
        history: MealPlanHistory
    ): SuggestionScore {
        val baseScore = BASE_SCORE
        val mealTypeBonus = calculateMealTypeBonus(meal, context)
        val frequencyPenalty = calculateFrequencyPenalty(history)
        val recencyPenalty = calculateRecencyPenalty(history, context.targetDate)
        
        return SuggestionScore(
            baseScore = baseScore,
            mealTypeBonus = mealTypeBonus,
            frequencyPenalty = frequencyPenalty,
            recencyPenalty = recencyPenalty
        )
    }
    
    /**
     * Calculates the meal type match bonus.
     * Awards +100 points if the meal has a tag matching the target meal type.
     *
     * Requirement 4.2: Assign a 2x score multiplier to meals matching the target meal type tag
     * (Implemented as +100 bonus on base 100 score = 2x multiplier)
     *
     * @param meal The meal to check
     * @param context The suggestion context with target meal type
     * @return Bonus points (0 or 100)
     */
    private fun calculateMealTypeBonus(meal: Meal, context: SuggestionContext): Double {
        val matchingTag = when (context.targetMealType) {
            com.shoppit.app.domain.model.MealType.BREAKFAST -> MealTag.BREAKFAST
            com.shoppit.app.domain.model.MealType.LUNCH -> MealTag.LUNCH
            com.shoppit.app.domain.model.MealType.DINNER -> MealTag.DINNER
            com.shoppit.app.domain.model.MealType.SNACK -> MealTag.SNACK
        }
        
        return if (meal.tags.contains(matchingTag)) {
            MEAL_TYPE_BONUS
        } else {
            0.0
        }
    }
    
    /**
     * Calculates the frequency penalty based on how often the meal was planned.
     * Penalizes meals that have been planned frequently in the past 30 days.
     *
     * Requirements:
     * - 3.2: Assign higher suggestion scores to meals planned less frequently
     * - 3.3: Assign lower suggestion scores to meals planned more than 3 times in past 30 days
     *
     * Penalty tiers:
     * - 0 times: 0 penalty
     * - 1-2 times: -10 penalty
     * - 3-4 times: -30 penalty
     * - 5+ times: -50 penalty
     *
     * @param history The meal's planning history
     * @return Penalty points (0 to 50)
     */
    private fun calculateFrequencyPenalty(history: MealPlanHistory): Double {
        return when (history.planCount) {
            0 -> 0.0
            1, 2 -> 10.0
            3, 4 -> 30.0
            else -> 50.0 // 5 or more times
        }
    }
    
    /**
     * Calculates the recency penalty based on when the meal was last planned.
     * Penalizes meals that were planned recently to encourage variety.
     *
     * Requirements:
     * - 3.4: Consider a meal as "recently used" IF it was planned within the past 7 days
     * - 3.5: Reduce suggestion scores by 50 percent for recently used meals
     *   (Implemented as -50 penalty on base 100 score = 50% reduction)
     *
     * @param history The meal's planning history
     * @param targetDate The date for which suggestions are being generated
     * @return Penalty points (0 or 50)
     */
    private fun calculateRecencyPenalty(history: MealPlanHistory, targetDate: LocalDate): Double {
        val lastPlannedDate = history.lastPlannedDate ?: return 0.0
        
        val daysSinceLastPlanned = ChronoUnit.DAYS.between(lastPlannedDate, targetDate)
        
        return if (daysSinceLastPlanned < RECENCY_THRESHOLD_DAYS) {
            RECENCY_PENALTY
        } else {
            0.0
        }
    }
    
    companion object {
        private const val BASE_SCORE = 100.0
        private const val MEAL_TYPE_BONUS = 100.0
        private const val RECENCY_PENALTY = 50.0
        private const val RECENCY_THRESHOLD_DAYS = 7L
    }
}
