package com.shoppit.app.presentation.ui.meal

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for MealListScreen integration with search and filter functionality.
 * 
 * Tests cover:
 * - Search filters meal list
 * - Tag selection filters meal list
 * - Combined search and filter
 * - Clear filters button
 * - Empty state for no results
 * 
 * Requirements: 1.1, 1.4, 3.5, 4.3, 4.4
 */
class MealListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testMeals = listOf(
        Meal(
            id = 1,
            name = "Spaghetti Carbonara",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("Eggs", "4", "pcs"),
                Ingredient("Bacon", "200", "g")
            ),
            tags = setOf(MealTag.DINNER, MealTag.COMFORT_FOOD)
        ),
        Meal(
            id = 2,
            name = "Caesar Salad",
            ingredients = listOf(
                Ingredient("Lettuce", "1", "head"),
                Ingredient("Croutons", "1", "cup"),
                Ingredient("Parmesan", "50", "g")
            ),
            tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN)
        ),
        Meal(
            id = 3,
            name = "Vegetable Pasta",
            ingredients = listOf(
                Ingredient("Pasta", "300", "g"),
                Ingredient("Tomatoes", "3", "pcs"),
                Ingredient("Zucchini", "1", "pc")
            ),
            tags = setOf(MealTag.DINNER, MealTag.VEGETARIAN)
        )
    )

    @Test
    fun mealListScreen_searchFiltersMealList() {
        // Given
        var searchQuery = ""
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals,
                        totalCount = testMeals.size,
                        filteredCount = testMeals.size,
                        isFiltered = false
                    ),
                    searchQuery = searchQuery,
                    selectedTags = emptySet(),
                    onSearchQueryChange = { searchQuery = it },
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - all meals should be displayed initially
        composeTestRule.onNodeWithText("Spaghetti Carbonara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Caesar Salad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vegetable Pasta").assertIsDisplayed()

        // When - search for "pasta"
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals.filter { meal ->
                            meal.name.contains("pasta", ignoreCase = true) ||
                            meal.ingredients.any { it.name.contains("pasta", ignoreCase = true) }
                        },
                        totalCount = testMeals.size,
                        filteredCount = 2,
                        isFiltered = true
                    ),
                    searchQuery = "pasta",
                    selectedTags = emptySet(),
                    onSearchQueryChange = { searchQuery = it },
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - only meals with "pasta" should be displayed
        composeTestRule.onNodeWithText("Spaghetti Carbonara").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vegetable Pasta").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 of 3 meals").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_tagSelectionFiltersMealList() {
        // Given
        val selectedTags = mutableSetOf<MealTag>()
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals,
                        totalCount = testMeals.size,
                        filteredCount = testMeals.size,
                        isFiltered = false
                    ),
                    searchQuery = "",
                    selectedTags = selectedTags,
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    },
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // When - select Vegetarian tag
        selectedTags.add(MealTag.VEGETARIAN)
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals.filter { meal ->
                            meal.tags.contains(MealTag.VEGETARIAN)
                        },
                        totalCount = testMeals.size,
                        filteredCount = 2,
                        isFiltered = true
                    ),
                    searchQuery = "",
                    selectedTags = selectedTags,
                    onTagToggle = { tag ->
                        if (selectedTags.contains(tag)) {
                            selectedTags.remove(tag)
                        } else {
                            selectedTags.add(tag)
                        }
                    },
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - only vegetarian meals should be displayed
        composeTestRule.onNodeWithText("Caesar Salad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vegetable Pasta").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 of 3 meals").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_combinedSearchAndFilter() {
        // Given
        val selectedTags = setOf(MealTag.VEGETARIAN)
        val searchQuery = "pasta"
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals.filter { meal ->
                            val matchesSearch = meal.name.contains(searchQuery, ignoreCase = true) ||
                                meal.ingredients.any { it.name.contains(searchQuery, ignoreCase = true) }
                            val matchesTags = meal.tags.contains(MealTag.VEGETARIAN)
                            matchesSearch && matchesTags
                        },
                        totalCount = testMeals.size,
                        filteredCount = 1,
                        isFiltered = true
                    ),
                    searchQuery = searchQuery,
                    selectedTags = selectedTags,
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - only meals matching both search and filter should be displayed
        composeTestRule.onNodeWithText("Vegetable Pasta").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 of 3 meals").assertIsDisplayed()
    }

    @Test
    fun mealListScreen_clearFiltersButton() {
        // Given
        var filtersCleared = false
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals.filter { it.tags.contains(MealTag.VEGETARIAN) },
                        totalCount = testMeals.size,
                        filteredCount = 2,
                        isFiltered = true
                    ),
                    searchQuery = "",
                    selectedTags = setOf(MealTag.VEGETARIAN),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = { filtersCleared = true },
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - clear filters button should be displayed
        composeTestRule.onNodeWithText("Clear filters").assertIsDisplayed()

        // When - click clear filters button
        composeTestRule.onNodeWithText("Clear filters").performClick()

        // Then - callback should be invoked
        assert(filtersCleared)
    }

    @Test
    fun mealListScreen_emptyStateForNoResults() {
        // Given - filters applied but no results
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = emptyList(),
                        totalCount = testMeals.size,
                        filteredCount = 0,
                        isFiltered = true
                    ),
                    searchQuery = "xyz",
                    selectedTags = emptySet(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - empty state message should be displayed
        composeTestRule
            .onNodeWithText("No meals match your search or filters")
            .assertIsDisplayed()
        
        // And - clear filters action should be available
        composeTestRule
            .onNodeWithText("Clear Filters")
            .assertIsDisplayed()
    }

    @Test
    fun mealListScreen_emptyStateForNoMeals() {
        // Given - no meals in library
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = emptyList(),
                        totalCount = 0,
                        filteredCount = 0,
                        isFiltered = false
                    ),
                    searchQuery = "",
                    selectedTags = emptySet(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - empty state message should be displayed
        composeTestRule
            .onNodeWithText("No meals yet. Add your first meal to get started!")
            .assertIsDisplayed()
        
        // And - add meal action should be available
        composeTestRule
            .onNodeWithText("Add Meal")
            .assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysFilteredCount() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals.take(1),
                        totalCount = testMeals.size,
                        filteredCount = 1,
                        isFiltered = true
                    ),
                    searchQuery = "carbonara",
                    selectedTags = emptySet(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - filtered count should be displayed
        composeTestRule
            .onNodeWithText("1 of 3 meals")
            .assertIsDisplayed()
    }

    @Test
    fun mealListScreen_displaysTotalCountWhenNotFiltered() {
        // Given
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealListUiState.Success(
                        meals = testMeals,
                        totalCount = testMeals.size,
                        filteredCount = testMeals.size,
                        isFiltered = false
                    ),
                    searchQuery = "",
                    selectedTags = emptySet(),
                    onSearchQueryChange = {},
                    onTagToggle = {},
                    onClearFilters = {},
                    onMealClick = {},
                    onAddMealClick = {},
                    onDeleteMeal = {}
                )
            }
        }

        // Then - total count should be displayed
        composeTestRule
            .onNodeWithText("3 meals")
            .assertIsDisplayed()
    }
}
