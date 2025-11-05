# Shoppit Android - AI Agent Instructions

## Project Overview

Shoppit is an offline-first Android meal planning app built with Jetpack Compose, Clean Architecture, and MVVM. Users create meals, plan weekly menus, and generate smart shopping lists.

**Tech Stack:** Kotlin 2.1.0, Compose BOM 2023.10.01, Hilt 2.56, Room 2.6.0, Gradle 8.9

## Dev Environment

**Prerequisites:**
- Java 17 (Oracle JDK)
- Android SDK (API 24-34)
- Gradle 8.9 (use wrapper)

**Setup:**
```powershell
# Build project
.\gradlew.bat build

# Run tests
.\gradlew.bat test

# Install on device
.\gradlew.bat installDebug

# Run security scans (MANDATORY after code changes)
pwd  # Get absolute path first
snyk_code_scan(path = "absolute-path")
```

## Project Structure

```
app/src/main/java/com/shoppit/app/
├── data/          # Repositories, DAOs, entities, DTOs
├── domain/        # Models, use cases, repository interfaces (pure Kotlin)
├── ui/            # Compose screens, ViewModels, navigation
└── di/            # Hilt modules
```

**Key principle:** Data → Domain ← UI (Domain has no Android dependencies)

## Code Conventions

### Architecture
- **Clean Architecture** with strict layer separation
- **MVVM** for UI layer
- **Repository pattern** for data access
- **Use cases** for business logic (single responsibility)

### Naming
- Screens: `[Feature]Screen.kt` (e.g., `MealListScreen.kt`)
- ViewModels: `[Feature]ViewModel.kt` (e.g., `MealViewModel.kt`)
- Use Cases: `[Action][Entity]UseCase.kt` (e.g., `AddMealUseCase.kt`)
- Repositories: `[Entity]Repository.kt` + `[Entity]RepositoryImpl.kt`
- DAOs: `[Entity]Dao.kt` (e.g., `MealDao.kt`)
- Entities: `[Entity]Entity.kt` (e.g., `MealEntity.kt`)

### State Management
- ViewModels expose `StateFlow<UiState>` (never `MutableStateFlow`)
- Use sealed classes for UI states: `Loading`, `Success(data)`, `Error(message)`
- Update state with `_state.update { ... }`

### Async & Data
- Use `Flow<T>` for reactive queries (Room DAOs)
- Use `suspend` for one-shot operations
- Apply `flowOn(Dispatchers.IO)` for database/network
- Return `Result<T>` for failable operations

### Compose
- Keep composables small and focused
- Hoist state to minimum common ancestor
- `Modifier` parameter last with default value
- Use `remember` for expensive computations
- Collect StateFlow with `collectAsState()`

### Dependency Injection
- Constructor injection with `@Inject constructor()`
- `@HiltViewModel` for ViewModels
- `@Module` + `@InstallIn(SingletonComponent::class)` for modules

## Testing

**Run tests:**
```powershell
.\gradlew.bat test                    # All unit tests
.\gradlew.bat testDebugUnitTest       # Debug unit tests
.\gradlew.bat connectedAndroidTest    # Instrumented tests (needs device)
```

**Test conventions:**
- Test file: `[ClassName]Test.kt`
- Use MockK for mocking
- Use `kotlinx-coroutines-test` for coroutines
- Test ViewModels with fake use cases
- Test repositories with fake DAOs
- Use in-memory database for DAO tests

## Security (MANDATORY)

**Every code change MUST include security scans:**

```powershell
# 1. Get absolute path
pwd

# 2. Run SAST scan (for code changes)
snyk_code_scan(path = "D:\\Users\\...\\shoppit-android\\app\\src")

# 3. Run SCA scan (for dependency changes)
snyk_sca_scan(path = "D:\\Users\\...\\shoppit-android", all_projects = true)

# 4. Fix Critical/High issues or document as accepted risk
# 5. Update SECURITY_ISSUES.md
```

**Never skip security scans.** They are mandatory before completing any task.

## Common Gotchas

1. **Windows paths:** Use `.\gradlew.bat` (not `./gradlew`) and backslashes in paths
2. **Security scans:** Always use absolute paths (get with `pwd`)
3. **Task execution:** Execute ONE task at a time from specs, then stop
4. **State exposure:** Never expose `MutableStateFlow` from ViewModels
5. **Layer violations:** Domain layer must not depend on Data or UI
6. **Database operations:** Never on main thread, always use `suspend` or `Flow`
7. **Error handling:** Catch exceptions at repository boundaries, map to `AppError`

## Spec-Driven Development

For complex features, use specs (`.kiro/specs/[feature-name]/`):
1. **requirements.md** - User stories and acceptance criteria (EARS format)
2. **design.md** - Architecture, components, data models
3. **tasks.md** - Step-by-step implementation plan

Execute tasks one at a time. After each task, run security scans and update docs.

## Git Workflow

**Branch naming:**
- `feature/123-short-description` - New features
- `bugfix/456-short-description` - Bug fixes
- `hotfix/1.2.1-short-description` - Production fixes

**Commit format:**
```
<type>(<scope>): <subject>

feat(meal): add ingredient validation
fix(shopping): prevent duplicate aggregation
refactor(data): extract mapper functions
```

**Types:** feat, fix, docs, style, refactor, perf, test, build, ci, chore

## Documentation

**Detailed guides in `docs/`:**
- `structure.md` - Project structure and architecture
- `compose-patterns.md` - Compose best practices
- `data-layer-patterns.md` - Repository and Room patterns
- `error-handling.md` - Error handling strategy
- `testing-strategy.md` - Testing guidelines
- `security.md` - Security best practices with Snyk
- `navigation-accessibility.md` - Navigation and accessibility
- `git-workflow.md` - Git conventions and workflows
- `WORKING_WITH_AI_AGENTS.md` - Comprehensive AI agent guide

**Steering rules in `.kiro/steering/`:**
- Auto-included context for all AI interactions
- Project-specific conventions and standards

## Quick Commands

```powershell
# Build
.\gradlew.bat assembleDebug

# Test
.\gradlew.bat test

# Install
.\gradlew.bat installDebug

# Lint
.\gradlew.bat lint

# Clean
.\gradlew.bat clean

# Security scan
pwd
snyk_code_scan(path = "absolute-path")

# Check dependencies
.\gradlew.bat dependencies
```

## Key Files to Reference

When working on features, reference these for patterns:
- `app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt` - ViewModel pattern
- `app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt` - Repository pattern
- `app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt` - Use case pattern
- `app/src/main/java/com/shoppit/app/data/local/dao/MealDao.kt` - DAO pattern
- `app/src/main/java/com/shoppit/app/ui/meal/MealListScreen.kt` - Compose screen pattern

## Need More Details?

- **Comprehensive AI guide:** `docs/WORKING_WITH_AI_AGENTS.md`
- **Architecture details:** `docs/structure.md`
- **All documentation:** `docs/` directory
- **Steering rules:** `.kiro/steering/` directory
