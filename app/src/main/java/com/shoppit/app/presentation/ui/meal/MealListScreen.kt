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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.LoadingScreen
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Stateful composable for the meal list screen.
 * Injects the ViewModel and manages state collection.
 *
 * Requirements:
 * - 2.1: Navigate to meal list screen and retrieve all meals
 * - 2.5: Tap meal to navigate to detail screen
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
    
    MealListContent(
        uiState = uiState,
        onMealClick = onMealClick,
        onAddMealClick = onAddMealClick,
        onDeleteMeal = viewModel::deleteMeal,
        modifier = modifier
    )
}

/**
 * Stateless composable for the meal list content.
 * Handles different UI states and renders appropriate content.
 *
 * Requirements:
 * - 2.2: Display meals in alphabetical order
 * - 2.3: Show meal name and ingredient count
 * - 2.4: Display empty state when no meals exist
 *
 * @param uiState The current UI state
 * @param onMealClick Callback when a meal is clicked
 * @param onAddMealClick Callback when the add meal button is clicked
 * @param onDeleteMeal Callback when a meal is deleted
 * @param modifier Optional modifier for the content
 */
@Composable
fun MealListContent(
    uiState: MealListUiState,
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMealClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal"
                )
            }
        }
    ) { padding ->
        when (uiState) {
            is MealListUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.padding(padding),
                    message = "Loading meals..."
                )
            }
            is MealListUiState.Success -> {
                if (uiState.meals.isEmpty()) {
                    EmptyState(
                        message = "No meals yet. Add your first meal to get started!",
                        actionLabel = "Add Meal",
                        onActionClick = onAddMealClick,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    MealList(
                        meals = uiState.meals,
                        onMealClick = onMealClick,
                        onDeleteMeal = onDeleteMeal,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MealListUiState.Error -> {
                ErrorScreen(
                    message = uiState.message,
                    onRetry = null,
                    modifier = Modifier.padding(padding)
                )
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
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
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentEmptyPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(emptyList()),
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentSuccessPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Success(
                listOf(
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
                )
            ),
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealListContentErrorPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealListUiState.Error("Failed to load meals. Please try again."),
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {}
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
