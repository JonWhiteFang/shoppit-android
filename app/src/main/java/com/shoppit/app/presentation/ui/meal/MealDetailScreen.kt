package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.LoadingScreen
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Stateful composable for the meal detail screen.
 * Injects the ViewModel and manages state collection.
 *
 * Requirements:
 * - 3.1: Open meal detail screen and retrieve specific meal from database
 * - 3.5: Display error message and navigate back if meal not found
 *
 * @param onNavigateBack Callback when the back button is clicked
 * @param onEditClick Callback when the edit button is clicked
 * @param onDeleteClick Callback when the delete button is clicked
 * @param viewModel The ViewModel for managing meal detail state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun MealDetailScreen(
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    viewModel: MealDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealDetailContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onEditClick = onEditClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier
    )
}

/**
 * Stateless composable for the meal detail content.
 * Handles different UI states and renders appropriate content.
 *
 * Requirements:
 * - 3.2: Display meal name, complete ingredient list with quantities and units, and notes
 * - 3.3: Provide edit action button on detail screen
 * - 3.4: Provide delete action button on detail screen
 *
 * @param uiState The current UI state
 * @param onNavigateBack Callback when the back button is clicked
 * @param onEditClick Callback when the edit button is clicked
 * @param onDeleteClick Callback when the delete button is clicked
 * @param modifier Optional modifier for the content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailContent(
    uiState: MealDetailUiState,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (uiState) {
                            is MealDetailUiState.Success -> uiState.meal.name
                            else -> "Meal Details"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    if (uiState is MealDetailUiState.Success) {
                        IconButton(onClick = { onEditClick(uiState.meal.id) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit meal"
                            )
                        }
                        IconButton(onClick = { onDeleteClick(uiState.meal.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete meal",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is MealDetailUiState.Loading -> {
                LoadingScreen(
                    modifier = Modifier.padding(padding),
                    message = "Loading meal details..."
                )
            }
            is MealDetailUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Meal name section
                    Text(
                        text = uiState.meal.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Ingredients section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ingredients",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IngredientList(
                                ingredients = uiState.meal.ingredients
                            )
                        }
                    }
                    
                    // Notes section (if present)
                    if (uiState.meal.notes.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.meal.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            is MealDetailUiState.Error -> {
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
 * Composable that displays a list of ingredients with their quantities and units.
 *
 * Requirements:
 * - 3.2: Display ingredients with quantity and unit
 * - Format ingredient display (e.g., "2 cups flour")
 *
 * @param ingredients List of ingredients to display
 * @param modifier Optional modifier for the list
 */
@Composable
fun IngredientList(
    ingredients: List<Ingredient>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ingredients.forEach { ingredient ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (ingredient.quantity.isNotBlank() || ingredient.unit.isNotBlank()) {
                    Text(
                        text = formatIngredientQuantity(ingredient),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Formats an ingredient's quantity and unit for display.
 * Examples: "2 cups", "400 g", "4 pcs"
 *
 * @param ingredient The ingredient to format
 * @return Formatted string with quantity and unit
 */
private fun formatIngredientQuantity(ingredient: Ingredient): String {
    return buildString {
        if (ingredient.quantity.isNotBlank()) {
            append(ingredient.quantity)
        }
        if (ingredient.unit.isNotBlank()) {
            if (ingredient.quantity.isNotBlank()) {
                append(" ")
            }
            append(ingredient.unit)
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun MealDetailContentLoadingPreview() {
    ShoppitTheme {
        MealDetailContent(
            uiState = MealDetailUiState.Loading,
            onNavigateBack = {},
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailContentSuccessPreview() {
    ShoppitTheme {
        MealDetailContent(
            uiState = MealDetailUiState.Success(
                Meal(
                    id = 1,
                    name = "Spaghetti Carbonara",
                    ingredients = listOf(
                        Ingredient("Pasta", "400", "g"),
                        Ingredient("Eggs", "4", "pcs"),
                        Ingredient("Bacon", "200", "g"),
                        Ingredient("Parmesan cheese", "100", "g"),
                        Ingredient("Black pepper", "", "")
                    ),
                    notes = "Cook pasta al dente. Mix eggs with cheese. Combine with hot pasta and bacon."
                )
            ),
            onNavigateBack = {},
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailContentSuccessWithoutNotesPreview() {
    ShoppitTheme {
        MealDetailContent(
            uiState = MealDetailUiState.Success(
                Meal(
                    id = 2,
                    name = "Caesar Salad",
                    ingredients = listOf(
                        Ingredient("Lettuce", "1", "head"),
                        Ingredient("Croutons", "1", "cup"),
                        Ingredient("Parmesan cheese", "50", "g")
                    ),
                    notes = ""
                )
            ),
            onNavigateBack = {},
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealDetailContentErrorPreview() {
    ShoppitTheme {
        MealDetailContent(
            uiState = MealDetailUiState.Error("Meal not found"),
            onNavigateBack = {},
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IngredientListPreview() {
    ShoppitTheme {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                IngredientList(
                    ingredients = listOf(
                        Ingredient("Pasta", "400", "g"),
                        Ingredient("Eggs", "4", "pcs"),
                        Ingredient("Bacon", "200", "g"),
                        Ingredient("Black pepper", "", "")
                    )
                )
            }
        }
    }
}
