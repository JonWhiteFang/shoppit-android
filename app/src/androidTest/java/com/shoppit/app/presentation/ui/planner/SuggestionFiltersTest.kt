package com.shoppit.app.presentation.ui.planner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for SuggestionFilters component.
 * 
 * Tests cover:
 * - Tag display with meal counts
 * - Multi-select functionality
 * - Visual feedback for selection
 * - Accessibility support
 * 
 * Requirements: 2.1-2.5, 9.1-9.2
 */
class SuggestionFiltersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun suggestionFilters_displaysAllTags() {
        // Given
        val tags = listOf(
            MealTag.BREAKFAST,
            MealTag.LUNCH,
            MealTag.DINNER,
            MealTag.VEGETARIAN
        )
        val mealCountByTag = tags.associateWith { 5 }

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = tags,
                    selectedTags = emptySet(),
                    mealCountByTag = mealCountByTag,
                    onTagToggle = {}
                )
            }
        }

        // Then - verify all tags are displayed
        tags.forEach { tag ->
            composeTestRule
                .onNodeWithText("${tag.displayName} (5)")
                .assertIsDisplayed()
        }
    }

    @Test
    fun suggestionFilters_displaysMealCounts() {
        // Given
        val mealCountByTag = mapOf(
            MealTag.BREAKFAST to 3,
            MealTag.LUNCH to 8,
            MealTag.DINNER to 12
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.BREAKFAST, MealTag.LUNCH, MealTag.DINNER),
                    selectedTags = emptySet(),
                    mealCountByTag = mealCountByTag,
                    onTagToggle = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("${MealTag.BREAKFAST.displayName} (3)")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("${MealTag.LUNCH.displayName} (8)")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("${MealTag.DINNER.displayName} (12)")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionFilters_tagSelectionToggles() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.VEGETARIAN),
                    selectedTags = selectedTags,
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 5),
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    }
                )
            }
        }

        // When - click to select
        composeTestRule
            .onNodeWithText("${MealTag.VEGETARIAN.displayName} (5)")
            .performClick()

        // Then
        assertTrue(selectedTags.contains(MealTag.VEGETARIAN))

        // When - re-render with updated state and click to deselect
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.VEGETARIAN),
                    selectedTags = selectedTags,
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 5),
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    }
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("${MealTag.VEGETARIAN.displayName} (5)")
            .performClick()

        // Then
        assertFalse(selectedTags.contains(MealTag.VEGETARIAN))
    }

    @Test
    fun suggestionFilters_supportsMultipleSelection() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        val tags = listOf(MealTag.VEGETARIAN, MealTag.QUICK, MealTag.HEALTHY)
        val mealCountByTag = tags.associateWith { 5 }
        
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = tags,
                    selectedTags = selectedTags,
                    mealCountByTag = mealCountByTag,
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    }
                )
            }
        }

        // When - select multiple tags
        composeTestRule
            .onNodeWithText("${MealTag.VEGETARIAN.displayName} (5)")
            .performClick()
        
        composeTestRule
            .onNodeWithText("${MealTag.QUICK.displayName} (5)")
            .performClick()

        // Then
        assertTrue(selectedTags.contains(MealTag.VEGETARIAN))
        assertTrue(selectedTags.contains(MealTag.QUICK))
        assertFalse(selectedTags.contains(MealTag.HEALTHY))
        assertEquals(2, selectedTags.size)
    }

    @Test
    fun suggestionFilters_hasAccessibilityContentDescriptions() {
        // Given
        val mealCountByTag = mapOf(
            MealTag.BREAKFAST to 5,
            MealTag.LUNCH to 8
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.BREAKFAST, MealTag.LUNCH),
                    selectedTags = setOf(MealTag.BREAKFAST),
                    mealCountByTag = mealCountByTag,
                    onTagToggle = {}
                )
            }
        }

        // Then - verify accessibility content descriptions
        composeTestRule
            .onNodeWithContentDescription("Filter by ${MealTag.BREAKFAST.displayName}", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Filter by ${MealTag.LUNCH.displayName}", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun suggestionFilters_showsSelectedState() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.VEGETARIAN),
                    selectedTags = setOf(MealTag.VEGETARIAN),
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 5),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify selected state is indicated in accessibility
        composeTestRule
            .onNodeWithContentDescription("Currently selected", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun suggestionFilters_showsNotSelectedState() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.VEGETARIAN),
                    selectedTags = emptySet(),
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 5),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify not selected state is indicated in accessibility
        composeTestRule
            .onNodeWithContentDescription("Not selected", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun suggestionFilters_handlesZeroMealCount() {
        // Given
        val mealCountByTag = mapOf(
            MealTag.BREAKFAST to 0,
            MealTag.LUNCH to 5
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = listOf(MealTag.BREAKFAST, MealTag.LUNCH),
                    selectedTags = emptySet(),
                    mealCountByTag = mealCountByTag,
                    onTagToggle = {}
                )
            }
        }

        // Then - should display 0 count
        composeTestRule
            .onNodeWithText("${MealTag.BREAKFAST.displayName} (0)")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("${MealTag.LUNCH.displayName} (5)")
            .assertIsDisplayed()
    }

    @Test
    fun suggestionFilters_handlesEmptyTagList() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                SuggestionFilters(
                    availableTags = emptyList(),
                    selectedTags = emptySet(),
                    mealCountByTag = emptyMap(),
                    onTagToggle = {}
                )
            }
        }

        // Then - should render without errors (no tags to display)
        // No assertions needed - just verify it doesn't crash
    }
}
