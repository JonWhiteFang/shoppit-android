# Hilt Quick Reference

Quick reference guide for common Hilt dependency injection patterns in Shoppit.

## Module Annotations

### @Module
Marks a class as a Hilt module that provides dependencies.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    // Providers here
}
```

### @InstallIn
Specifies which Hilt component the module should be installed in.

**Common Components:**
- `SingletonComponent::class` - Application-wide singletons
- `ViewModelComponent::class` - ViewModel-scoped dependencies
- `ActivityComponent::class` - Activity-scoped dependencies

## Providing Dependencies

### @Provides
Used in object modules to provide dependencies.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "shoppit_database"
        ).build()
    }
}
```

### @Binds
Used in abstract modules to bind interfaces to implementations.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
}
```

**When to use:**
- `@Provides` - For concrete types, third-party classes, or complex construction
- `@Binds` - For simple interface-to-implementation binding (more efficient)

## Scopes

### @Singleton
One instance for the entire application lifecycle.

```kotlin
@Provides
@Singleton
fun provideDatabase(...): AppDatabase
```

### No Scope
New instance every time it's injected.

```kotlin
@Provides
fun provideUseCase(...): AddMealUseCase
```

**Note:** ViewModelComponent has its own scope - dependencies are scoped to ViewModel lifecycle.

## Injection

### Constructor Injection
Preferred method for classes you control.

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val apiService: MealApiService
) : MealRepository {
    // Implementation
}
```

### Field Injection
Use only when constructor injection isn't possible.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var repository: MealRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // repository is now available
    }
}
```

### ViewModel Injection

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

## Android Components

### Application

```kotlin
@HiltAndroidApp
class ShoppitApplication : Application() {
    // Application code
}
```

### Activity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Activity code
}
```

### Fragment

```kotlin
@AndroidEntryPoint
class MealFragment : Fragment() {
    // Fragment code
}
```

### ViewModel

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val useCase: GetMealsUseCase
) : ViewModel() {
    // ViewModel code
}
```

## Qualifiers

Use qualifiers when you need multiple instances of the same type.

### Define Qualifier

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher
```

### Provide Qualified Dependencies

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

### Inject Qualified Dependencies

```kotlin
class MealRepository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    // Use ioDispatcher
}
```

## Testing

### Test Application

Use Hilt's built-in test application via custom runner:

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

### Test Module Replacement

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

### Test Class Setup

```kotlin
@HiltAndroidTest
class MealDaoTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var database: AppDatabase
    
    @Before
    fun setUp() {
        hiltRule.inject()
    }
    
    @Test
    fun testInsertMeal() {
        // Test code using database
    }
}
```

### Uninstall Modules

```kotlin
@HiltAndroidTest
@UninstallModules(RepositoryModule::class)
class MealIntegrationTest {
    // Test with custom module
}
```

## Common Patterns

### Providing DAOs

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "shoppit_database"
    ).build()
    
    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = 
        database.mealDao()
    
    @Provides
    fun provideIngredientDao(database: AppDatabase): IngredientDao = 
        database.ingredientDao()
}
```

### Binding Repositories

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
    
    @Binds
    abstract fun bindPlannerRepository(
        impl: PlannerRepositoryImpl
    ): PlannerRepository
}
```

### Providing Use Cases

```kotlin
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    fun provideGetMealsUseCase(
        repository: MealRepository
    ): GetMealsUseCase = GetMealsUseCase(repository)
    
    @Provides
    fun provideAddMealUseCase(
        repository: MealRepository,
        validator: MealValidator
    ): AddMealUseCase = AddMealUseCase(repository, validator)
}
```

## Troubleshooting

### "Cannot find symbol" errors
**Solution:** Clean and rebuild
```bash
./gradlew clean
./gradlew kspDebugKotlin
./gradlew assembleDebug
```

### "Dagger does not support injection into private fields"
**Solution:** Make field internal or use constructor injection

### "No instrumentation registered"
**Solution:** Check test runner in `build.gradle.kts`:
```kotlin
testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
```

### "Cannot process test roots and app roots"
**Solution:** Don't create custom test application with `@HiltAndroidApp`. Use `HiltTestApplication` via `HiltTestRunner`.

## Cheat Sheet

| Task | Pattern |
|------|---------|
| Provide singleton | `@Provides @Singleton` in `SingletonComponent` |
| Bind interface | `@Binds` in abstract module |
| Inject into class | `@Inject constructor(...)` |
| Inject into ViewModel | `@HiltViewModel` + `@Inject constructor(...)` |
| Inject into Activity | `@AndroidEntryPoint` + `@Inject lateinit var` |
| Replace module in tests | `@TestInstallIn(replaces = [...])` |
| Inject in tests | `@HiltAndroidTest` + `HiltAndroidRule` |

## Further Reading

- [Dependency Injection Guide](../guides/dependency-injection.md) - Detailed guide
- [Hilt Documentation](https://dagger.dev/hilt/) - Official docs
- [Testing Guide](../guides/testing.md) - Testing patterns
