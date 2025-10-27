# Meal Planning API Reference

## Overview

This document provides API reference for the meal planning feature of Shoppit. Meal planning allows users to assign meals to specific dates and meal types (breakfast, lunch, dinner, snack) in a weekly calendar view.

**Package**: `com.shoppit.app.domain`

## Use Cases

### GetMealPlansForWeekUseCase

Retrieves all meal plans for a week (Monday to Sunday) with associated meal details.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class GetMealPlansForWeekUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository
)
```

#### invoke()

```kotlin
operator fun invoke(weekStart: LocalDate): Flow<Result<WeekPlanData>>
```

**Parameters**:
- `weekStart`: The Monday of the week to retrieve (must be a Monday)

**Returns**: Flow emitting `Result<WeekPlanData>` containing meal plans grouped by date

**Use Cases**:
- Display weekly meal planner calendar
- Navigate between weeks
- Show meal assignments for the week

**Example**:
```kotlin
class MealPlannerViewModel @Inject constructor(
    private val getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase
) : ViewModel() {
    
    fun loadWeek(weekStart: LocalDate) {
        viewModelScope.launch {
            getMealPlansForWeekUseCase(weekStart)
                .collect { result ->
                    result.fold(
                        onSuccess = { weekData ->
                            _uiState.update { MealPlannerUiState.Success(weekData) }
                        },
                        onFailure = { error ->
                            _uiState.update { MealPlannerUiState.Error(error.message ?: "Failed to load week") }
                        }
                    )
                }
        }
    }
}
```

---

### AssignMealToPlanUseCase

Assigns a meal to a specific date and meal type slot in the planner.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class AssignMealToPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(mealId: Long, date: LocalDate, mealType: MealType): Result<Long>
```

**Parameters**:
- `mealId`: ID of the meal to assign
- `date`: Date for the meal plan
- `mealType`: Type of meal (BREAKFAST, LUNCH, DINNER, SNACK)

**Returns**: `Result<Long>` containing the ID of the created meal plan

**Errors**:
- `DatabaseError`: Slot already occupied (unique constraint violation)
- `DatabaseError`: Meal doesn't exist (foreign key violation)
- `UnknownError`: Unexpected error

**Use Cases**:
- Assign meal to empty slot
- Replace meal in occupied slot (uses REPLACE strategy)

---

### UpdateMealPlanUseCase

Updates an existing meal plan to use a different meal.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class UpdateMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(planId: Long, newMealId: Long): Result<Unit>
```

**Parameters**:
- `planId`: ID of the meal plan to update
- `newMealId`: ID of the new meal to assign

**Returns**: `Result<Unit>` indicating success or failure

---

### DeleteMealPlanUseCase

Removes a meal plan from the planner.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class DeleteMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(planId: Long): Result<Unit>
```

**Parameters**:
- `planId`: ID of the meal plan to delete

**Returns**: `Result<Unit>` indicating success or failure

---

### CopyDayPlansUseCase

Copies all meal plans from one date to another.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class CopyDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(
    sourceDate: LocalDate,
    targetDate: LocalDate,
    replaceExisting: Boolean = false
): Result<Unit>
```

**Parameters**:
- `sourceDate`: Date to copy plans from
- `targetDate`: Date to copy plans to
- `replaceExisting`: If true, clears target date before copying

**Returns**: `Result<Unit>` indicating success or failure

---

### ClearDayPlansUseCase

Removes all meal plans for a specific date.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class ClearDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(date: LocalDate): Result<Unit>
```

**Parameters**:
- `date`: Date to clear all plans from

**Returns**: `Result<Unit>` indicating success or failure

---

## Repository Interface

### MealPlanRepository

Interface for meal plan data persistence and retrieval.

**Package**: `com.shoppit.app.domain.repository`

```kotlin
interface MealPlanRepository {
    fun getMealPlansForWeek(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<MealPlan>>>
    fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>>
    fun getMealPlanById(id: Long): Flow<Result<MealPlan>>
    suspend fun addMealPlan(mealPlan: MealPlan): Result<Long>
    suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<Unit>
    suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit>
    suspend fun deleteMealPlan(planId: Long): Result<Unit>
    suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit>
}
```

---

## Domain Models

### MealPlan

Represents a meal assignment to a specific date and meal type.

**Package**: `com.shoppit.app.domain.model`

```kotlin
data class MealPlan(
    val id: Long = 0,
    val mealId: Long,
    val date: LocalDate,
    val mealType: MealType
)
```

**Properties**:
- `id`: Unique identifier (0 for new plans, auto-generated on insert)
- `mealId`: Reference to the meal being planned
- `date`: Date for the meal plan
- `mealType`: Type of meal (BREAKFAST, LUNCH, DINNER, SNACK)

**Example**:
```kotlin
val plan = MealPlan(
    mealId = 5,
    date = LocalDate.of(2024, 1, 15),
    mealType = MealType.DINNER
)
```

---

### MealType

Enum representing the type of meal in a day.

**Package**: `com.shoppit.app.domain.model`

```kotlin
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;
    
    fun displayName(): String = when (this) {
        BREAKFAST -> "Breakfast"
        LUNCH -> "Lunch"
        DINNER -> "Dinner"
        SNACK -> "Snack"
    }
}
```

**Values**:
- `BREAKFAST`: Morning meal
- `LUNCH`: Midday meal
- `DINNER`: Evening meal
- `SNACK`: Snack or light meal

---

### MealPlanWithMeal

Combines a meal plan with its associated meal details.

**Package**: `com.shoppit.app.domain.model`

```kotlin
data class MealPlanWithMeal(
    val plan: MealPlan,
    val meal: Meal?
)
```

**Properties**:
- `plan`: The meal plan assignment
- `meal`: The associated meal details (null if meal was deleted)

**Example**:
```kotlin
val planWithMeal = MealPlanWithMeal(
    plan = MealPlan(id = 1, mealId = 5, date = LocalDate.now(), mealType = MealType.DINNER),
    meal = Meal(id = 5, name = "Spaghetti", ingredients = listOf(...))
)
```

---

### WeekPlanData

Contains all meal plans for a week, grouped by date.

**Package**: `com.shoppit.app.domain.model`

```kotlin
data class WeekPlanData(
    val weekStart: LocalDate,
    val plansByDate: Map<LocalDate, List<MealPlanWithMeal>>
)
```

**Properties**:
- `weekStart`: The Monday of the week
- `plansByDate`: Map of date to list of meal plans for that date

**Example**:
```kotlin
val weekData = WeekPlanData(
    weekStart = LocalDate.of(2024, 1, 15), // Monday
    plansByDate = mapOf(
        LocalDate.of(2024, 1, 15) to listOf(
            MealPlanWithMeal(plan1, meal1),
            MealPlanWithMeal(plan2, meal2)
        ),
        LocalDate.of(2024, 1, 16) to listOf(
            MealPlanWithMeal(plan3, meal3)
        )
    )
)
```

---

## UI State Models

### MealPlannerUiState

UI state for the meal planner screen.

**Package**: `com.shoppit.app.ui.planner`

```kotlin
data class MealPlannerUiState(
    val weekData: WeekPlanData? = null,
    val currentWeekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showMealSelection: Boolean = false,
    val selectedSlot: MealSlot? = null,
    val availableMeals: List<Meal> = emptyList()
)
```

**Properties**:
- `weekData`: Current week's meal plans
- `currentWeekStart`: Monday of the currently displayed week
- `isLoading`: Loading indicator
- `error`: Error message if any
- `showMealSelection`: Whether to show meal selection dialog
- `selectedSlot`: Currently selected slot for meal assignment
- `availableMeals`: List of meals available for selection

---

### MealSlot

Represents a calendar slot for meal assignment.

**Package**: `com.shoppit.app.ui.planner`

```kotlin
data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val existingPlan: MealPlanWithMeal? = null
)
```

**Properties**:
- `date`: Date of the slot
- `mealType`: Type of meal for the slot
- `existingPlan`: Existing meal plan if slot is occupied

---

## Common Patterns

### Loading Week Data

```kotlin
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentWeek()
    }
    
    private fun loadCurrentWeek() {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY)
        loadWeek(monday)
    }
    
    private fun loadWeek(weekStart: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentWeekStart = weekStart) }
            
            getMealPlansForWeekUseCase(weekStart)
                .collect { result ->
                    result.fold(
                        onSuccess = { weekData ->
                            _uiState.update {
                                it.copy(
                                    weekData = weekData,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load week"
                                )
                            }
                        }
                    )
                }
        }
    }
}
```

### Assigning Meal to Slot

```kotlin
fun onSlotClick(slot: MealSlot) {
    _uiState.update {
        it.copy(
            showMealSelection = true,
            selectedSlot = slot
        )
    }
}

fun onMealSelected(meal: Meal) {
    val slot = _uiState.value.selectedSlot ?: return
    
    viewModelScope.launch {
        assignMealToPlanUseCase(meal.id, slot.date, slot.mealType).fold(
            onSuccess = {
                _uiState.update {
                    it.copy(
                        showMealSelection = false,
                        selectedSlot = null
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        error = "Failed to assign meal: ${error.message}"
                    )
                }
            }
        )
    }
}
```

### Week Navigation

```kotlin
fun navigateToNextWeek() {
    val nextWeek = _uiState.value.currentWeekStart.plusWeeks(1)
    loadWeek(nextWeek)
}

fun navigateToPreviousWeek() {
    val previousWeek = _uiState.value.currentWeekStart.minusWeeks(1)
    loadWeek(previousWeek)
}

fun navigateToToday() {
    val monday = LocalDate.now().with(DayOfWeek.MONDAY)
    loadWeek(monday)
}
```

### Copying Day Plans

```kotlin
fun copyDay(sourceDate: LocalDate, targetDate: LocalDate, replace: Boolean) {
    viewModelScope.launch {
        copyDayPlansUseCase(sourceDate, targetDate, replace).fold(
            onSuccess = {
                // Plans copied successfully
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(error = "Failed to copy day: ${error.message}")
                }
            }
        )
    }
}
```

## See Also

- [API Reference](api-reference.md) - Main domain layer API
- [Database Schema](database-schema.md) - Room database schema
- [Architecture Overview](../architecture/overview.md) - Clean Architecture principles
- [Testing Guide](../guides/testing.md) - Testing strategies
