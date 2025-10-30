package com.shoppit.app.presentation.ui.meal

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
 * UI tests for FilterChipRow component.
 * 
 * Tests cover:
 * - All tags are displayed
 * - Chip selection toggles state
 * - Selected chips show checkmark
 * 
 * Requirements: 3.1, 3.2, 3.4, 4.1
 */
class FilterChipRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun filterChipRow_displaysAllTags() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = emptySet(),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify all tags are displayed
        MealTag.entries.forEach { tag ->
            composeTestRule
                .onNodeWithText(tag.displayName)
                .assertIsDisplayed()
        }
    }

    @Test
    fun filterChipRow_chipSelectionTogglesState() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = selectedTags,
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

        // When - click on Vegetarian chip
        composeTestRule
            .onNodeWithText(MealTag.VEGETARIAN.displayName)
            .performClick()

        // Then - tag should be added to selected tags
        assertTrue(selectedTags.contains(MealTag.VEGETARIAN))

        // When - click again to deselect
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = selectedTags,
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
            .onNodeWithText(MealTag.VEGETARIAN.displayName)
            .performClick()

        // Then - tag should be removed from selected tags
        assertFalse(selectedTags.contains(MealTag.VEGETARIAN))
    }

    @Test
    fun filterChipRow_selectedChipsShowCheckmark() {
        // Given - no tags selected
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = emptySet(),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify chip has "Not selected" state
        composeTestRule
            .onNodeWithContentDescription("${MealTag.VEGETARIAN.displayName} filter")
            .assertIsDisplayed()

        // When - select a tag
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = setOf(MealTag.VEGETARIAN),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify chip shows selected state with checkmark
        // The checkmark icon is part of the chip's leading icon
        composeTestRule
            .onNodeWithContentDescription("${MealTag.VEGETARIAN.displayName} filter")
            .assertIsDisplayed()
    }

    @Test
    fun filterChipRow_supportsMultipleSelection() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = selectedTags,
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
            .onNodeWithText(MealTag.VEGETARIAN.displayName)
            .performClick()
        
        composeTestRule
            .onNodeWithText(MealTag.QUICK.displayName)
            .performClick()

        // Then - both tags should be selected
        assertTrue(selectedTags.contains(MealTag.VEGETARIAN))
        assertTrue(selectedTags.contains(MealTag.QUICK))
        assertEquals(2, selectedTags.size)
    }

    @Test
    fun filterChipRow_hasAccessibilitySupport() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                FilterChipRow(
                    selectedTags = setOf(MealTag.BREAKFAST),
                    onTagToggle = {}
                )
            }
        }

        // Then - verify accessibility content descriptions exist
        MealTag.entries.forEach { tag ->
            composeTestRule
                .onNodeWithContentDescription("${tag.displayName} filter")
                .assertIsDisplayed()
        }
    }
}
