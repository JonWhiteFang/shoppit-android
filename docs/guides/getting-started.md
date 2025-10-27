# Getting Started with Shoppit

This guide will help you set up the Shoppit Android project and start developing.

## Prerequisites

### Required Software

- **JDK 17** or higher
- **Android Studio** Hedgehog (2023.1.1) or newer
- **Android SDK** with:
  - Min SDK 24 (Android 7.0)
  - Target SDK 34
- **Git** for version control

### Recommended Tools

- **Gradle 8.9** (included via wrapper)
- **KSP 2.0.21-1.0.28** (configured in project for Room and Hilt)
- **Android Emulator** or physical device running Android 7.0+

## Project Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd shoppit-android
```

### 2. Open in Android Studio

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the cloned directory
4. Click "OK"

Android Studio will automatically:
- Download Gradle dependencies
- Configure the Android SDK
- Index the project files

### 3. Sync Gradle

If sync doesn't start automatically:
1. Click "File" → "Sync Project with Gradle Files"
2. Wait for sync to complete (may take a few minutes on first run)

### 4. Build the Project

```bash
# From command line
./gradlew assembleDebug

# Or in Android Studio
Build → Make Project (Ctrl+F9 / Cmd+F9)
```

### 5. Run the App

**Using Android Studio:**
1. Select a device/emulator from the device dropdown
2. Click the "Run" button (▶️) or press Shift+F10

**Using Command Line:**
```bash
# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## Project Structure

```
shoppit-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/shoppit/app/
│   │   │   │   ├── data/           # Data layer
│   │   │   │   │   ├── local/      # Room database, DAOs, entities
│   │   │   │   │   ├── remote/     # Retrofit APIs, DTOs (future)
│   │   │   │   │   ├── repository/ # Repository implementations
│   │   │   │   │   └── mapper/     # Entity/DTO/model conversions
│   │   │   │   ├── domain/         # Business logic (pure Kotlin)
│   │   │   │   │   ├── model/      # Domain entities
│   │   │   │   │   ├── repository/ # Repository interfaces
│   │   │   │   │   ├── usecase/    # Single-responsibility use cases
│   │   │   │   │   └── validator/  # Input validation logic
│   │   │   │   ├── ui/             # Presentation layer
│   │   │   │   │   ├── meal/       # Meal management feature
│   │   │   │   │   ├── planner/    # Meal planner feature
│   │   │   │   │   ├── shopping/   # Shopping list feature
│   │   │   │   │   ├── common/     # Reusable composables
│   │   │   │   │   ├── navigation/ # NavHost setup
│   │   │   │   │   └── theme/      # Material3 theme
│   │   │   │   └── di/             # Hilt dependency injection modules
│   │   │   └── res/                # Resources
│   │   ├── test/                   # Unit tests
│   │   └── androidTest/            # Instrumented tests
│   └── build.gradle.kts
├── docs/                           # Documentation
├── gradle/                         # Gradle wrapper & version catalog
│   ├── libs.versions.toml          # Centralized dependency versions
│   └── wrapper/
└── build.gradle.kts               # Root build file
```

## Architecture Overview

Shoppit follows **Clean Architecture** with three main layers organized in a single-module structure:

### 1. UI Layer (`ui/`)
- **Screens**: Jetpack Compose screens organized by feature
- **ViewModels**: State management with `StateFlow<UiState>` (never expose `MutableStateFlow`)
- **Navigation**: Compose Navigation with type-safe routes
- **Theme**: Material3 theme configuration

**Key Patterns**:
- ViewModels expose immutable `StateFlow<UiState>`
- Use sealed classes for UI states: `Loading`, `Success(data)`, `Error(message)`
- State hoisting: Keep composables stateless, hoist state to ViewModels
- Collect state with `collectAsState()` in composables

### 2. Domain Layer (`domain/`)
- **Models**: Pure Kotlin domain entities (Meal, Ingredient, MealPlan, ShoppingListItem)
- **Use Cases**: Single-responsibility business logic operations
- **Repository Interfaces**: Data access contracts (no implementations)
- **Validators**: Input validation logic
- **No Android dependencies** - Pure Kotlin only

**Key Patterns**:
- Use cases have one public `operator fun invoke()` function
- Return `Flow<T>` for reactive data or `suspend` for one-shot operations
- Use `Result<T>` for operations that can fail

### 3. Data Layer (`data/`)
- **Local**: Room database, DAOs (return `Flow<T>` for queries, `suspend` for mutations), entities
- **Remote**: Retrofit services, DTOs (future implementation)
- **Repositories**: Implementation of domain repository interfaces
- **Mappers**: Extension functions for entity/DTO/model conversions

**Key Patterns**:
- Repositories catch exceptions at boundaries and map to domain errors
- DAOs return `Flow<T>` for observed queries, `suspend` for mutations
- Apply `flowOn(Dispatchers.IO)` for database/network operations

### Dependency Injection (`di/`)
- **DatabaseModule**: Provides Room database and DAOs
- **NetworkModule**: Provides Retrofit, OkHttp, interceptors (future)
- **RepositoryModule**: Binds repository interfaces to implementations
- **UseCaseModule**: Provides use case instances (if needed)

**Key Patterns**:
- Use constructor injection with `@Inject constructor()`
- ViewModels require `@HiltViewModel` annotation
- Modules use `@Module` + `@InstallIn(SingletonComponent::class)`
- Bind interfaces with `@Binds` in abstract modules

### Dependency Direction
- UI depends on Domain
- Data depends on Domain
- Domain depends on nothing (pure Kotlin)
- **Never**: Domain → UI or Domain → Data

See [Architecture Overview](../architecture/overview.md) for detailed information.

## Key Technologies

### Core Stack
- **Kotlin 2.0.21** - Programming language with Java 17
- **Jetpack Compose (BOM 2023.10.01)** - Modern declarative UI toolkit
- **Material3** - Material Design 3 system
- **Hilt 2.48** - Dependency injection with KSP
- **Room 2.6.0** - Local SQLite database with type-safe queries
- **Coroutines 1.7.3 & Flow** - Asynchronous programming and reactive streams
- **Navigation Compose 2.7.4** - Type-safe navigation
- **Retrofit 2.9.0 + OkHttp 4.12.0** - Network layer (future)
- **Timber 5.0.1** - Logging

### Build System
- **Gradle with Kotlin DSL** - Build configuration
- **AGP 8.7.3** - Android Gradle Plugin
- **KSP 2.0.21-1.0.28** - Kotlin Symbol Processing for Room and Hilt
- **Version Catalogs** - Centralized dependency management in `gradle/libs.versions.toml`

### Testing
- **JUnit 4.13.2** - Unit testing framework
- **MockK 1.13.8** - Mocking library for Kotlin
- **kotlinx-coroutines-test** - Coroutine testing utilities
- **Compose UI Testing** - Declarative UI testing
- **Espresso 3.5.1** - Instrumented UI testing
- **Hilt Testing** - DI testing support
- **Room Testing** - In-memory database testing

See [Gradle Commands Reference](../reference/gradle-commands.md) for build commands and [Tech Stack Details](../../.kiro/steering/tech.md) for complete information.

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feature/123-meal-list-ui
```

### 2. Implement the Feature

Follow the Clean Architecture pattern:

1. **Define domain model** in `domain/model/` (e.g., `Meal.kt`)
2. **Create repository interface** in `domain/repository/` (e.g., `MealRepository.kt`)
3. **Implement use case** in `domain/usecase/` (e.g., `AddMealUseCase.kt`)
4. **Create Room entity and DAO** in `data/local/entity/` and `data/local/dao/`
5. **Implement repository** in `data/repository/` (e.g., `MealRepositoryImpl.kt`)
6. **Create ViewModel with UiState** in `ui/[feature]/` (e.g., `ui/meal/MealViewModel.kt`, `MealUiState.kt`)
7. **Build Compose screen** in `ui/[feature]/` (e.g., `ui/meal/MealListScreen.kt`)
8. **Add navigation route** in `ui/navigation/`
9. **Wire up DI** in appropriate `di/` modules

**File Naming Conventions**:
- Screens: `[Feature]Screen.kt` → `MealListScreen.kt`
- ViewModels: `[Feature]ViewModel.kt` → `MealViewModel.kt`
- UI State: `[Feature]UiState.kt` → `MealUiState.kt`
- Use Cases: `[Action][Entity]UseCase.kt` → `AddMealUseCase.kt`
- Repositories: `[Entity]Repository.kt` + `[Entity]RepositoryImpl.kt`
- DAOs: `[Entity]Dao.kt` → `MealDao.kt`
- Entities: `[Entity]Entity.kt` → `MealEntity.kt`

See [Project Structure Guide](../../.kiro/steering/structure.md) for detailed conventions.

### 3. Write Tests

Follow the test pyramid approach:

- **Unit tests** (`test/`) for:
  - Use cases (80%+ coverage) - Test business logic with fake repositories
  - ViewModels (40%+ coverage) - Test state transitions and user actions
  - Validators - Test validation rules
  - Repositories (60%+ coverage) - Test with fake DAOs

- **Instrumented tests** (`androidTest/`) for:
  - Room DAOs - Test with in-memory database
  - Compose UI - Test critical user flows only

**Testing Guidelines**:
- Test behavior, not implementation
- Use MockK for mocking external dependencies
- Use `runTest` and `MainDispatcherRule` for coroutine testing
- Name tests descriptively: `` `loads meals successfully when repository returns data` ``

See [Testing Guide](testing.md) for comprehensive patterns and examples.

### 4. Commit Changes

Follow conventional commit format:

```bash
git add .
git commit -m "feat(meal): add meal list screen with filtering"
```

**Commit Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Other changes (tooling, etc.)

See [Git Workflow Guide](git-workflow.md) for detailed commit conventions and branching strategy.

### 5. Push and Create PR

```bash
git push -u origin feature/123-meal-list-ui
```

Then create a Pull Request on GitHub.

## Common Tasks

### Running Tests

```bash
# All unit tests
./gradlew test

# Specific test class
./gradlew test --tests "MealViewModelTest"

# All instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest
```

### Code Quality

```bash
# Run lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

### Clean Build

```bash
# Clean build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDebug
```

### Generate Sources

```bash
# Generate Room and Hilt sources
./gradlew kspDebugKotlin
```

## IDE Configuration

### Recommended Plugins

1. **Kotlin** (bundled)
2. **Android** (bundled)
3. **Compose Multiplatform** (optional)

### Code Style

The project uses standard Kotlin coding conventions:
- 4 spaces for indentation
- 120 character line limit
- Trailing commas in multi-line lists

Import the code style:
1. File → Settings → Editor → Code Style
2. Scheme → Import Scheme → IntelliJ IDEA code style XML
3. Select `.editorconfig` from project root

## Troubleshooting

### Build Fails with "Cannot find symbol"

**Problem:** Hilt annotation processor not running

**Solution:**
```bash
./gradlew clean
./gradlew kspDebugKotlin
./gradlew assembleDebug
```

### "SDK location not found"

**Problem:** Android SDK path not configured

**Solution:** Create `local.properties` in project root:
```properties
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Tests Fail with "No instrumentation registered"

**Problem:** Test runner not configured

**Solution:** Verify in `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
    }
}
```

### Compose Preview Not Working

**Problem:** Preview annotations not recognized

**Solution:**
1. Invalidate Caches: File → Invalidate Caches → Invalidate and Restart
2. Rebuild project: Build → Rebuild Project

## Next Steps

Now that you have the project set up:

1. **Explore the codebase** - Start with `ShoppitApplication.kt` and understand the app initialization
2. **Read the architecture docs** - [Architecture Overview](../architecture/overview.md) for Clean Architecture implementation
3. **Understand data flow** - [Data Flow Guide](../architecture/data-flow.md) for how data moves through layers
4. **Learn about DI** - [Dependency Injection Guide](dependency-injection.md) for Hilt patterns
5. **Master state management** - [State Management Guide](../architecture/state-management.md) for ViewModel and Compose state
6. **Write your first test** - [Testing Guide](testing.md) for testing patterns and examples
7. **Learn Compose patterns** - [Compose Patterns Guide](compose-patterns.md) for reusable components
8. **Check coding standards** - [Code Style Guide](code-style.md) for conventions and best practices

## First Feature Implementation

Ready to implement your first feature? Here's a quick walkthrough:

### Example: Adding a "Mark Meal as Favorite" Feature

1. **Domain Model** - Add `isFavorite: Boolean` to `Meal` model
2. **Use Case** - Create `ToggleMealFavoriteUseCase.kt`
3. **Repository** - Add `toggleFavorite(mealId: Long)` to `MealRepository`
4. **DAO** - Add `@Query("UPDATE meals SET is_favorite = NOT is_favorite WHERE id = :mealId")` to `MealDao`
5. **ViewModel** - Add `fun toggleFavorite(meal: Meal)` to `MealViewModel`
6. **UI** - Add favorite icon button to `MealCard` composable
7. **Test** - Write unit tests for use case and ViewModel

This follows the dependency flow: UI → ViewModel → UseCase → Repository → DAO → Database

## Getting Help

- **Documentation**: Check the `docs/` directory
- **Code Comments**: Most files have detailed documentation
- **Team**: Ask questions in team chat or during standup
- **Issues**: Check GitHub issues for known problems

## Useful Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
