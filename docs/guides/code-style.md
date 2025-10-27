# Code Style Guide

This guide defines the coding conventions and style guidelines for the Shoppit Android project.

## Overview

Consistent code style improves readability, maintainability, and collaboration. Shoppit follows standard Kotlin conventions with project-specific patterns for Clean Architecture.

### Key Principles

- **Readability first** - Code is read more often than written
- **Consistency** - Follow established patterns
- **Simplicity** - Prefer simple solutions over clever ones
- **Expressiveness** - Use meaningful names and clear structure

## File Naming Conventions

Use descriptive, PascalCase names that clearly indicate the file's purpose:

### UI Layer

```kotlin
// Screens - composable functions that represent full screens
MealListScreen.kt
MealDetailScreen.kt
AddMealScreen.kt

// ViewModels - manage UI state and handle user actions
MealViewModel.kt
MealListViewModel.kt
MealDetailViewModel.kt

// UI State - sealed classes or data classes for UI state
MealUiState.kt
MealListUiState.kt

// Reusable composables
MealCard.kt
IngredientList.kt
EmptyState.kt
```

### Domain Layer

```kotlin
// Domain models - plain names, no suffixes
Meal.kt
Ingredient.kt
MealPlan.kt
ShoppingListItem.kt

// Use cases - [Action][Entity]UseCase pattern
AddMealUseCase.kt
GetMealsUseCase.kt
DeleteMealUseCase.kt
UpdateMealUseCase.kt

// Repository interfaces - [Entity]Repository pattern
MealRepository.kt
MealPlanRepository.kt
ShoppingListRepository.kt

// Validators - [Entity]Validator pattern
MealValidator.kt
IngredientValidator.kt
```

### Data Layer

```kotlin
// Repository implementations - [Entity]RepositoryImpl pattern
MealRepositoryImpl.kt
MealPlanRepositoryImpl.kt

// Room entities - [Entity]Entity pattern
MealEntity.kt
MealPlanEntity.kt
IngredientEntity.kt

// DAOs - [Entity]Dao pattern
MealDao.kt
MealPlanDao.kt
ShoppingListDao.kt

// Database - [App]Database pattern
ShoppitDatabase.kt

// DTOs - [Entity]Dto pattern (for network)
MealDto.kt
IngredientDto.kt

// Mappers - [Entity]Mapper pattern
MealMapper.kt
IngredientMapper.kt
```

### DI Layer

```kotlin
// Modules - [Purpose]Module pattern
DatabaseModule.kt
NetworkModule.kt
RepositoryModule.kt
UseCaseModule.kt
```

## Package Organization

### Package Structure

```
com.shoppit.app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entity/
│   │   └── database/
│   ├── remote/
│   │   ├── api/
│   │   └── dto/
│   ├── repository/
│   └── mapper/
├── domain/
│   ├── model/
│   ├── repository/
│   ├── usecase/
│   └── validator/
├── ui/
│   ├── meal/
│   ├── planner/
│   ├── shopping/
│   ├── common/
│   ├── navigation/
│   └── theme/
└── di/
```

### Package Naming

- Use lowercase, no underscores
- Use singular nouns: `model` not `models`
- Group by feature in UI layer: `ui/meal/`, `ui/planner/`
- Group by type in other layers: `domain/usecase/`, `data/repository/`

## Kotlin Conventions

### Variables and Constants

```kotlin
// Use val over var whenever possible
val userName = "John Doe"  // Good
var userName = "John Doe"  // Avoid unless mutability is needed

// Constants - UPPER_SNAKE_CASE in companion object
companion object {
    private const val MAX_MEAL_NAME_LENGTH = 100
    private const val DATABASE_NAME = "shoppit_database"
}

// Private constants at file level
private const val TAG = "MealViewModel"
private const val DEFAULT_PAGE_SIZE = 20

// Use meaningful names
val mealCount = meals.size  // Good
val c = meals.size          // Bad

// Boolean variables - use is/has/can prefix
val isLoading = true
val hasIngredients = meal.ingredients.isNotEmpty()
val canDelete = meal.id > 0
```

### Functions

```kotlin
// Use expression bodies for single-expression functions
fun getMealName(meal: Meal): String = meal.name

// Use block body for complex functions
fun validateMeal(meal: Meal): Result<Unit> {
    if (meal.name.isBlank()) {
        return Result.failure(ValidationError("Meal name cannot be empty"))
    }
    if (meal.name.length > MAX_MEAL_NAME_LENGTH) {
        return Result.failure(ValidationError("Meal name too long"))
    }
    return Result.success(Unit)
}

// Use default parameters instead of overloads
fun loadMeals(
    sortBy: SortOrder = SortOrder.NAME,
    filterFavorites: Boolean = false
) {
    // Implementation
}

// Use named parameters for clarity
loadMeals(
    sortBy = SortOrder.DATE,
    filterFavorites = true
)

// Extension functions for utility operations
fun Meal.toEntity(): MealEntity = MealEntity(
    id = id,
    name = name,
    createdAt = System.currentTimeMillis()
)
```

### Classes and Data Classes

```kotlin
// Data classes for immutable data
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val isFavorite: Boolean = false
)

// Use trailing commas in multi-line declarations
data class MealPlan(
    val id: Long = 0,
    val date: Long,
    val mealType: MealType,
    val mealId: Long,
)

// Sealed classes for state
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}

// Use sealed interface over sealed class for better flexibility
sealed interface AppError {
    data class NetworkError(val message: String) : AppError
    data class DatabaseError(val message: String) : AppError
    data class ValidationError(val message: String) : AppError
}

// Constructor injection with @Inject
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
) : MealRepository {
    // Implementation
}
```

### Properties

```kotlin
// Use explicit types for public APIs
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    // Public immutable state - explicit type
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    // Infer types for private/local variables
    private val meals = mutableListOf<Meal>()
}

// Use backing properties for encapsulation
private val _items = mutableListOf<Item>()
val items: List<Item> = _items

// Lazy initialization
val database: AppDatabase by lazy {
    Room.databaseBuilder(context, AppDatabase::class.java, "shoppit.db").build()
}
```

### Control Flow

```kotlin
// Use when instead of if-else chains
when (mealType) {
    MealType.BREAKFAST -> "Breakfast"
    MealType.LUNCH -> "Lunch"
    MealType.DINNER -> "Dinner"
    MealType.SNACK -> "Snack"
}

// Use when as expression
val mealTypeName = when (mealType) {
    MealType.BREAKFAST -> "Breakfast"
    MealType.LUNCH -> "Lunch"
    MealType.DINNER -> "Dinner"
    MealType.SNACK -> "Snack"
}

// Use elvis operator for null handling
val name = meal?.name ?: "Unknown"

// Use safe calls and let for null checks
meal?.let { 
    saveMeal(it)
}

// Use require() for preconditions
fun addMeal(meal: Meal) {
    require(meal.name.isNotBlank()) { "Meal name cannot be blank" }
    // Implementation
}

// Use check() for state validation
fun deleteMeal(mealId: Long) {
    check(mealId > 0) { "Invalid meal ID" }
    // Implementation
}
```

### Collections

```kotlin
// Use collection literals
val meals = listOf(meal1, meal2, meal3)
val mealMap = mapOf("breakfast" to meal1, "lunch" to meal2)

// Use collection operations
val favoriteMeals = meals.filter { it.isFavorite }
val mealNames = meals.map { it.name }
val totalIngredients = meals.flatMap { it.ingredients }

// Use sequences for large collections
meals.asSequence()
    .filter { it.isFavorite }
    .map { it.name }
    .toList()

// Use trailing commas in multi-line collections
val meals = listOf(
    meal1,
    meal2,
    meal3,
)
```

## Compose Best Practices

### Composable Functions

```kotlin
// Composable function naming - PascalCase, noun
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Content
    }
}

// Modifier parameter last with default value
@Composable
fun MealList(
    meals: List<Meal>,
    onMealClick: (Meal) -> Unit,
    modifier: Modifier = Modifier  // Always last, always default
) {
    LazyColumn(modifier = modifier) {
        items(meals) { meal ->
            MealCard(meal = meal, onClick = { onMealClick(meal) })
        }
    }
}

// State hoisting - stateless composables
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is MealUiState.Loading -> LoadingScreen()
        is MealUiState.Success -> MealList(
            meals = uiState.meals,
            onMealClick = onMealClick
        )
        is MealUiState.Error -> ErrorScreen(message = uiState.message)
    }
}

// Stateful screen composable
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealClick: (Meal) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = onMealClick,
        onAddMealClick = viewModel::addMeal
    )
}
```

### State Management

```kotlin
// Use remember for expensive computations
@Composable
fun MealList(meals: List<Meal>) {
    val sortedMeals = remember(meals) {
        meals.sortedBy { it.name }
    }
    // Use sortedMeals
}

// Use derivedStateOf for computed values
@Composable
fun ShoppingList(items: List<ShoppingListItem>) {
    val uncheckedCount by remember {
        derivedStateOf {
            items.count { !it.isChecked }
        }
    }
    Text("$uncheckedCount items remaining")
}

// Use rememberCoroutineScope for event handlers
@Composable
fun MealScreen() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    Button(
        onClick = {
            scope.launch {
                snackbarHostState.showSnackbar("Meal saved")
            }
        }
    ) {
        Text("Save")
    }
}
```

## Architecture Patterns

### ViewModels

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    init {
        loadMeals()
    }
    
    // Public functions for user actions
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
    
    fun addMeal(meal: Meal) {
        viewModelScope.launch {
            addMealUseCase(meal).fold(
                onSuccess = { loadMeals() },
                onFailure = { _uiState.update { MealUiState.Error(it.message ?: "Failed to add meal") } }
            )
        }
    }
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = { loadMeals() },
                onFailure = { _uiState.update { MealUiState.Error(it.message ?: "Failed to delete meal") } }
            )
        }
    }
}
```

### Use Cases

```kotlin
// Single responsibility - one public operator function
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(meal: Meal): Result<Long> {
        // Validation
        if (meal.name.isBlank()) {
            return Result.failure(ValidationError("Meal name cannot be empty"))
        }
        if (meal.name.length > MAX_MEAL_NAME_LENGTH) {
            return Result.failure(ValidationError("Meal name too long"))
        }
        
        // Delegate to repository
        return repository.addMeal(meal)
    }
    
    companion object {
        private const val MAX_MEAL_NAME_LENGTH = 100
    }
}
```

### Repositories

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        mealDao.getAllMeals()
            .catch { e ->
                emit(Result.failure(AppError.DatabaseError(e.message ?: "Database error")))
            }
            .collect { entities ->
                emit(Result.success(entities.map { it.toDomainModel() }))
            }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = mealDao.insertMeal(meal.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to add meal"))
        }
    }
    
    override suspend fun deleteMeal(mealId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            mealDao.deleteMealById(mealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to delete meal"))
        }
    }
}
```

## Documentation

### KDoc Comments

```kotlin
/**
 * Represents a meal with ingredients and optional notes.
 *
 * @property id Unique identifier for the meal
 * @property name Display name of the meal
 * @property ingredients List of ingredients required for the meal
 * @property notes Optional cooking notes or instructions
 * @property isFavorite Whether the meal is marked as favorite
 */
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val isFavorite: Boolean = false
)

/**
 * Adds a new meal to the repository.
 *
 * Validates the meal before adding. Returns the ID of the newly created meal
 * on success, or an error if validation fails or database operation fails.
 *
 * @param meal The meal to add
 * @return Result containing the meal ID on success, or error on failure
 */
suspend fun addMeal(meal: Meal): Result<Long>
```

### Inline Comments

```kotlin
// Use comments to explain why, not what
fun calculateShoppingList(mealPlans: List<MealPlan>): List<ShoppingListItem> {
    // Group ingredients by name to aggregate quantities
    // This handles cases where the same ingredient appears in multiple meals
    val ingredientMap = mutableMapOf<String, MutableList<Ingredient>>()
    
    mealPlans.forEach { plan ->
        plan.meal.ingredients.forEach { ingredient ->
            ingredientMap.getOrPut(ingredient.name) { mutableListOf() }.add(ingredient)
        }
    }
    
    // Convert to shopping list items with aggregated quantities
    return ingredientMap.map { (name, ingredients) ->
        ShoppingListItem(
            name = name,
            quantity = aggregateQuantities(ingredients),
            isChecked = false
        )
    }
}
```

## Formatting

### Indentation and Spacing

```kotlin
// 4 spaces for indentation (no tabs)
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    fun loadMeals() {
        viewModelScope.launch {
            // Code
        }
    }
}

// Blank line between functions
fun function1() {
    // Code
}

fun function2() {
    // Code
}

// No blank line between properties
val property1 = "value1"
val property2 = "value2"
```

### Line Length

```kotlin
// Max 120 characters per line
// Break long lines at logical points

// Good
val meal = Meal(
    id = 1,
    name = "Spaghetti Carbonara",
    ingredients = listOf(ingredient1, ingredient2),
    notes = "Cook pasta al dente"
)

// Break long function calls
repository.addMeal(
    meal = meal,
    onSuccess = { id -> handleSuccess(id) },
    onError = { error -> handleError(error) }
)
```

### Import Organization

```kotlin
// Android imports
import android.content.Context
import android.os.Bundle

// AndroidX imports
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.lifecycle.ViewModel

// Third-party imports
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Project imports
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.usecase.GetMealsUseCase
```

## Anti-Patterns to Avoid

### Don't

❌ **Expose mutable state from ViewModels**
```kotlin
// Bad
val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)

// Good
private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
```

❌ **Use magic numbers**
```kotlin
// Bad
if (meal.name.length > 100) { }

// Good
private const val MAX_MEAL_NAME_LENGTH = 100
if (meal.name.length > MAX_MEAL_NAME_LENGTH) { }
```

❌ **Create God classes**
```kotlin
// Bad - one class doing everything
class MealManager {
    fun addMeal() { }
    fun deleteMeal() { }
    fun updateMeal() { }
    fun getMeals() { }
    fun validateMeal() { }
    fun exportMeals() { }
}

// Good - separate responsibilities
class AddMealUseCase
class DeleteMealUseCase
class GetMealsUseCase
class MealValidator
```

❌ **Use var when val is sufficient**
```kotlin
// Bad
var meals = repository.getMeals()

// Good
val meals = repository.getMeals()
```

❌ **Ignore exceptions**
```kotlin
// Bad
try {
    saveMeal(meal)
} catch (e: Exception) {
    // Ignored
}

// Good
try {
    saveMeal(meal)
} catch (e: Exception) {
    Timber.e(e, "Failed to save meal")
    return Result.failure(AppError.DatabaseError(e.message))
}
```

## Code Review Checklist

When reviewing code, check for:

✅ **Naming**
- Descriptive variable and function names
- Follows naming conventions
- No abbreviations or unclear names

✅ **Structure**
- Proper package organization
- Correct file naming
- Follows Clean Architecture layers

✅ **Kotlin Style**
- Uses `val` over `var`
- Expression bodies for simple functions
- Trailing commas in multi-line declarations

✅ **Compose**
- Modifier parameter last with default
- State hoisting applied
- Stateless composables where possible

✅ **Architecture**
- ViewModels expose immutable state
- Use cases have single responsibility
- Repositories catch and map exceptions

✅ **Documentation**
- Complex logic has comments
- Public APIs have KDoc
- Comments explain why, not what

✅ **Testing**
- Unit tests for business logic
- Tests follow naming conventions
- Tests are maintainable

## Tools and Automation

### ktlint

Format code automatically:

```bash
# Check code style
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

### Android Studio

Configure Android Studio:
1. File → Settings → Editor → Code Style → Kotlin
2. Set from: Kotlin style guide
3. Tabs and Indents: 4 spaces
4. Wrapping and Braces: 120 characters

### EditorConfig

The project includes `.editorconfig`:

```ini
[*.kt]
indent_size = 4
max_line_length = 120
insert_final_newline = true
```

## Further Reading

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)
- [Compose API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
- [Architecture Overview](../architecture/overview.md) - Understanding project structure
- [Getting Started Guide](getting-started.md) - Project setup and conventions
