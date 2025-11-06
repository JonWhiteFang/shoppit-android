# Analyzer Reference

This document provides detailed information about each analyzer in the Code Quality Analysis System, including what they check, examples of violations, and how to fix them.

## Table of Contents

1. [Code Smell Analyzer](#code-smell-analyzer)
2. [Architecture Analyzer](#architecture-analyzer)
3. [Compose Analyzer](#compose-analyzer)
4. [State Management Analyzer](#state-management-analyzer)
5. [Error Handling Analyzer](#error-handling-analyzer)
6. [Dependency Injection Analyzer](#dependency-injection-analyzer)
7. [Database Analyzer](#database-analyzer)
8. [Performance Analyzer](#performance-analyzer)
9. [Naming Analyzer](#naming-analyzer)
10. [Test Coverage Analyzer](#test-coverage-analyzer)
11. [Documentation Analyzer](#documentation-analyzer)
12. [Security Analyzer](#security-analyzer)

---

## Code Smell Analyzer

**ID**: `code-smell`  
**Category**: CODE_SMELL  
**Priority**: MEDIUM

### What It Checks

The Code Smell Analyzer detects common code smells that indicate potential maintainability issues:

- Long functions (> 50 lines)
- Large classes (> 300 lines)
- Too many parameters (> 5)
- High cyclomatic complexity (> 15)
- Deep nesting (> 4 levels)
- Duplicate code blocks
- Magic numbers

### Examples

#### Long Function

**Violation**:
```kotlin
fun processOrder(order: Order) {
    // 60+ lines of code
    validateOrder(order)
    calculateTotals(order)
    applyDiscounts(order)
    processPayment(order)
    updateInventory(order)
    sendConfirmation(order)
    logTransaction(order)
    // ... many more lines
}
```

**Fix**:
```kotlin
fun processOrder(order: Order) {
    validateOrder(order)
    val totals = calculateTotals(order)
    val discountedOrder = applyDiscounts(order, totals)
    processPayment(discountedOrder)
    updateInventory(discountedOrder)
    sendConfirmation(discountedOrder)
    logTransaction(discountedOrder)
}

// Extract helper functions
private fun calculateTotals(order: Order): OrderTotals { /* ... */ }
private fun applyDiscounts(order: Order, totals: OrderTotals): Order { /* ... */ }
```

#### Too Many Parameters

**Violation**:
```kotlin
fun createMeal(
    name: String,
    description: String,
    ingredients: List<Ingredient>,
    prepTime: Int,
    cookTime: Int,
    servings: Int,
    difficulty: String
) {
    // Implementation
}
```

**Fix**:
```kotlin
data class MealDetails(
    val name: String,
    val description: String,
    val ingredients: List<Ingredient>,
    val prepTime: Int,
    val cookTime: Int,
    val servings: Int,
    val difficulty: String
)

fun createMeal(details: MealDetails) {
    // Implementation
}
```

#### High Cyclomatic Complexity

**Violation**:
```kotlin
fun calculateDiscount(order: Order): Double {
    var discount = 0.0
    
    if (order.total > 100) {
        if (order.customer.isPremium) {
            if (order.items.size > 5) {
                discount = 0.20
            } else if (order.items.size > 3) {
                discount = 0.15
            } else {
                discount = 0.10
            }
        } else {
            if (order.items.size > 5) {
                discount = 0.10
            } else {
                discount = 0.05
            }
        }
    } else if (order.total > 50) {
        if (order.customer.isPremium) {
            discount = 0.10
        } else {
            discount = 0.05
        }
    }
    
    return discount
}
```

**Fix**:
```kotlin
fun calculateDiscount(order: Order): Double {
    return when {
        order.total > 100 -> calculateLargeOrderDiscount(order)
        order.total > 50 -> calculateMediumOrderDiscount(order)
        else -> 0.0
    }
}

private fun calculateLargeOrderDiscount(order: Order): Double {
    val baseDiscount = if (order.customer.isPremium) 0.10 else 0.05
    val itemBonus = when {
        order.items.size > 5 -> 0.10
        order.items.size > 3 -> 0.05
        else -> 0.0
    }
    return baseDiscount + itemBonus
}

private fun calculateMediumOrderDiscount(order: Order): Double {
    return if (order.customer.isPremium) 0.10 else 0.05
}
```

#### Deep Nesting

**Violation**:
```kotlin
fun processData(data: List<Item>) {
    for (item in data) {
        if (item.isValid) {
            if (item.category == "food") {
                if (item.price > 10) {
                    if (item.inStock) {
                        // Process item
                    }
                }
            }
        }
    }
}
```

**Fix**:
```kotlin
fun processData(data: List<Item>) {
    data.filter { it.isValid }
        .filter { it.category == "food" }
        .filter { it.price > 10 }
        .filter { it.inStock }
        .forEach { processItem(it) }
}

// Or with early returns
fun processItem(item: Item) {
    if (!item.isValid) return
    if (item.category != "food") return
    if (item.price <= 10) return
    if (!item.inStock) return
    
    // Process item
}
```

---

## Architecture Analyzer

**ID**: `architecture`  
**Category**: ARCHITECTURE  
**Priority**: HIGH

### What It Checks

The Architecture Analyzer validates Clean Architecture principles:

- Domain layer has no Android imports
- Repository implementations in data layer
- ViewModels expose StateFlow, not MutableStateFlow
- Use cases have single operator function
- Proper dependency flow (UI → Domain ← Data)

### Examples

#### Android Imports in Domain Layer

**Violation**:
```kotlin
// domain/model/Meal.kt
package com.shoppit.app.domain.model

import android.os.Parcelable  // ❌ Android import in domain
import kotlinx.parcelize.Parcelize

@Parcelize
data class Meal(
    val id: Long,
    val name: String
) : Parcelable
```

**Fix**:
```kotlin
// domain/model/Meal.kt
package com.shoppit.app.domain.model

// ✅ Pure Kotlin, no Android dependencies
data class Meal(
    val id: Long,
    val name: String
)
```

#### Exposed MutableStateFlow

**Violation**:
```kotlin
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ❌ Exposed mutable state
    val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
}
```

**Fix**:
```kotlin
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ✅ Private mutable, public immutable
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
}
```

#### Use Case with Multiple Public Functions

**Violation**:
```kotlin
class MealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    // ❌ Multiple public functions
    suspend fun getMeals(): Result<List<Meal>> = repository.getMeals()
    suspend fun addMeal(meal: Meal): Result<Long> = repository.addMeal(meal)
    suspend fun deleteMeal(id: Long): Result<Unit> = repository.deleteMeal(id)
}
```

**Fix**:
```kotlin
// ✅ Separate use cases with single responsibility
class GetMealsUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(): Flow<Result<List<Meal>>> = repository.getMeals()
}

class AddMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(meal: Meal): Result<Long> = repository.addMeal(meal)
}

class DeleteMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = repository.deleteMeal(id)
}
```

---

## Compose Analyzer

**ID**: `compose`  
**Category**: COMPOSE  
**Priority**: MEDIUM

### What It Checks

The Compose Analyzer validates Jetpack Compose best practices:

- Composable functions have Modifier parameter
- State mutations use `update { }`
- Expensive computations wrapped in `remember`
- LazyColumn items have stable keys
- No nested LazyColumns

### Examples

#### Missing Modifier Parameter

**Violation**:
```kotlin
@Composable
fun MealCard(meal: Meal, onClick: () -> Unit) {  // ❌ No Modifier parameter
    Card(onClick = onClick) {
        Text(meal.name)
    }
}
```

**Fix**:
```kotlin
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier  // ✅ Modifier parameter with default
) {
    Card(
        onClick = onClick,
        modifier = modifier  // ✅ Apply to root element
    ) {
        Text(meal.name)
    }
}
```

#### Missing Remember for Expensive Computation

**Violation**:
```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    // ❌ Expensive computation on every recomposition
    val sortedMeals = meals.sortedBy { it.name }
    
    LazyColumn {
        items(sortedMeals) { meal ->
            MealCard(meal = meal)
        }
    }
}
```

**Fix**:
```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    // ✅ Computed only when meals change
    val sortedMeals = remember(meals) {
        meals.sortedBy { it.name }
    }
    
    LazyColumn {
        items(sortedMeals, key = { it.id }) { meal ->
            MealCard(meal = meal)
        }
    }
}
```

#### LazyColumn Without Stable Keys

**Violation**:
```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    LazyColumn {
        items(meals) { meal ->  // ❌ No key parameter
            MealCard(meal = meal)
        }
    }
}
```

**Fix**:
```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    LazyColumn {
        items(meals, key = { it.id }) { meal ->  // ✅ Stable key
            MealCard(meal = meal)
        }
    }
}
```

#### Nested LazyColumns

**Violation**:
```kotlin
@Composable
fun MealCategories(categories: List<Category>) {
    LazyColumn {  // ❌ Outer LazyColumn
        items(categories) { category ->
            Text(category.name)
            LazyColumn {  // ❌ Nested LazyColumn
                items(category.meals) { meal ->
                    MealCard(meal = meal)
                }
            }
        }
    }
}
```

**Fix**:
```kotlin
@Composable
fun MealCategories(categories: List<Category>) {
    LazyColumn {  // ✅ Single LazyColumn
        categories.forEach { category ->
            item {
                Text(category.name)
            }
            items(category.meals, key = { it.id }) { meal ->
                MealCard(meal = meal)
            }
        }
    }
}
```

---

## State Management Analyzer

**ID**: `state-management`  
**Category**: STATE_MANAGEMENT  
**Priority**: HIGH

### What It Checks

The State Management Analyzer validates state management patterns:

- Private mutable state not exposed
- State updates use `_state.update { }`
- Sealed classes for mutually exclusive states
- `flowOn(Dispatchers.IO)` for database operations
- ViewModels use `viewModelScope`

### Examples

#### Direct State Mutation

**Violation**:
```kotlin
class MealViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    fun loadMeals() {
        viewModelScope.launch {
            _uiState.value = MealUiState.Loading  // ❌ Direct assignment
            // ...
        }
    }
}
```

**Fix**:
```kotlin
class MealViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    fun loadMeals() {
        viewModelScope.launch {
            _uiState.update { MealUiState.Loading }  // ✅ Use update { }
            // ...
        }
    }
}
```

#### Missing flowOn for Database Operations

**Violation**:
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        mealDao.getAllMeals()  // ❌ No flowOn(Dispatchers.IO)
            .collect { entities ->
                emit(Result.success(entities.map { it.toMeal() }))
            }
    }
}
```

**Fix**:
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        mealDao.getAllMeals()
            .collect { entities ->
                emit(Result.success(entities.map { it.toMeal() }))
            }
    }.flowOn(Dispatchers.IO)  // ✅ Apply flowOn
}
```

---

## Error Handling Analyzer

**ID**: `error-handling`  
**Category**: ERROR_HANDLING  
**Priority**: HIGH

### What It Checks

The Error Handling Analyzer validates error handling patterns:

- Repository functions catch and map exceptions
- Result type used for failable operations
- No exceptions reach UI layer
- No empty catch blocks

### Examples

#### Missing Exception Handling in Repository

**Violation**:
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    override suspend fun addMeal(meal: Meal): Result<Long> {
        // ❌ No try-catch, exceptions propagate
        val id = mealDao.insertMeal(meal.toEntity())
        return Result.success(id)
    }
}
```

**Fix**:
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = mealDao.insertMeal(meal.toEntity())
            Result.success(id)
        } catch (e: SQLiteException) {
            Result.failure(AppError.DatabaseError("Failed to save meal", e))
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Unknown error", e))
        }
    }
}
```

#### Empty Catch Block

**Violation**:
```kotlin
fun loadData() {
    try {
        val data = fetchData()
        processData(data)
    } catch (e: Exception) {
        // ❌ Empty catch block
    }
}
```

**Fix**:
```kotlin
fun loadData() {
    try {
        val data = fetchData()
        processData(data)
    } catch (e: Exception) {
        // ✅ Proper error handling
        Timber.e(e, "Failed to load data")
        _errorState.update { ErrorState.LoadFailed(e.message ?: "Unknown error") }
    }
}
```

---

## Dependency Injection Analyzer

**ID**: `dependency-injection`  
**Category**: DEPENDENCY_INJECTION  
**Priority**: MEDIUM

### What It Checks

The Dependency Injection Analyzer validates Hilt usage:

- ViewModels annotated with `@HiltViewModel`
- Constructor injection with `@Inject constructor()`
- Modules use `@Module` and `@InstallIn`
- `@Binds` used for interface binding

### Examples

#### Missing @HiltViewModel

**Violation**:
```kotlin
class MealViewModel @Inject constructor(  // ❌ Missing @HiltViewModel
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ...
}
```

**Fix**:
```kotlin
@HiltViewModel  // ✅ Add @HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ...
}
```

#### Missing @Inject Constructor

**Violation**:
```kotlin
class MealRepositoryImpl(  // ❌ Missing @Inject
    private val mealDao: MealDao
) : MealRepository {
    // ...
}
```

**Fix**:
```kotlin
class MealRepositoryImpl @Inject constructor(  // ✅ Add @Inject
    private val mealDao: MealDao
) : MealRepository {
    // ...
}
```

---

## Database Analyzer

**ID**: `database`  
**Category**: DATABASE  
**Priority**: HIGH

### What It Checks

The Database Analyzer validates Room database patterns:

- DAO query functions return Flow
- DAO mutations are suspend functions
- `flowOn(Dispatchers.IO)` applied
- Foreign keys with CASCADE
- Parameterized queries

### Examples

#### DAO Query Not Returning Flow

**Violation**:
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<MealEntity>  // ❌ Should return Flow
}
```

**Fix**:
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<MealEntity>>  // ✅ Returns Flow for reactive updates
}
```

#### DAO Mutation Not Suspend

**Violation**:
```kotlin
@Dao
interface MealDao {
    @Insert
    fun insertMeal(meal: MealEntity): Long  // ❌ Should be suspend
}
```

**Fix**:
```kotlin
@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity): Long  // ✅ Suspend function
}
```

#### SQL Injection Risk

**Violation**:
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE name = '$name'")  // ❌ String concatenation
    fun searchMeals(name: String): Flow<List<MealEntity>>
}
```

**Fix**:
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE name = :name")  // ✅ Parameterized query
    fun searchMeals(name: String): Flow<List<MealEntity>>
}
```

---

## Performance Analyzer

**ID**: `performance`  
**Category**: PERFORMANCE  
**Priority**: MEDIUM

### What It Checks

The Performance Analyzer identifies optimization opportunities:

- Inefficient list iterations
- String concatenation in loops
- Unnecessary object allocations
- Unstable Compose parameters

### Examples

#### Inefficient List Iteration

**Violation**:
```kotlin
fun processLargeList(items: List<Item>): List<Result> {
    return items
        .filter { it.isValid }  // ❌ Creates intermediate list
        .map { it.process() }   // ❌ Creates another intermediate list
        .filter { it.isSuccess }  // ❌ Creates another intermediate list
}
```

**Fix**:
```kotlin
fun processLargeList(items: List<Item>): List<Result> {
    return items.asSequence()  // ✅ Use sequence for lazy evaluation
        .filter { it.isValid }
        .map { it.process() }
        .filter { it.isSuccess }
        .toList()
}
```

#### String Concatenation in Loop

**Violation**:
```kotlin
fun buildReport(items: List<Item>): String {
    var report = ""
    for (item in items) {
        report += "${item.name}: ${item.value}\n"  // ❌ String concatenation
    }
    return report
}
```

**Fix**:
```kotlin
fun buildReport(items: List<Item>): String {
    return buildString {  // ✅ Use StringBuilder
        items.forEach { item ->
            append("${item.name}: ${item.value}\n")
        }
    }
}
```

---

## Naming Analyzer

**ID**: `naming`  
**Category**: NAMING  
**Priority**: LOW

### What It Checks

The Naming Analyzer validates naming conventions:

- File naming conventions
- Class names (PascalCase)
- Function names (camelCase)
- Constants (UPPER_SNAKE_CASE)
- Private mutable state (underscore prefix)

### Examples

#### Incorrect File Naming

**Violation**:
```
meal_view_model.kt  // ❌ Snake case
```

**Fix**:
```
MealViewModel.kt  // ✅ PascalCase
```

#### Incorrect Class Naming

**Violation**:
```kotlin
class meal_repository_impl { }  // ❌ Snake case
```

**Fix**:
```kotlin
class MealRepositoryImpl { }  // ✅ PascalCase
```

#### Incorrect Function Naming

**Violation**:
```kotlin
fun GetMeals() { }  // ❌ PascalCase
```

**Fix**:
```kotlin
fun getMeals() { }  // ✅ camelCase
```

#### Incorrect Constant Naming

**Violation**:
```kotlin
const val maxRetries = 3  // ❌ camelCase
```

**Fix**:
```kotlin
const val MAX_RETRIES = 3  // ✅ UPPER_SNAKE_CASE
```

#### Missing Underscore Prefix for Private Mutable State

**Violation**:
```kotlin
class MealViewModel {
    private val uiState = MutableStateFlow<UiState>(Loading)  // ❌ No underscore
    val state: StateFlow<UiState> = uiState.asStateFlow()
}
```

**Fix**:
```kotlin
class MealViewModel {
    private val _uiState = MutableStateFlow<UiState>(Loading)  // ✅ Underscore prefix
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}
```

---

## Test Coverage Analyzer

**ID**: `test-coverage`  
**Category**: TEST_COVERAGE  
**Priority**: MEDIUM

### What It Checks

The Test Coverage Analyzer validates test presence:

- ViewModels have test files
- Use cases have test files
- Repositories have test files
- Test naming convention

### Examples

#### Missing ViewModel Test

**Violation**:
```
app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt  ✓ Exists
app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt  ❌ Missing
```

**Fix**:
```kotlin
// Create MealViewModelTest.kt
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
    fun `loads meals successfully`() = runTest {
        // Test implementation
    }
}
```

---

## Documentation Analyzer

**ID**: `documentation`  
**Category**: DOCUMENTATION  
**Priority**: LOW

### What It Checks

The Documentation Analyzer validates documentation:

- Public API has KDoc comments
- Complex algorithms have inline comments
- Data class properties documented
- Sealed class subclasses documented

### Examples

#### Missing KDoc for Public API

**Violation**:
```kotlin
// ❌ No KDoc
fun getMeals(): Flow<Result<List<Meal>>> {
    return repository.getMeals()
}
```

**Fix**:
```kotlin
/**
 * Retrieves all meals from the repository.
 *
 * @return Flow emitting Result containing list of meals or error
 */
fun getMeals(): Flow<Result<List<Meal>>> {
    return repository.getMeals()
}
```

#### Missing Property Documentation

**Violation**:
```kotlin
data class Meal(
    val id: Long,  // ❌ No documentation
    val name: String,
    val ingredients: List<Ingredient>
)
```

**Fix**:
```kotlin
/**
 * Represents a meal with ingredients.
 *
 * @property id Unique identifier for the meal
 * @property name Display name of the meal
 * @property ingredients List of ingredients required for the meal
 */
data class Meal(
    val id: Long,
    val name: String,
    val ingredients: List<Ingredient>
)
```

---

## Security Analyzer

**ID**: `security`  
**Category**: SECURITY  
**Priority**: CRITICAL

### What It Checks

The Security Analyzer detects security issues:

- Hardcoded secrets
- Logging sensitive information
- SQL injection risks
- Insecure data storage

### Examples

#### Hardcoded API Key

**Violation**:
```kotlin
const val API_KEY = "sk_live_1234567890abcdef"  // ❌ Hardcoded secret
```

**Fix**:
```kotlin
// ✅ Use BuildConfig or secure storage
val apiKey = BuildConfig.API_KEY

// Or from encrypted preferences
val apiKey = securePreferences.getString("api_key", "")
```

#### Logging Sensitive Information

**Violation**:
```kotlin
fun login(username: String, password: String) {
    Log.d(TAG, "Login: $username, $password")  // ❌ Logging password
}
```

**Fix**:
```kotlin
fun login(username: String, password: String) {
    Log.d(TAG, "Login attempt for user: $username")  // ✅ No sensitive data
}
```

#### SQL Injection Risk

**Violation**:
```kotlin
@Query("SELECT * FROM meals WHERE name = '$name'")  // ❌ String interpolation
fun searchMeals(name: String): Flow<List<MealEntity>>
```

**Fix**:
```kotlin
@Query("SELECT * FROM meals WHERE name = :name")  // ✅ Parameterized query
fun searchMeals(name: String): Flow<List<MealEntity>>
```

---

## Summary

Each analyzer focuses on specific aspects of code quality:

| Analyzer | Focus | Priority | Common Issues |
|----------|-------|----------|---------------|
| Code Smell | Maintainability | MEDIUM | Long functions, high complexity |
| Architecture | Layer separation | HIGH | Android imports in domain, exposed mutable state |
| Compose | UI best practices | MEDIUM | Missing Modifier, no remember |
| State Management | State patterns | HIGH | Direct mutations, missing flowOn |
| Error Handling | Exception handling | HIGH | Missing try-catch, empty catch blocks |
| Dependency Injection | Hilt usage | MEDIUM | Missing annotations |
| Database | Room patterns | HIGH | Non-reactive queries, SQL injection |
| Performance | Optimization | MEDIUM | Inefficient iterations, string concatenation |
| Naming | Conventions | LOW | Incorrect casing, missing prefixes |
| Test Coverage | Test presence | MEDIUM | Missing test files |
| Documentation | KDoc comments | LOW | Missing documentation |
| Security | Security issues | CRITICAL | Hardcoded secrets, sensitive logging |

For more information, see:
- [Usage Guide](USAGE_GUIDE.md)
- [Example Reports](examples/)
- [CI/CD Integration](CI_CD_INTEGRATION.md)
