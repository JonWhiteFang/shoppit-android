# Design Document

## Overview

The Meal Management feature implements core CRUD operations for meals using Clean Architecture principles. The design follows an offline-first approach with Room database as the single source of truth, reactive data flow using Kotlin Flow, and a clear separation between domain logic and UI concerns. The feature provides a foundation for meal planning and shopping list generation.

## Architecture

### Layer Structure

```
com.shoppit.app/
├── ui/
│   └── meal/
│       ├── MealListScreen.kt          # Meal library screen
│       ├── MealDetailScreen.kt        # Meal detail view
│       ├── AddEditMealScreen.kt       # Create/edit meal form
│       ├── MealViewModel.kt           # State management for list
│       ├── MealDetailViewModel.kt     # State management for detail
│       ├── AddEditMealViewModel.kt    # State management for form
│       └── MealUiState.kt             # UI state definitions
├── domain/
│   ├── model/
│   │   ├── Meal.kt                    # Domain meal entity
│   │   └── Ingredient.kt              # Domain ingredient entity
│   ├── repository/
│   │   └── MealRepository.kt          # Repository interface
│   ├── usecase/
│   │   ├── GetMealsUseCase.kt         # Retrieve all meals
│   │   ├── GetMealByIdUseCase.kt      # Retrieve single meal
│   │   ├── AddMealUseCase.kt          # Create new meal
│   │   ├── UpdateMealUseCase.kt       # Update existing meal
│   │   └── DeleteMealUseCase.kt       # Delete meal
│   └── validator/
│       └── MealValidator.kt           # Meal validation logic
└── data/
    ├── local/
    │   ├── entity/
    │   │   ├── MealEntity.kt          # Room entity
    │   │   └── IngredientEntity.kt    # Embedded ingredient
    │   ├── dao/
    │   │   └── MealDao.kt             # Data access object
    │   └── database/
    │       └── AppDatabase.kt         # Updated with MealDao
    ├── repository/
    │   └── MealRepositoryImpl.kt      # Repository implementation
    └── mapper/
        └── MealMapper.kt              # Entity/model conversions
```

## Components and Interfaces

### 1. Domain Models

```kotlin
// Pure Kotlin domain model
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Ingredient(
    val name: String,
    val quantity: String = "",
    val unit: String = ""
)
```

### 2. Repository Interface

```kotlin
interface MealRepository {
    // Reactive queries return Flow for real-time updates
    fun getMeals(): Flow<Result<List<Meal>>>
    fun getMealById(id: Long): Flow<Result<Meal>>
    
    // Mutations return Result for error handling
    suspend fun addMeal(meal: Meal): Result<Long>
    suspend fun updateMeal(meal: Meal): Result<Unit>
    suspend fun deleteMeal(mealId: Long): Result<Unit>
}
```

### 3. Use Cases

```kotlin
// Get all meals with reactive updates
class GetMealsUseCase @Inject constructor(
    private val repository: MealRepository
) {
    operator fun invoke(): Flow<Result<List<Meal>>> {
        return repository.getMeals()
    }
}

// Get single meal by ID
class GetMealByIdUseCase @Inject constructor(
    private val repository: MealRepository
) {
    operator fun invoke(mealId: Long): Flow<Result<Meal>> {
        return repository.getMealById(mealId)
    }
}

// Add new meal with validation
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Long> {
        return validator.validate(meal).fold(
            onSuccess = { repository.addMeal(meal) },
            onFailure = { Result.failure(it) }
        )
    }
}

// Update existing meal with validation
class UpdateMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Unit> {
        return validator.validate(meal).fold(
            onSuccess = { repository.updateMeal(meal) },
            onFailure = { Result.failure(it) }
        )
    }
}

// Delete meal
class DeleteMealUseCase @Inject constructor(
    private val repository: MealRepository
) {
    suspend operator fun invoke(mealId: Long): Result<Unit> {
        return repository.deleteMeal(mealId)
    }
}
```

### 4. Validator

```kotlin
class MealValidator @Inject constructor() {
    fun validate(meal: Meal): Result<Unit> {
        return when {
            meal.name.isBlank() -> 
                Result.failure(ValidationException("Meal name cannot be empty"))
            meal.ingredients.isEmpty() -> 
                Result.failure(ValidationException("Meal must have at least one ingredient"))
            meal.ingredients.any { it.name.isBlank() } -> 
                Result.failure(ValidationException("Ingredient name cannot be empty"))
            else -> Result.success(Unit)
        }
    }
}

class ValidationException(message: String) : Exception(message)
```

## Data Models

### Room Entities

```kotlin
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "ingredients")
    val ingredients: List<IngredientEntity>,
    
    @ColumnInfo(name = "notes")
    val notes: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

// Embedded in MealEntity, stored as JSON
data class IngredientEntity(
    val name: String,
    val quantity: String,
    val unit: String
)
```

### Type Converters

```kotlin
class MealConverters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromIngredientList(value: List<IngredientEntity>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toIngredientList(value: String): List<IngredientEntity> {
        val listType = object : TypeToken<List<IngredientEntity>>() {}.type
        return gson.fromJson(value, listType)
    }
}
```

### Data Access Object

```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<MealEntity>>
    
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealById(mealId: Long): Flow<MealEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    @Update
    suspend fun updateMeal(meal: MealEntity)
    
    @Delete
    suspend fun deleteMeal(meal: MealEntity)
    
    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)
}
```

### Repository Implementation

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> {
        return mealDao.getAllMeals()
            .map { entities -> 
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> 
                emit(Result.failure(DatabaseException("Failed to load meals", e)))
            }
    }
    
    override fun getMealById(id: Long): Flow<Result<Meal>> {
        return mealDao.getMealById(id)
            .map { entity ->
                entity?.let { Result.success(it.toDomainModel()) }
                    ?: Result.failure(NotFoundException("Meal not found"))
            }
            .catch { e ->
                emit(Result.failure(DatabaseException("Failed to load meal", e)))
            }
    }
    
    override suspend fun addMeal(meal: Meal): Result<Long> {
        return try {
            val id = mealDao.insertMeal(meal.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to add meal", e))
        }
    }
    
    override suspend fun updateMeal(meal: Meal): Result<Unit> {
        return try {
            mealDao.updateMeal(meal.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to update meal", e))
        }
    }
    
    override suspend fun deleteMeal(mealId: Long): Result<Unit> {
        return try {
            mealDao.deleteMealById(mealId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to delete meal", e))
        }
    }
}

class DatabaseException(message: String, cause: Throwable? = null) : Exception(message, cause)
class NotFoundException(message: String) : Exception(message)
```

### Mappers

```kotlin
// Extension functions for clean mapping
fun MealEntity.toDomainModel(): Meal {
    return Meal(
        id = id,
        name = name,
        ingredients = ingredients.map { it.toDomainModel() },
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Meal.toEntity(): MealEntity {
    return MealEntity(
        id = id,
        name = name,
        ingredients = ingredients.map { it.toEntity() },
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun IngredientEntity.toDomainModel(): Ingredient {
    return Ingredient(
        name = name,
        quantity = quantity,
        unit = unit
    )
}

fun Ingredient.toEntity(): IngredientEntity {
    return IngredientEntity(
        name = name,
        quantity = quantity,
        unit = unit
    )
}
```

## UI Layer Design

### UI State Models

```kotlin
// Meal list screen state
sealed interface MealListUiState {
    data object Loading : MealListUiState
    data class Success(val meals: List<Meal>) : MealListUiState
    data class Error(val message: String) : MealListUiState
}

// Meal detail screen state
sealed interface MealDetailUiState {
    data object Loading : MealDetailUiState
    data class Success(val meal: Meal) : MealDetailUiState
    data class Error(val message: String) : MealDetailUiState
}

// Add/Edit meal form state
data class AddEditMealUiState(
    val meal: Meal = Meal(name = "", ingredients = emptyList()),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
```

### ViewModels

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MealListUiState>(MealListUiState.Loading)
    val uiState: StateFlow<MealListUiState> = _uiState.asStateFlow()
    
    init {
        loadMeals()
    }
    
    private fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { e ->
                    _uiState.update { MealListUiState.Error(e.message ?: "Unknown error") }
                }
                .collect { result ->
                    _uiState.update {
                        result.fold(
                            onSuccess = { MealListUiState.Success(it) },
                            onFailure = { MealListUiState.Error(it.message ?: "Failed to load meals") }
                        )
                    }
                }
        }
    }
    
    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            deleteMealUseCase(mealId).fold(
                onSuccess = { /* Meal list updates automatically via Flow */ },
                onFailure = { _uiState.update { MealListUiState.Error(it.message ?: "Failed to delete meal") } }
            )
        }
    }
}

@HiltViewModel
class MealDetailViewModel @Inject constructor(
    private val getMealByIdUseCase: GetMealByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: 0
    
    private val _uiState = MutableStateFlow<MealDetailUiState>(MealDetailUiState.Loading)
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadMeal()
    }
    
    private fun loadMeal() {
        viewModelScope.launch {
            getMealByIdUseCase(mealId)
                .catch { e ->
                    _uiState.update { MealDetailUiState.Error(e.message ?: "Unknown error") }
                }
                .collect { result ->
                    _uiState.update {
                        result.fold(
                            onSuccess = { MealDetailUiState.Success(it) },
                            onFailure = { MealDetailUiState.Error(it.message ?: "Failed to load meal") }
                        )
                    }
                }
        }
    }
}

@HiltViewModel
class AddEditMealViewModel @Inject constructor(
    private val addMealUseCase: AddMealUseCase,
    private val updateMealUseCase: UpdateMealUseCase,
    private val getMealByIdUseCase: GetMealByIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val mealId: Long? = savedStateHandle.get<Long>("mealId")
    
    private val _uiState = MutableStateFlow(AddEditMealUiState())
    val uiState: StateFlow<AddEditMealUiState> = _uiState.asStateFlow()
    
    init {
        mealId?.let { loadMeal(it) }
    }
    
    private fun loadMeal(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getMealByIdUseCase(id).first().fold(
                onSuccess = { meal ->
                    _uiState.update { it.copy(meal = meal, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }
    
    fun updateMealName(name: String) {
        _uiState.update { it.copy(meal = it.meal.copy(name = name)) }
    }
    
    fun updateMealNotes(notes: String) {
        _uiState.update { it.copy(meal = it.meal.copy(notes = notes)) }
    }
    
    fun addIngredient(ingredient: Ingredient) {
        _uiState.update { 
            it.copy(meal = it.meal.copy(ingredients = it.meal.ingredients + ingredient))
        }
    }
    
    fun removeIngredient(index: Int) {
        _uiState.update {
            it.copy(meal = it.meal.copy(ingredients = it.meal.ingredients.filterIndexed { i, _ -> i != index }))
        }
    }
    
    fun saveMeal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            val result = if (mealId == null) {
                addMealUseCase(uiState.value.meal)
            } else {
                updateMealUseCase(uiState.value.meal)
            }
            
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false) }
                    // Navigation handled by screen
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Failed to save meal"
                        )
                    }
                }
            )
        }
    }
}
```

### Screen Composables

```kotlin
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel(),
    onMealClick: (Long) -> Unit,
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

@Composable
fun MealListContent(
    uiState: MealListUiState,
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMealClick) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { padding ->
        when (uiState) {
            is MealListUiState.Loading -> LoadingScreen()
            is MealListUiState.Success -> {
                if (uiState.meals.isEmpty()) {
                    EmptyState(
                        message = "No meals yet",
                        actionLabel = "Add your first meal",
                        onActionClick = onAddMealClick
                    )
                } else {
                    MealList(
                        meals = uiState.meals,
                        onMealClick = onMealClick,
                        onDeleteMeal = onDeleteMeal,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            is MealListUiState.Error -> ErrorScreen(
                message = uiState.message,
                onRetry = { /* Reload handled by ViewModel */ }
            )
        }
    }
}
```

## Error Handling

### Exception Types

```kotlin
sealed class MealException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationException(message: String) : MealException(message)
    class DatabaseException(message: String, cause: Throwable? = null) : MealException(message, cause)
    class NotFoundException(message: String) : MealException(message)
}
```

### Error Mapping

- **ValidationException** → Display field-specific error in UI
- **DatabaseException** → Display generic "Something went wrong" message, log details
- **NotFoundException** → Navigate back with error message
- **Unknown exceptions** → Display generic error, log full stack trace

## Testing Strategy

### Unit Tests

```kotlin
// Use case tests with fake repository
class AddMealUseCaseTest {
    private lateinit var repository: FakeMealRepository
    private lateinit var validator: MealValidator
    private lateinit var useCase: AddMealUseCase
    
    @Test
    fun `adds valid meal successfully`() = runTest {
        val meal = Meal(name = "Pasta", ingredients = listOf(Ingredient("Pasta")))
        val result = useCase(meal)
        assertTrue(result.isSuccess)
    }
    
    @Test
    fun `returns error for invalid meal`() = runTest {
        val meal = Meal(name = "", ingredients = emptyList())
        val result = useCase(meal)
        assertTrue(result.isFailure)
    }
}

// ViewModel tests with fake use cases
class MealViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var getMealsUseCase: FakeGetMealsUseCase
    private lateinit var viewModel: MealViewModel
    
    @Test
    fun `loads meals successfully`() = runTest {
        val meals = listOf(Meal(name = "Pasta", ingredients = emptyList()))
        getMealsUseCase.setMeals(meals)
        
        viewModel = MealViewModel(getMealsUseCase, deleteMealUseCase)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state is MealListUiState.Success)
        assertEquals(meals, (state as MealListUiState.Success).meals)
    }
}
```

### Instrumented Tests

```kotlin
// DAO tests with in-memory database
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var mealDao: MealDao
    
    @Test
    fun insertAndRetrieveMeal() = runTest {
        val meal = MealEntity(name = "Pasta", ingredients = emptyList())
        mealDao.insertMeal(meal)
        
        val meals = mealDao.getAllMeals().first()
        assertEquals(1, meals.size)
        assertEquals("Pasta", meals[0].name)
    }
}
```

## Performance Considerations

### Database Optimization
- Index on meal name for faster sorting
- Limit ingredient list size to 50 items
- Use transactions for batch operations

### UI Performance
- LazyColumn with stable keys for efficient recomposition
- Remember expensive computations
- Debounce search/filter operations

### Memory Management
- Flow-based reactive queries prevent memory leaks
- Proper lifecycle management in ViewModels
- Cancel coroutines on ViewModel clear

## Navigation Integration

```kotlin
sealed class Screen(val route: String) {
    object MealList : Screen("meal_list")
    object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long) = "meal_detail/$mealId"
    }
    object AddMeal : Screen("add_meal")
    object EditMeal : Screen("edit_meal/{mealId}") {
        fun createRoute(mealId: Long) = "edit_meal/$mealId"
    }
}
```

## Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object MealModule {
    
    @Provides
    @Singleton
    fun provideMealValidator(): MealValidator = MealValidator()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MealRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
}
```
