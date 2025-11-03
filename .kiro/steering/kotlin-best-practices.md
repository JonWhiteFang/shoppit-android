---
inclusion: fileMatch
fileMatchPattern: ['**/*.kt', '**/*.kts']
---

# Kotlin Best Practices

## Naming Conventions

### General Principles
- Use concise, meaningful names without redundant type information
- Avoid repeating parameter types in function names
- Use simple property names within the same entity hierarchy
- Follow camelCase for functions and properties, PascalCase for classes

### Classes and Interfaces

```kotlin
// Bad: Redundant suffixes
class MealDataClass
interface IMealRepository
abstract class AbstractMealProcessor

// Good: Clean names
class Meal
interface MealRepository
abstract class MealProcessor
```

### Functions

```kotlin
// Bad: Redundant type information
fun findClassByClassId(classId: ClassId)
fun getMealList(): List<Meal>
fun createNewMeal()

// Good: Clean, descriptive names
fun findClass(classId: ClassId)
fun getMeals(): List<Meal>
fun createMeal()
```

### Properties

```kotlin
// Bad: Redundant type information
val annotationList: List<Annotation>
val mealDataList: List<Meal>
val isEnabledFlag: Boolean

// Good: Clean, descriptive names
val annotations: List<Annotation>
val meals: List<Meal>
val isEnabled: Boolean
```

### Constants

```kotlin
// Use UPPER_SNAKE_CASE for constants
const val MAX_RETRY_COUNT = 3
const val DEFAULT_TIMEOUT_MS = 5000L
const val API_BASE_URL = "https://api.shoppit.app"

// Companion object constants
companion object {
    const val TAG = "MealViewModel"
    const val REQUEST_CODE = 1001
}
```

### Boolean Properties

```kotlin
// Use is/has/can prefix for boolean properties
val isLoading: Boolean
val hasIngredients: Boolean
val canEdit: Boolean
val shouldSync: Boolean

// Avoid negative names
val isNotEmpty: Boolean  // Bad
val isEmpty: Boolean     // Good
```

## Code Style

### Immutability & Declarations
- Use `val` over `var` (immutability by default)
- Prefer expression bodies for single-expression functions
- Use trailing commas in multi-line lists
- Use explicit types for public APIs, infer for private/local
- Avoid unnecessary type annotations when type is obvious

```kotlin
// Expression body
fun square(x: Int) = x * x
fun isValid(meal: Meal) = meal.name.isNotBlank() && meal.ingredients.isNotEmpty()

// Trailing commas (easier to add/remove items)
val list = listOf(
    "item1",
    "item2",
    "item3",  // Trailing comma
)

val meal = Meal(
    id = 1,
    name = "Pasta",
    ingredients = listOf(),  // Trailing comma
)

// Type inference
val name = "Pasta"  // Type inferred as String
val count = 5       // Type inferred as Int

// Explicit types for public API
fun getMeals(): List<Meal> = repository.getMeals()
```

### Scope Functions

Use scope functions appropriately:

```kotlin
// let - for null checks and transformations
val length = name?.let { it.length } ?: 0

// apply - for object configuration
val meal = Meal().apply {
    name = "Pasta"
    ingredients = listOf()
}

// also - for side effects
val result = calculateResult()
    .also { Timber.d("Result: $it") }

// run - for executing a block and returning result
val result = run {
    val a = 10
    val b = 20
    a + b
}

// with - for calling multiple methods on an object
with(meal) {
    println(name)
    println(ingredients.size)
}
```

### String Templates

```kotlin
// Use string templates instead of concatenation
val message = "Hello, $name!"  // Good
val message = "Hello, " + name + "!"  // Bad

// Use curly braces for expressions
val message = "Meal has ${meal.ingredients.size} ingredients"

// Multi-line strings
val json = """
    {
        "name": "$name",
        "count": $count
    }
""".trimIndent()
```

### Elvis Operator

```kotlin
// Use Elvis operator for default values
val name = meal.name ?: "Unnamed"
val count = meal.ingredients.size ?: 0

// With throw
val id = meal.id ?: throw IllegalStateException("Meal must have ID")

// With return
fun process(meal: Meal?) {
    val validMeal = meal ?: return
    // Process validMeal
}
```

### Validation
- Use `require()` for preconditions (validates arguments)
- Use `check()` for state validation (validates object state)
- Use `requireNotNull()` for null checks with custom messages
- Use `checkNotNull()` for state null checks

```kotlin
// require() - validates function arguments
fun processUser(age: Int, name: String) {
    require(age >= 0) { "Age must be non-negative, got: $age" }
    require(name.isNotBlank()) { "Name cannot be blank" }
}

// check() - validates object state
fun save() {
    check(isInitialized) { "Object must be initialized before saving" }
    check(!isClosed) { "Cannot save to closed connection" }
}

// requireNotNull() - validates non-null arguments
fun process(meal: Meal?) {
    val validMeal = requireNotNull(meal) { "Meal cannot be null" }
    // validMeal is now non-null
}

// checkNotNull() - validates non-null state
fun getMeal(): Meal {
    return checkNotNull(cachedMeal) { "Meal not loaded yet" }
}

// error() - throws IllegalStateException
fun handleState(state: State) = when (state) {
    State.LOADING -> showLoading()
    State.SUCCESS -> showSuccess()
    State.ERROR -> showError()
    else -> error("Unknown state: $state")
}
```

### Destructuring
- Use underscore `_` to ignore unused components

```kotlin
val (first, _, third) = triple
```

## Idiomatic Kotlin

### Extension Functions
- Use extensions for better readability and discoverability
- Provide overloaded extensions with specialized return types
- Prefer extensions over utility classes

```kotlin
// Base extension
val KtDeclaration.symbol: Symbol

// Specialized versions
val KtClass.symbol: ClassSymbol
val KtFunction.symbol: FunctionSymbol

// Extension for domain logic
fun Meal.hasIngredient(name: String): Boolean =
    ingredients.any { it.name.equals(name, ignoreCase = true) }

// Extension for formatting
fun Meal.toDisplayString(): String =
    "$name (${ingredients.size} ingredients)"

// Extension for validation
fun Meal.isValid(): Boolean =
    name.isNotBlank() && ingredients.isNotEmpty()
```

### Data Classes

```kotlin
// Use data classes for value objects
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String? = null
)

// Provides: equals(), hashCode(), toString(), copy(), componentN()

// Use copy() for immutable updates
val updatedMeal = meal.copy(name = "New Name")

// Destructuring
val (id, name, ingredients) = meal
```

### Sealed Classes

```kotlin
// Use sealed classes for restricted hierarchies
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}

// Exhaustive when expressions
fun render(state: MealUiState) = when (state) {
    is MealUiState.Loading -> showLoading()
    is MealUiState.Success -> showMeals(state.meals)
    is MealUiState.Error -> showError(state.message)
    // No else needed - compiler ensures exhaustiveness
}
```

### Inline Classes (Value Classes)

```kotlin
// Use value classes for type-safe wrappers
@JvmInline
value class MealId(val value: Long)

@JvmInline
value class Quantity(val value: Double) {
    init {
        require(value > 0) { "Quantity must be positive" }
    }
}

// Usage
fun getMeal(id: MealId): Meal
fun setQuantity(quantity: Quantity)

// No runtime overhead - inlined to primitive type
```

### Type Aliases

```kotlin
// Use type aliases for complex types
typealias MealList = List<Meal>
typealias MealMap = Map<Long, Meal>
typealias MealFilter = (Meal) -> Boolean

// Usage
fun filterMeals(meals: MealList, filter: MealFilter): MealList =
    meals.filter(filter)
```

### Function Types
- Prefer Kotlin function types over Java interfaces
- Use `fun interface` for SAM conversions
- Use descriptive parameter names in function types

```kotlin
// Kotlin function type with named parameters
fun processItems(filter: (item: Item, index: Int) -> Boolean)

// SAM interface for Java interop
fun interface ItemProcessor {
    fun process(item: Item)
}

// Higher-order functions
fun <T, R> List<T>.mapNotNull(transform: (T) -> R?): List<R> =
    mapNotNull(transform)

// Function references
val meals = items.map(::toMeal)
val filtered = meals.filter(Meal::isValid)
```

### Collections

```kotlin
// Use appropriate collection types
val list = listOf(1, 2, 3)           // Immutable list
val mutableList = mutableListOf(1, 2) // Mutable list
val set = setOf(1, 2, 3)             // Immutable set
val map = mapOf("a" to 1, "b" to 2)  // Immutable map

// Collection operations
val filtered = meals.filter { it.isValid() }
val mapped = meals.map { it.name }
val grouped = meals.groupBy { it.category }
val sorted = meals.sortedBy { it.name }

// Sequence for lazy evaluation
val result = meals.asSequence()
    .filter { it.isValid() }
    .map { it.name }
    .take(10)
    .toList()
```

### Null Safety

```kotlin
// Safe call operator
val length = name?.length

// Safe call chain
val city = user?.address?.city

// let for null checks
name?.let { println("Name: $it") }

// Elvis operator
val displayName = name ?: "Unknown"

// Not-null assertion (use sparingly)
val length = name!!.length  // Throws if null

// Safe cast
val meal = item as? Meal
```

### Operator Overloading
- Use operators only for conventional meanings
- Avoid unconventional operator usage

```kotlin
// Good: Conventional usage
operator fun contains(name: Name): Boolean
operator fun get(index: Int): Item

// Avoid: Unclear meaning
operator fun plus(other: Type): Type  // What does this mean?
```

## API Design

### Consistency
- Ensure predictable behavior across similar methods
- Use consistent return types for "not found" scenarios (prefer `null`)

```kotlin
// Consistent "find" methods
fun findClass(id: ClassId): ClassSymbol?
fun findFunction(id: CallableId): FunctionSymbol?
```

### Kotlin Conventions
- Reuse standard interfaces where appropriate
- Use conventional method names (`toString()` not `asString()`)
- Follow patterns from Kotlin standard library

```kotlin
// Implement standard interfaces
interface AnnotationList : List<Annotation> {
    operator fun contains(classId: ClassId): Boolean
}
```

### Evolution Considerations
- Avoid over-reliance on `inline` functions (limits future changes)
- Provide regular functions with optional inline wrappers

```kotlin
// Regular function allows internal changes
fun <T : Symbol> topLevelSymbols(klass: KClass<T>): Sequence<T>

// Inline wrapper for convenience
inline fun <reified T : Symbol> topLevelSymbols(): Sequence<T> =
    topLevelSymbols(T::class)
```

## Documentation

### KDoc Guidelines
- Use Markdown headers (`###`) to organize extensive documentation
- Document equality semantics when overriding `equals()` and `hashCode()`
- Address edge cases, evaluation behavior, and cross-language compatibility
- Answer key questions users might have
- Use `@param`, `@return`, `@throws` tags appropriately
- Include code examples for complex APIs

```kotlin
/**
 * The compile-time constant initializer for the given property.
 *
 * ### Resolvability
 * May be `null` if not resolvable from this module.
 *
 * ### Type arguments
 * Type arguments are not converted automatically.
 *
 * ### Example
 * ```kotlin
 * val property = findProperty("MAX_SIZE")
 * val value = property.compileTimeInitializer // Returns 100
 * ```
 *
 * @return The constant value, or null if not available
 * @see ConstantValue
 */
val PropertySymbol.compileTimeInitializer: ConstantValue?

/**
 * Validates and saves a meal to the database.
 *
 * ### Validation
 * - Meal name must not be blank
 * - Must have at least one ingredient
 * - All ingredients must have valid quantities
 *
 * ### Thread Safety
 * This function is thread-safe and can be called from any thread.
 *
 * @param meal The meal to save
 * @return The ID of the saved meal
 * @throws ValidationError if the meal is invalid
 * @throws DatabaseError if the save operation fails
 */
suspend fun saveMeal(meal: Meal): Long
```

### Documentation Best Practices

```kotlin
// Document public APIs
/**
 * Retrieves all meals from the database.
 *
 * @return A flow of meal lists that updates when data changes
 */
fun getMeals(): Flow<List<Meal>>

// Document complex logic
/**
 * Aggregates ingredients from multiple meals.
 *
 * Ingredients with the same name (case-insensitive) are combined,
 * and their quantities are summed. Units are not converted.
 */
fun aggregateIngredients(meals: List<Meal>): List<Ingredient>

// Document edge cases
/**
 * Finds a meal by ID.
 *
 * @param id The meal ID to search for
 * @return The meal if found, null otherwise
 * @throws IllegalArgumentException if id is negative
 */
fun findMeal(id: Long): Meal?
```

## Literals

### Numeric Literals
- Use underscores for readability in long numbers
- Binary: `0b` prefix, Hexadecimal: `0x` prefix

```kotlin
val binary = 0b0_1_0_1_0_1
val hex = 0xFF_EC_DE_5E
val large = 1_000_000
val scientific = 1.23e-4
val precise = 3.141_592_653_589
```

## Placeholder Code
- Use `TODO()` function for unimplemented code
- Throws `NotImplementedError` when executed

```kotlin
fun complexCalculation(): Int {
    TODO("Implement algorithm")
}
```

## Comments
- Use `//` for single-line comments
- Use `/* */` for multi-line block comments
- Prefer KDoc (`/** */`) for public API documentation
- Avoid obvious comments (code should be self-documenting)
- Comment "why", not "what"

```kotlin
val value = 9999 // End-of-line comment

/*
 * Multi-line comment
 * explaining complex logic
 */

/**
 * KDoc for public API
 * @param x The input value
 * @return The processed result
 */
fun process(x: Int): Int = x * 2
```

## Quick Reference

### Declarations
- Use `val` over `var` (immutability)
- Use expression bodies for simple functions
- Use trailing commas in multi-line lists
- Explicit types for public APIs, infer for private
- Use data classes for value objects
- Use sealed classes for restricted hierarchies

### Validation
- `require()` for argument validation
- `check()` for state validation
- `requireNotNull()` / `checkNotNull()` for null checks
- `error()` for throwing IllegalStateException
- `TODO()` for unimplemented code

### Naming
- Avoid redundant type information in names
- Use simple, descriptive names
- Follow Kotlin conventions (not Java)
- Use UPPER_SNAKE_CASE for constants
- Use is/has/can prefix for boolean properties

### Null Safety
- Use safe call operator `?.`
- Use Elvis operator `?:` for defaults
- Use `let` for null checks
- Avoid `!!` (not-null assertion)

### Collections
- Use immutable collections by default
- Use `listOf()`, `setOf()`, `mapOf()` for immutable
- Use `mutableListOf()`, etc. for mutable
- Use sequences for lazy evaluation

### Scope Functions
- `let` - null checks and transformations
- `apply` - object configuration
- `also` - side effects
- `run` - execute block and return result
- `with` - call multiple methods on object

### Best Practices
- Prefer extension functions over utility classes
- Use type aliases for complex types
- Use value classes for type-safe wrappers
- Document public APIs with KDoc
- Write self-documenting code
- Use string templates instead of concatenation
- Prefer when expressions over if-else chains
