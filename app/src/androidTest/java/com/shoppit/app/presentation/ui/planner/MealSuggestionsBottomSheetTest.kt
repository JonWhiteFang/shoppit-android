package com.shoppit.app.presentation.ui.planner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * UI tests for MealSuggestionsBottomSheet component.
 * 
 * Tests cover:
 * - Suggestion display
 * - Search functionality
 * - Filter interaction
 * - Empty states
 * - Error states
 * - Accessibility
 * 
 * Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 8.1-8.5
 */
class MealSuggestionsBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomSheet_displaysLoadingState() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = SuggestionUiState.Loading,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Finding suggestions...")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Loading meal suggestions")
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_displaysSuccessStateWithSuggestions() {
        // Given
        val suggestions = listOf(
            createTestSuggestion(mealName = "Pasta"),
            createTestSuggestion(mealName = "Salad")
        )
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        // When
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = listOf(MealTag.LUNCH),
                    mealCountByTag = mapOf(MealTag.LUNCH to 2),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Pasta")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Salad")
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_displaysEmptyStateNoMeals() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = SuggestionUiState.Empty(EmptyReason.NO_MEALS),
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("No meals in your library yet", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Add Meal")
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_displaysEmptyStateNoMatches() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = SuggestionUiState.Empty(EmptyReason.NO_MATCHES),
                    searchQuery = "test",
                    selectedTags = setOf(MealTag.VEGETARIAN),
                    availableTags = listOf(MealTag.VEGETARIAN),
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 0),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("No meals match your current filters", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_displaysEmptyStateAllPlanned() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = SuggestionUiState.Empty(EmptyReason.ALL_PLANNED),
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("All your meals are already planned", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_displaysErrorState() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = SuggestionUiState.Error("Failed to load suggestions"),
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Failed to load suggestions")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Retry")
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_searchBarWorks() {
        // Given
        var searchQuery = ""
        val suggestions = listOf(createTestSuggestion(mealName = "Pasta"))
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = { searchQuery = it },
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // When - type in search bar
        composeTestRule
            .onNodeWithContentDescription("Search for meals by name")
            .performClick()
        composeTestRule
            .onNodeWithContentDescription("Search for meals by name")
            .performTextInput("Pasta")

        // Then
        assertTrue(searchQuery.contains("Pasta"))
    }

    @Test
    fun bottomSheet_clearSearchButtonWorks() {
        // Given
        var searchQuery = "Pasta"
        val suggestions = listOf(createTestSuggestion(mealName = "Pasta"))
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = { searchQuery = it },
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // When - click clear button
        composeTestRule
            .onNodeWithContentDescription("Clear search")
            .performClick()

        // Then
        assertTrue(searchQuery.isEmpty())
    }

    @Test
    fun bottomSheet_filterChipsDisplayed() {
        // Given
        val suggestions = listOf(createTestSuggestion())
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = listOf(MealTag.LUNCH, MealTag.VEGETARIAN),
                    mealCountByTag = mapOf(
                        MealTag.LUNCH to 5,
                        MealTag.VEGETARIAN to 3
                    ),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("${MealTag.LUNCH.displayName} (5)")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("${MealTag.VEGETARIAN.displayName} (3)")
            .assertIsDisplayed()
    }

    @Test
    fun bottomSheet_filterToggleWorks() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        val suggestions = listOf(createTestSuggestion())
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = selectedTags,
                    availableTags = listOf(MealTag.VEGETARIAN),
                    mealCountByTag = mapOf(MealTag.VEGETARIAN to 3),
                    onSearchQueryChange = {},
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    },
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // When - click filter chip
        composeTestRule
            .onNodeWithText("${MealTag.VEGETARIAN.displayName} (3)")
            .performClick()

        // Then
        assertTrue(selectedTags.contains(MealTag.VEGETARIAN))
    }

    @Test
    fun bottomSheet_mealSelectionWorks() {
        // Given
        var selectedMeal: Meal? = null
        val meal = Meal(id = 1, name = "Pasta", ingredients = listOf(Ingredient("Pasta", "1", "kg")))
        val suggestions = listOf(
            MealSuggestion(
                meal = meal,
                score = 150.0,
                reasons = listOf("Great choice"),
                lastPlannedDate = null,
                planCount = 0
            )
        )
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = { selectedMeal = it },
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // When - click on meal card
        composeTestRule
            .onNodeWithText("Pasta")
            .performClick()

        // Then
        assertTrue(selectedMeal != null)
        assertTrue(selectedMeal?.name == "Pasta")
    }

    @Test
    fun bottomSheet_browseAllButtonWorks() {
        // Given
        var browseAllClicked = false
        val suggestions = listOf(createTestSuggestion())
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = { browseAllClicked = true },
                    onDismiss = {}
                )
            }
        }

        // When - click Browse All button
        composeTestRule
            .onNodeWithText("Browse All Meals")
            .performClick()

        // Then
        assertTrue(browseAllClicked)
    }

    @Test
    fun bottomSheet_hasAccessibilitySupport() {
        // Given
        val suggestions = listOf(createTestSuggestion(mealName = "Pasta"))
        val uiState = SuggestionUiState.Success(
            suggestions = suggestions,
            context = SuggestionContext(
                targetDate = LocalDate.now(),
                targetMealType = MealType.LUNCH
            )
        )

        composeTestRule.setContent {
            ShoppitTheme {
                MealSuggestionsBottomSheet(
                    uiState = uiState,
                    searchQuery = "",
                    selectedTags = emptySet(),
                    availableTags = emptyList(),
                    mealCountByTag = emptyMap(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onMealSelected = {},
                    onViewDetails = {},
                    onBrowseAll = {},
                    onDismiss = {}
                )
            }
        }

        // Then - verify accessibility content descriptions
        composeTestRule
            .onNodeWithContentDescription("Search for meals by name")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Browse all meals in library")
            .assertIsDisplayed()
    }

    private fun createTestSuggestion(
        mealName: String = "Test Meal"
    ): MealSuggestion {
        return MealSuggestion(
            meal = Meal(
                id = 1,
                name = mealName,
                ingredients = listOf(Ingredient("Ingredient", "1", "unit")),
                tags = setOf(MealTag.LUNCH)
            ),
            score = 120.0,
            reasons = listOf("Test reason"),
            lastPlannedDate = null,
            planCount = 0
        )
    }
}
