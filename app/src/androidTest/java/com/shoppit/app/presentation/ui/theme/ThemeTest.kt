package com.shoppit.app.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for Material3 theme configuration.
 * Validates theme setup and composable rendering.
 */
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun theme_appliesCorrectly() {
        // When
        composeTestRule.setContent {
            ShoppitTheme {
                Text("Test Text")
            }
        }

        // Then
        composeTestRule.onNodeWithText("Test Text").assertIsDisplayed()
    }

    @Test
    fun theme_providesColorScheme() {
        // Given
        var primaryColorSet = false

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                primaryColorSet = MaterialTheme.colorScheme.primary != null
            }
        }

        // Then
        assert(primaryColorSet)
    }

    @Test
    fun theme_providesTypography() {
        // Given
        var typographySet = false

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                typographySet = MaterialTheme.typography.bodyLarge != null
            }
        }

        // Then
        assert(typographySet)
    }

    @Test
    fun darkTheme_appliesCorrectly() {
        // When
        composeTestRule.setContent {
            ShoppitTheme(darkTheme = true) {
                Text("Dark Theme Test")
            }
        }

        // Then
        composeTestRule.onNodeWithText("Dark Theme Test").assertIsDisplayed()
    }

    @Test
    fun lightTheme_appliesCorrectly() {
        // When
        composeTestRule.setContent {
            ShoppitTheme(darkTheme = false) {
                Text("Light Theme Test")
            }
        }

        // Then
        composeTestRule.onNodeWithText("Light Theme Test").assertIsDisplayed()
    }
}
