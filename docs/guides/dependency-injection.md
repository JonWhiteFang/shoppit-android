# Dependency Injection with Hilt

This guide covers how dependency injection is configured and used in the Shoppit application using Hilt.

## Overview

Shoppit uses [Hilt 2.48](https://dagger.dev/hilt/) for dependency injection, which provides:
- **Compile-time dependency validation** - Catches errors at build time
- **Automatic lifecycle management** - Dependencies scoped to Android components
- **Simplified testing** - Easy module replacement with `@TestInstallIn`
- **Integration with Android components** - Works seamlessly with Activities, Fragments, ViewModels
- **KSP annotation processing** - Faster builds compared to KAPT

### Why Hilt?

- **Reduces boilerplate** - No manual factory classes or service locators
- **Type safety** - Compile-time verification of dependency graph
- **Testability** - Easy to swap implementations for testing
- **Standard solution** - Official Android recommendation for DI
- **ViewModel integration** - Built-in support for `@HiltViewModel`

## Architecture

### Module Structure

The application uses three main Hilt modules:

```
di/
├── DatabaseModule.kt      # Database and DAO dependencies
├── RepositoryModule.kt    # Repository implementations
└── UseCaseModule.kt       # Use case dependencies (optional)
```

### Component Hierarchy

Hilt provides predefined Android components with automatic lifecycle management:

```
SingletonComponent (Application scope)
├── ActivityComponent (Activity scope)
│   └── FragmentComponent (Fragment scope)
│       └── ViewComponent (View scope)
└── ViewModelComponent (ViewModel scope)
    └── ViewWithFragmentComponent

ServiceComponent (Service scope)
```

**Shoppit uses**:
- `SingletonComponent` - Database, repositories (app-wide singletons)
- `ViewModelComponent` - Use cases (ViewModel-scoped)

**Scoping Rules**:
- `@Singleton` in `SingletonComponent` - One instance for entire app
- No scope in `ViewModelComponent` - New instance per ViewModel
- Unscoped dependencies - New instance every injection

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
    
    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = 
        database.mealDao()
    
    @Provides
    fun provideMealPlanDao(database: AppDatabase): MealPlanDao = 
        database.mealPlanDao()
    
    @Provides
    fun provideShoppingListDao(database: AppDatabase): ShoppingListDao = 
        database.shoppingListDao()
}
```

**Key Points:**
- Uses `@Singleton` to ensure single database instance
- Configured with `fallbackToDestructiveMigration()` for development (remove for production)
- DAOs are provided as functions that call database accessor methods
- All database-related dependencies are singletons

**Production Configuration**:
For production, replace `fallbackToDestructiveMigration()` with proper migrations:
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

### RepositoryModule

Binds repository interfaces to their implementations.

**Location:** `app/src/main/java/com/shoppit/app/di/RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
    
    @Binds
    abstract fun bindMealPlanRepository(
        impl: MealPlanRepositoryImpl
    ): MealPlanRepository
    
    @Binds
    abstract fun bindShoppingListRepository(
        impl: ShoppingListRepositoryImpl
    ): ShoppingListRepository
}
```

**Key Points:**
- Abstract class allows use of `@Binds` for interface binding
- `@Binds` is more efficient than `@Provides` (no implementation code generated)
- Repositories are singletons by default in `SingletonComponent`
- Implementation classes must have `@Inject constructor()`

**When to use `@Binds` vs `@Provides`**:
- Use `@Binds` when binding interface to implementation (preferred)
- Use `@Provides` when you need custom instantiation logic

### UseCaseModule

Provides use case instances for ViewModels (optional - use cases can use constructor injection directly).

**Location:** `app/src/main/java/com/shoppit/app/di/UseCaseModule.kt`

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    // Usually not needed - use cases can use constructor injection
    // Only add providers if you need custom instantiation logic
    
    // Example if needed:
    // @Provides
    // fun provideAddMealUseCase(
    //     repository: MealRepository
    // ): AddMealUseCase = AddMealUseCase(repository)
}
```

**Key Points:**
- Installed in `ViewModelComponent` for ViewModel-scoped dependencies
- Use cases are created per ViewModel instance (not singletons)
- Automatically cleaned up when ViewModel is cleared
- **Often not needed** - use cases with `@Inject constructor()` work without module

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
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
```

**Key Points:**
- `@HiltAndroidApp` triggers Hilt code generation
- Must be declared in `AndroidManifest.xml`
- Initializes Hilt's dependency graph

### Android Components

Activities and ViewModels must be annotated to use Hilt:

```kotlin
// Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppitTheme {
                ShoppitNavHost()
            }
        }
    }
}

// ViewModel
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    // ViewModel implementation
}
```

## Usage Patterns

### Constructor Injection (Preferred)

The preferred method for dependency injection:

```kotlin
// Repository implementation
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        mealDao.getAllMeals()
            .catch { e -> emit(Result.failure(AppError.DatabaseError(e.message))) }
            .collect { entities ->
                emit(Result.success(entities.map { it.toDomainModel() }))
            }
    }.flowOn(Dispatchers.IO)
}

// Use case
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(meal: Meal): Result<Long> {
        return repository.addMeal(meal)
    }
}

// ViewModel
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
}
```

### Field Injection (Avoid if Possible)

Use only when constructor injection is not possible (e.g., Android framework classes without Hilt support):

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

**Note**: With Compose, fragments are rarely needed. Use ViewModels instead.

### ViewModel Injection in Compose

ViewModels use constructor injection with `@HiltViewModel`:

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    // ViewModel implementation
}

// In Composable - stateful screen
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealClick: (Meal) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = onMealClick,
        onAddMeal = viewModel::addMeal
    )
}

// Stateless content composable
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
    onAddMeal: (Meal) -> Unit
) {
    // UI implementation
}
```

### Injection Scopes

```kotlin
// Singleton - one instance for entire app
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        // ...
    }
}

// ViewModel-scoped - new instance per ViewModel
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun provideUseCase(repository: MealRepository): GetMealsUseCase {
        return GetMealsUseCase(repository)
    }
}

// Unscoped - new instance every injection
@Module
@InstallIn(SingletonComponent::class)
object UtilityModule {
    @Provides
    fun provideJsonParser(): JsonParser {
        return JsonParser() // New instance each time
    }
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

dependencies {
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
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
    
    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = 
        database.mealDao()
}
```

**Key Points:**
- Uses `@TestInstallIn` to replace production module
- In-memory database doesn't persist between tests
- `allowMainThreadQueries()` simplifies test code
- Each test gets a fresh database

### Writing Tests

#### Unit Tests (No Hilt)

For unit tests, use MockK to mock dependencies - no Hilt needed:

```kotlin
@ExperimentalCoroutinesApi
class MealViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: MealViewModel
    private lateinit var getMealsUseCase: GetMealsUseCase
    private lateinit var addMealUseCase: AddMealUseCase
    
    @Before
    fun setUp() {
        getMealsUseCase = mockk()
        addMealUseCase = mockk()
        viewModel = MealViewModel(getMealsUseCase, addMealUseCase)
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
}
```

#### Instrumented Tests with Hilt

For instrumented tests that need real dependencies:

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: AppDatabase
    
    private lateinit var mealDao: MealDao
    
    @Before
    fun setUp() {
        hiltRule.inject()
        mealDao = database.mealDao()
    }
    
    @After
    fun tearDown() {
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
    }
}
```

#### Compose UI Tests with Hilt

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MealListScreenTest {
    
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
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
}
```

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
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var repository: MealRepository // Will be FakeMealRepository
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testWithFakeRepository() {
        // Test using fake repository
        assertTrue(repository is FakeMealRepository)
    }
}
```

## Common Patterns

### Providing Context

```kotlin
// Application context (preferred)
class MyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Use context
}

// Activity context (rarely needed)
@Module
@InstallIn(ActivityComponent::class)
object ActivityModule {
    @Provides
    fun provideActivityContext(
        activity: Activity
    ): Context = activity
}
```

### Qualifiers for Multiple Implementations

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalDataSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteDataSource

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    
    @Binds
    @LocalDataSource
    abstract fun bindLocalDataSource(
        impl: LocalDataSourceImpl
    ): DataSource
    
    @Binds
    @RemoteDataSource
    abstract fun bindRemoteDataSource(
        impl: RemoteDataSourceImpl
    ): DataSource
}

// Usage
class MyRepository @Inject constructor(
    @LocalDataSource private val localDataSource: DataSource,
    @RemoteDataSource private val remoteDataSource: DataSource
) {
    // Use both data sources
}
```

### Providing Third-Party Libraries

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.shoppit.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideMealApiService(retrofit: Retrofit): MealApiService {
        return retrofit.create(MealApiService::class.java)
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
- Clear naming conventions (`DatabaseModule`, `NetworkModule`)

✅ **Use `@Binds` over `@Provides` when possible**
```kotlin
@Binds
abstract fun bindRepository(impl: MealRepositoryImpl): MealRepository
```

✅ **Inject `@ApplicationContext` when you need Context**
```kotlin
class MyRepository @Inject constructor(
    @ApplicationContext private val context: Context
)
```

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

❌ **Don't inject Android Context directly without qualifier**
```kotlin
// Bad
class MyRepository @Inject constructor(
    private val context: Context // Which context?
)

// Good
class MyRepository @Inject constructor(
    @ApplicationContext private val context: Context
)
```

❌ **Don't make everything a singleton**
```kotlin
// Bad - use case doesn't need to be singleton
@Provides
@Singleton
fun provideUseCase(repo: Repository): UseCase

// Good - new instance per ViewModel
@Provides
fun provideUseCase(repo: Repository): UseCase
```

❌ **Don't use Hilt in unit tests**
```kotlin
// Bad - unnecessary complexity
@HiltAndroidTest
class ViewModelTest {
    @Inject lateinit var useCase: UseCase
}

// Good - use MockK
class ViewModelTest {
    private val useCase: UseCase = mockk()
    private val viewModel = MyViewModel(useCase)
}
```

## Troubleshooting

### Common Issues

#### "Cannot find symbol" errors

**Problem:** Hilt annotation processor not running

**Solution:** 
```bash
./gradlew clean
./gradlew kspDebugKotlin
./gradlew assembleDebug
```

**Verify KSP configuration in `build.gradle.kts`:**
```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
```

#### "Dagger does not support injection into private fields"

**Problem:** Trying to inject into private field

**Solution:** Make field internal or use constructor injection (preferred)

#### Test fails with "No instrumentation registered"

**Problem:** Test runner not configured

**Solution:** Verify `testInstrumentationRunner` in `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
    }
}
```

#### "Cannot process test roots and app roots in same compilation"

**Problem:** Trying to use `@HiltAndroidApp` in test code

**Solution:** Use Hilt's built-in `HiltTestApplication` via `HiltTestRunner`

#### "MissingBinding" error

**Problem:** Hilt can't find a provider for a dependency

**Solution:**
1. Check that the dependency has `@Inject constructor()`
2. Or add a `@Provides` function in a module
3. Verify the module is installed in the correct component

#### Build is slow after adding Hilt

**Problem:** Annotation processing takes time

**Solution:**
- Use KSP instead of KAPT (already configured in Shoppit)
- Enable Gradle build cache
- Use incremental compilation

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
2. Create implementation in `data/repository/` with `@Inject constructor()`
3. Add binding to `RepositoryModule`:

```kotlin
@Binds
abstract fun bindMealRepository(
    impl: MealRepositoryImpl
): MealRepository
```

### Adding a Use Case

1. Create use case in `domain/usecase/` with `@Inject constructor()`
2. No module needed - constructor injection works automatically
3. (Optional) Add provider to `UseCaseModule` if custom logic needed

### Adding a Network Service

1. Create Retrofit interface in `data/remote/api/`
2. Add provider to `NetworkModule`:

```kotlin
@Provides
@Singleton
fun provideMealApiService(retrofit: Retrofit): MealApiService {
    return retrofit.create(MealApiService::class.java)
}
```

## Migration Guide

### From Manual DI to Hilt

If you have manual dependency injection:

1. **Add Hilt dependencies** to `build.gradle.kts`
2. **Annotate Application class** with `@HiltAndroidApp`
3. **Create modules** for existing factories
4. **Replace manual injection** with `@Inject constructor()`
5. **Annotate Activities** with `@AndroidEntryPoint`
6. **Annotate ViewModels** with `@HiltViewModel`
7. **Update tests** to use `@HiltAndroidTest`

### From KAPT to KSP

Shoppit already uses KSP, but if migrating:

1. Replace `kapt` with `ksp` in dependencies
2. Replace `kaptAndroidTest` with `kspAndroidTest`
3. Remove KAPT plugin
4. Add KSP plugin
5. Clean and rebuild

## Further Reading

- [Hilt Official Documentation](https://dagger.dev/hilt/)
- [Hilt Testing Guide](https://developer.android.com/training/dependency-injection/hilt-testing)
- [Dependency Injection Best Practices](https://developer.android.com/training/dependency-injection)
- [Testing Guide](testing.md) - Project-specific testing patterns
- [Hilt Quick Reference](../reference/hilt-quick-reference.md) - Quick lookup for common patterns
- [Architecture Overview](../architecture/overview.md) - How DI fits into Clean Architecture
