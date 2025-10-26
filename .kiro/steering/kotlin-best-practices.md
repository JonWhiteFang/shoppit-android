---
inclusion: fileMatch
fileMatchPattern: ['**/*.kt', '**/*.kts']
---

# Kotlin Best Practices

## Naming Conventions

- Use concise, meaningful names without redundant type information
- Avoid repeating parameter types in function names
- Use simple property names within the same entity hierarchy

```kotlin
// Bad: Redundant type information
val annotationList: List<Annotation>
fun findClassByClassId(classId: ClassId)

// Good: Clean, descriptive names
val annotations: List<Annotation>
fun findClass(classId: ClassId)
```

## Code Style

### Immutability & Declarations
- Use `val` over `var` (immutability by default)
- Prefer expression bodies for single-expression functions
- Use trailing commas in multi-line lists
- Use explicit types for public APIs, infer for private/local

```kotlin
// Expression body
fun square(x: Int) = x * x

// Trailing commas
val list = listOf(
    "item1",
    "item2",
)
```

### Validation
- Use `require()` for preconditions (validates arguments)
- Use `check()` for state validation (validates object state)

```kotlin
fun processUser(age: Int) {
    require(age >= 0) { "Age must be non-negative" }
    check(isInitialized) { "Object must be initialized" }
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

```kotlin
// Base extension
val KtDeclaration.symbol: Symbol

// Specialized versions
val KtClass.symbol: ClassSymbol
val KtFunction.symbol: FunctionSymbol
```

### Function Types
- Prefer Kotlin function types over Java interfaces
- Use `fun interface` for SAM conversions

```kotlin
// Kotlin function type
fun processItems(filter: (Item) -> Boolean)

// SAM interface
fun interface ItemProcessor {
    fun process(item: Item)
}
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
 * @return The constant value, or null if not available
 */
val PropertySymbol.compileTimeInitializer: ConstantValue?
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
