package com.shoppit.app.domain.model

/**
 * Detailed breakdown of a suggestion score calculation.
 * Provides transparency into how the suggestion score was computed.
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
 * @property baseScore Starting score (100)
 * @property mealTypeBonus Bonus for matching meal type (0-100)
 * @property frequencyPenalty Penalty for frequent use (0-50)
 * @property recencyPenalty Penalty for recent use (0-50)
 */
data class SuggestionScore(
    val baseScore: Double = 100.0,
    val mealTypeBonus: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
    val recencyPenalty: Double = 0.0
) {
    /**
     * Calculates the total score by combining all components.
     *
     * @return The final calculated score
     */
    val totalScore: Double
        get() = baseScore + mealTypeBonus - frequencyPenalty - recencyPenalty
}
