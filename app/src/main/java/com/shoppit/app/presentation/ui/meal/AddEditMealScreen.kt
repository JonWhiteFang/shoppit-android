package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.presentation.ui.common.ErrorEvent
import com.shoppit.app.presentation.ui.common.ErrorSnackbarHandler
import com.shoppit.app.presentation.ui.common.LoadingOverlay
import com.shoppit.app.presentation.ui.common.LoadingScreen
import com.shoppit.app.presentation.ui.common.ValidatedTextField
import com.shoppit.app.presentation.ui.navigation.util.UnsavedChangesBackHandler
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Stateful composable for the add/edit meal screen.
 * Injects the ViewModel and manages state collection.
 *
 * Requirements:
 * - 1.1: Create new meals with ingredient lists
 * - 4.1: Pre-populate form with existing meal data in edit mode
 * - 5.3: Show confirmation when navigating back from forms with unsaved data
 *
 * @param onNavigateBack Callback when navigation back is requested
 * @param onMealSaved Callback when meal is successfully saved
 * @param viewModel The ViewModel for managing add/edit meal state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun AddEditMealScreen(
    onNavigateBack: () -> Unit,
    onMealSaved: () -> Unit,
    viewModel: AddEditMealViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Observe error events and display snackbars
    ErrorSnackbarHandler(
        errorEventFlow = viewModel.errorEvent,
        snackbarHostState = snackbarHostState
    )
    
    // Handle navigation after successful save
    LaunchedEffect(uiState.isSaving) {
        if (!uiState.isSaving && uiState.error == null && uiState.validationErrors.isEmpty()) {
            // Check if we just finished saving (was saving, now not saving, no errors)
            val wasSaving = uiState.isSaving
            if (!wasSaving && uiState.meal.id != 0L) {
                onMealSaved()
            }
        }
    }
    
    // Requirement 5.3: Handle back navigation with unsaved changes
    UnsavedChangesBackHandler(
        hasUnsavedChanges = uiState.hasUnsavedChanges(),
        onConfirmBack = onNavigateBack
    )
    
    AddEditMealContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onMealNameChange = viewModel::updateMealName,
        onMealNotesChange = viewModel::updateMealNotes,
        onAddIngredient = viewModel::addIngredient,
        onRemoveIngredient = viewModel::removeIngredient,
        onSaveMeal = viewModel::saveMeal,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

/**
 * Stateless composable for the add/edit meal content.
 * Handles the form layout and user interactions.
 *
 * Requirements:
 * - 1.1: Form with meal name TextField
 * - 1.2: Validate meal name and ingredients
 * - 4.1: Pre-populate form in edit mode
 * - 4.2: Validate updated meal data
 * - 4.5: Add/remove/modify ingredients
 * - 6.1: Interface to add ingredient entries
 * - 6.4: Allow removing ingredients
 * - 8.1: Display validation errors inline
 * - 1.1, 9.1, 9.2, 9.3, 9.4: Display error and success messages via snackbar
 *
 * @param uiState The current UI state
 * @param onNavigateBack Callback when back button is clicked
 * @param onMealNameChange Callback when meal name changes
 * @param onMealNotesChange Callback when meal notes change
 * @param onAddIngredient Callback when an ingredient is added
 * @param onRemoveIngredient Callback when an ingredient is removed
 * @param onSaveMeal Callback when save button is clicked
 * @param snackbarHostState The SnackbarHostState for displaying snackbar messages
 * @param modifier Optional modifier for the content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealContent(
    uiState: AddEditMealUiState,
    onNavigateBack: () -> Unit,
    onMealNameChange: (String) -> Unit,
    onMealNotesChange: (String) -> Unit,
    onAddIngredient: (Ingredient) -> Unit,
    onRemoveIngredient: (Int) -> Unit,
    onSaveMeal: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    // Show loading screen while initial meal data is being loaded
    if (uiState.isLoading) {
        LoadingScreen(
            modifier = modifier,
            message = "Loading meal..."
        )
        return
    }
    
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.meal.id == 0L) "Add Meal" else "Edit Meal")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    // Save button in app bar
                    IconButton(
                        onClick = onSaveMeal,
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save meal"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Meal name field
            item {
                ValidatedTextField(
                    value = uiState.meal.name,
                    onValueChange = onMealNameChange,
                    label = "Meal Name *",
                    error = uiState.validationErrors["name"],
                    placeholder = { Text("e.g., Spaghetti Carbonara") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isSaving
                )
            }
            
            // Meal notes field (optional)
            item {
                OutlinedTextField(
                    value = uiState.meal.notes,
                    onValueChange = onMealNotesChange,
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add cooking instructions or notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !uiState.isSaving
                )
            }
            
            // Ingredients section header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingredients *",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${uiState.meal.ingredients.size} item${if (uiState.meal.ingredients.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Show validation error for ingredients
                uiState.validationErrors["ingredients"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Ingredient input form
            item {
                IngredientInput(
                    onAddIngredient = onAddIngredient,
                    validationErrors = uiState.validationErrors,
                    enabled = !uiState.isSaving
                )
            }
            
            // Current ingredients list
            itemsIndexed(
                items = uiState.meal.ingredients,
                key = { index, _ -> index }
            ) { index, ingredient ->
                IngredientListItem(
                    ingredient = ingredient,
                    onRemove = { onRemoveIngredient(index) },
                    enabled = !uiState.isSaving
                )
            }
            
            // General error message
            if (uiState.error != null && uiState.validationErrors.isEmpty()) {
                item {
                    Text(
                        text = uiState.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
            
            // Loading overlay for save operation (Requirements: 8.1, 8.2, 8.3, 8.4)
            LoadingOverlay(isLoading = uiState.isSaving)
        }
    }
}

/**
 * Composable for adding a new ingredient.
 * Provides input fields for ingredient name, quantity, and unit.
 *
 * Requirements:
 * - 6.1: Interface to add ingredient entries
 * - 6.2: Validate ingredient has non-empty name
 * - 6.3: Allow quantity and unit to be optional
 * - 6.4: Add ingredient to list
 * - 4.1, 4.2, 4.3, 4.4: Display inline validation errors for ingredient fields
 *
 * @param onAddIngredient Callback when ingredient is added
 * @param validationErrors Map of field names to error messages for inline display
 * @param enabled Whether the input is enabled
 * @param modifier Optional modifier for the component
 */
@Composable
fun IngredientInput(
    onAddIngredient: (Ingredient) -> Unit,
    validationErrors: Map<String, String> = emptyMap(),
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var ingredientName by remember { mutableStateOf("") }
    var ingredientQuantity by remember { mutableStateOf("") }
    var ingredientUnit by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Ingredient",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Ingredient name field
            ValidatedTextField(
                value = ingredientName,
                onValueChange = {
                    ingredientName = it
                    if (showError && it.isNotBlank()) {
                        showError = false
                    }
                },
                label = "Ingredient Name *",
                error = if (showError) "Ingredient name is required" else null,
                placeholder = { Text("e.g., Pasta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = enabled
            )
            
            // Quantity and unit fields in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ValidatedTextField(
                    value = ingredientQuantity,
                    onValueChange = { ingredientQuantity = it },
                    label = "Quantity",
                    placeholder = { Text("e.g., 2") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = enabled
                )
                
                ValidatedTextField(
                    value = ingredientUnit,
                    onValueChange = { ingredientUnit = it },
                    label = "Unit",
                    placeholder = { Text("e.g., cups") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = enabled
                )
            }
            
            // Add button
            OutlinedButton(
                onClick = {
                    if (ingredientName.isBlank()) {
                        showError = true
                    } else {
                        onAddIngredient(
                            Ingredient(
                                name = ingredientName.trim(),
                                quantity = ingredientQuantity.trim(),
                                unit = ingredientUnit.trim()
                            )
                        )
                        // Clear fields after adding
                        ingredientName = ""
                        ingredientQuantity = ""
                        ingredientUnit = ""
                        showError = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Add Ingredient")
            }
        }
    }
}

/**
 * Composable for displaying a single ingredient in the list.
 * Shows ingredient details with a remove button.
 *
 * Requirements:
 * - 6.4: Display current ingredient list with remove buttons
 *
 * @param ingredient The ingredient to display
 * @param onRemove Callback when remove button is clicked
 * @param enabled Whether the remove button is enabled
 * @param modifier Optional modifier for the component
 */
@Composable
fun IngredientListItem(
    ingredient: Ingredient,
    onRemove: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (ingredient.quantity.isNotBlank() || ingredient.unit.isNotBlank()) {
                    Text(
                        text = buildString {
                            if (ingredient.quantity.isNotBlank()) {
                                append(ingredient.quantity)
                            }
                            if (ingredient.unit.isNotBlank()) {
                                if (ingredient.quantity.isNotBlank()) append(" ")
                                append(ingredient.unit)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(
                onClick = onRemove,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove ingredient",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun AddEditMealContentPreview() {
    ShoppitTheme {
        AddEditMealContent(
            uiState = AddEditMealUiState(
                meal = Meal(
                    name = "Spaghetti Carbonara",
                    ingredients = listOf(
                        Ingredient("Pasta", "400", "g"),
                        Ingredient("Eggs", "4", "pcs"),
                        Ingredient("Bacon", "200", "g")
                    ),
                    notes = "Cook pasta al dente"
                )
            ),
            onNavigateBack = {},
            onMealNameChange = {},
            onMealNotesChange = {},
            onAddIngredient = {},
            onRemoveIngredient = {},
            onSaveMeal = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditMealContentEmptyPreview() {
    ShoppitTheme {
        AddEditMealContent(
            uiState = AddEditMealUiState(),
            onNavigateBack = {},
            onMealNameChange = {},
            onMealNotesChange = {},
            onAddIngredient = {},
            onRemoveIngredient = {},
            onSaveMeal = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditMealContentWithErrorsPreview() {
    ShoppitTheme {
        AddEditMealContent(
            uiState = AddEditMealUiState(
                validationErrors = mapOf(
                    "name" to "Meal name cannot be empty",
                    "ingredients" to "Meal must have at least one ingredient"
                )
            ),
            onNavigateBack = {},
            onMealNameChange = {},
            onMealNotesChange = {},
            onAddIngredient = {},
            onRemoveIngredient = {},
            onSaveMeal = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddEditMealContentSavingPreview() {
    ShoppitTheme {
        AddEditMealContent(
            uiState = AddEditMealUiState(
                meal = Meal(
                    name = "Pasta",
                    ingredients = listOf(Ingredient("Pasta", "400", "g"))
                ),
                isSaving = true
            ),
            onNavigateBack = {},
            onMealNameChange = {},
            onMealNotesChange = {},
            onAddIngredient = {},
            onRemoveIngredient = {},
            onSaveMeal = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IngredientInputPreview() {
    ShoppitTheme {
        IngredientInput(
            onAddIngredient = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IngredientListItemPreview() {
    ShoppitTheme {
        IngredientListItem(
            ingredient = Ingredient("Pasta", "400", "g"),
            onRemove = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IngredientListItemNoQuantityPreview() {
    ShoppitTheme {
        IngredientListItem(
            ingredient = Ingredient("Salt"),
            onRemove = {}
        )
    }
}
