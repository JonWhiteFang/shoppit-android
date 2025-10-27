# Detailed Architecture Design

Comprehensive architecture specification for the Shoppit Android application.

## Overview

Shoppit is an offline-first Android meal planning application that follows Clean Architecture principles with MVVM pattern. The app enables users to manage meals, plan weekly menus, and generate smart shopping lists from planned meals.

### Core Principles

- **Clean Architecture**: Clear separation of concerns across three layers
- **Offline-First**: Full functionality without network, sync when available
- **Reactive**: UI updates automatically with Flow-based data streams
- **Testable**: Layer boundaries enable comprehensive testing
- **Scalable**: Modular design supports future enhancements

## Architecture Layers

### Presentation Layer (UI)

**Location:** `app/src/main/java/com/shoppit/app/ui/`

**Responsibility:** Display data to users, handle user interactions, manage UI state, and navigate between screens.

**Technology Stack:**
- Jetpack Compose (BOM 2023.10.01) - Declarative UI
- Material3 - Design system
- ViewModel - State management
- StateFlow - Reactive state
- Navigation Compose 2.7.4 - Type-safe navigation

**Package Structure:**
```
ui/
├── meal/                      # Meal management feature
│   ├── MealListScreen.kt      # Meal list composable
│   ├── MealDetailScreen.kt    # Meal detail/edit composable
│   ├── MealViewModel.kt       # State management
│   └── MealUiState.kt         # UI state definition
├── planner/                   # Meal planning feature
│   ├── PlannerScreen.kt
│   ├── PlannerViewModel.kt
│   └── PlannerUiState.kt
├── shopping/                  # Shopping list feature
│   ├── ShoppingListScreen.kt
│   ├── ShoppingListViewModel.kt
│   └── ShoppingUiState.kt
├── common/                    # Reusable UI components
│   ├── ErrorScreen.kt
│   ├── LoadingScreen.kt
│   └── EmptyState.kt
├── navigation/                # Navigation setup
│   ├── NavHost.kt
│   └── Screen.kt              # Route definitions
└── theme/                     # Material3 theme
    ├── Color.kt
    ├── Type.kt
    └── Theme.kt
```

**File Naming Conventions:**
- Screens: `[Feature]Screen.kt` (e.g., `MealListScreen.kt`)
- ViewModels: `[Feature]ViewModel.kt` (e.g., `MealViewModel.kt`)
- UI State: `[Feature]UiState.kt` (e.g., `MealUiState.kt`)

**UI State Pattern:**
```kotlin
// Sealed interface for mutually exclusive states
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}

// Data class for multiple independent states
data class MealDetailUiState(
    val meal: Meal? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
```

**ViewModel Pattern:**
```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    // Update state with .update { }
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
}
```

**Screen Pattern:**
```kotlin
// Stateful screen composable
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealListContent(
        uiState = uiState,
        onMealClick = onMealClick,
        onAddMealClick = onAddMealClick,
        onDeleteMeal = viewModel::deleteMeal
    )
}

// Stateless content composable
@Composable
fun MealListContent(
    uiState: MealUiState,
    onMealClick: (Meal) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Meal) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is MealUiState.Loading -> LoadingScreen()
        is MealUiState.Success -> MealList(
            meals = uiState.meals,
            onMealClick = onMealClick,
            onDeleteMeal = onDeleteMeal
        )
        is MealUiState.Error -> ErrorScreen(message = uiState.message)
    }
}
```

### Domain Layer

**Location:** `app/src/main/java/com/shoppit/app/domain/`

**Responsibility:** Define business entities, implement business logic, define repository contracts, and handle domain errors. This layer has no Android dependencies (pure Kotlin).

**Technology Stack:**
- Pure Kotlin - No Android dependencies
- Kotlin Coroutines 1.7.3 - Asynchronous operations
- Result type - Error handling

**Package Structure:**
```
domain/
├── model/                     # Domain entities (pure Kotlin)
│   ├── Meal.kt
│   ├── Ingredient.kt
│   ├── MealPlan.kt
│   ├── ShoppingListItem.kt
│   └── MealType.kt            # Enums
├── repository/                # Repository interfaces
│   ├── MealRepository.kt
│   ├── PlannerRepository.kt
│   └── ShoppingRepository.kt
├── usecase/                   # Single-responsibility use cases
│   ├── AddMealUseCase.kt
│   ├── GetMealsUseCase.kt
│   ├── PlanWeekUseCase.kt
│   └── GenerateShoppingListUseCase.kt
└── validator/                 # Input validation logic
    ├── MealValidator.kt
    └── IngredientValidator.kt
```

**File Naming Conventions:**
- Models: Plain names (e.g., `Meal.kt`, `Ingredient.kt`)
- Use Cases: `[Action][Entity]UseCase.kt` (e.g., `AddMealUseCase.kt`)
- Repositories: `[Entity]Repository.kt` (e.g., `MealRepository.kt`)
- Validators: `[Entity]Validator.kt` (e.g., `MealValidator.kt`)

**Domain Models:**
```kotlin
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String
)

data class MealPlan(
    val id: Long = 0,
    val mealId: Long,
    val date: LocalDate,
    val mealType: MealType
)

data class ShoppingListItem(
    val name: String,
    val totalQuantity: String,
    val unit: String,
    val isChecked: Boolean = false
)

enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}
```

**Error Handling:**
```kotlin
sealed class AppError : Exception() {
    data class NetworkError(override val message: String) : AppError()
    data class DatabaseError(override val message: String) : AppError()
    data class ValidationError(override val message: String) : AppError()
    data class UnknownError(val throwable: Throwable) : AppError() {
        override val message: String get() = throwable.message ?: "Unknown error"
    }
}

typealias AppResult<T> = Result<T>
```

**Use Case Pattern:**
```kotlin
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Long> {
        return try {
            // Validate input
            validator.validate(meal).getOrThrow()
            
            // Execute business logic
            repository.addMeal(meal)
        } catch (e: Exception) {
            Result.failure(AppError.ValidationError(e.message ?: "Validation failed"))
        }
    }
}

class GetMealsUseCase @Inject constructor(
    private val repository: MealRepository
) {
    operator fun invoke(): Flow<Result<List<Meal>>> {
        return repository.getMeals()
    }
}
```

**Repository Interface:**
```kotlin
interface MealRepository {
    fun getMeals(): Flow<Result<List<Meal>>>
    suspend fun getMealById(id: Long): Result<Meal?>
    suspend fun addMeal(meal: Meal): Result<Long>
    suspend fun updateMeal(meal: Meal): Result<Unit>
    suspend fun deleteMeal(id: Long): Result<Unit>
}
```

### Data Layer

**Location:** `app/src/main/java/com/shoppit/app/data/`

**Responsibility:** Persist data locally, implement repository interfaces, map between data and domain models, and handle data errors.

**Technology Stack:**
- Room 2.6.0 - Local database
- SQLite - Database engine
- Retrofit 2.9.0 + OkHttp 4.12.0 - Network layer (future)
- KSP - Annotation processing

**Package Structure:**
```
data/
├── local/                     # Local data sources
│   ├── dao/                   # Room DAOs
│   │   ├── MealDao.kt
│   │   ├── IngredientDao.kt
│   │   ├── MealPlanDao.kt
│   │   └── ShoppingListDao.kt
│   ├── entity/                # Database entities
│   │   ├── MealEntity.kt
│   │   ├── IngredientEntity.kt
│   │   ├── MealPlanEntity.kt
│   │   └── ShoppingListItemEntity.kt
│   └── database/              # Database configuration
│       ├── AppDatabase.kt
│       └── Converters.kt      # Type converters
├── remote/                    # Network data sources (future)
│   ├── api/                   # Retrofit service interfaces
│   │   └── MealApiService.kt
│   └── dto/                   # Network data transfer objects
│       └── MealDto.kt
├── repository/                # Repository implementations
│   ├── MealRepositoryImpl.kt
│   ├── PlannerRepositoryImpl.kt
│   └── ShoppingRepositoryImpl.kt
└── mapper/                    # Entity/DTO/model conversions
    ├── MealMapper.kt
    └── IngredientMapper.kt
```

**File Naming Conventions:**
- DAOs: `[Entity]Dao.kt` (e.g., `MealDao.kt`)
- Entities: `[Entity]Entity.kt` (e.g., `MealEntity.kt`)
- Repositories: `[Entity]RepositoryImpl.kt` (e.g., `MealRepositoryImpl.kt`)
- DTOs: `[Entity]Dto.kt` (e.g., `MealDto.kt`)
- Mappers: `[Entity]Mapper.kt` (e.g., `MealMapper.kt`)

**Database Schema:**
```kotlin
@Database(
    entities = [
        MealEntity::class,
        IngredientEntity::class,
        MealPlanEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
}
```

**Entity Example:**
```kotlin
@Entity(
    tableName = "meals",
    indices = [Index(value = ["name"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val notes: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["meal_id"])]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    
    val name: String,
    val quantity: String,
    val unit: String
)
```

**DAO Example:**
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<MealEntity>>
    
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    @Update
    suspend fun updateMeal(meal: MealEntity)
    
    @Delete
    suspend fun deleteMeal(meal: MealEntity)
    
    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealWithIngredients(id: Long): MealWithIngredients?
}

data class MealWithIngredients(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "meal_id"
    )
    val ingredients: List<IngredientEntity>
)
```

**Repository Implementation:**
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val ingredientDao: IngredientDao
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> {
        return mealDao.getAllMeals()
            .map { entities -> entities.map { it.toDomainModel() } }
            .map { Result.success(it) }
            .catch { emit(Result.failure(AppError.DatabaseError(it.message ?: "Database error"))) }
            .flowOn(Dispatchers.IO)
    }
    
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val mealId = mealDao.insertMeal(meal.toEntity())
            meal.ingredients.forEach { ingredient ->
                ingredientDao.insertIngredient(ingredient.toEntity(mealId))
            }
            Result.success(mealId)
        } catch (e: Exception) {
            Result.failure(AppError.DatabaseError(e.message ?: "Failed to add meal"))
        }
    }
}
```

**Mapper Extensions:**
```kotlin
// Entity to Domain
fun MealEntity.toDomainModel(): Meal = Meal(
    id = id,
    name = name,
    ingredients = emptyList(), // Loaded separately
    notes = notes,
    createdAt = LocalDateTime.ofEpochSecond(createdAt, 0, ZoneOffset.UTC)
)

// Domain to Entity
fun Meal.toEntity(): MealEntity = MealEntity(
    id = id,
    name = name,
    notes = notes,
    createdAt = createdAt.toEpochSecond(ZoneOffset.UTC)
)

fun Ingredient.toEntity(mealId: Long): IngredientEntity = IngredientEntity(
    mealId = mealId,
    name = name,
    quantity = quantity,
    unit = unit
)
```

## State Management Patterns

### ViewModel State Exposure

Always expose immutable state from ViewModels:

```kotlin
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    // Private mutable state
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    
    // Public immutable state
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    // Update state with .update { }
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
}
```

### Coroutines and Flow Usage

- Use `viewModelScope` for ViewModel coroutines
- Repository functions return `Flow<T>` for reactive data or `suspend` for one-shot operations
- Apply `flowOn(Dispatchers.IO)` for database/network operations
- DAOs return `Flow<T>` for observed queries, `suspend` for mutations

```kotlin
// Repository with Flow for reactive data
override fun getMeals(): Flow<Result<List<Meal>>> {
    return mealDao.getAllMeals()
        .map { entities -> entities.map { it.toDomainModel() } }
        .map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
        .flowOn(Dispatchers.IO)
}

// Repository with suspend for one-shot operations
override suspend fun addMeal(meal: Meal): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        mealDao.insertMeal(meal.toEntity())
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Error Handling Strategy

### Error Flow

1. **Data Layer**: Catches exceptions, maps to domain errors
2. **Domain Layer**: Validates input, returns Result types
3. **Presentation Layer**: Displays user-friendly error messages

```kotlin
// Data Layer - Catch and map exceptions
override suspend fun addMeal(meal: Meal): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        mealDao.insertMeal(meal.toEntity())
        Result.success(Unit)
    } catch (e: SQLiteException) {
        Result.failure(AppError.DatabaseError("Failed to save meal"))
    } catch (e: Exception) {
        Result.failure(AppError.UnknownError(e))
    }
}

// Domain Layer - Validate and return Result
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Unit> {
        return try {
            validator.validate(meal).getOrThrow()
            repository.addMeal(meal)
        } catch (e: Exception) {
            Result.failure(AppError.ValidationError(e.message ?: "Validation failed"))
        }
    }
}

// Presentation Layer - Display error
fun addMeal(meal: Meal) {
    viewModelScope.launch {
        addMealUseCase(meal).fold(
            onSuccess = { _uiState.update { MealUiState.Success(meals) } },
            onFailure = { error ->
                _uiState.update { 
                    MealUiState.Error(
                        when (error) {
                            is AppError.ValidationError -> error.message
                            is AppError.DatabaseError -> "Failed to save meal"
                            else -> "An unexpected error occurred"
                        }
                    )
                }
            }
        )
    }
}
```

## Dependency Injection (Hilt)

**Location:** `app/src/main/java/com/shoppit/app/di/`

**File Naming:** `[Purpose]Module.kt` (e.g., `DatabaseModule.kt`, `NetworkModule.kt`)

### Module Structure

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
    
    @Provides
    fun provideMealDao(database: AppDatabase): MealDao {
        return database.mealDao()
    }
    
    @Provides
    fun provideIngredientDao(database: AppDatabase): IngredientDao {
        return database.ingredientDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
    
    @Binds
    @Singleton
    abstract fun bindPlannerRepository(
        impl: PlannerRepositoryImpl
    ): PlannerRepository
}

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    fun provideGetMealsUseCase(
        repository: MealRepository
    ): GetMealsUseCase {
        return GetMealsUseCase(repository)
    }
    
    @Provides
    fun provideAddMealUseCase(
        repository: MealRepository,
        validator: MealValidator
    ): AddMealUseCase {
        return AddMealUseCase(repository, validator)
    }
}
```

### Injection Patterns

```kotlin
// Constructor injection in ViewModel
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase
) : ViewModel()

// Constructor injection in Repository
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val ingredientDao: IngredientDao
) : MealRepository

// Constructor injection in Use Case
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
)
```

## Testing Architecture

### Test Organization

```
test/                          # Unit tests
├── domain/
│   ├── usecase/
│   │   ├── AddMealUseCaseTest.kt
│   │   └── GetMealsUseCaseTest.kt
│   └── validator/
│       └── MealValidatorTest.kt
├── data/
│   └── repository/
│       └── MealRepositoryImplTest.kt
└── ui/
    └── meal/
        └── MealViewModelTest.kt

androidTest/                   # Instrumented tests
├── data/
│   └── local/
│       └── dao/
│           └── MealDaoTest.kt
└── ui/
    └── meal/
        └── MealListScreenTest.kt
```

### Testing Patterns

**ViewModel Testing:**
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
    fun `loads meals successfully`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Pasta", ingredients = emptyList()))
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

**Repository Testing:**
```kotlin
class MealRepositoryImplTest {
    private lateinit var mealDao: MealDao
    private lateinit var repository: MealRepositoryImpl
    
    @Before
    fun setup() {
        mealDao = mockk()
        repository = MealRepositoryImpl(mealDao)
    }
    
    @Test
    fun `getMeals returns flow of meals`() = runTest {
        // Given
        val entities = listOf(MealEntity(id = 1, name = "Pasta", notes = "", createdAt = 0L))
        every { mealDao.getAllMeals() } returns flowOf(entities)
        
        // When
        val result = repository.getMeals().first()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }
}
```

## Performance Optimization

### Database Optimization

- **Indexing**: Add indexes on frequently queried columns
- **Foreign Keys**: Use CASCADE for automatic cleanup
- **Transactions**: Use `@Transaction` for complex operations
- **Flow**: Use Flow for reactive queries to minimize memory usage

### UI Performance

- **Compose**: Use `remember` for expensive computations
- **State**: Use `derivedStateOf` for computed values
- **Lists**: Use `LazyColumn` with stable keys
- **Recomposition**: Mark data classes as `@Immutable` or `@Stable`

### Memory Management

- **Lifecycle**: Use `viewModelScope` for automatic cleanup
- **Coroutines**: Cancel coroutines when no longer needed
- **Database**: Close database connections properly

## Key Architectural Decisions

### Why Clean Architecture?
- **Testability**: Each layer can be tested independently
- **Maintainability**: Clear boundaries make changes easier
- **Flexibility**: Easy to swap implementations
- **Scalability**: Well-organized structure supports growth

### Why Offline-First?
- **User Experience**: App works without internet
- **Performance**: No network latency for common operations
- **Reliability**: Not dependent on network availability
- **Data Ownership**: User data stored locally

### Why StateFlow over LiveData?
- **Kotlin-first**: Better integration with coroutines
- **Type-safe**: Compile-time safety
- **Composable-friendly**: Natural integration with Compose
- **Testability**: Easier to test with coroutines-test

### Why Hilt over Manual DI?
- **Compile-time safety**: Errors caught at compile time
- **Android integration**: Built for Android lifecycle
- **Scoping**: Automatic lifecycle management
- **Testing support**: Easy to swap implementations in tests

## Further Reading

- **[Architecture Overview](overview.md)** - High-level architecture and principles
- **[Data Flow](data-flow.md)** - Detailed data flow patterns
- **[State Management](state-management.md)** - ViewModel and Compose state patterns
- **[Dependency Injection Guide](../guides/dependency-injection.md)** - Hilt configuration
- **[Testing Guide](../guides/testing.md)** - Testing strategies for each layer
- **[Code Style Guide](../guides/code-style.md)** - Coding conventions and standards
