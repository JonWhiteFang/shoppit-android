# Domain Layer API Reference

## Overview

This document provides a comprehensive reference for the domain layer of Shoppit. The domain layer contains pure Kotlin code with no Android dependencies, implementing business logic, use cases, and domain models following Clean Architecture principles.

**Package**: `com.shoppit.app.domain`

## Use Cases

Use cases encapsulate single business operations and coordinate between repositories and validators. Each use case has a single responsibility and is invoked using the `operator fun invoke()` pattern.

### GetMealsUseCase

Retrieves all meals from the repository as a reactive stream.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class GetMealsUseCase @Inject constructor(
    private val repository: MealRepository
)
```

#### invoke()

```kotlin
operator fun invoke(): Flow<Result<List<Meal>>>
```

**Returns**: Flow emitting `Result<List<Meal>>` that updates whenever meals change

**Use Cases**:
- Display meal list in UI
- Search/filter meals
- Export meal data

**Example**:
```kotlin
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    init {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            _uiState.update { MealUiState.Success(meals) }
                        },
                        onFailure = { error ->
                            _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                        }
                    )
                }
        }
    }
}
```

**Requirements**: 2.1 - Retrieve all meals from database

---

### GetMealByIdUseCase

Retrieves a specific meal by its unique identifier as a reactive stream.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class GetMealByIdUseCase @Inject constructor(
    private val repository: MealRepository
)
```

#### invoke()

```kotlin
operator fun invoke(mealId: Long): Flow<Result<Meal>>
```

**Parameters**:
- `mealId`: Unique identifier of the meal to retrieve

**Returns**: Flow emitting `Result<Meal>` that updates whenever the meal changes

**Errors**:
- `DatabaseError`: Meal not found or database error
- `UnknownError`: Unexpected error

**Use Cases**:
- Display meal details
- Edit meal form
- View meal ingredients

**Example**:
```kotlin
class MealDetailViewModel @Inject constructor(
    private val getMealByIdUseCase: GetMealByIdUseCase
) : ViewModel() {
    
    fun loadMeal(mealId: Long) {
        viewModelScope.launch {
            getMealByIdUseCase(mealId)
                .collect { result ->
                    result.fold(
                        onSuccess = { meal ->
                            _uiState.update { MealDetailUiState.Success(meal) }
                        },
                        onFailure = { error ->
                            _uiState.update { MealDetailUiState.Error(error.message ?: "Meal not found") }
                        }
                    )
                }
        }
    }
}
```

**Requirements**: 3.1 - Retrieve specific meal from database by identifier

---

### AddMealUseCase

Adds a new meal after validating it meets business rules.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
)
```

#### invoke()

```kotlin
suspend operator fun invoke(meal: Meal): Result<Long>
```

**Parameters**:
- `meal`: The meal to add (ID will be auto-generated)

**Returns**: `Result<Long>` containing the ID of the newly created meal

**Validation Rules**:
- Meal name must not be blank
- Must have at least one ingredient
- All ingredient names must not be blank

**Errors**:
- `ValidationException`: Validation failed (see error message)
- `DatabaseError`: Database operation failed
- `UnknownError`: Unexpected error

**Use Cases**:
- Create new meal from UI form
- Import meals from external source
- Duplicate existing meal

**Example**:
```kotlin
class AddEditMealViewModel @Inject constructor(
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    
    fun saveMeal() {
        viewModelScope.launch {
            val meal = Meal(
                name = name.value,
                ingredients = ingredients.value,
                notes = notes.value
            )
            
            addMealUseCase(meal).fold(
                onSuccess = { mealId ->
                    _uiState.update { AddEditMealUiState.Success(mealId) }
                },
                onFailure = { error ->
                    _uiState.update { AddEditMealUiState.Error(error.message ?: "Failed to save meal") }
                }
            )
        }
    }
}
```

**Requirements**:
- 1.1: Validate meal name contains at least one non-whitespace character
- 1.2: Validate at least one ingredient is included
- 1.3: Persist meal to database with unique identifier
- 1.4: Display confirmation message on success

---

### UpdateMealUseCase

Updates an existing meal after validating it meets business rules.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class UpdateMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
)
```

#### invoke()

```kotlin
suspend operator fun invoke(meal: Meal): Result<Unit>
```

**Parameters**:
- `meal`: The meal with updated data (must have valid ID > 0)

**Returns**: `Result<Unit>` indicating success or failure

**Validation Rules**:
- Same as AddMealUseCase
- Meal ID must be valid (> 0)

**Errors**:
- `ValidationException`: Validation failed
- `DatabaseError`: Meal not found or database error
- `UnknownError`: Unexpected error

**Use Cases**:
- Edit existing meal
- Update meal ingredients
- Modify meal notes

**Example**:
```kotlin
class AddEditMealViewModel @Inject constructor(
    private val updateMealUseCase: UpdateMealUseCase
) : ViewModel() {
    
    fun updateMeal(existingMeal: Meal) {
        viewModelScope.launch {
            val updatedMeal = existingMeal.copy(
                name = name.value,
                ingredients = ingredients.value,
                notes = notes.value,
                updatedAt = System.currentTimeMillis()
            )
            
            updateMealUseCase(updatedMeal).fold(
                onSuccess = {
                    _uiState.update { AddEditMealUiState.Success(updatedMeal.id) }
                },
                onFailure = { error ->
                    _uiState.update { AddEditMealUiState.Error(error.message ?: "Failed to update meal") }
                }
            )
        }
    }
}
```

**Requirements**:
- 4.2: Validate updated meal data
- 4.3: Update meal in database preserving original identifier
- 4.4: Display confirmation message on success

---

### DeleteMealUseCase

Deletes a meal from the repository.

**Package**: `com.shoppit.app.domain.usecase`

```kotlin
class DeleteMealUseCase @Inject constructor(
    private val repository: MealRepository
)
```

#### invoke()

```kotlin
suspend operator fun invoke(mealId: Long): Result<Unit>
```

**Parameters**:
- `mealId`: The ID of the meal to delete

**Returns**: `Result<Unit>` indicating success or failure

**Errors**:
- `DatabaseError`: Database operation failed
- `UnknownError`: Unexpected error

**Note**: Silently succeeds if meal doesn't exist

**Use Cases**:
- Delete meal from list
- Remove meal after confirmation dialog
- Bulk delete operations

**Example**:
```kotlin
class MealViewModel @Inject constructor(
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // Meal deleted, list will update automatically via Flow
                },
                onFailure = { error ->
                    _uiState.update { 
                        MealUiState.Error(error.message ?: "Failed to delete meal") 
                    }
                }
            )
        }
    }
}
```

**Requirements**: 5.2 - Remove meal from database when user confirms deletion

---

## Repository Interfaces

Repository interfaces define contracts for data operations. Implementations are in the data layer.

### MealRepository

Interface for meal data persistence and retrieval.

**Package**: `com.shoppit.app.domain.repository`

```kotlin
interface MealRepository {
    fun getMeals(): Flow<Result<List<Meal>>>
    fun getMealById(id: Long): Flow<Result<Meal>>
    suspend fun addMeal(meal: Meal): Result<Long>
    suspend fun updateMeal(meal: Meal): Result<Unit>
    suspend fun deleteMeal(mealId: Long): Result<Unit>
}
```

#### getMeals()

```kotlin
fun getMeals(): Flow<Result<List<Meal>>>
```

Retrieves all meals as a reactive Flow. Emits new list whenever meals change.

**Returns**: Flow emitting `Result<List<Meal>>`

---

#### getMealById()

```kotlin
fun getMealById(id: Long): Flow<Result<Meal>>
```

Retrieves a specific meal by ID as a reactive Flow.

**Parameters**:
- `id`: Unique identifier of the meal

**Returns**: Flow emitting `Result<Meal>` or error if not found

---

#### addMeal()

```kotlin
suspend fun addMeal(meal: Meal): Result<Long>
```

Adds a new meal to the repository.

**Parameters**:
- `meal`: The meal to add

**Returns**: `Result<Long>` with the ID of the newly created meal

---

#### updateMeal()

```kotlin
suspend fun updateMeal(meal: Meal): Result<Unit>
```

Updates an existing meal.

**Parameters**:
- `meal`: The meal with updated data (must have valid ID)

**Returns**: `Result<Unit>` indicating success or failure

---

#### deleteMeal()

```kotlin
suspend fun deleteMeal(mealId: Long): Result<Unit>
```

Deletes a meal by ID.

**Parameters**:
- `mealId`: The ID of the meal to delete

**Returns**: `Result<Unit>` indicating success or failure

---

## Domain Models

Domain models are pure Kotlin data classes with no Android dependencies.

### Meal

Represents a meal with its ingredients and metadata.

**Package**: `com.shoppit.app.domain.model`

```kotlin
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

**Properties**:
- `id`: Unique identifier (0 for new meals, auto-generated on insert)
- `name`: Name of the meal (required, non-blank)
- `ingredients`: List of ingredients (required, non-empty)
- `notes`: Optional cooking instructions or notes
- `createdAt`: Unix timestamp (milliseconds) when meal was created
- `updatedAt`: Unix timestamp (milliseconds) when meal was last updated

**Example**:
```kotlin
val meal = Meal(
    name = "Spaghetti Carbonara",
    ingredients = listOf(
        Ingredient("Spaghetti", "400", "g"),
        Ingredient("Eggs", "4", "pcs"),
        Ingredient("Parmesan", "100", "g"),
        Ingredient("Pancetta", "150", "g")
    ),
    notes = "Cook pasta al dente. Mix eggs with cheese before adding to hot pasta."
)
```

---

### Ingredient

Represents an ingredient within a meal.

**Package**: `com.shoppit.app.domain.model`

```kotlin
data class Ingredient(
    val name: String,
    val quantity: String = "",
    val unit: String = ""
)
```

**Properties**:
- `name`: Name of the ingredient (required, non-blank)
- `quantity`: Amount of ingredient (optional, e.g., "2", "1.5")
- `unit`: Unit of measurement (optional, e.g., "cups", "grams", "pieces")

**Example**:
```kotlin
val ingredient1 = Ingredient("Flour", "2", "cups")
val ingredient2 = Ingredient("Salt", "1", "tsp")
val ingredient3 = Ingredient("Eggs", "3", "pcs")
```

---

## Error Types

Error types provide type-safe error handling across all layers.

### AppError

Sealed class representing all possible application errors.

**Package**: `com.shoppit.app.domain.error`

```kotlin
sealed class AppError {
    data object NetworkError : AppError()
    data object DatabaseError : AppError()
    data class ValidationError(val message: String) : AppError()
    data class UnknownError(val throwable: Throwable) : AppError()
}
```

#### NetworkError

Network-related errors (connectivity, timeouts, server errors).

**Use Cases**:
- No internet connection
- API request timeout
- Server returned error response

---

#### DatabaseError

Database-related errors (query failures, constraint violations).

**Use Cases**:
- Database query failed
- Constraint violation (e.g., duplicate key)
- Database file corrupted
- Record not found

---

#### ValidationError

Validation errors with descriptive messages.

**Properties**:
- `message`: Human-readable description of validation failure

**Use Cases**:
- Empty meal name
- No ingredients provided
- Invalid ingredient data

**Example**:
```kotlin
AppError.ValidationError("Meal name cannot be empty")
AppError.ValidationError("Meal must have at least one ingredient")
```

---

#### UnknownError

Unknown or unexpected errors with the original throwable.

**Properties**:
- `throwable`: The original exception that was caught

**Use Cases**:
- Unexpected exceptions
- Third-party library errors
- System errors

---

### ValidationException

Exception thrown when validation fails.

**Package**: `com.shoppit.app.domain.validator`

```kotlin
class ValidationException(message: String) : Exception(message)
```

**Properties**:
- `message`: Description of validation failure

**Example**:
```kotlin
throw ValidationException("Meal name cannot be empty")
throw ValidationException("Ingredient name cannot be empty")
```

---

## Validators

Validators ensure domain objects meet business rules before persistence.

### MealValidator

Validates meal domain objects.

**Package**: `com.shoppit.app.domain.validator`

```kotlin
class MealValidator @Inject constructor()
```

#### validate()

```kotlin
fun validate(meal: Meal): Result<Unit>
```

**Parameters**:
- `meal`: The meal to validate

**Returns**: `Result<Unit>` indicating success or validation error

**Validation Rules**:
1. Meal name must contain at least one non-whitespace character
2. Meal must have at least one ingredient
3. Each ingredient must have a non-empty name

**Errors**:
- `ValidationException("Meal name cannot be empty")`
- `ValidationException("Meal must have at least one ingredient")`
- `ValidationException("Ingredient name cannot be empty")`

**Example**:
```kotlin
val validator = MealValidator()

// Valid meal
val validMeal = Meal(
    name = "Pasta",
    ingredients = listOf(Ingredient("Pasta", "400", "g"))
)
validator.validate(validMeal) // Success

// Invalid meal - empty name
val invalidMeal1 = Meal(
    name = "   ",
    ingredients = listOf(Ingredient("Pasta", "400", "g"))
)
validator.validate(invalidMeal1) // Failure: "Meal name cannot be empty"

// Invalid meal - no ingredients
val invalidMeal2 = Meal(
    name = "Pasta",
    ingredients = emptyList()
)
validator.validate(invalidMeal2) // Failure: "Meal must have at least one ingredient"

// Invalid meal - empty ingredient name
val invalidMeal3 = Meal(
    name = "Pasta",
    ingredients = listOf(Ingredient("", "400", "g"))
)
validator.validate(invalidMeal3) // Failure: "Ingredient name cannot be empty"
```

---

## Common Patterns

### Using Use Cases in ViewModels

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
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
                    result.fold(
                        onSuccess = { meals ->
                            _uiState.update { MealUiState.Success(meals) }
                        },
                        onFailure = { error ->
                            _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                        }
                    )
                }
        }
    }
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = {
                    // List updates automatically via Flow
                },
                onFailure = { error ->
                    _uiState.update { MealUiState.Error(error.message ?: "Failed to delete") }
                }
            )
        }
    }
}
```

### Error Handling Pattern

```kotlin
// In repository implementation
override suspend fun addMeal(meal: Meal): Result<Long> {
    return try {
        val id = mealDao.insertMeal(meal.toEntity())
        Result.success(id)
    } catch (e: SQLiteException) {
        Result.failure(AppError.DatabaseError)
    } catch (e: Exception) {
        Result.failure(AppError.UnknownError(e))
    }
}

// In ViewModel
addMealUseCase(meal).fold(
    onSuccess = { mealId ->
        // Handle success
    },
    onFailure = { error ->
        val message = when (error) {
            is ValidationException -> error.message
            is AppError.DatabaseError -> "Database error occurred"
            is AppError.NetworkError -> "Network error occurred"
            is AppError.ValidationError -> error.message
            is AppError.UnknownError -> "Unexpected error: ${error.throwable.message}"
            else -> "Unknown error"
        }
        _uiState.update { UiState.Error(message) }
    }
)
```

### Creating Domain Models

```kotlin
// Creating a new meal
val newMeal = Meal(
    name = "Chicken Curry",
    ingredients = listOf(
        Ingredient("Chicken breast", "500", "g"),
        Ingredient("Curry powder", "2", "tbsp"),
        Ingredient("Coconut milk", "400", "ml"),
        Ingredient("Onion", "1", "pcs")
    ),
    notes = "Marinate chicken for 30 minutes before cooking"
)

// Updating an existing meal
val updatedMeal = existingMeal.copy(
    name = "Spicy Chicken Curry",
    notes = "Marinate chicken for 1 hour. Add extra chili for heat.",
    updatedAt = System.currentTimeMillis()
)
```

### Validation Before Persistence

```kotlin
// Always validate before persisting
suspend fun saveMeal(meal: Meal) {
    validator.validate(meal).fold(
        onSuccess = {
            repository.addMeal(meal).fold(
                onSuccess = { id -> /* Success */ },
                onFailure = { error -> /* Handle error */ }
            )
        },
        onFailure = { error ->
            // Show validation error to user
            _uiState.update { UiState.Error(error.message ?: "Validation failed") }
        }
    )
}
```

## Testing Domain Layer

### Testing Use Cases

```kotlin
class AddMealUseCaseTest {
    private lateinit var repository: MealRepository
    private lateinit var validator: MealValidator
    private lateinit var useCase: AddMealUseCase
    
    @Before
    fun setup() {
        repository = mockk()
        validator = MealValidator()
        useCase = AddMealUseCase(repository, validator)
    }
    
    @Test
    fun `adds valid meal successfully`() = runTest {
        // Given
        val meal = Meal(
            name = "Pasta",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        coEvery { repository.addMeal(meal) } returns Result.success(1L)
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { repository.addMeal(meal) }
    }
    
    @Test
    fun `returns validation error for empty name`() = runTest {
        // Given
        val meal = Meal(
            name = "",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationException)
        coVerify(exactly = 0) { repository.addMeal(any()) }
    }
}
```

### Testing Validators

```kotlin
class MealValidatorTest {
    private lateinit var validator: MealValidator
    
    @Before
    fun setup() {
        validator = MealValidator()
    }
    
    @Test
    fun `validates meal with valid data`() {
        val meal = Meal(
            name = "Pasta",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        val result = validator.validate(meal)
        
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `fails validation for blank name`() {
        val meal = Meal(
            name = "   ",
            ingredients = listOf(Ingredient("Pasta", "400", "g"))
        )
        
        val result = validator.validate(meal)
        
        assertTrue(result.isFailure)
        assertEquals("Meal name cannot be empty", result.exceptionOrNull()?.message)
    }
}
```

## See Also

- [Architecture Overview](../architecture/overview.md) - Clean Architecture principles
- [Data Flow](../architecture/data-flow.md) - How data moves through layers
- [Database Schema](database-schema.md) - Room database schema
- [Testing Guide](../guides/testing.md) - Testing strategies
- [Dependency Injection](../guides/dependency-injection.md) - Hilt DI patterns
