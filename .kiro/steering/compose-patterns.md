# Compose Patterns

## State Management Patterns

### ViewModel State Exposure

Always expose immutable state from ViewModels:

```kotlin
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    // Update state with .update { }
    fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                }
                .collect { result ->
                    _uiState.update { 
                        result.fold(
                            onSuccess = { MealUiState.Success(it) },
                            onFailure = { MealUiState.Error(it.message ?: "Unknown error") }
                        )
                    }
                }
        }
    }
}
```

### UI State Classes

Use sealed classes for mutually exclusive states:

```kotlin
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}

// For screens with multiple independent states, use data class
data class MealDetailUiState(
    val meal: Meal? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
```

### State Hoisting

Hoist state to the minimum common ancestor:

```kotlin
// Screen composable - stateful
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = viewModel::onMealClick,
        onAddMealClick = viewModel::onAddMealClick,
        onDeleteMeal = viewModel::deleteMeal
    )
}

// Content composable - stateless
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Meal) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is MealUiState.Loading -> LoadingScreen()
        is MealUiState.Success -> MealList(
            meals = uiState.meals,
            onMealClick = onMealClick,
            onDeleteMeal = onDeleteMeal
        )
        is MealUiState.Error -> ErrorScreen(message = uiState.message)
    }
}
```

## Composable Structure

### Standard Composable Signature

```kotlin
@Composable
fun ComponentName(
    // Required parameters first
    data: DataType,
    onAction: (Type) -> Unit,
    
    // Optional parameters with defaults
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    
    // Content lambda last (if applicable)
    content: @Composable () -> Unit = {}
) {
    // Implementation
}
```

### Modifier Parameter Rules

- Always include `modifier: Modifier = Modifier` parameter
- Place it after required parameters, before optional ones
- Apply it to the root composable element
- Never create modifiers inside composables (use `remember` if needed)

```kotlin
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // Always include
) {
    Card(
        modifier = modifier // Apply to root element
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Content
    }
}
```

## Performance Optimization

### Remember Expensive Computations

```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    // Remember expensive calculations
    val sortedMeals = remember(meals) {
        meals.sortedBy { it.name }
    }
    
    // Remember lambdas that capture state
    val onMealClick = remember {
        { meal: Meal -> /* handle click */ }
    }
    
    LazyColumn {
        items(sortedMeals, key = { it.id }) { meal ->
            MealCard(meal = meal, onClick = { onMealClick(meal) })
        }
    }
}
```

### Derived State

Use `derivedStateOf` for computed values that depend on state:

```kotlin
@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    
    // Only recomposes when the derived value changes
    val uncheckedCount by remember {
        derivedStateOf {
            items.count { !it.isChecked }
        }
    }
    
    Text("$uncheckedCount items remaining")
}
```

### Stable Collections

Mark data classes as `@Immutable` or `@Stable` when appropriate:

```kotlin
@Immutable
data class Meal(
    val id: Long,
    val name: String,
    val ingredients: List<Ingredient>
)

@Stable
data class MealUiState(
    val meals: List<Meal> = emptyList(),
    val isLoading: Boolean = false
)
```

### LazyColumn Optimization

```kotlin
@Composable
fun MealList(meals: List<Meal>, onMealClick: (Meal) -> Unit) {
    LazyColumn {
        items(
            items = meals,
            key = { it.id } // Stable key for efficient recomposition
        ) { meal ->
            MealCard(
                meal = meal,
                onClick = { onMealClick(meal) }
            )
        }
    }
}
```

## Navigation Patterns

### Type-Safe Navigation Routes

```kotlin
sealed class Screen(val route: String) {
    data object MealList : Screen("meal_list")
    data object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long) = "meal_detail/$mealId"
    }
    data object AddMeal : Screen("add_meal")
}

@Composable
fun ShoppitNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.MealList.route,
        modifier = modifier
    ) {
        composable(Screen.MealList.route) {
            MealListScreen(
                onMealClick = { meal ->
                    navController.navigate(Screen.MealDetail.createRoute(meal.id))
                },
                onAddMealClick = {
                    navController.navigate(Screen.AddMeal.route)
                }
            )
        }
        
        composable(
            route = Screen.MealDetail.route,
            arguments = listOf(navArgument("mealId") { type = NavType.LongType })
        ) { backStackEntry ->
            val mealId = backStackEntry.arguments?.getLong("mealId") ?: return@composable
            MealDetailScreen(
                mealId = mealId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

## Reusable Components

### Loading State

```kotlin
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### Error State

```kotlin
@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            onRetry?.let {
                Button(onClick = it) {
                    Text("Retry")
                }
            }
        }
    }
}
```

### Empty State

```kotlin
@Composable
fun EmptyState(
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Button(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}
```

### Swipe to Delete

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )
    
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(16.dp)
                )
            }
        },
        content = { content() }
    )
}
```

## Dialog Patterns

### Confirmation Dialog

```kotlin
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
```

### Input Dialog

```kotlin
@Composable
fun InputDialog(
    title: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    label: String = "Input"
) {
    var text by remember { mutableStateOf(initialValue) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

## Preview Patterns

### Preview Provider

```kotlin
@PreviewParameter
class MealPreviewProvider : PreviewParameterProvider<Meal> {
    override val values = sequenceOf(
        Meal(
            id = 1,
            name = "Spaghetti Carbonara",
            ingredients = listOf(
                Ingredient("Pasta", "400", "g"),
                Ingredient("Eggs", "4", "pcs")
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
}

@Preview(showBackground = true)
@Composable
fun MealCardPreview(
    @PreviewParameter(MealPreviewProvider::class) meal: Meal
) {
    ShoppitTheme {
        MealCard(
            meal = meal,
            onClick = {}
        )
    }
}
```

### Multiple Previews

```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MealListPreview() {
    ShoppitTheme {
        MealListContent(
            uiState = MealUiState.Success(
                meals = listOf(
                    Meal(id = 1, name = "Pasta"),
                    Meal(id = 2, name = "Salad")
                )
            ),
            onMealClick = {},
            onAddMealClick = {},
            onDeleteMeal = {}
        )
    }
}
```

## Side Effects

### LaunchedEffect for One-Time Events

```kotlin
@Composable
fun MealDetailScreen(
    mealId: Long,
    viewModel: MealDetailViewModel = hiltViewModel()
) {
    // Load meal when screen is first composed
    LaunchedEffect(mealId) {
        viewModel.loadMeal(mealId)
    }
    
    // Content
}
```

### DisposableEffect for Cleanup

```kotlin
@Composable
fun TimerScreen() {
    DisposableEffect(Unit) {
        val timer = Timer()
        timer.schedule(/* ... */)
        
        onDispose {
            timer.cancel()
        }
    }
}
```

### rememberCoroutineScope for Event Handlers

```kotlin
@Composable
fun MealListScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Button(
            onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("Meal deleted")
                }
            }
        ) {
            Text("Delete")
        }
    }
}
```

## Testing Composables

### Test Tags

```kotlin
@Composable
fun MealCard(meal: Meal, onClick: () -> Unit) {
    Card(
        modifier = Modifier.testTag("meal_card_${meal.id}"),
        onClick = onClick
    ) {
        Text(
            text = meal.name,
            modifier = Modifier.testTag("meal_name")
        )
    }
}

// In tests
composeTestRule.onNodeWithTag("meal_card_1").performClick()
composeTestRule.onNodeWithTag("meal_name").assertTextEquals("Pasta")
```

## Common Pitfalls

### Avoid
- Creating new lambdas in composable body without `remember`
- Mutating state directly (always use `_state.update { }`)
- Heavy computations without `remember` or `derivedStateOf`
- Accessing ViewModel state directly (always use `collectAsState()`)
- Nested LazyColumns (use single LazyColumn with different item types)
- Recreating modifiers inside composables

### Prefer
- Stateless composables with hoisted state
- Immutable data classes for state (`@Immutable` or `@Stable`)
- Stable keys for LazyColumn items (use unique IDs)
- Single source of truth in ViewModel
- Separation of screen (stateful) and content (stateless) composables
- Expression bodies for simple composables
