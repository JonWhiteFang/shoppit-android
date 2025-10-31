package com.shoppit.app.presentation.ui.planner

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests for SuggestionCard component.
 * 
 * Tests cover:
 * - Meal information display
 * - Click handlers
 * - High score indicator
 * - Accessibility support
 * 
 * Requirements: 4.4, 6.4, 10.1-10.5
 */
class SuggestionCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun suggestionCard_displaysMealName() {
        // Given
        val suggestion = createTestSuggestion(
            mealName = "Spaghetti Carbonara"
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Spaghetti Carbonara")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionCard_displaysMealTypeTags() {
        // Given
        val suggestion = createTestSuggestion(
            tags = setOf(MealTag.DINNER, MealTag.QUICK)
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(MealTag.DINNER.displayName)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(MealTag.QUICK.displayName)
            .assertIsDisplayed()
    }

    @Test
    fun suggestionCard_displaysIngredientCount() {
        // Given
        val suggestion = createTestSuggestion(
            ingredientCount = 5
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("5 ingredients")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionCard_displaysLastPlannedDate() {
        // Given
        val lastPlanned = LocalDate.now().minusDays(14)
        val suggestion = createTestSuggestion(
            lastPlannedDate = lastPlanned
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - verify date is displayed (format: "Last: MMM d")
        composeTestRule
            .onNodeWithText("Last: ${lastPlanned.format(java.time.format.DateTimeFormatter.ofPattern("MMM d"))}")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionCard_doesNotDisplayLastPlannedDateWhenNull() {
        // Given
        val suggestion = createTestSuggestion(
            lastPlannedDate = null
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - verify "Last:" text is not displayed
        composeTestRule
            .onNodeWithText("Last:", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun suggestionCard_showsHighScoreIndicator() {
        // Given - score > 150
        val suggestion = createTestSuggestion(
            score = 180.0
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - verify star icon is displayed
        composeTestRule
            .onNodeWithContentDescription("Highly recommended")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionCard_doesNotShowHighScoreIndicatorForLowScore() {
        // Given - score <= 150
        val suggestion = createTestSuggestion(
            score = 120.0
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - verify star icon is not displayed
        composeTestRule
            .onNodeWithContentDescription("Highly recommended")
            .assertDoesNotExist()
    }

    @Test
    fun suggestionCard_clickHandlerWorks() {
        // Given
        var clicked = false
        val suggestion = createTestSuggestion()

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = { clicked = true },
                    onViewDetails = {}
                )
            }
        }

        // When - click the card
        composeTestRule
            .onNodeWithText("Test Meal")
            .performClick()

        // Then
        assertTrue(clicked)
    }

    @Test
    fun suggestionCard_viewDetailsButtonWorks() {
        // Given
        var viewDetailsClicked = false
        val suggestion = createTestSuggestion()

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = { viewDetailsClicked = true }
                )
            }
        }

        // When - click View Details button
        composeTestRule
            .onNodeWithText("View Details")
            .performClick()

        // Then
        assertTrue(viewDetailsClicked)
    }

    @Test
    fun suggestionCard_hasAccessibilityContentDescription() {
        // Given
        val suggestion = createTestSuggestion(
            mealName = "Caesar Salad",
            ingredientCount = 3,
            score = 180.0
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - verify card has content description
        composeTestRule
            .onNodeWithContentDescription("Meal suggestion: Caesar Salad", substring = true)
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun suggestionCard_viewDetailsButtonHasAccessibility() {
        // Given
        val suggestion = createTestSuggestion(mealName = "Pasta")

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("View details for Pasta")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun suggestionCard_displaysWithoutTags() {
        // Given
        val suggestion = createTestSuggestion(
            tags = emptySet()
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionCard(
                    suggestion = suggestion,
                    onClick = {},
                    onViewDetails = {}
                )
            }
        }

        // Then - card should still display without errors
        composeTestRule
            .onNodeWithText("Test Meal")
            .assertIsDisplayed()
    }

    private fun createTestSuggestion(
        mealName: String = "Test Meal",
        ingredientCount: Int = 3,
        tags: Set<MealTag> = setOf(MealTag.LUNCH),
        score: Double = 120.0,
        lastPlannedDate: LocalDate? = null
    ): MealSuggestion {
        val ingredients = List(ingredientCount) { index ->
            Ingredient("Ingredient $index", "1", "unit")
        }
        
        return MealSuggestion(
            meal = Meal(
                id = 1,
                name = mealName,
                ingredients = ingredients,
                tags = tags
            ),
            score = score,
            reasons = listOf("Test reason"),
            lastPlannedDate = lastPlannedDate,
            planCount = 0
        )
    }
}
