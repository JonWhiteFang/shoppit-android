# Navigation State Management Patterns

## Overview

This document describes the state management patterns used in the Shoppit navigation system, including how state is preserved, restored, and managed across different navigation scenarios.

## State Management Layers

### 1. Navigation State

**Managed by:** Jetpack Navigation Component

**What it includes:**
- Current destination
- Back stack entries
- Navigation arguments
- Saved state bundles

**Preservation mechanism:**
```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true  // Saves navigation state
    }
    restoreState = true   // Restores navigation state
}
```

### 2. UI State

**Managed by:** Compose State System

**What it includes:**
- Scroll positions (LazyColumn, LazyRow)
- Text field values (with rememberSaveable)
- Dialog visibility
- Expanded/collapsed states

**Preservation mechanism:**
```kotlin
// Automatic preservation with rememberSaveable
val scrollState = rememberLazyListState()

// Manual preservation for complex state
var textValue by rememberSaveable { mutableStateOf("") }
```

### 3. ViewModel State

**Managed by:** ViewModel + SavedStateHandle

**What it includes:**
- Business logic state
- Form data
- Loading states
- Error states

**Preservation mechanism:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Survives process death
    var formData by savedStateHandle.saveable { mutableStateOf("") }
}
```

### 4. Database State

**Managed by:** Room Database + Flow

**What it includes:**
- Persistent app data
- User content
- Settings and preferences

**Preservation mechanism:**
```kotlin
// Automatic persistence with Room
@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<MealEntity>>
}
```

## State Preservation Scenarios

### Scenario 1: Switching Between Bottom Navigation Tabs

**What happens:**
1. User is on Meals tab, scrolled down in the list
2. User taps Planner tab
3. User returns to Meals tab

**Expected behavior:**
- Meals list should be at the same scroll position
- Any expanded items should remain expanded
- Search/filter state should be preserved

**Implementation:**

```kotlin
// In MainScreen
NavigationBarItem(
    selected = selected,
    onClick = {
        navController.navigate(item.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true  // KEY: Saves all state
            }
            launchSingleTop = true
            restoreState = true   // KEY: Restores all state
        }
    }
)

// In MealListScreen
@Composable
fun MealListScreen() {
    // Scroll state is automatically preserved
    val listState = rememberLazyListState()
    
    LazyColumn(state = listState) {
        // Items
    }
}
```

**State preservation flow:**
```
1. User on Meals tab (scroll position = 100)
2. Taps Planner tab
   → saveState = true saves:
     - Scroll position (100)
     - Expanded items
     - Any rememberSaveable state
3. User on Planner tab
4. Taps Meals tab
   → restoreState = true restores:
     - Scroll position (100)
     - Expanded items
     - All saved state
```

### Scenario 2: Navigating to Detail Screen and Back

**What happens:**
1. User is on Meals list, scrolled to position 100
2. User taps a meal to view details
3. User presses back

**Expected behavior:**
- Meals list should be at position 100
- Detail screen should not be in back stack
- Transition should be smooth

**Implementation:**

```kotlin
// Navigate to detail
navController.navigate(Screen.MealDetail.createRoute(mealId))

// Navigate back
navController.popBackStack()

// State is preserved because:
// 1. ViewModel survives navigation
// 2. Compose state is saved automatically
// 3. Database provides persistent data
```

**State preservation flow:**
```
1. MealListScreen (scroll = 100)
   → ViewModel holds meal list
   → Compose saves scroll state
2. Navigate to MealDetailScreen
   → MealListScreen is in back stack
   → State is retained in memory
3. Pop back stack
   → MealListScreen is restored
   → Scroll position = 100
   → ViewModel still has data
```

### Scenario 3: Process Death and Recreation

**What happens:**
1. User is editing a meal form
2. Android kills the app process (low memory)
3. User returns to the app

**Expected behavior:**
- Form data should be preserved
- User should be on the same screen
- Navigation stack should be intact

**Implementation:**

```kotlin
@HiltViewModel
class AddEditMealViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Survives process death
    var mealName by savedStateHandle.saveable { 
        mutableStateOf("") 
    }
    
    var ingredients by savedStateHandle.saveable { 
        mutableStateOf(listOf<Ingredient>()) 
    }
}

@Composable
fun AddEditMealScreen(
    viewModel: AddEditMealViewModel = hiltViewModel()
) {
    // State is automatically restored after process death
    OutlinedTextField(
        value = viewModel.mealName,
        onValueChange = viewModel::updateMealName
    )
}
```

**State preservation flow:**
```
1. User editing form
   → mealName = "Pasta"
   → ingredients = [Tomato, Cheese]
   → SavedStateHandle saves to Bundle
2. Process death
   → App process killed
   → Bundle saved by Android
3. App recreated
   → Bundle restored by Android
   → SavedStateHandle reads from Bundle
   → mealName = "Pasta"
   → ingredients = [Tomato, Cheese]
```

### Scenario 4: Configuration Change (Rotation)

**What happens:**
1. User is viewing meal list in portrait
2. User rotates device to landscape
3. Activity is recreated

**Expected behavior:**
- Scroll position preserved
- Selected items preserved
- Loading states preserved
- No data reload

**Implementation:**

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // Survives configuration changes
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    init {
        loadMeals()
    }
}

@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    // ViewModel survives rotation
    val uiState by viewModel.uiState.collectAsState()
    
    // Scroll state preserved automatically
    val listState = rememberLazyListState()
    
    LazyColumn(state = listState) {
        // Items
    }
}
```

**State preservation flow:**
```
1. Portrait mode
   → ViewModel created
   → Meals loaded
   → Scroll position = 100
2. Rotate to landscape
   → Activity destroyed
   → ViewModel retained
   → Scroll state saved
3. Landscape mode
   → Activity recreated
   → Same ViewModel instance
   → Scroll state restored
   → No data reload needed
```

## State Management Best Practices

### 1. Choose the Right State Container

| State Type | Container | Survives | Use Case |
|------------|-----------|----------|----------|
| UI State | Compose State | Config change | Scroll position, expanded state |
| Form Data | SavedStateHandle | Process death | User input, form fields |
| Business Logic | ViewModel | Config change | Loading states, data |
| Persistent Data | Database | Everything | User content, settings |

### 2. State Hoisting Pattern

```kotlin
// Screen (Stateful)
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = viewModel::onMealClick,
        onDeleteMeal = viewModel::deleteMeal
    )
}

// Content (Stateless)
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
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

### 3. SavedStateHandle for Critical Data

```kotlin
@HiltViewModel
class AddEditMealViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    // Critical form data that must survive process death
    var mealName by savedStateHandle.saveable { mutableStateOf("") }
        private set
    
    var ingredients by savedStateHandle.saveable { 
        mutableStateOf(listOf<Ingredient>()) 
    }
        private set
    
    // Non-critical state can use regular StateFlow
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
}
```

### 4. Database as Source of Truth

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // Database is the source of truth
    val uiState: StateFlow<MealUiState> = getMealsUseCase()
        .map { result ->
            result.fold(
                onSuccess = { MealUiState.Success(it) },
                onFailure = { MealUiState.Error(it.message ?: "Unknown error") }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MealUiState.Loading
        )
}
```

## State Clearing Scenarios

### When to Clear State

1. **User logs out:** Clear all user data
2. **Data is deleted:** Clear related cached state
3. **User explicitly requests:** Clear filters, search, etc.
4. **Navigation to unrelated section:** Clear temporary state

### How to Clear State

```kotlin
// Clear navigation state
navController.navigate(route) {
    popUpTo(navController.graph.startDestinationId) {
        inclusive = true
        saveState = false  // Don't save state
    }
}

// Clear ViewModel state
viewModel.clearState()

// Clear SavedStateHandle
savedStateHandle.remove<String>("key")

// Clear database (if needed)
repository.clearAllData()
```

## Debugging State Issues

### Common Issues and Solutions

**Issue: State not preserved when switching tabs**

Solution:
```kotlin
// Ensure saveState and restoreState are true
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true  // Must be true
    }
    restoreState = true   // Must be true
}
```

**Issue: Form data lost after process death**

Solution:
```kotlin
// Use SavedStateHandle for critical data
var formData by savedStateHandle.saveable { mutableStateOf("") }
```

**Issue: Scroll position not preserved**

Solution:
```kotlin
// Use rememberLazyListState (not remember)
val listState = rememberLazyListState()
```

**Issue: State persists when it shouldn't**

Solution:
```kotlin
// Clear state explicitly
navController.navigate(route) {
    popUpTo(route) {
        saveState = false  // Don't save state
    }
}
```

## Testing State Management

### Test State Preservation

```kotlin
@Test
fun `state is preserved when switching tabs`() {
    // Navigate to Meals tab
    composeTestRule.onNodeWithText("Meals").performClick()
    
    // Scroll down
    composeTestRule.onNodeWithTag("meal_list")
        .performScrollToIndex(10)
    
    // Switch to Planner tab
    composeTestRule.onNodeWithText("Planner").performClick()
    
    // Switch back to Meals tab
    composeTestRule.onNodeWithText("Meals").performClick()
    
    // Verify scroll position is preserved
    composeTestRule.onNodeWithTag("meal_list")
        .assertScrollPosition(10)
}
```

### Test Process Death

```kotlin
@Test
fun `form data survives process death`() {
    // Enter form data
    composeTestRule.onNodeWithTag("meal_name_field")
        .performTextInput("Pasta")
    
    // Simulate process death
    activityScenario.recreate()
    
    // Verify data is preserved
    composeTestRule.onNodeWithTag("meal_name_field")
        .assertTextEquals("Pasta")
}
```

## Related Documentation

- [Navigation Architecture](./NAVIGATION_ARCHITECTURE.md)
- [Common Scenarios](./COMMON_SCENARIOS.md)
- [Compose Patterns](../../.kiro/steering/compose-patterns.md)
