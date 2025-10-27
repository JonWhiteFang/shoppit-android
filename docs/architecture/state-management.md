# State Management Architecture

Comprehensive guide to state management patterns using ViewModel, StateFlow, and Compose in the Shoppit application.

## Overview

Shoppit uses a unidirectional data flow architecture where state flows from ViewModels to UI through StateFlow, and user actions flow from UI to ViewModels through event handlers. This ensures predictable state updates and makes the application easier to test and debug.

## State Management Principles

### 1. Single Source of Truth
- ViewModel holds the single source of truth for UI state
- UI observes state and recomposes when it changes
- No state duplication between layers

### 2. Unidirectional Data Flow
- State flows down: ViewModel → UI
- Events flow up: UI → ViewModel
- No bidirectional data binding

### 3. Immutable State
- State is immutable and read-only from UI
- State updates happen only through ViewModel functions
- Use data classes with `val` properties

### 4. Reactive Updates
- UI automatically updates when state changes
- Use StateFlow for state observation
- Compose recomposes efficiently

## ViewModel State Patterns

### Pattern 1: Sealed Interface for Mutually Exclusive States

Use sealed interfaces when UI can be in one of several mutually exclusive states:

```kotlin
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}

@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    init {
        loadMeals()
    }
    
    private fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                }
                .collect { result ->
                    _uiState.update {
                        result.fold(
                            onSuccess = { MealUiState.Success(it) },
                            onFailure = { MealUiState.Error(it.message ?: "Failed to load meals") }
                        )
                    }
                }
        }
    }
}
```

**When to use:**
- Loading, Success, Error states
- Different screen modes (View, Edit, Create)
- Wizard steps or multi-step flows

### Pattern 2: Data Class for Multiple Independent States

Use data classes when UI has multiple independent state properties:

```kotlin
data class MealDetailUiState(
    val meal: Meal? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)

@HiltViewModel
class MealDetailViewModel @Inject constructor(
    private val getMealUseCase: GetMealByIdUseCase,
    private val saveMealUseCase: SaveMealUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MealDetailUiState())
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()
    
    fun loadMeal(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            getMealUseCase(id).fold(
                onSuccess = { meal ->
                    _uiState.update { it.copy(meal = meal, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to load meal"
                        ) 
                    }
                }
            )
        }
    }
    
    fun saveMeal(meal: Meal) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            saveMealUseCase(meal).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, meal = meal) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isSaving = false, 
                            error = error.message ?: "Failed to save meal"
                        ) 
                    }
                }
            )
        }
    }
}
```

**When to use:**
- Forms with multiple fields
- Screens with multiple loading indicators
- Complex UI with independent state properties

### Pattern 3: Combining Both Patterns

For complex screens, combine sealed interfaces with data classes:

```kotlin
sealed interface ShoppingListUiState {
    data object Loading : ShoppingListUiState
    data class Content(
        val items: List<ShoppingListItem>,
        val filter: FilterState = FilterState.ALL,
        val sortOrder: SortOrder = SortOrder.CATEGORY,
        val isRefreshing: Boolean = false
    ) : ShoppingListUiState
    data class Error(val message: String) : ShoppingListUiState
}

enum class FilterState { ALL, UNCHECKED, CHECKED }
enum class SortOrder { CATEGORY, NAME, ADDED_DATE }

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val getShoppingListUseCase: GetShoppingListUseCase,
    private val toggleItemUseCase: ToggleShoppingItemUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ShoppingListUiState>(ShoppingListUiState.Loading)
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()
    
    fun setFilter(filter: FilterState) {
        _uiState.update { state ->
            if (state is ShoppingListUiState.Content) {
                state.copy(filter = filter)
            } else {
                state
            }
        }
    }
    
    fun setSortOrder(sortOrder: SortOrder) {
        _uiState.update { state ->
            if (state is ShoppingListUiState.Content) {
                state.copy(sortOrder = sortOrder)
            } else {
                state
            }
        }
    }
}
```

## UI State Classes

### Immutable State Design

```kotlin
// Good - Immutable with val properties
data class MealUiState(
    val meals: List<Meal>,
    val isLoading: Boolean,
    val error: String?
)

// Bad - Mutable with var properties
data class MealUiState(
    var meals: List<Meal>,
    var isLoading: Boolean,
    var error: String?
)
```

### State Update Pattern

```kotlin
// Good - Use update { } for atomic state updates
_uiState.update { currentState ->
    currentState.copy(isLoading = false, meals = newMeals)
}

// Bad - Direct assignment can cause race conditions
_uiState.value = _uiState.value.copy(isLoading = false, meals = newMeals)
```

### Derived State

Use `derivedStateOf` for computed values that depend on state:

```kotlin
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Derived state - only recomputes when items change
    val uncheckedCount by remember {
        derivedStateOf {
            when (uiState) {
                is ShoppingListUiState.Content -> 
                    (uiState as ShoppingListUiState.Content).items.count { !it.isChecked }
                else -> 0
            }
        }
    }
    
    Text("$uncheckedCount items remaining")
}
```

## State Hoisting

### Hoisting State to Minimum Common Ancestor

```kotlin
// Stateful screen composable
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = onMealClick,
        onAddMealClick = onAddMealClick,
        onDeleteMeal = viewModel::deleteMeal,
        onRefresh = viewModel::refresh
    )
}

// Stateless content composable
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Meal) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is MealUiState.Loading -> LoadingScreen()
        is MealUiState.Success -> {
            LazyColumn(modifier = modifier) {
                items(uiState.meals, key = { it.id }) { meal ->
                    MealCard(
                        meal = meal,
                        onClick = { onMealClick(meal) },
                        onDelete = { onDeleteMeal(meal) }
                    )
                }
            }
        }
        is MealUiState.Error -> ErrorScreen(
            message = uiState.message,
            onRetry = onRefresh
        )
    }
}
```

### Local State vs Hoisted State

```kotlin
@Composable
fun AddMealScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealSaved: () -> Unit
) {
    // Local state - only used in this composable
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf(listOf<Ingredient>()) }
    
    // Hoisted state - from ViewModel
    val saveResult by viewModel.saveResult.collectAsState(initial = null)
    
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Meal Name") }
        )
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") }
        )
        
        // Ingredient input...
        
        Button(
            onClick = {
                val meal = Meal(name = name, notes = notes, ingredients = ingredients)
                viewModel.saveMeal(meal)
            }
        ) {
            Text("Save")
        }
    }
    
    // Handle save result
    LaunchedEffect(saveResult) {
        saveResult?.onSuccess { onMealSaved() }
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
    // Load meal when screen is first composed or mealId changes
    LaunchedEffect(mealId) {
        viewModel.loadMeal(mealId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    // UI content...
}
```

### DisposableEffect for Cleanup

```kotlin
@Composable
fun TimerScreen() {
    DisposableEffect(Unit) {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                // Timer logic
            }
        }, 0, 1000)
        
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

## Compose State Management

### State in Composables

```kotlin
@Composable
fun SearchBar(
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Remember state across recompositions
    var query by remember { mutableStateOf("") }
    
    OutlinedTextField(
        value = query,
        onValueChange = { newQuery ->
            query = newQuery
            onSearchQueryChanged(newQuery)
        },
        label = { Text("Search") },
        modifier = modifier
    )
}
```

### Remember for Expensive Computations

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

### Stable and Immutable Annotations

```kotlin
// Mark data classes as immutable for better performance
@Immutable
data class Meal(
    val id: Long,
    val name: String,
    val ingredients: List<Ingredient>
)

// Mark classes with stable state as stable
@Stable
data class MealUiState(
    val meals: List<Meal> = emptyList(),
    val isLoading: Boolean = false
)
```

## Testing State

### ViewModel State Testing

```kotlin
@ExperimentalCoroutinesApi
class MealViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var viewModel: MealViewModel
    
    @Before
    fun setup() {
        getMealsUseCase = mockk()
        viewModel = MealViewModel(getMealsUseCase)
    }
    
    @Test
    fun `initial state is loading`() {
        assertEquals(MealUiState.Loading, viewModel.uiState.value)
    }
    
    @Test
    fun `loads meals successfully`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Pasta", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MealUiState.Success)
        assertEquals(meals, (state as MealUiState.Success).meals)
    }
    
    @Test
    fun `handles error when loading fails`() = runTest {
        // Given
        val error = AppError.DatabaseError("Database error")
        coEvery { getMealsUseCase() } returns flowOf(Result.failure(error))
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MealUiState.Error)
        assertEquals("Database error", (state as MealUiState.Error).message)
    }
    
    @Test
    fun `state updates are atomic`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Pasta", ingredients = emptyList()))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))
        
        // Collect all state emissions
        val states = mutableListOf<MealUiState>()
        val job = launch {
            viewModel.uiState.collect { states.add(it) }
        }
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        assertEquals(2, states.size)
        assertTrue(states[0] is MealUiState.Loading)
        assertTrue(states[1] is MealUiState.Success)
        
        job.cancel()
    }
}
```

### Testing State Updates

```kotlin
@Test
fun `updating filter changes state correctly`() = runTest {
    // Given
    val items = listOf(
        ShoppingListItem(name = "Milk", isChecked = false),
        ShoppingListItem(name = "Bread", isChecked = true)
    )
    val initialState = ShoppingListUiState.Content(items = items)
    viewModel.setState(initialState)
    
    // When
    viewModel.setFilter(FilterState.UNCHECKED)
    advanceUntilIdle()
    
    // Then
    val state = viewModel.uiState.value
    assertTrue(state is ShoppingListUiState.Content)
    assertEquals(FilterState.UNCHECKED, (state as ShoppingListUiState.Content).filter)
}
```

## Event Handling

### One-Time Events with SharedFlow

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    // Use SharedFlow for one-time events
    private val _events = MutableSharedFlow<MealEvent>()
    val events: SharedFlow<MealEvent> = _events.asSharedFlow()
    
    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            deleteMealUseCase(meal.id).fold(
                onSuccess = {
                    _events.emit(MealEvent.MealDeleted(meal.name))
                },
                onFailure = { error ->
                    _events.emit(MealEvent.Error(error.message ?: "Failed to delete meal"))
                }
            )
        }
    }
}

sealed interface MealEvent {
    data class MealDeleted(val mealName: String) : MealEvent
    data class Error(val message: String) : MealEvent
}

@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect one-time events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MealEvent.MealDeleted -> {
                    snackbarHostState.showSnackbar("${event.mealName} deleted")
                }
                is MealEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Content...
    }
}
```

## Best Practices

### Do's ✅
- Expose immutable StateFlow from ViewModels
- Use sealed interfaces for mutually exclusive states
- Use data classes for multiple independent states
- Update state with `_state.update { }` for atomic updates
- Hoist state to the minimum common ancestor
- Use `remember` for expensive computations
- Mark data classes as `@Immutable` or `@Stable`
- Test state transitions in ViewModels
- Use SharedFlow for one-time events
- Collect flows in `LaunchedEffect` with proper keys

### Don'ts ❌
- Don't expose MutableStateFlow from ViewModels
- Don't use `var` in state data classes
- Don't mutate state directly
- Don't create state in composables that should be hoisted
- Don't forget to handle all state cases in UI
- Don't use LiveData (use StateFlow instead)
- Don't perform heavy operations in state updates
- Don't forget to cancel coroutines properly
- Don't use `remember` without proper keys
- Don't collect flows outside of `LaunchedEffect`

## Common Patterns

### Loading State Pattern

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@HiltViewModel
class GenericViewModel<T> @Inject constructor(
    private val useCase: UseCase<T>
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState<T>>(UiState.Loading)
    val uiState: StateFlow<UiState<T>> = _uiState.asStateFlow()
}
```

### Pagination State Pattern

```kotlin
data class PaginatedUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PaginatedViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PaginatedUiState<Meal>())
    val uiState: StateFlow<PaginatedUiState<Meal>> = _uiState.asStateFlow()
    
    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            // Load more logic...
        }
    }
}
```

### Form State Pattern

```kotlin
data class FormState(
    val name: String = "",
    val notes: String = "",
    val ingredients: List<Ingredient> = emptyList(),
    val validationErrors: Map<String, String> = emptyMap(),
    val isValid: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class FormViewModel @Inject constructor(
    private val validator: MealValidator
) : ViewModel() {
    
    private val _formState = MutableStateFlow(FormState())
    val formState: StateFlow<FormState> = _formState.asStateFlow()
    
    fun updateName(name: String) {
        _formState.update { state ->
            val newState = state.copy(name = name)
            validate(newState)
        }
    }
    
    private fun validate(state: FormState): FormState {
        val errors = mutableMapOf<String, String>()
        
        if (state.name.isBlank()) {
            errors["name"] = "Name is required"
        }
        if (state.ingredients.isEmpty()) {
            errors["ingredients"] = "At least one ingredient is required"
        }
        
        return state.copy(
            validationErrors = errors,
            isValid = errors.isEmpty()
        )
    }
}
```

## Further Reading

- **[Architecture Overview](overview.md)** - High-level architecture principles
- **[Detailed Design](detailed-design.md)** - Comprehensive architecture specification
- **[Data Flow](data-flow.md)** - Data movement patterns
- **[Compose Patterns Guide](../guides/compose-patterns.md)** - Compose best practices
- **[Testing Guide](../guides/testing.md)** - Testing ViewModels and state
- **[Dependency Injection Guide](../guides/dependency-injection.md)** - Hilt configuration
