# Architecture Overview

Shoppit follows **Clean Architecture** principles with a clear separation of concerns across three main layers.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────┐
│              Presentation Layer                      │
│  (Compose UI, ViewModels, Navigation)               │
│                                                      │
│  Dependencies: Domain                                │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│               Domain Layer                           │
│  (Entities, Use Cases, Repository Interfaces)       │
│                                                      │
│  Dependencies: None (Pure Kotlin)                   │
└──────────────────┬──────────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────────┐
│                Data Layer                            │
│  (Room, DAOs, Repository Implementations)           │
│                                                      │
│  Dependencies: Domain                                │
└─────────────────────────────────────────────────────┘
```

## Core Principles

### 1. Dependency Rule
- **Presentation** depends on **Domain**
- **Data** depends on **Domain**
- **Domain** has no dependencies (pure Kotlin)

### 2. Offline-First
- All data stored locally in Room database
- App works without internet connection
- Background sync when network available

### 3. Reactive
- UI updates automatically when data changes
- Flow-based data streams
- StateFlow for UI state management

### 4. Testable
- Clear separation enables easy testing
- Mock dependencies at layer boundaries
- Comprehensive test coverage

## Layer Responsibilities

### Presentation Layer

**Location:** `app/src/main/java/com/shoppit/app/presentation/`

**Responsibilities:**
- Display data to users
- Handle user interactions
- Manage UI state
- Navigate between screens

**Components:**
- **Screens**: Jetpack Compose UI
- **ViewModels**: State management with StateFlow
- **Navigation**: Compose Navigation
- **Theme**: Material3 theming

**Example:**
```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .collect { result ->
                    _uiState.update { 
                        result.fold(
                            onSuccess = { MealUiState.Success(it) },
                            onFailure = { MealUiState.Error(it.message) }
                        )
                    }
                }
        }
    }
}
```

### Domain Layer

**Location:** `app/src/main/java/com/shoppit/app/domain/`

**Responsibilities:**
- Define business entities
- Implement business logic
- Define repository contracts
- Handle domain errors

**Components:**
- **Entities**: Core business models (Meal, Ingredient, etc.)
- **Use Cases**: Business operations
- **Repository Interfaces**: Data access contracts
- **Error Types**: Domain-specific errors

**Example:**
```kotlin
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Unit> {
        return try {
            validator.validate(meal)
            repository.addMeal(meal)
        } catch (e: ValidationException) {
            Result.failure(e)
        }
    }
}
```

### Data Layer

**Location:** `app/src/main/java/com/shoppit/app/data/`

**Responsibilities:**
- Persist data locally
- Implement repository interfaces
- Map between data and domain models
- Handle data errors

**Components:**
- **DAOs**: Room data access objects
- **Entities**: Database entities
- **Repositories**: Repository implementations
- **Mappers**: Data transformation

**Example:**
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {
    
    override fun getMealsFlow(): Flow<Result<List<Meal>>> {
        return mealDao.getAllMealsFlow()
            .map { entities -> 
                entities.map { it.toDomainModel() }
            }
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
    
    override suspend fun addMeal(meal: Meal): Result<Unit> {
        return try {
            mealDao.insertMeal(meal.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## Data Flow

### Reading Data (Query Flow)

```
User Action
    ↓
Composable
    ↓
ViewModel.loadMeals()
    ↓
GetMealsUseCase()
    ↓
MealRepository.getMealsFlow()
    ↓
MealDao.getAllMealsFlow()
    ↓
Room Database
    ↓
Flow<List<MealEntity>>
    ↓
Map to Domain Models
    ↓
Flow<Result<List<Meal>>>
    ↓
ViewModel updates StateFlow
    ↓
Composable recomposes with new data
```

### Writing Data (Command Flow)

```
User Action (e.g., Save Button)
    ↓
ViewModel.saveMeal(meal)
    ↓
AddMealUseCase(meal)
    ↓
Validate meal
    ↓
MealRepository.addMeal(meal)
    ↓
Map to Entity
    ↓
MealDao.insertMeal(entity)
    ↓
Room Database
    ↓
Result<Unit>
    ↓
ViewModel updates UI state
    ↓
Show success/error message
```

## Dependency Injection

**Location:** `app/src/main/java/com/shoppit/app/di/`

Hilt manages all dependencies with three main modules:

### DatabaseModule
- Provides Room database instance
- Provides DAO instances
- Singleton scope

### RepositoryModule
- Binds repository interfaces to implementations
- Singleton scope

### UseCaseModule
- Provides use case instances
- ViewModel scope

See [Dependency Injection Guide](../guides/dependency-injection.md) for details.

## Error Handling

### Domain Errors

```kotlin
sealed class AppError {
    object NetworkError : AppError()
    object DatabaseError : AppError()
    data class ValidationError(val message: String) : AppError()
    data class UnknownError(val throwable: Throwable) : AppError()
}

typealias AppResult<T> = Result<T>
```

### Error Flow

1. **Data Layer**: Catches exceptions, maps to domain errors
2. **Domain Layer**: Validates input, returns Result types
3. **Presentation Layer**: Displays user-friendly error messages

## State Management

### UI State Pattern

```kotlin
sealed interface MealUiState {
    data object Loading : MealUiState
    data class Success(val meals: List<Meal>) : MealUiState
    data class Error(val message: String) : MealUiState
}
```

### ViewModel Pattern

- Expose immutable `StateFlow<UiState>`
- Update state with `_state.update { }`
- Handle user actions with public functions
- Launch coroutines in `viewModelScope`

## Testing Strategy

### Unit Tests
- **Domain Layer**: Test use cases with fake repositories
- **Presentation Layer**: Test ViewModels with fake use cases
- **Data Layer**: Test repositories with fake DAOs

### Integration Tests
- **DAOs**: Test with in-memory Room database
- **Repositories**: Test with real DAOs

### UI Tests
- **Screens**: Test with Compose Testing
- **Navigation**: Test user flows

See [Testing Guide](../guides/testing.md) for detailed patterns.

## Package Structure

```
com.shoppit.app/
├── data/
│   ├── local/
│   │   ├── dao/              # Room DAOs
│   │   ├── entity/           # Database entities
│   │   └── database/         # Database configuration
│   ├── repository/           # Repository implementations
│   └── mapper/               # Data mappers
├── domain/
│   ├── entity/               # Domain models
│   ├── repository/           # Repository interfaces
│   ├── usecase/              # Business logic
│   └── error/                # Error types
├── presentation/
│   ├── ui/
│   │   ├── meal/             # Meal feature screens
│   │   ├── planner/          # Planner feature screens
│   │   ├── shopping/         # Shopping feature screens
│   │   ├── common/           # Shared UI components
│   │   ├── navigation/       # Navigation setup
│   │   └── theme/            # Material3 theme
│   └── viewmodel/            # ViewModels (if shared)
├── di/                       # Hilt modules
└── util/                     # Utilities
```

## Key Technologies

- **Kotlin 1.9.20** - Language
- **Jetpack Compose** - UI framework
- **Material3** - Design system
- **Hilt** - Dependency injection
- **Room** - Local database
- **Coroutines + Flow** - Async programming
- **ViewModel** - State management
- **Navigation Compose** - Navigation

## Best Practices

### Do's
✅ Keep domain layer pure Kotlin (no Android dependencies)
✅ Use Flow for reactive data streams
✅ Expose immutable state from ViewModels
✅ Handle errors at repository boundaries
✅ Write tests for business logic

### Don'ts
❌ Don't access database directly from ViewModels
❌ Don't put business logic in ViewModels
❌ Don't use LiveData (use StateFlow instead)
❌ Don't create circular dependencies
❌ Don't skip error handling

## Further Reading

- [Detailed Design Document](detailed-design.md) - Comprehensive architecture details
- [Clean Architecture Guide](clean-architecture.md) - Layer-by-layer breakdown
- [Data Flow](data-flow.md) - Detailed data flow examples
- [Getting Started](../guides/getting-started.md) - Setup instructions
- [Dependency Injection](../guides/dependency-injection.md) - Hilt configuration
