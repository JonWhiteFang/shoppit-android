package com.shoppit.app.presentation.ui.meal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.lifecycle.SavedStateHandle
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.usecase.DeleteMealUseCase
import com.shoppit.app.domain.usecase.FakeMealRepository
import com.shoppit.app.domain.usecase.FilterMealsByTagsUseCase
import com.shoppit.app.domain.usecase.GetMealsUseCase
import com.shoppit.app.domain.usecase.SearchMealsUseCase
import com.shoppit.app.util.ViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MealViewModel search and filter functionality.
 * Tests search query updates, tag filtering, and state preservation.
 *
 * Requirements:
 * - 1.1: Filter meals by name containing search text
 * - 1.5: Update results in real-time as query changes
 * - 3.1: Display only meals with corresponding tag when filter selected
 * - 3.2: Remove filter and update results when filter deselected
 * - 4.1: Display filter chip in selected state when selected
 * - 4.2: Display clear button when search bar contains text
 * - 4.3: Display count of search results matching current criteria
 * - 4.4: Display total count when no filters active
 */
@ExperimentalCoroutinesApi
class MealViewModelSearchFilterTest : ViewModelTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var deleteMealUseCase: DeleteMealUseCase
    private lateinit var searchMealsUseCase: SearchMealsUseCase
    private lateinit var filterMealsByTagsUseCase: FilterMealsByTagsUseCase
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: MealViewModel
    private lateinit var testMeals: List<Meal>

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        getMealsUseCase = GetMealsUseCase(repository)
        deleteMealUseCase = DeleteMealUseCase(repository)
        searchMealsUseCase = SearchMealsUseCase()
        filterMealsByTagsUseCase = FilterMealsByTagsUseCase()
        savedStateHandle = SavedStateHandle()
        
        // Create test meals with various names, ingredients, and tags
        testMeals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(
                    Ingredient(name = "Pasta"),
                    Ingredient(name = "Eggs"),
                    Ingredient(name = "Bacon")
                ),
                tags = setOf(MealTag.DINNER)
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(
                    Ingredient(name = "Lettuce"),
                    Ingredient(name = "Croutons")
                ),
                tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN)
            ),
            Meal(
                id = 3,
                name = "Chicken Pasta",
                ingredients = listOf(
                    Ingredient(name = "Pasta"),
                    Ingredient(name = "Chicken")
                ),
                tags = setOf(MealTag.DINNER)
            ),
            Meal(
                id = 4,
                name = "Vegan Buddha Bowl",
                ingredients = listOf(
                    Ingredient(name = "Quinoa"),
                    Ingredient(name = "Vegetables")
                ),
                tags = setOf(MealTag.LUNCH, MealTag.VEGAN, MealTag.HEALTHY)
            )
        )
    }

    @Test
    fun `updateSearchQuery updates search query state`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // Then
        assertEquals("pasta", viewModel.searchQuery.value)
    }

    @Test
    fun `updateSearchQuery filters meals by name`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
        assertTrue(state.meals.any { it.name == "Pasta Carbonara" })
        assertTrue(state.meals.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `updateSearchQuery filters meals by ingredient`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("lettuce")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(1, state.meals.size)
        assertEquals("Caesar Salad", state.meals[0].name)
    }

    @Test
    fun `updateSearchQuery updates filtered count`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(4, state.totalCount)
        assertEquals(2, state.filteredCount)
        assertTrue(state.isFiltered)
    }

    @Test
    fun `updateSearchQuery saves to SavedStateHandle`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // Then
        assertEquals("pasta", savedStateHandle.get<String>("search_query"))
    }

    @Test
    fun `toggleTag adds tag when not selected`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.selectedTags.value.contains(MealTag.DINNER))
    }

    @Test
    fun `toggleTag removes tag when already selected`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then
        assertFalse(viewModel.selectedTags.value.contains(MealTag.DINNER))
    }

    @Test
    fun `toggleTag filters meals by selected tag`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
        assertTrue(state.meals.any { it.name == "Pasta Carbonara" })
        assertTrue(state.meals.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `toggleTag with multiple tags uses AND logic`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.LUNCH)
        viewModel.toggleTag(MealTag.VEGETARIAN)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(1, state.meals.size)
        assertEquals("Caesar Salad", state.meals[0].name)
    }

    @Test
    fun `toggleTag saves to SavedStateHandle`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then
        val savedTags = savedStateHandle.get<Set<MealTag>>("selected_tags")
        assertTrue(savedTags?.contains(MealTag.DINNER) == true)
    }

    @Test
    fun `clearFilters resets search query`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `clearFilters resets selected tags`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.selectedTags.value.isEmpty())
    }

    @Test
    fun `clearFilters shows all meals`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()
        viewModel.updateSearchQuery("pasta")
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(4, state.meals.size)
        assertFalse(state.isFiltered)
    }

    @Test
    fun `clearFilters updates SavedStateHandle`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()
        viewModel.updateSearchQuery("pasta")
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // When
        viewModel.clearFilters()
        advanceUntilIdle()

        // Then
        assertEquals("", savedStateHandle.get<String>("search_query"))
        assertTrue(savedStateHandle.get<Set<MealTag>>("selected_tags")?.isEmpty() == true)
    }

    @Test
    fun `combined search and filter applies both criteria`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When - search for "pasta" and filter by DINNER
        viewModel.updateSearchQuery("pasta")
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then - should return only dinner pasta meals
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
        assertTrue(state.meals.any { it.name == "Pasta Carbonara" })
        assertTrue(state.meals.any { it.name == "Chicken Pasta" })
    }

    @Test
    fun `combined search and filter with no matches returns empty list`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When - search for "pasta" and filter by VEGAN (no vegan pasta)
        viewModel.updateSearchQuery("pasta")
        viewModel.toggleTag(MealTag.VEGAN)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertTrue(state.meals.isEmpty())
        assertTrue(state.isFiltered)
    }

    @Test
    fun `state preservation - search query restored from SavedStateHandle`() = runTest {
        // Given - SavedStateHandle with search query
        savedStateHandle["search_query"] = "pasta"
        repository.setMeals(testMeals)

        // When - ViewModel is created
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then - search query should be restored
        assertEquals("pasta", viewModel.searchQuery.value)
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
    }

    @Test
    fun `state preservation - selected tags restored from SavedStateHandle`() = runTest {
        // Given - SavedStateHandle with selected tags
        savedStateHandle["selected_tags"] = setOf(MealTag.DINNER)
        repository.setMeals(testMeals)

        // When - ViewModel is created
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then - selected tags should be restored
        assertTrue(viewModel.selectedTags.value.contains(MealTag.DINNER))
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
    }

    @Test
    fun `state preservation - both search and tags restored from SavedStateHandle`() = runTest {
        // Given - SavedStateHandle with both search query and tags
        savedStateHandle["search_query"] = "pasta"
        savedStateHandle["selected_tags"] = setOf(MealTag.DINNER)
        repository.setMeals(testMeals)

        // When - ViewModel is created
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then - both should be restored and applied
        assertEquals("pasta", viewModel.searchQuery.value)
        assertTrue(viewModel.selectedTags.value.contains(MealTag.DINNER))
        val state = viewModel.uiState.value as MealListUiState.Success
        assertEquals(2, state.meals.size)
        assertTrue(state.isFiltered)
    }

    @Test
    fun `isFiltered is false when no filters active`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertFalse(state.isFiltered)
    }

    @Test
    fun `isFiltered is true when search query is active`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.updateSearchQuery("pasta")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertTrue(state.isFiltered)
    }

    @Test
    fun `isFiltered is true when tags are selected`() = runTest {
        // Given
        repository.setMeals(testMeals)
        viewModel = MealViewModel(
            getMealsUseCase,
            deleteMealUseCase,
            searchMealsUseCase,
            filterMealsByTagsUseCase,
            savedStateHandle
        )
        advanceUntilIdle()

        // When
        viewModel.toggleTag(MealTag.DINNER)
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value as MealListUiState.Success
        assertTrue(state.isFiltered)
    }
}
