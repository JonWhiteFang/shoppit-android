package com.shoppit.app.presentation.ui.meal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for MealSearchBar component.
 * 
 * Tests cover:
 * - Text input updates query
 * - Clear button appears when query is not empty
 * - Clear button clears query
 * 
 * Requirements: 1.1, 1.2, 4.2
 */
class MealSearchBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchBar_textInputUpdatesQuery() {
        // Given
        var currentQuery = ""
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Search meals or ingredients")
            .performTextInput("pasta")

        // Then
        assertEquals("pasta", currentQuery)
    }

    @Test
    fun searchBar_clearButtonAppearsWhenQueryNotEmpty() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = "",
                    onQueryChange = {}
                )
            }
        }

        // Then - clear button should not be displayed when query is empty
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertDoesNotExist()

        // When - set query to non-empty
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = "pasta",
                    onQueryChange = {}
                )
            }
        }

        // Then - clear button should be displayed
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_clearButtonClearsQuery() {
        // Given
        var currentQuery = "pasta"
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = currentQuery,
                    onQueryChange = { currentQuery = it }
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .performClick()

        // Then
        assertEquals("", currentQuery)
    }

    @Test
    fun searchBar_displaysPlaceholderText() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = "",
                    onQueryChange = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Search meals or ingredients...")
            .assertIsDisplayed()
    }

    @Test
    fun searchBar_displaysSearchIcon() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSearchBar(
                    query = "",
                    onQueryChange = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Search icon")
            .assertIsDisplayed()
    }
}
