# Design Document: Fix Failing Unit Tests

## Overview

This design addresses 14 failing unit tests by implementing missing functionality in the MealPlannerViewModel, MealViewModel, and NavigationErrorHandler. The tests are well-written and follow best practices - the implementations need to be updated to match the expected behavior defined in the tests.

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
├─────────────────────────────────────────────────────────────┤
│  MealPlannerViewModel                                        │
│  ├─ errorEvent: SharedFlow<ErrorEvent>                      │
│  ├─ onMealSelected(mealId: Long)                           │
│  ├─ deleteMealPlan(planId: Long)                           │
│  ├─ selectSuggestion(meal: Meal)                           │
│  └─ dismissMealSelection()                                  │
├─────────────────────────────────────────────────────────────┤
│  MealViewModel                                               │
│  ├─ deleteMeal(mealId: Long)                               │
│  └─ SavedStateHandle integration                            │
├─────────────────────────────────────────────────────────────┤
│  NavigationErrorHandler (Utility)                           │
│  └─ safeNavigate(...)                                       │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. MealPlannerViewModel Error Event System

**Purpose:** Emit success and error events to the UI layer for user feedback.

**Implementation:**

```kotlin
class MealPlannerViewModel(...) : ViewModel() {
    
    // Add error event flow
    private val _errorEvent = MutableSharedFlow<ErrorEvent>()
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent.asSharedFlow()
    
    fun onMealSelected(mealId: Long) {
        viewModelScope.launch {
            // Existing logic...
            result.fold(
                onSuccess = {
                    _errorEvent.emit(ErrorEvent.Success("Meal added to plan"))
                    // Close dialog
                    _uiState.update { it.copy(showMealSelection = false, selectedSlot = null) }
                },
                onFailure = { error ->
                    _errorEvent.emit(ErrorEvent.Error(error.message ?: "Failed to add meal"))
                    errorLogger.logError(
                        error = error,
                        context = "MealPlannerViewModel.onMealSelected",
                        additionalData = mapOf("mealId" to mealId.toString())
                    )
                }
            )
        }
    }
    
    fun deleteMealPlan(planId: Long) {
        viewModelScope.launch {
            deleteMealPlanUseCase(planId).fold(
                onSuccess = {
                    _errorEvent.emit(ErrorEvent.Success("Meal removed from plan"))
                },
                onFailure = { error ->
                    _errorEvent.emit(ErrorEvent.Error(error.message ?: "Failed to remove meal"))
                    errorLogger.logError(
                        error = error,
                        context = "MealPlannerViewModel.deleteMealPlan",
                        additionalData = mapOf("planId" to planId.toString())
                    )
                }
            )
        }
    }
    
    fun selectSuggestion(meal: Meal) {
        viewModelScope.launch {
            // Existing logic...
            result.fold(
                onSuccess = {
                    _errorEvent.emit(ErrorEvent.Success("Meal added to plan"))
                    _suggestionState.value = SuggestionUiState.Hidden
                },
                onFailure = { error ->
                    _errorEvent.emit(ErrorEvent.Error(error.message ?: "Failed to add meal"))
                    errorLogger.logError(
                        error = error,
                        context = "MealPlannerViewModel.selectSuggestion",
                        additionalData = mapOf("mealId" to meal.id.toString())
                    )
                }
            )
        }
    }
    
    fun dismissMealSelection() {
        _uiState.update { it.copy(showMealSelection = false, selectedSlot = null) }
    }
}
```

**Key Design Decisions:**
- Use `SharedFlow` for one-time events (not state)
- Emit events after successful operations
- Log errors with context for debugging
- Update UI state synchronously for dialog dismissal

### 2. MealViewModel Data Refresh on Deletion

**Purpose:** Automatically refresh the meal list when a meal is deleted.

**Current Issue:** The ViewModel doesn't reload data after deletion, causing stale UI state.

**Solution:**

```kotlin
class MealViewModel(...) : ViewModel() {
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // Reload meals after successful deletion
                    loadMeals()
                    _errorEvent.emit(ErrorEvent.Success("Meal deleted"))
                },
                onFailure = { error ->
                    _errorEvent.emit(ErrorEvent.Error(error.message ?: "Failed to delete meal"))
                    errorLogger.logError(
                        error = error,
                        context = "MealViewModel.deleteMeal",
                        additionalData = mapOf("mealId" to mealId.toString())
                    )
                }
            )
        }
    }
    
    private fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { MealListUiState.Error(error.message ?: "Unknown error") }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            // Apply filters and update state
                            val filtered = applyFilters(meals)
                            _uiState.update { MealListUiState.Success(filtered) }
                        },
                        onFailure = { error ->
                            _uiState.update { MealListUiState.Error(error.message ?: "Unknown error") }
                        }
                    )
                }
        }
    }
}
```

**Key Design Decisions:**
- Call `loadMeals()` after successful deletion
- Preserve search query and tag filters during reload
- Handle errors without corrupting state

### 3. MealViewModel SavedStateHandle Integration

**Purpose:** Persist search query and selected tags across process death.

**Current Issue:** State is not properly saved/restored from SavedStateHandle.

**Solution:**

```kotlin
class MealViewModel(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val searchMealsUseCase: SearchMealsUseCase,
    private val filterMealsByTagsUseCase: FilterMealsByTagsUseCase,
    private val errorLogger: ErrorLogger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Restore from SavedStateHandle or use defaults
    private val _searchQuery = savedStateHandle.getStateFlow("search_query", "")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedTags = savedStateHandle.getStateFlow<Set<MealTag>>("selected_tags", emptySet())
    val selectedTags: StateFlow<Set<MealTag>> = _selectedTags.asStateFlow()
    
    fun updateSearchQuery(query: String) {
        savedStateHandle["search_query"] = query
        applyFiltersAndUpdate()
    }
    
    fun toggleTag(tag: MealTag) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        savedStateHandle["selected_tags"] = currentTags
        applyFiltersAndUpdate()
    }
}
```

**Key Design Decisions:**
- Use `SavedStateHandle.getStateFlow()` for automatic persistence
- Provide default values for missing keys
- Update SavedStateHandle immediately when state changes
- Preserve state even during error conditions

### 4. NavigationErrorHandler Implementation

**Purpose:** Provide safe navigation with automatic fallback on errors.

**Current Issue:** NavigationErrorHandler class doesn't exist or is incomplete.

**Solution:**

```kotlin
object NavigationErrorHandler {
    
    fun safeNavigate(
        navController: NavHostController,
        route: String,
        fallbackRoute: String,
        builder: (NavOptionsBuilder.() -> Unit)? = null
    ) {
        try {
            if (builder != null) {
                navController.navigate(route, builder)
            } else {
                navController.navigate(route)
            }
        } catch (e: Exception) {
            Timber.e(e, "Navigation failed for route: $route, using fallback: $fallbackRoute")
            try {
                navController.navigate(fallbackRoute)
            } catch (fallbackError: Exception) {
                Timber.e(fallbackError, "Fallback navigation also failed")
            }
        }
    }
    
    fun handleInvalidArguments(
        navController: NavHostController,
        route: String,
        arguments: Map<String, Any?>,
        fallbackRoute: String,
        exception: Exception
    ) {
        Timber.e(exception, "Invalid arguments for route $route: $arguments")
        navController.navigate(fallbackRoute)
    }
    
    fun handleMissingArguments(
        navController: NavHostController,
        route: String,
        requiredArgs: List<String>,
        fallbackRoute: String
    ) {
        Timber.e("Missing required arguments for route $route: $requiredArgs")
        navController.navigate(fallbackRoute)
    }
    
    fun handleInvalidRoute(
        navController: NavHostController,
        invalidRoute: String,
        fallbackRoute: String,
        exception: Exception
    ) {
        Timber.e(exception, "Invalid route: $invalidRoute")
        navController.navigate(fallbackRoute)
    }
    
    fun validateArguments(bundle: Bundle, requiredArgs: List<String>): List<String> {
        val errors = mutableListOf<String>()
        for (arg in requiredArgs) {
            if (!bundle.containsKey(arg)) {
                errors.add("Required argument '$arg' is missing")
            }
        }
        return errors
    }
}
```

**Key Design Decisions:**
- Use try-catch to handle navigation exceptions
- Always attempt fallback navigation on error
- Log all navigation errors for debugging
- Provide validation utilities for argument checking

## Data Models

### ErrorEvent (Existing)

```kotlin
sealed interface ErrorEvent {
    data class Error(val message: String) : ErrorEvent
    data class Success(val message: String) : ErrorEvent
}
```

### MealPlannerUiState Updates

```kotlin
data class MealPlannerUiState(
    val currentWeekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val weekData: WeekData? = null,
    val availableMeals: List<Meal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showMealSelection: Boolean = false,  // Add this
    val selectedSlot: SlotInfo? = null        // Add this
)

data class SlotInfo(
    val date: LocalDate,
    val mealType: MealType
)
```

## Error Handling

### ViewModel Error Handling Pattern

```kotlin
viewModelScope.launch {
    useCase(params).fold(
        onSuccess = { result ->
            // Update state
            _uiState.update { /* success state */ }
            // Emit success event
            _errorEvent.emit(ErrorEvent.Success("Operation successful"))
        },
        onFailure = { error ->
            // Emit error event
            _errorEvent.emit(ErrorEvent.Error(error.message ?: "Operation failed"))
            // Log error with context
            errorLogger.logError(
                error = error,
                context = "ClassName.methodName",
                additionalData = mapOf("param" to value.toString())
            )
        }
    )
}
```

### Navigation Error Handling Pattern

```kotlin
NavigationErrorHandler.safeNavigate(
    navController = navController,
    route = Screen.MealDetail.createRoute(mealId),
    fallbackRoute = Screen.MealList.route
)
```

## Testing Strategy

### Unit Test Verification

1. **MealPlannerViewModelTest**
   - Verify error events are emitted correctly
   - Verify error logging includes proper context
   - Verify dialog state management
   - Verify async operations complete before assertions

2. **StateManagementEdgeCasesTest**
   - Verify state updates after meal deletion
   - Verify SavedStateHandle integration
   - Verify state preservation across recreations
   - Verify error state doesn't corrupt saved state

3. **NavigationErrorHandlerTest**
   - Verify safe navigation with valid routes
   - Verify fallback navigation on errors
   - Verify error logging
   - Verify argument validation

### Test Execution

```bash
# Run all tests
.\gradlew.bat test

# Run specific test class
.\gradlew.bat test --tests "MealPlannerViewModelTest"

# Run with stacktrace for debugging
.\gradlew.bat test --stacktrace
```

## Implementation Notes

### Async Operation Handling

The tests use `advanceUntilIdle()` to wait for async operations. Ensure all coroutines are launched in `viewModelScope` so they complete during test execution.

### SavedStateHandle Best Practices

- Use `getStateFlow()` for automatic persistence
- Provide default values for all keys
- Update immediately when state changes
- Don't rely on SavedStateHandle for complex objects (use primitives and simple collections)

### Error Event Best Practices

- Use `SharedFlow` for one-time events
- Emit events after state updates
- Include meaningful messages for users
- Log errors with context for developers

## Migration Path

1. Add error event flow to MealPlannerViewModel
2. Update onMealSelected, deleteMealPlan, and selectSuggestion to emit events
3. Add dismissMealSelection method
4. Update MealViewModel to reload data after deletion
5. Integrate SavedStateHandle in MealViewModel
6. Create NavigationErrorHandler utility class
7. Run tests to verify all fixes
8. Update UI components to collect and display error events

## Dependencies

- Existing: kotlinx.coroutines, androidx.lifecycle, Timber
- No new dependencies required

## Performance Considerations

- Error events use `SharedFlow` with replay=0 to avoid memory leaks
- SavedStateHandle updates are synchronous and lightweight
- Navigation error handling adds minimal overhead (try-catch only)
- Data reload after deletion is necessary for correctness

## Security Considerations

- No security implications
- Error messages should not expose sensitive data
- Navigation errors are logged but not shown to users directly
