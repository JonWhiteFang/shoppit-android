# Testing Strategy

## Testing Philosophy

- **Test behavior, not implementation**: Focus on what the code does, not how it does it
- **Minimal but meaningful**: Write tests that catch real bugs, avoid over-testing edge cases
- **Fast feedback**: Unit tests should run in milliseconds, instrumented tests in seconds
- **Test pyramid**: Many unit tests, fewer integration tests, minimal UI tests

## Test Coverage Guidelines

### Domain Layer (High Coverage - 80%+)
- **Use Cases**: Test all business logic paths, error handling, and validation
- **Validators**: Test all validation rules and edge cases
- **Models**: Test any computed properties or business logic

### Data Layer (Medium Coverage - 60%+)
- **Repositories**: Test data flow, error mapping, and caching logic
- **Mappers**: Test entity/DTO/model conversions
- **DAOs**: Instrumented tests for complex queries only

### UI Layer (Selective Coverage - 40%+)
- **ViewModels**: Test state transitions, user actions, and error handling
- **Screens**: Test critical user flows and error states only

## Unit Testing Patterns

### ViewModel Testing

```kotlin
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
    fun `initial state is loading`() {
        assertEquals(MealUiState.Loading, viewModel.uiState.value)
    }
    
    @Test
    fun `loads meals successfully`() = runTest {
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
    fun `handles error when loading fails`() = runTest {
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
}
```

### Use Case Testing

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
    fun `adds meal successfully`() = runTest {
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
}
```

### Repository Testing

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
        val entities = listOf(MealEntity(id = 1, name = "Pasta"))
        every { dao.getAllMeals() } returns flowOf(entities)
        
        // When
        val result = repository.getMeals().first()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Pasta", result.getOrNull()?.first()?.name)
    }
    
    @Test
    fun `addMeal catches and maps exceptions`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        coEvery { dao.insertMeal(any()) } throws SQLiteException("Constraint violation")
        
        // When
        val result = repository.addMeal(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.DatabaseError)
    }
}
```

## Instrumented Testing Patterns

### Room DAO Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    private lateinit var database: ShoppitDatabase
    private lateinit var mealDao: MealDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShoppitDatabase::class.java)
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
        val meal = MealEntity(id = 1, name = "Pasta", createdAt = System.currentTimeMillis())
        
        // When
        mealDao.insertMeal(meal)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertEquals(1, meals.size)
        assertEquals("Pasta", meals[0].name)
    }
    
    @Test
    fun deleteMealRemovesFromDatabase() = runTest {
        // Given
        val meal = MealEntity(id = 1, name = "Pasta", createdAt = System.currentTimeMillis())
        mealDao.insertMeal(meal)
        
        // When
        mealDao.deleteMeal(meal)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertTrue(meals.isEmpty())
    }
}
```

### Compose UI Testing

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
            MealListScreen(
                uiState = MealUiState.Loading,
                onMealClick = {},
                onAddMealClick = {}
            )
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
            MealListScreen(
                uiState = MealUiState.Success(meals),
                onMealClick = {},
                onAddMealClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Pasta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salad").assertIsDisplayed()
    }
    
    @Test
    fun clickingMealTriggersCallback() {
        val meals = listOf(Meal(id = 1, name = "Pasta"))
        var clickedMeal: Meal? = null
        
        composeTestRule.setContent {
            MealListScreen(
                uiState = MealUiState.Success(meals),
                onMealClick = { clickedMeal = it },
                onAddMealClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Pasta").performClick()
        assertEquals(meals[0], clickedMeal)
    }
}
```

## Test Data Builders

Create reusable test data builders for complex objects:

```kotlin
object TestDataBuilders {
    fun meal(
        id: Long = 1,
        name: String = "Test Meal",
        ingredients: List<Ingredient> = emptyList(),
        notes: String = ""
    ) = Meal(id, name, ingredients, notes)
    
    fun ingredient(
        name: String = "Test Ingredient",
        quantity: String = "1",
        unit: String = "cup"
    ) = Ingredient(name, quantity, unit)
    
    fun mealEntity(
        id: Long = 1,
        name: String = "Test Meal",
        createdAt: Long = System.currentTimeMillis()
    ) = MealEntity(id, name, createdAt)
}
```

## Mocking Guidelines

### When to Mock
- External dependencies (network, database)
- Complex collaborators in unit tests
- Time-dependent operations

### When to Use Fakes
- Repositories in ViewModel tests
- Use cases in integration tests
- Simple in-memory implementations for testing

### MockK Patterns

```kotlin
// Relaxed mocks for simple cases
val repository: MealRepository = mockk(relaxed = true)

// Specific behavior
coEvery { repository.getMeals() } returns flowOf(Result.success(meals))

// Verify calls
coVerify { repository.addMeal(any()) }
coVerify(exactly = 0) { repository.deleteMeal(any()) }

// Argument capture
val slot = slot<Meal>()
coVerify { repository.addMeal(capture(slot)) }
assertEquals("Pasta", slot.captured.name)
```

## Coroutine Testing

### Setup

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
```

### Usage

```kotlin
@Test
fun testCoroutine() = runTest {
    // Use advanceUntilIdle() to execute all pending coroutines
    viewModel.loadData()
    advanceUntilIdle()
    
    // Use advanceTimeBy() for delayed operations
    advanceTimeBy(1000)
    
    // Assertions
    assertEquals(expected, viewModel.state.value)
}
```

## Test Organization

### File Structure
```
test/
├── com/shoppit/app/
│   ├── domain/
│   │   ├── usecase/
│   │   │   ├── AddMealUseCaseTest.kt
│   │   │   └── GetMealsUseCaseTest.kt
│   │   └── validator/
│   │       └── MealValidatorTest.kt
│   ├── data/
│   │   └── repository/
│   │       └── MealRepositoryImplTest.kt
│   └── ui/
│       └── meal/
│           └── MealViewModelTest.kt

androidTest/
├── com/shoppit/app/
│   ├── data/
│   │   └── local/
│   │       └── dao/
│   │           └── MealDaoTest.kt
│   └── ui/
│       └── meal/
│           └── MealListScreenTest.kt
```

## Running Tests

### Gradle Commands
```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests "MealViewModelTest"

# Specific test method
./gradlew test --tests "MealViewModelTest.loads meals successfully"

# All instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest
```

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Run unit tests
  run: ./gradlew test --stacktrace

- name: Run instrumented tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 29
    script: ./gradlew connectedAndroidTest
```

## Common Pitfalls

### Avoid
- Testing implementation details (private methods, internal state)
- Over-mocking (mock only external dependencies)
- Brittle tests that break with refactoring
- Testing framework code (Room, Retrofit, Compose)
- Slow tests (use fakes instead of real databases in unit tests)

### Prefer
- Testing public API and observable behavior
- Fake implementations for complex dependencies
- Tests that survive refactoring
- Fast, isolated unit tests
- Clear test names that describe behavior

## Test Naming Convention

Use descriptive names that explain the scenario:

```kotlin
// Good
@Test
fun `loads meals successfully when repository returns data`()

@Test
fun `displays error message when network request fails`()

@Test
fun `aggregates ingredients from multiple meals in shopping list`()

// Avoid
@Test
fun testLoadMeals()

@Test
fun test1()
```
