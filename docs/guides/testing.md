# Testing Guide

This guide covers testing strategies, patterns, and best practices for the Shoppit Android application.

## Testing Philosophy

Shoppit follows a pragmatic testing approach that balances thoroughness with development velocity:

- **Test behavior, not implementation** - Focus on what the code does, not how it does it
- **Minimal but meaningful** - Write tests that catch real bugs, avoid over-testing edge cases
- **Fast feedback** - Unit tests should run in milliseconds, instrumented tests in seconds
- **Test pyramid** - Many unit tests, fewer integration tests, minimal UI tests

### Why We Test

- **Catch bugs early** - Find issues before they reach production
- **Enable refactoring** - Change code confidently without breaking functionality
- **Document behavior** - Tests serve as executable documentation
- **Design feedback** - Hard-to-test code often indicates design issues

## Test Coverage Guidelines

Follow the test pyramid with different coverage targets per layer:

### Domain Layer (High Coverage - 80%+)

The domain layer contains business logic and should have the highest test coverage:

- **Use Cases** - Test all business logic paths, error handling, and validation
- **Validators** - Test all validation rules and edge cases
- **Models** - Test any computed properties or business logic

**Why high coverage?** Domain logic is pure Kotlin with no Android dependencies, making it fast and easy to test.

### Data Layer (Medium Coverage - 60%+)

The data layer handles data access and transformation:

- **Repositories** - Test data flow, error mapping, and caching logic
- **Mappers** - Test entity/DTO/model conversions
- **DAOs** - Instrumented tests for complex queries only

**Why medium coverage?** Focus on business-critical data flows and error handling.

### UI Layer (Selective Coverage - 40%+)

The UI layer should focus on critical paths:

- **ViewModels** - Test state transitions, user actions, and error handling
- **Screens** - Test critical user flows and error states only

**Why selective coverage?** UI tests are slower and more brittle. Focus on high-value scenarios.

## Test Types

### Unit Tests

**Location:** `app/src/test/`

**Characteristics:**
- Fast (milliseconds)
- No Android dependencies
- Use MockK for mocking
- Run on JVM

**What to test:**
- Use cases
- ViewModels
- Repositories (with mocked DAOs)
- Validators
- Mappers

### Instrumented Tests

**Location:** `app/src/androidTest/`

**Characteristics:**
- Slower (seconds)
- Require Android device/emulator
- Use real Android framework
- Can test Room, Compose UI

**What to test:**
- Room DAOs
- Compose UI (critical flows only)
- Integration tests

## Unit Testing Patterns

### ViewModel Testing

ViewModels manage UI state and handle user actions. Test state transitions and error handling.

```kotlin
@ExperimentalCoroutinesApi
class MealViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var addMealUseCase: AddMealUseCase
    private lateinit var viewModel: MealViewModel
    
    @Before
    fun setup() {
        getMealsUseCase = mockk()
        addMealUseCase = mockk()
        viewModel = MealViewModel(getMealsUseCase, addMealUseCase)
    }
    
    @Test
    fun `initial state is loading`() {
        assertEquals(MealUiState.Loading, viewModel.uiState.value)
    }
    
    @Test
    fun `loads meals successfully when repository returns data`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Pasta"))
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
    fun `displays error message when loading fails`() = runTest {
        // Given
        val error = AppError.NetworkError("Connection failed")
        coEvery { getMealsUseCase() } returns flowOf(Result.failure(error))
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MealUiState.Error)
        assertEquals("Connection failed", (state as MealUiState.Error).message)
    }
    
    @Test
    fun `adds meal and refreshes list`() = runTest {
        // Given
        val newMeal = Meal(name = "Salad", ingredients = listOf())
        coEvery { addMealUseCase(newMeal) } returns Result.success(2L)
        coEvery { getMealsUseCase() } returns flowOf(Result.success(listOf(newMeal)))
        
        // When
        viewModel.addMeal(newMeal)
        advanceUntilIdle()
        
        // Then
        coVerify { addMealUseCase(newMeal) }
        coVerify { getMealsUseCase() }
    }
}
```

**Key Points:**
- Use `MainDispatcherRule` to replace main dispatcher with test dispatcher
- Use `runTest` for coroutine tests
- Use `advanceUntilIdle()` to execute all pending coroutines
- Test initial state, success paths, and error paths
- Verify use case calls with `coVerify`

### Use Case Testing

Use cases contain business logic. Test all paths including validation and error handling.

```kotlin
class AddMealUseCaseTest {
    
    private lateinit var repository: MealRepository
    private lateinit var useCase: AddMealUseCase
    
    @Before
    fun setup() {
        repository = mockk()
        useCase = AddMealUseCase(repository)
    }
    
    @Test
    fun `adds meal successfully when valid`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        coEvery { repository.addMeal(meal) } returns Result.success(1L)
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { repository.addMeal(meal) }
    }
    
    @Test
    fun `returns error when meal name is empty`() = runTest {
        // Given
        val meal = Meal(name = "", ingredients = listOf())
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationError)
        coVerify(exactly = 0) { repository.addMeal(any()) }
    }
    
    @Test
    fun `returns error when meal name is too long`() = runTest {
        // Given
        val meal = Meal(name = "a".repeat(101), ingredients = listOf())
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ValidationError)
    }
    
    @Test
    fun `propagates repository errors`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        val error = AppError.DatabaseError("Database error")
        coEvery { repository.addMeal(meal) } returns Result.failure(error)
        
        // When
        val result = useCase(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
}
```

**Key Points:**
- Test validation logic before repository calls
- Test success and failure paths
- Verify repository is not called when validation fails
- Test error propagation from repository

### Repository Testing

Repositories handle data access and error mapping. Test data flow and exception handling.

```kotlin
class MealRepositoryImplTest {
    
    private lateinit var dao: MealDao
    private lateinit var repository: MealRepositoryImpl
    
    @Before
    fun setup() {
        dao = mockk()
        repository = MealRepositoryImpl(dao)
    }
    
    @Test
    fun `getMeals returns flow of meals`() = runTest {
        // Given
        val entities = listOf(
            MealEntity(id = 1, name = "Pasta", createdAt = 123L),
            MealEntity(id = 2, name = "Salad", createdAt = 456L)
        )
        every { dao.getAllMeals() } returns flowOf(entities)
        
        // When
        val result = repository.getMeals().first()
        
        // Then
        assertTrue(result.isSuccess)
        val meals = result.getOrNull()
        assertEquals(2, meals?.size)
        assertEquals("Pasta", meals?.first()?.name)
    }
    
    @Test
    fun `addMeal maps entity and inserts`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        coEvery { dao.insertMeal(any()) } returns 1L
        
        // When
        val result = repository.addMeal(meal)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { dao.insertMeal(match { it.name == "Pasta" }) }
    }
    
    @Test
    fun `addMeal catches and maps database exceptions`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        coEvery { dao.insertMeal(any()) } throws SQLiteException("Constraint violation")
        
        // When
        val result = repository.addMeal(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.DatabaseError)
    }
    
    @Test
    fun `deleteMeal removes from database`() = runTest {
        // Given
        val mealId = 1L
        coEvery { dao.deleteMealById(mealId) } just Runs
        
        // When
        val result = repository.deleteMeal(mealId)
        
        // Then
        assertTrue(result.isSuccess)
        coVerify { dao.deleteMealById(mealId) }
    }
}
```

**Key Points:**
- Mock DAO dependencies
- Test entity-to-model mapping
- Test exception catching and error mapping
- Verify DAO method calls

## Instrumented Testing Patterns

### Room DAO Testing

DAOs interact with the database. Use in-memory database for fast, isolated tests.

```kotlin
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    
    private lateinit var database: ShoppitDatabase
    private lateinit var mealDao: MealDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ShoppitDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        mealDao = database.mealDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveMeal() = runTest {
        // Given
        val meal = MealEntity(
            id = 1,
            name = "Pasta",
            createdAt = System.currentTimeMillis()
        )
        
        // When
        mealDao.insertMeal(meal)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertEquals(1, meals.size)
        assertEquals("Pasta", meals[0].name)
        assertEquals(1, meals[0].id)
    }
    
    @Test
    fun deleteMealRemovesFromDatabase() = runTest {
        // Given
        val meal = MealEntity(id = 1, name = "Pasta", createdAt = 123L)
        mealDao.insertMeal(meal)
        
        // When
        mealDao.deleteMeal(meal)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertTrue(meals.isEmpty())
    }
    
    @Test
    fun updateMealModifiesExisting() = runTest {
        // Given
        val meal = MealEntity(id = 1, name = "Pasta", createdAt = 123L)
        mealDao.insertMeal(meal)
        
        // When
        val updated = meal.copy(name = "Spaghetti")
        mealDao.updateMeal(updated)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertEquals(1, meals.size)
        assertEquals("Spaghetti", meals[0].name)
    }
    
    @Test
    fun getMealByIdReturnsCorrectMeal() = runTest {
        // Given
        val meal1 = MealEntity(id = 1, name = "Pasta", createdAt = 123L)
        val meal2 = MealEntity(id = 2, name = "Salad", createdAt = 456L)
        mealDao.insertMeal(meal1)
        mealDao.insertMeal(meal2)
        
        // When
        val result = mealDao.getMealById(1).first()
        
        // Then
        assertNotNull(result)
        assertEquals("Pasta", result?.name)
    }
}
```

**Key Points:**
- Use `Room.inMemoryDatabaseBuilder()` for isolated tests
- Use `allowMainThreadQueries()` to simplify test code
- Close database in `@After` to prevent leaks
- Test CRUD operations and queries
- Use `first()` to collect Flow values in tests

### Compose UI Testing

Test critical user flows and interactions. Keep UI tests focused and minimal.

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MealListScreenTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun displaysLoadingState() {
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealUiState.Loading,
                    onMealClick = {},
                    onAddMealClick = {}
                )
            }
        }
        
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }
    
    @Test
    fun displaysMealList() {
        val meals = listOf(
            Meal(id = 1, name = "Pasta"),
            Meal(id = 2, name = "Salad")
        )
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealUiState.Success(meals),
                    onMealClick = {},
                    onAddMealClick = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Pasta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salad").assertIsDisplayed()
    }
    
    @Test
    fun clickingMealTriggersCallback() {
        val meals = listOf(Meal(id = 1, name = "Pasta"))
        var clickedMeal: Meal? = null
        
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealUiState.Success(meals),
                    onMealClick = { clickedMeal = it },
                    onAddMealClick = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Pasta").performClick()
        assertEquals(meals[0], clickedMeal)
    }
    
    @Test
    fun displaysErrorState() {
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealUiState.Error("Failed to load meals"),
                    onMealClick = {},
                    onAddMealClick = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("Failed to load meals").assertIsDisplayed()
    }
    
    @Test
    fun displaysEmptyState() {
        composeTestRule.setContent {
            ShoppitTheme {
                MealListContent(
                    uiState = MealUiState.Success(emptyList()),
                    onMealClick = {},
                    onAddMealClick = {}
                )
            }
        }
        
        composeTestRule.onNodeWithText("No meals yet").assertIsDisplayed()
    }
}
```

**Key Points:**
- Use `createAndroidComposeRule<MainActivity>()` for activity-based tests
- Test stateless content composables, not stateful screens
- Use `testTag` for finding UI elements reliably
- Test all UI states (loading, success, error, empty)
- Test user interactions with `performClick()`, `performTextInput()`, etc.

## Test Data Builders

Create reusable builders for complex test data:

```kotlin
object TestDataBuilders {
    
    fun meal(
        id: Long = 1,
        name: String = "Test Meal",
        ingredients: List<Ingredient> = emptyList(),
        notes: String = "",
        isFavorite: Boolean = false
    ) = Meal(
        id = id,
        name = name,
        ingredients = ingredients,
        notes = notes,
        isFavorite = isFavorite
    )
    
    fun ingredient(
        name: String = "Test Ingredient",
        quantity: String = "1",
        unit: String = "cup"
    ) = Ingredient(
        name = name,
        quantity = quantity,
        unit = unit
    )
    
    fun mealEntity(
        id: Long = 1,
        name: String = "Test Meal",
        createdAt: Long = System.currentTimeMillis()
    ) = MealEntity(
        id = id,
        name = name,
        createdAt = createdAt
    )
    
    fun mealPlan(
        id: Long = 1,
        date: Long = System.currentTimeMillis(),
        mealType: MealType = MealType.DINNER,
        mealId: Long = 1
    ) = MealPlan(
        id = id,
        date = date,
        mealType = mealType,
        mealId = mealId
    )
}

// Usage in tests
@Test
fun `test with custom meal`() = runTest {
    val meal = TestDataBuilders.meal(
        name = "Spaghetti",
        ingredients = listOf(
            TestDataBuilders.ingredient(name = "Pasta", quantity = "400", unit = "g")
        )
    )
    // Use meal in test
}
```

## Mocking Guidelines

### When to Mock

Mock external dependencies and collaborators:

- **External dependencies** - Network, database (in unit tests)
- **Complex collaborators** - Repositories in ViewModel tests, use cases in integration tests
- **Time-dependent operations** - Clock, date providers
- **Android framework** - Context, Resources (when needed)

### When to Use Fakes

Use fake implementations for simpler, more maintainable tests:

- **Repositories in ViewModel tests** - Fake repository with in-memory storage
- **Use cases in integration tests** - Fake use case with predictable behavior
- **Data sources** - In-memory data source instead of database

### MockK Patterns

```kotlin
// Relaxed mocks - return default values
val repository: MealRepository = mockk(relaxed = true)

// Specific behavior with coEvery
coEvery { repository.getMeals() } returns flowOf(Result.success(meals))
coEvery { repository.addMeal(any()) } returns Result.success(1L)

// Verify calls
coVerify { repository.addMeal(any()) }
coVerify(exactly = 0) { repository.deleteMeal(any()) }
coVerify(exactly = 2) { repository.getMeals() }

// Verify call order
coVerifyOrder {
    repository.addMeal(any())
    repository.getMeals()
}

// Argument capture
val slot = slot<Meal>()
coVerify { repository.addMeal(capture(slot)) }
assertEquals("Pasta", slot.captured.name)

// Match arguments
coVerify { repository.addMeal(match { it.name == "Pasta" }) }

// Answer with lambda
coEvery { repository.addMeal(any()) } answers {
    val meal = firstArg<Meal>()
    Result.success(meal.id)
}

// Throw exception
coEvery { repository.getMeals() } throws SQLiteException("Database error")

// Return different values on subsequent calls
coEvery { repository.getMeals() } returnsMany listOf(
    flowOf(Result.success(emptyList())),
    flowOf(Result.success(listOf(meal)))
)
```

## Coroutine Testing

### MainDispatcherRule

Replace main dispatcher with test dispatcher for ViewModel tests:

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

// Usage
class MyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    // Tests...
}
```

### runTest and Time Control

```kotlin
@Test
fun `test with coroutines`() = runTest {
    // Execute all pending coroutines
    viewModel.loadData()
    advanceUntilIdle()
    
    // Advance virtual time
    advanceTimeBy(1000)
    
    // Run until specific condition
    runCurrent() // Execute immediately scheduled coroutines
    
    // Assertions
    assertEquals(expected, viewModel.state.value)
}
```

### Testing Flows

```kotlin
@Test
fun `test flow emissions`() = runTest {
    // Collect flow values
    val values = mutableListOf<Int>()
    val job = launch {
        repository.getUpdates().collect { values.add(it) }
    }
    
    // Trigger emissions
    repository.update(1)
    repository.update(2)
    advanceUntilIdle()
    
    // Assert
    assertEquals(listOf(1, 2), values)
    job.cancel()
}

@Test
fun `test flow with first()`() = runTest {
    // Get first emission
    val result = repository.getData().first()
    assertEquals(expected, result)
}

@Test
fun `test flow with toList()`() = runTest {
    // Collect all emissions (for finite flows)
    val results = repository.getAll().toList()
    assertEquals(3, results.size)
}
```

## Test Organization

### File Structure

Mirror production code structure in test directories:

```
test/com/shoppit/app/
├── domain/
│   ├── usecase/
│   │   ├── meal/
│   │   │   ├── AddMealUseCaseTest.kt
│   │   │   ├── GetMealsUseCaseTest.kt
│   │   │   └── DeleteMealUseCaseTest.kt
│   │   ├── planner/
│   │   └── shopping/
│   └── validator/
│       └── MealValidatorTest.kt
├── data/
│   ├── repository/
│   │   ├── MealRepositoryImplTest.kt
│   │   ├── MealPlanRepositoryImplTest.kt
│   │   └── ShoppingListRepositoryImplTest.kt
│   └── mapper/
│       └── MealMapperTest.kt
└── ui/
    ├── meal/
    │   ├── MealViewModelTest.kt
    │   └── MealListViewModelTest.kt
    ├── planner/
    └── shopping/

androidTest/com/shoppit/app/
├── data/
│   └── local/
│       └── dao/
│           ├── MealDaoTest.kt
│           ├── MealPlanDaoTest.kt
│           └── ShoppingListDaoTest.kt
└── ui/
    ├── meal/
    │   └── MealListScreenTest.kt
    ├── planner/
    └── shopping/
```

### Test File Naming

- Test files end with `Test.kt`
- Match the class being tested: `MealViewModel.kt` → `MealViewModelTest.kt`
- Group related tests in the same file

## Running Tests

### Gradle Commands

```bash
# All unit tests
./gradlew test

# Specific module tests
./gradlew :app:test

# Specific test class
./gradlew test --tests "MealViewModelTest"

# Specific test method
./gradlew test --tests "MealViewModelTest.loads meals successfully when repository returns data"

# All instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Run tests in continuous mode
./gradlew test --continuous
```

### Android Studio

- **Run all tests in file** - Right-click test file → Run
- **Run single test** - Click green arrow next to test method
- **Run with coverage** - Right-click → Run with Coverage
- **Debug test** - Click debug icon next to test

### Command Line Options

```bash
# Show test output
./gradlew test --info

# Show stack traces
./gradlew test --stacktrace

# Fail fast (stop on first failure)
./gradlew test --fail-fast

# Parallel execution
./gradlew test --parallel --max-workers=4
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Android CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Run unit tests
      run: ./gradlew test --stacktrace
    
    - name: Run instrumented tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 29
        target: default
        arch: x86_64
        profile: Nexus 6
        script: ./gradlew connectedAndroidTest
    
    - name: Upload test reports
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: test-reports
        path: app/build/reports/tests/
```

## Test Naming Conventions

Use descriptive names that explain the scenario:

```kotlin
// Good - describes what is being tested
@Test
fun `loads meals successfully when repository returns data`()

@Test
fun `displays error message when network request fails`()

@Test
fun `aggregates ingredients from multiple meals in shopping list`()

@Test
fun `validates meal name is not empty before saving`()

// Avoid - unclear what is being tested
@Test
fun testLoadMeals()

@Test
fun test1()

@Test
fun testError()
```

**Naming Pattern:**
- Use backticks for readable test names
- Start with action: `loads`, `displays`, `validates`, `handles`
- Include condition: `when repository returns data`, `when network fails`
- Be specific about expected outcome

## Common Pitfalls

### Avoid

❌ **Testing implementation details**
```kotlin
// Bad - tests private method
@Test
fun `test private method`() {
    val result = viewModel.privateMethod()
    // ...
}

// Good - test public behavior
@Test
fun `loads meals and updates state`() {
    viewModel.loadMeals()
    // Assert on public state
}
```

❌ **Over-mocking**
```kotlin
// Bad - mocking everything
val mapper = mockk<MealMapper>()
val validator = mockk<MealValidator>()
val logger = mockk<Logger>()

// Good - only mock external dependencies
val repository = mockk<MealRepository>()
```

❌ **Brittle tests that break with refactoring**
```kotlin
// Bad - depends on internal state
assertEquals(3, viewModel.internalCounter)

// Good - test observable behavior
assertEquals(MealUiState.Success(meals), viewModel.uiState.value)
```

❌ **Testing framework code**
```kotlin
// Bad - testing Room/Compose framework
@Test
fun `room saves data correctly`() // Room is already tested

// Good - test your business logic
@Test
fun `repository maps database errors to domain errors`()
```

❌ **Slow tests**
```kotlin
// Bad - using real database in unit test
val database = Room.databaseBuilder(context, AppDatabase::class.java, "test.db").build()

// Good - mock DAO in unit test
val dao = mockk<MealDao>()
```

### Prefer

✅ **Testing public API and observable behavior**
✅ **Fake implementations for complex dependencies**
✅ **Tests that survive refactoring**
✅ **Fast, isolated unit tests**
✅ **Clear test names that describe behavior**

## Best Practices Summary

1. **Follow the test pyramid** - Many unit tests, fewer integration tests, minimal UI tests
2. **Test behavior, not implementation** - Focus on what, not how
3. **Keep tests fast** - Unit tests in milliseconds, instrumented tests in seconds
4. **Use descriptive names** - Test names should explain the scenario
5. **Arrange-Act-Assert** - Structure tests with Given-When-Then
6. **One assertion per test** - Or closely related assertions
7. **Mock external dependencies only** - Don't over-mock
8. **Use test data builders** - Create reusable test data
9. **Test error paths** - Don't just test happy paths
10. **Keep tests maintainable** - Tests should be easy to understand and modify

## Further Reading

- [Testing Guide (Android)](https://developer.android.com/training/testing)
- [Kotlin Coroutines Testing](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
- [MockK Documentation](https://mockk.io/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Dependency Injection Guide](dependency-injection.md) - Testing with Hilt
- [Architecture Overview](../architecture/overview.md) - Understanding layers for testing
