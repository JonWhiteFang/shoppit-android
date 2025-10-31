package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.ErrorSnackbarHandler
import com.shoppit.app.presentation.ui.common.LoadingScreen
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Stateful composable for the meal list screen.
 * Injects the ViewModel and manages state collection.
 *
 * Requirements:
 * - 1.1: Display search input field
 * - 1.5: Preserve search state across navigation
 * - 2.1: Navigate to meal list screen and retrieve all meals
 * - 2.5: Tap meal to navigate to detail screen
 * - 3.1: Display filter chips for meal tags
 * - 3.2: Support multiple tag selection
 *
 * @param onMealClick Callback when a meal is clicked
 * @param onAddMealClick Callback when the add meal button is clicked
 * @param viewModel The ViewModel for managing meal list state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun MealListScreen(
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    viewModel: MealViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Requirement 9.1: Observe error events and display snackbars
    ErrorSnackbarHandler(
        errorEventFlow = viewModel.errorEvent,
        snackbarHostState = snackbarHostState
    )
    
    MealListContent(
        uiState = uiState,
        searchQuery = searchQuery,
        selectedTags = selectedTags,
        snackbarHostState = snackbarHostState,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onTagToggle = viewModel::toggleTag,
        onClearFilters = viewModel::clearFilters,
        onMealClick = onMealClick,
        onAddMealClick = onAddMealClick,
        onDeleteMeal = viewModel::deleteMeal,
        onRetry = viewModel::loadMeals,
        modifier = modifier
    )
}

/**
 * Stateless composable for the meal list content.
 * Handles different UI states and renders appropriate content.
 *
 * Requirements:
 * - 1.1: Display search input field
 * - 1.2: Support real-time search
 * - 1.4: Display empty state for no results
 * - 2.2: Display meals in alphabetical order
 * - 2.3: Show meal name and ingredient count
 * - 2.4: Display empty state when no meals exist
 * - 3.1: Display filter chips for meal tags
 * - 3.2: Support multiple tag selection
 * - 4.3: Display filtered count vs total count
 * - 4.4: Show clear filters button
 *
 * @param uiState The current UI state
 * @param searchQuery Current search query
 * @param selectedTags Currently selected filter tags
 * @param onSearchQueryChange Callback when search query changes
 * @param onTagToggle Callback when a tag is toggled
 * @param onClearFilters Callback when clear filters is clicked
 * @param onMealClick Callback when a meal is clicked
 * @param onAddMealClick Callback when the add meal button is clicked
 * @param onDeleteMeal Callback when a meal is deleted
 * @param modifier Optional modifier for the content
 */
@Composable
fun MealListContent(
    uiState: MealListUiState,
    searchQuery: String,
    selectedTags: Set<MealTag>,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (MealTag) -> Unit,
    onClearFilters: () -> Unit,
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    MealListKeyboardHandler(
        searchQuery = searchQuery,
        onClearSearch = { onSearchQueryChange("") },
        modifier = modifier
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddMealClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add meal"
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
            // Search bar
            MealSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(16.dp)
            )
            
            // Filter chips
            FilterChipRow(
                selectedTags = selectedTags,
                onTagToggle = onTagToggle,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Content based on state
            when (uiState) {
                is MealListUiState.Loading -> {
                    LoadingScreen(
                        message = "Loading meals..."
                    )
                }
                is MealListUiState.Success -> {
                    if (uiState.meals.isEmpty() && !uiState.isFiltered) {
                        EmptyState(
                            message = "No meals yet. Add your first meal to get started!",
                            actionLabel = "Add Meal",
                            onActionClick = onAddMealClick
                        )
                    } else if (uiState.meals.isEmpty() && uiState.isFiltered) {
                        EmptyState(
                            message = "No meals match your search or filters",
                            actionLabel = "Clear Filters",
                            onActionClick = onClearFilters
                        )
                    } else {
                        Column {
                            ResultsHeader(
                                totalCount = uiState.totalCount,
                                filteredCount = uiState.filteredCount,
                                isFiltered = uiState.isFiltered,
                                onClearFilters = onClearFilters
                            )
                            MealList(
                                meals = uiState.meals,
                                onMealClick = onMealClick,
                                onDeleteMeal = onDeleteMeal
                            )
                        }
                    }
                }
                is MealListUiState.Error -> {
                    // Requirement 9.2: Pass retry callback to ErrorScreen
                    ErrorScreen(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
            }
        }
        }
    }
}

/**
 * Composable that displays a list of meals using LazyColumn.
 *
 * Requirements:
 * - 2.2: Display meals in list view
 * - 2.5: Handle meal click for navigation
 *
 * @param meals List of meals to display
 * @param onMealClick Callback when a meal is clicked
 * @param onDeleteMeal Callback when a meal is deleted
 * @param modifier Optional modifier for the list
 */
@Composable
fun MealList(
    meals: List<Meal>,
    onMealClick: (Long) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Requirement 6.1: Preserve scroll position across navigation
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(
            items = meals,
            key = { meal -> meal.id }
        ) { meal ->
            MealCard(
                meal = meal,
                onClick = { onMealClick(meal.id) },
                onDelete = { onDeleteMeal(meal.id) }
            )
        }
    }
}

/**
 * Composable that displays a single meal card with swipe-to-delete functionality.
 *
 * Requirements:
 * - 2.3: Display meal name and ingredient count
 * - 2.5: Handle click for navigation
 * - 5.1: Display confirmation dialog before deletion
 * - 5.4: Cancel deletion when user dismisses dialog
 *
 * @param meal The meal to display
 * @param onClick Callback when the card is clicked
 * @param onDelete Callback when the meal is deleted
 * @param modifier Optional modifier for the card
 */
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${meal.ingredients.size} ingredient${if (meal.ingredients.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            androidx.compose.material3.IconButton(
                onClick = { showDeleteDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete meal",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            mealName = meal.name,
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

/**
 * Confirmation dialog for deleting a meal.
 *
 * Requirements:
 * - 5.1: Display confirmation dialog with meal name
 * - 5.4: Allow user to cancel deletion
 *
 * @param mealName The name of the meal to delete
 * @param onConfirm Callback when deletion is confirmed
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun DeleteConfirmationDialog(
    mealName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Meal")
        },
        text = {
            Text("Are you sure you want to delete \"$mealName\"? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun MealListContentLoadingPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Loading,
            searchQuery = "",
            selectedTags = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentEmptyPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(emptyList()),
            searchQuery = "",
            selectedTags = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentSuccessPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(
                meals = listOf(
                    Meal(
                        id = 1,
                        name = "Spaghetti Carbonara",
                        ingredients = listOf(
                            Ingredient("Pasta", "400", "g"),
                            Ingredient("Eggs", "4", "pcs"),
                            Ingredient("Bacon", "200", "g")
                        )
                    ),
                    Meal(
                        id = 2,
                        name = "Caesar Salad",
                        ingredients = listOf(
                            Ingredient("Lettuce", "1", "head"),
                            Ingredient("Croutons", "1", "cup")
                        )
                    )
                ),
                totalCount = 2,
                filteredCount = 2,
                isFiltered = false
            ),
            searchQuery = "",
            selectedTags = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentErrorPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Error("Failed to load meals. Please try again."),
            searchQuery = "",
            selectedTags = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentWithFiltersPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(
                meals = listOf(
                    Meal(
                        id = 1,
                        name = "Spaghetti Carbonara",
                        ingredients = listOf(
                            Ingredient("Pasta", "400", "g")
                        )
                    )
                ),
                totalCount = 10,
                filteredCount = 1,
                isFiltered = true
            ),
            searchQuery = "pasta",
            selectedTags = setOf(MealTag.VEGETARIAN),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentNoResultsPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(
                meals = emptyList(),
                totalCount = 10,
                filteredCount = 0,
                isFiltered = true
            ),
            searchQuery = "xyz",
            selectedTags = emptySet(),
            snackbarHostState = remember { SnackbarHostState() },
            onSearchQueryChange = {},
            onTagToggle = {},
            onClearFilters = {},
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealCardPreview() {
    ShoppitTheme {
        MealCard(
            meal = Meal(
                id = 1,
                name = "Spaghetti Carbonara",
                ingredients = listOf(
                    Ingredient("Pasta", "400", "g"),
                    Ingredient("Eggs", "4", "pcs"),
                    Ingredient("Bacon", "200", "g")
                )
            ),
            onClick = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeleteConfirmationDialogPreview() {
    ShoppitTheme {
        DeleteConfirmationDialog(
            mealName = "Spaghetti Carbonara",
            onConfirm = {},
            onDismiss = {}
        )
    }
}
