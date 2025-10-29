---
inclusion: always
---

# Project Structure & Architecture

## Clean Architecture Layers

Single-module Android app organized in three layers: **Data** → **Domain** ← **UI**

### Package Structure
```
com.shoppit.app/
├── data/          # Data sources, repositories, mappers
├── domain/        # Business logic (pure Kotlin, no Android deps)
├── ui/            # Compose screens, ViewModels, navigation
└── di/            # Hilt dependency injection modules
```

## File Naming Conventions

Follow these patterns strictly:

- **Screens**: `[Feature]Screen.kt` → `MealListScreen.kt`
- **ViewModels**: `[Feature]ViewModel.kt` → `MealViewModel.kt`
- **UI State**: `[Feature]UiState.kt` → `MealUiState.kt`
- **Use Cases**: `[Action][Entity]UseCase.kt` → `AddMealUseCase.kt`
- **Repositories**: Interface `[Entity]Repository.kt` + Implementation `[Entity]RepositoryImpl.kt`
- **DAOs**: `[Entity]Dao.kt` → `MealDao.kt`
- **Room Entities**: `[Entity]Entity.kt` → `MealEntity.kt`
- **Domain Models**: Plain names → `Meal.kt`, `Ingredient.kt`
- **DTOs**: `[Entity]Dto.kt` → `MealDto.kt`
- **Hilt Modules**: `[Purpose]Module.kt` → `DatabaseModule.kt`, `NetworkModule.kt`

## Layer Organization

### Data Layer (`data/`)
- **local/dao/**: Room DAOs with suspend functions and Flow returns
- **local/entity/**: Room entities with `@Entity` annotations
- **local/database/**: Database class with `@Database` annotation
- **remote/api/**: Retrofit service interfaces
- **remote/dto/**: Network data transfer objects
- **repository/**: Repository implementations (inject DAOs and APIs)
- **mapper/**: Extension functions for entity/DTO/model conversions

### Domain Layer (`domain/`)
- **model/**: Pure Kotlin domain entities (Meal, Ingredient, MealPlan, ShoppingListItem)
- **repository/**: Repository interfaces (no implementations)
- **usecase/**: Single-responsibility use cases (one public operator function)
- **validator/**: Input validation logic

### UI Layer (`ui/`)
- **[feature]/**: Feature-based packages (meal, planner, shopping)
  - `[Feature]Screen.kt`: Composable with `@Composable` annotation
  - `[Feature]ViewModel.kt`: Extends `ViewModel`, exposes `StateFlow<UiState>`
  - `[Feature]UiState.kt`: Sealed class or data class for UI state
- **common/**: Reusable composables (ErrorScreen, LoadingScreen, etc.)
- **navigation/**: NavHost setup, navigation routes, and deep link configuration
- **theme/**: Material3 theme configuration
- **accessibility/**: Accessibility utilities and keyboard navigation handlers

### DI Layer (`di/`)
- **DatabaseModule.kt**: Provides Room database and DAOs
- **NetworkModule.kt**: Provides Retrofit, OkHttp, interceptors
- **RepositoryModule.kt**: Binds repository interfaces to implementations
- **UseCaseModule.kt**: Provides use case instances

## Architectural Rules

### Dependency Direction
- UI depends on Domain
- Data depends on Domain
- Domain depends on nothing (pure Kotlin)
- Never: Domain → UI or Domain → Data

### Data Flow Pattern
```
UI → ViewModel → UseCase → Repository → DataSource (Room/Retrofit)
DataSource → Repository (Flow) → ViewModel (StateFlow) → UI (collectAsState)
```

### State Management
- ViewModels expose `StateFlow<UiState>` (never `MutableStateFlow`)
- Use sealed classes for UI states: `Loading`, `Success(data)`, `Error(message)`
- Single source of truth: ViewModel holds state, UI observes
- Update state with `_state.update { ... }` in ViewModels

### Error Handling
- Use `Result<T>` for operations that can fail
- Define sealed `AppError` class for typed errors
- Catch exceptions at repository boundaries, never let them reach UI
- Map exceptions to domain errors in repositories

### Coroutines & Flow
- Use `viewModelScope` for ViewModel coroutines
- Repository functions return `Flow<T>` for reactive data or `suspend` for one-shot operations
- DAOs return `Flow<T>` for observed queries, `suspend` for mutations
- Use `flowOn(Dispatchers.IO)` for database/network operations

## Testing Conventions

### Unit Tests (`test/`)
- **domain/usecase/**: Test use cases with fake repositories
- **domain/validator/**: Test validation logic
- **data/repository/**: Test repositories with fake DAOs/APIs
- **ui/viewmodel/**: Test ViewModels with fake use cases
- Use MockK for mocking, `kotlinx-coroutines-test` for coroutine testing
- Test file naming: `[ClassName]Test.kt` → `AddMealUseCaseTest.kt`

### Instrumented Tests (`androidTest/`)
- **data/local/**: Test Room DAOs with in-memory database
- **ui/**: Test Compose UI with `createComposeRule()`
- Use `@HiltAndroidTest` for tests requiring DI

## Code Style

### Kotlin Conventions
- Use `val` over `var` whenever possible
- Prefer expression bodies for single-expression functions
- Use trailing commas in multi-line lists
- Use explicit types for public APIs, infer for private/local
- Use `require()` for preconditions, `check()` for state validation

### Compose Best Practices
- Keep composables small and focused
- Extract reusable UI into separate composables
- Use `remember` for expensive computations
- Hoist state to the minimum common ancestor
- Use `Modifier` parameter as last parameter with default value

### Hilt Annotations
- `@HiltAndroidApp` on Application class
- `@AndroidEntryPoint` on Activities and Fragments
- `@HiltViewModel` on ViewModels
- `@Inject constructor()` for constructor injection
- `@Module` + `@InstallIn` for Hilt modules

## Feature Implementation Checklist

When adding a new feature:
1. Define domain model in `domain/model/`
2. Create repository interface in `domain/repository/`
3. Implement use cases in `domain/usecase/`
4. Create Room entity and DAO in `data/local/`
5. Implement repository in `data/repository/`
6. Create ViewModel with UiState in `ui/[feature]/`
7. Build Compose screen in `ui/[feature]/`
8. Add navigation route in `ui/navigation/`
9. Wire up DI in appropriate modules
10. Write unit tests for use cases and ViewModel
