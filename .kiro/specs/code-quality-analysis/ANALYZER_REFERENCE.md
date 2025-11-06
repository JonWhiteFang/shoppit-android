# Analyzer Reference Guide

This document provides detailed information about each analyzer in the Code Quality Analysis System, including what they check, examples of violations, and how to fix them.

## Table of Contents

1. [Architecture Analyzer](#architecture-analyzer)
2. [Compose Analyzer](#compose-analyzer)
3. [State Management Analyzer](#state-management-analyzer)
4. [Error Handling Analyzer](#error-handling-analyzer)
5. [Dependency Injection Analyzer](#dependency-injection-analyzer)
6. [Database Analyzer](#database-analyzer)
7. [Performance Analyzer](#performance-analyzer)
8. [Naming Analyzer](#naming-analyzer)
9. [Test Coverage Analyzer](#test-coverage-analyzer)
10. [Documentation Analyzer](#documentation-analyzer)
11. [Security Analyzer](#security-analyzer)
12. [Detekt Integration](#detekt-integration)

---

## Architecture Analyzer

**ID**: `architecture`  
**Category**: Architecture  
**Purpose**: Validates Clean Architecture principles and layer separation

### What It Checks

1. **Domain Layer Purity**: Domain layer must not import Android framework classes
2. **Repository Implementation Location**: Repository implementations must be in data layer
3. **ViewModel State Exposure**: ViewModels must expose StateFlow, not MutableStateFlow
4. **Use Case Structure**: Use cases must have single operator function

### Examples

#### ❌ Violation: Domain Layer with Android Import

```kotlin
// domain/model/Meal.kt
package com.shoppit.app.domain.model

import android.os.Parcelable // ❌ Android import in domain layer

data class Meal(
    val id: Long,
    val name: String
) : Parcelable
```

#### ✅ Fix: Remove Android Dependencies

```kotlin
// domain/model/Meal.kt
package com.shoppit.app.domain.model

data class Meal(
    val id: Long,
    val name: String
)
```

#### ❌ Violation: Exposed Mutable State

```kotlin
class MealViewModel : ViewModel() {
    val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading) // ❌ Exposed mutable state
}
```

#### ✅ Fix: Expose Immutable State

```kotlin
class MealViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow() // ✅ Immutable state
}
```

---

## Compose Analyzer

**ID**: `compose`  
**Category**: Compose  
**Purpose**: Validates Jetpack Compose best practices

### What It Checks

1. **Modifier Parameter**: Composables should have Modifier parameter with default value
2. **Remember Usage**: Expensive computations should be wrapped in remember
3. **LazyColumn Keys**: LazyColumn items should have stable keys
4. **Nested LazyColumns**: Avoid nested LazyColumns

### Examples

#### ❌ Violation: Missing Modifier Parameter

```kotlin
@Composable
fun MealCard(meal: Meal) { // ❌ No Modifier parameter
    Card {
        Text(meal.name)
    }
}
```

#### ✅ Fix: Add Modifier Parameter

```kotlin
@Composable
fun MealCard(
    meal: Meal,
    modifier: Modifier = Modifier // ✅ Modifier with default
) {
    Card(modifier = modifier) {
        Text(meal.name)
    }
}
```

#### ❌ Violation: Missing Remember

```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    val sortedMeals = meals.sortedBy { it.name } // ❌ Expensive computation without remember
    
    LazyColumn {
        items(sortedMeals) { meal ->
            MealCard(meal)
        }
    }
}
```

#### ✅ Fix: Use Remember

```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    val sortedMeals = remember(meals) { // ✅ Wrapped in remember
        meals.sortedBy { it.name }
    }
    
    LazyColumn {
        items(sortedMeals, key = { it.id }) { meal -> // ✅ Stable key
            MealCard(meal)
        }
    }
}
```

---

## State Management Analyzer

**ID**: `state-management`  
**Category**: State Management  
**Purpose**: Validates state management patterns

### What It Checks

1. **State Exposure**: Private mutable state should not be exposed
2. **State Updates**: State should be updated using `.update { }` pattern
3. **Flow Dispatchers**: Flow operations should use `flowOn(Dispatchers.IO)`
4. **ViewModel Scope**: ViewModels should use `viewModelScope`

### Examples

#### ❌ Violation: Direct State Mutation

```kotlin
class MealViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    fun loadMeals() {
        _uiState.value = MealUiState.Success(meals) // ❌ Direct assignment
    }
}
```

#### ✅ Fix: Use Update Pattern

```kotlin
class MealViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    fun loadMeals() {
        _uiState.update { MealUiState.Success(meals) } // ✅ Update pattern
    }
}
```

#### ❌ Violation: Missing Flow Dispatcher

```kotlin
override fun getMeals(): Flow<List<Meal>> = flow {
    val meals = mealDao.getAllMeals() // ❌ No dispatcher specified
    emit(meals)
}
```

#### ✅ Fix: Add Flow Dispatcher

```kotlin
override fun getMeals(): Flow<List<Meal>> = flow {
    val meals = mealDao.getAllMeals()
    emit(meals)
}.flowOn(Dispatchers.IO) // ✅ Explicit dispatcher
```

---

## Error Handling Analyzer

**ID**: `error-handling`  
**Category**: Error Handling  
**Purpose**: Validates error handling patterns

### What It Checks

1. **Exception Mapping**: Repository functions should catch and map exceptions
2. **Result Type**: Failable operations should return Result<T>
3. **Empty Catch Blocks**: Catch blocks should not be empty
4. **Exception Propagation**: Exceptions should not reach UI layer

### Examples

#### ❌ Violation: Unhandled Exception

```kotlin
override suspend fun addMeal(meal: Meal): Long {
    return mealDao.insertMeal(meal.toEntity()) // ❌ No error handling
}
```

#### ✅ Fix: Catch and Map Exceptions

```kotlin
override suspend fun addMeal(meal: Meal): Result<Long> {
    return try {
        val id = mealDao.insertMeal(meal.toEntity())
        Result.success(id)
    } catch (e: SQLiteException) {
        Result.failure(AppError.DatabaseError("Failed to add meal", e))
    } catch (e: Exception) {
        Result.failure(AppError.UnknownError(e.message, e))
    }
}
```

#### ❌ Violation: Empty Catch Block

```kotlin
try {
    performOperation()
} catch (e: Exception) {
    // ❌ Empty catch block
}
```

#### ✅ Fix: Handle or Log Exception

```kotlin
try {
    performOperation()
} catch (e: Exception) {
    Timber.e(e, "Operation failed")
    return Result.failure(AppError.UnknownError(e.message, e))
}
```

---

## Dependency Injection Analyzer

**ID**: `dependency-injection`  
**Category**: Dependency Injection  
**Purpose**: Validates Hilt dependency injection patterns

### What It Checks

1. **ViewModel Annotation**: ViewModels should be annotated with @HiltViewModel
2. **Constructor Injection**: Classes should use @Inject constructor()
3. **Module Annotations**: Modules should have @Module and @InstallIn
4. **Binds Usage**: Interface binding should use @Binds

### Examples

#### ❌ Violation: Missing @HiltViewModel

```kotlin
class MealViewModel(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() { // ❌ No @HiltViewModel annotation
    // ...
}
```

#### ✅ Fix: Add @HiltViewModel

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor( // ✅ @HiltViewModel and @Inject
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ...
}
```

#### ❌ Violation: Missing Module Annotations

```kotlin
object DatabaseModule { // ❌ No @Module or @InstallIn
    fun provideDatabase(context: Context): ShoppitDatabase {
        // ...
    }
}
```

#### ✅ Fix: Add Module Annotations

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShoppitDatabase {
        // ...
    }
}
```

---

## Database Analyzer

**ID**: `database`  
**Category**: Database  
**Purpose**: Validates Room database patterns

### What It Checks

1. **DAO Query Return Types**: Query functions should return Flow
2. **DAO Mutation Modifiers**: Mutation functions should be suspend
3. **Query Parameterization**: Queries should use parameterized queries
4. **Foreign Key Cascades**: Foreign keys should specify CASCADE behavior

### Examples

#### ❌ Violation: Non-Flow Query

```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    fun getAllMeals(): List<MealEntity> // ❌ Should return Flow
}
```

#### ✅ Fix: Return Flow

```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals")
    fun getAllMeals(): Flow<List<MealEntity>> // ✅ Returns Flow
}
```

#### ❌ Violation: Non-Suspend Mutation

```kotlin
@Dao
interface MealDao {
    @Insert
    fun insertMeal(meal: MealEntity): Long // ❌ Should be suspend
}
```

#### ✅ Fix: Make Suspend

```kotlin
@Dao
interface MealDao {
    @Insert
    suspend fun insertMeal(meal: MealEntity): Long // ✅ Suspend function
}
```

#### ❌ Violation: SQL Injection Risk

```kotlin
@Query("SELECT * FROM meals WHERE name = '" + name + "'") // ❌ String concatenation
fun searchMeals(name: String): Flow<List<MealEntity>>
```

#### ✅ Fix: Use Parameterized Query

```kotlin
@Query("SELECT * FROM meals WHERE name = :name") // ✅ Parameterized
fun searchMeals(name: String): Flow<List<MealEntity>>
```

---

## Performance Analyzer

**ID**: `performance`  
**Category**: Performance  
**Purpose**: Identifies performance issues

### What It Checks

1. **Inefficient List Operations**: List operations in loops
2. **String Concatenation**: String concatenation in loops
3. **Unnecessary Allocations**: Unnecessary object allocations
4. **Unstable Compose Parameters**: Unstable parameters in Composables

### Examples

#### ❌ Violation: List Operations in Loop

```kotlin
fun processItems(items: List<Item>): List<String> {
    val result = mutableListOf<String>()
    for (item in items) {
        result.add(item.name.uppercase()) // ❌ Multiple list operations
    }
    return result
}
```

#### ✅ Fix: Use Sequence or Map

```kotlin
fun processItems(items: List<Item>): List<String> {
    return items.map { it.name.uppercase() } // ✅ Single operation
}

// Or for large lists:
fun processItems(items: List<Item>): List<String> {
    return items.asSequence()
        .map { it.name.uppercase() }
        .toList()
}
```

#### ❌ Violation: String Concatenation in Loop

```kotlin
fun buildMessage(items: List<String>): String {
    var message = ""
    for (item in items) {
        message += item + ", " // ❌ String concatenation in loop
    }
    return message
}
```

#### ✅ Fix: Use StringBuilder

```kotlin
fun buildMessage(items: List<String>): String {
    return buildString {
        items.forEachIndexed { index, item ->
            append(item)
            if (index < items.size - 1) append(", ")
        }
    }
}
```

---

## Naming Analyzer

**ID**: `naming`  
**Category**: Naming Conventions  
**Purpose**: Validates naming conventions

### What It Checks

1. **File Naming**: Files should follow naming conventions
2. **Class Naming**: Classes should use PascalCase
3. **Function Naming**: Functions should use camelCase
4. **Constant Naming**: Constants should use UPPER_SNAKE_CASE
5. **Private State Naming**: Private mutable state should have underscore prefix

### Examples

#### ❌ Violation: Incorrect Class Name

```kotlin
class meal_view_model : ViewModel() { // ❌ Should be PascalCase
    // ...
}
```

#### ✅ Fix: Use PascalCase

```kotlin
class MealViewModel : ViewModel() { // ✅ PascalCase
    // ...
}
```

#### ❌ Violation: Incorrect Function Name

```kotlin
fun GetMeals(): List<Meal> { // ❌ Should be camelCase
    // ...
}
```

#### ✅ Fix: Use camelCase

```kotlin
fun getMeals(): List<Meal> { // ✅ camelCase
    // ...
}
```

#### ❌ Violation: Incorrect Constant Name

```kotlin
const val maxRetries = 3 // ❌ Should be UPPER_SNAKE_CASE
```

#### ✅ Fix: Use UPPER_SNAKE_CASE

```kotlin
const val MAX_RETRIES = 3 // ✅ UPPER_SNAKE_CASE
```

---

## Test Coverage Analyzer

**ID**: `test-coverage`  
**Category**: Test Coverage  
**Purpose**: Validates test coverage

### What It Checks

1. **ViewModel Tests**: ViewModels should have corresponding test files
2. **Use Case Tests**: Use cases should have corresponding test files
3. **Repository Tests**: Repositories should have corresponding test files
4. **Test Naming**: Test files should follow naming convention

### Examples

#### ❌ Violation: Missing Test File

```
app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt
❌ No corresponding test file found
```

#### ✅ Fix: Create Test File

```
app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt
```

```kotlin
class MealViewModelTest {
    @Test
    fun `loads meals successfully`() {
        // Test implementation
    }
}
```

---

## Documentation Analyzer

**ID**: `documentation`  
**Category**: Documentation  
**Purpose**: Validates documentation completeness

### What It Checks

1. **Public API Documentation**: Public functions and classes should have KDoc
2. **Complex Algorithm Comments**: Complex functions should have inline comments
3. **Data Class Documentation**: Data class properties should be documented
4. **Sealed Class Documentation**: Sealed class subclasses should be documented

### Examples

#### ❌ Violation: Missing KDoc

```kotlin
fun calculateTotal(items: List<Item>): Double { // ❌ No KDoc
    return items.sumOf { it.price }
}
```

#### ✅ Fix: Add KDoc

```kotlin
/**
 * Calculates the total price of all items.
 *
 * @param items List of items to calculate total for
 * @return Total price of all items
 */
fun calculateTotal(items: List<Item>): Double {
    return items.sumOf { it.price }
}
```

#### ❌ Violation: Undocumented Complex Function

```kotlin
fun processData(data: List<Data>): Result {
    // Complex algorithm with no comments
    val filtered = data.filter { it.isValid }
    val grouped = filtered.groupBy { it.category }
    val aggregated = grouped.mapValues { (_, items) ->
        items.fold(0.0) { acc, item -> acc + item.value }
    }
    return Result(aggregated)
}
```

#### ✅ Fix: Add Inline Comments

```kotlin
fun processData(data: List<Data>): Result {
    // Step 1: Filter out invalid data entries
    val filtered = data.filter { it.isValid }
    
    // Step 2: Group by category for aggregation
    val grouped = filtered.groupBy { it.category }
    
    // Step 3: Calculate sum for each category
    val aggregated = grouped.mapValues { (_, items) ->
        items.fold(0.0) { acc, item -> acc + item.value }
    }
    
    return Result(aggregated)
}
```

---

## Security Analyzer

**ID**: `security`  
**Category**: Security  
**Purpose**: Identifies security vulnerabilities

### What It Checks

1. **Hardcoded Secrets**: API keys, passwords, tokens in code
2. **Sensitive Logging**: Logging sensitive information
3. **SQL Injection**: SQL injection vulnerabilities
4. **Insecure Data Storage**: Insecure data storage patterns

### Examples

#### ❌ Violation: Hardcoded API Key

```kotlin
const val API_KEY = "sk_live_1234567890abcdef" // ❌ Hardcoded secret
```

#### ✅ Fix: Use BuildConfig or Secure Storage

```kotlin
val apiKey = BuildConfig.API_KEY // ✅ From BuildConfig

// Or from secure storage
val apiKey = securePreferences.getString("api_key", "")
```

#### ❌ Violation: Logging Sensitive Data

```kotlin
fun login(username: String, password: String) {
    Timber.d("Login attempt: $username / $password") // ❌ Logging password
}
```

#### ✅ Fix: Don't Log Sensitive Data

```kotlin
fun login(username: String, password: String) {
    Timber.d("Login attempt for user: $username") // ✅ No password logged
}
```

---

## Detekt Integration

**ID**: `detekt`  
**Category**: Various  
**Purpose**: Comprehensive static analysis using Detekt

### What It Checks

Detekt provides comprehensive static analysis covering:

- **Complexity**: Cyclomatic complexity, nested blocks, long methods
- **Style**: Code formatting, naming conventions
- **Comments**: Documentation requirements
- **Coroutines**: Coroutine best practices
- **Empty Blocks**: Empty catch, if, while blocks
- **Exceptions**: Exception handling patterns
- **Naming**: Naming conventions
- **Performance**: Performance anti-patterns
- **Potential Bugs**: Common bug patterns

### Configuration

Detekt is configured via `app/detekt-config.yml`. Key settings:

```yaml
complexity:
  CyclomaticComplexMethod:
    threshold: 15
  LongMethod:
    threshold: 50
  LargeClass:
    threshold: 300

style:
  MaxLineLength:
    maxLineLength: 120
  MagicNumber:
    ignoreNumbers: ['-1', '0', '1', '2']
```

### Examples

See Detekt documentation for comprehensive examples: https://detekt.dev/

---

## Summary

Each analyzer focuses on specific aspects of code quality:

- **Architecture**: Layer separation and Clean Architecture
- **Compose**: Jetpack Compose best practices
- **State Management**: State management patterns
- **Error Handling**: Exception handling and Result types
- **Dependency Injection**: Hilt patterns
- **Database**: Room database patterns
- **Performance**: Performance optimizations
- **Naming**: Naming conventions
- **Test Coverage**: Test completeness
- **Documentation**: Documentation completeness
- **Security**: Security vulnerabilities
- **Detekt**: Comprehensive static analysis

Run all analyzers for comprehensive code quality analysis, or focus on specific analyzers based on your needs.
