# Dependency Injection with Hilt

This guide covers how dependency injection is configured and used in the Shoppit application using Hilt.

## Overview

Shoppit uses [Hilt](https://dagger.dev/hilt/) for dependency injection, which provides:
- Compile-time dependency validation
- Automatic lifecycle management
- Simplified testing with module replacement
- Integration with Android components

## Architecture

### Module Structure

The application uses three main Hilt modules:

```
di/
├── DatabaseModule.kt      # Database and DAO dependencies
├── RepositoryModule.kt    # Repository implementations
└── UseCaseModule.kt       # Use case dependencies
```

### Component Hierarchy

```
SingletonComponent (Application scope)
├── DatabaseModule
└── RepositoryModule

ViewModelComponent (ViewModel scope)
└── UseCaseModule
```

## Core Modules

### DatabaseModule

Provides Room database and DAO instances as singletons.

**Location:** `app/src/main/java/com/shoppit/app/di/DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "shoppit_database"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    // Future DAO providers will be added here
    // Example:
    // @Provides
    // fun provideMealDao(database: AppDatabase): MealDao = 
    //     database.mealDao()
}
```

**Key Points:**
- Uses `@Singleton` to ensure single database instance
- Configured with fallback to destructive migration for development
- DAOs will be provided as needed when features are implemented

### RepositoryModule

Binds repository interfaces to their implementations.

**Location:** `app/src/main/java/com/shoppit/app/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // Repository bindings will be added in future tasks
    
    // Example:
    // @Binds
    // abstract fun bindMealRepository(
    //     impl: MealRepositoryImpl
    // ): MealRepository
}
```

**Key Points:**
- Abstract class allows use of `@Binds` for interface binding
- More efficient than `@Provides` for simple interface implementations
- Repositories are singletons by default in SingletonComponent

### UseCaseModule

Provides use case instances for ViewModels.

**Location:** `app/src/main/java/com/shoppit/app/di/UseCaseModule.kt`

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    // Use case providers will be added in future tasks
    
    // Example:
    // @Provides
    // fun provideAddMealUseCase(
    //     repository: MealRepository
    // ): AddMealUseCase = AddMealUseCase(repository)
}
```

**Key Points:**
- Installed in `ViewModelComponent` for ViewModel-scoped dependencies
- Use cases are created per ViewModel instance
- Automatically cleaned up when ViewModel is cleared

## Application Setup

### ShoppitApplication

The application class is annotated with `@HiltAndroidApp` to enable Hilt.

**Location:** `app/src/main/java/com/shoppit/app/ShoppitApplication.kt`

```kotlin
@HiltAndroidApp
class ShoppitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Timber initialization and other setup
    }
}
```

### Android Components

Activities and ViewModels must be annotated to use Hilt:

```kotlin
// Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Hilt will inject dependencies
}

// ViewModel
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    // ViewModel implementation
}
```

## Usage Patterns

### Constructor Injection

The preferred method for dependency injection:

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val apiService: MealApiService
) : MealRepository {
    // Implementation
}
```

### Field Injection

Use only when constructor injection is not possible:

```kotlin
@AndroidEntryPoint
class MealFragment : Fragment() {
    
    @Inject
    lateinit var repository: MealRepository
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // repository is now available
    }
}
```

### ViewModel Injection

ViewModels use constructor injection with `@HiltViewModel`:

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    // ViewModel implementation
}

// In Composable
@Composable
fun MealScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    // Use viewModel
}
```

## Testing with Hilt

### Test Infrastructure

The project includes a complete Hilt testing setup:

#### HiltTestRunner

Custom test runner that replaces the application with `HiltTestApplication`.

**Location:** `app/src/androidTest/java/com/shoppit/app/HiltTestRunner.kt`

```kotlin
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(
            cl, 
            HiltTestApplication::class.java.name, 
            context
        )
    }
}
```

**Configuration:** Set in `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
    }
}
```

#### TestDatabaseModule

Replaces production database with in-memory database for tests.

**Location:** `app/src/androidTest/java/com/shoppit/app/di/TestDatabaseModule.kt`

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideTestDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
}
```

**Key Points:**
- Uses `@TestInstallIn` to replace production module
- In-memory database doesn't persist between tests
- `allowMainThreadQueries()` simplifies test code

### Writing Tests

#### Unit Tests

For unit tests, use MockK to mock dependencies:

```kotlin
@ExperimentalCoroutinesApi
class MealViewModelTest : ViewModelTest() {
    
    private lateinit var viewModel: MealViewModel
    private lateinit var getMealsUseCase: GetMealsUseCase
    
    @Before
    fun setUp() {
        getMealsUseCase = mockk()
        viewModel = MealViewModel(getMealsUseCase)
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
    }
}
```

#### Instrumented Tests

For instrumented tests with Hilt:

```kotlin
@HiltAndroidTest
class MealDaoTest : DatabaseTest() {
    
    @Test
    fun insertAndRetrieveMeal() = runTest {
        // Given
        val meal = MealEntity(
            id = 1, 
            name = "Pasta", 
            createdAt = System.currentTimeMillis()
        )
        
        // When
        database.mealDao().insertMeal(meal)
        val meals = database.mealDao().getAllMeals().first()
        
        // Then
        assertEquals(1, meals.size)
        assertEquals("Pasta", meals[0].name)
    }
}
```

**Base Test Classes:**
- `ViewModelTest` - For ViewModel unit tests with coroutine support
- `RepositoryTest` - For repository unit tests with coroutine support
- `DatabaseTest` - For DAO instrumented tests with Hilt injection

### Custom Test Modules

To replace dependencies in specific tests:

```kotlin
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RepositoryModule::class]
)
abstract class FakeRepositoryModule {
    
    @Binds
    abstract fun bindMealRepository(
        fake: FakeMealRepository
    ): MealRepository
}

@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
class MealIntegrationTest {
    
    @Inject
    lateinit var repository: MealRepository // Will be FakeMealRepository
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testWithFakeRepository() {
        // Test using fake repository
    }
}
```

## Best Practices

### Do's

✅ **Use constructor injection** whenever possible
```kotlin
class MyRepository @Inject constructor(
    private val dao: MyDao
) : Repository
```

✅ **Inject interfaces, not implementations**
```kotlin
class MyViewModel @Inject constructor(
    private val repository: MealRepository // Interface
) : ViewModel()
```

✅ **Use appropriate scopes**
- `@Singleton` for app-wide dependencies (database, network)
- `ViewModelComponent` for ViewModel dependencies
- No scope for stateless utilities

✅ **Keep modules focused**
- One module per concern (database, network, repositories)
- Clear naming conventions

### Don'ts

❌ **Don't use field injection in classes you control**
```kotlin
// Bad
class MyRepository {
    @Inject lateinit var dao: MyDao
}

// Good
class MyRepository @Inject constructor(
    private val dao: MyDao
)
```

❌ **Don't create circular dependencies**
```kotlin
// Bad: A depends on B, B depends on A
class A @Inject constructor(private val b: B)
class B @Inject constructor(private val a: A)
```

❌ **Don't inject Android Context directly**
```kotlin
// Bad
class MyRepository @Inject constructor(
    private val context: Context
)

// Good
class MyRepository @Inject constructor(
    @ApplicationContext private val context: Context
)
```

## Troubleshooting

### Common Issues

#### "Cannot find symbol" errors

**Problem:** Hilt annotation processor not running

**Solution:** 
1. Clean and rebuild: `./gradlew clean build`
2. Verify KSP is configured in `build.gradle.kts`
3. Check that `@HiltAndroidApp` is on Application class

#### "Dagger does not support injection into private fields"

**Problem:** Trying to inject into private field

**Solution:** Make field internal or use constructor injection

#### Test fails with "No instrumentation registered"

**Problem:** Test runner not configured

**Solution:** Verify `testInstrumentationRunner` in `build.gradle.kts`:
```kotlin
testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
```

#### "Cannot process test roots and app roots in same compilation"

**Problem:** Custom test application with `@HiltAndroidApp` annotation

**Solution:** Use Hilt's built-in `HiltTestApplication` via `HiltTestRunner`

## Adding New Dependencies

### Adding a DAO

1. Create the DAO interface in `data/local/dao/`
2. Add abstract function to `AppDatabase`
3. Add provider to `DatabaseModule`:

```kotlin
@Provides
fun provideMealDao(database: AppDatabase): MealDao = 
    database.mealDao()
```

### Adding a Repository

1. Create interface in `domain/repository/`
2. Create implementation in `data/repository/`
3. Add binding to `RepositoryModule`:

```kotlin
@Binds
abstract fun bindMealRepository(
    impl: MealRepositoryImpl
): MealRepository
```

### Adding a Use Case

1. Create use case in `domain/usecase/`
2. Add provider to `UseCaseModule`:

```kotlin
@Provides
fun provideAddMealUseCase(
    repository: MealRepository
): AddMealUseCase = AddMealUseCase(repository)
```

## Further Reading

- [Hilt Official Documentation](https://dagger.dev/hilt/)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Dependency Injection Best Practices](https://developer.android.com/training/dependency-injection)
- [Testing Guide](testing.md) - Project-specific testing patterns
