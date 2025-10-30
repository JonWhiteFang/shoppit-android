---
inclusion: always
---

# Technology Stack

## Build System
- Gradle with Kotlin DSL (build.gradle.kts)
- Version catalogs in `gradle/libs.versions.toml`
- Kotlin 1.9.20, Java 17, AGP 8.1.2
- KSP for annotation processing (Room, Hilt)

## Core Stack
- **UI**: Jetpack Compose + Material3 (BOM 2023.10.01)
- **Architecture**: Clean Architecture + MVVM + Offline-First
- **DI**: Hilt 2.48 with `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`
- **Async**: Kotlin Coroutines 1.7.3 + Flow for reactive streams
- **Database**: Room 2.6.0 with SQLite
- **Network**: Retrofit 2.9.0 + OkHttp 4.12.0 + Gson
- **Navigation**: Navigation Compose 2.7.4
- **Logging**: Timber 5.0.1

## Testing Stack
- **Unit**: JUnit 4.13.2, MockK 1.13.8, kotlinx-coroutines-test
- **Instrumented**: AndroidX Test, Espresso 3.5.1, Compose UI testing
- **DI Testing**: hilt-android-testing
- **Database Testing**: room-testing with in-memory database

## Build Configuration
- Namespace: `com.shoppit.app`
- Min SDK 24 (Android 7.0) → Target SDK 34
- Compose Compiler 1.5.4
- ProGuard: debug off, release on

## Common Gradle Commands

### Build & Install
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug            # Install on device
./gradlew clean                   # Clean build
```

### Testing
```bash
./gradlew test                    # All unit tests
./gradlew testDebugUnitTest       # Debug unit tests only
./gradlew connectedAndroidTest    # Instrumented tests (needs device)
./gradlew test --tests "ClassName" # Specific test class
```

### Code Quality
```bash
./gradlew lint                    # Run lint checks
./gradlew lintDebug               # Generate lint report
```

### Development
```bash
./gradlew kspDebugKotlin          # Generate Room/Hilt sources
./gradlew dependencies            # Check dependency tree
./gradlew tasks                   # List all tasks
```

## Key Implementation Notes

### Dependency Injection
- Use constructor injection with `@Inject constructor()`
- ViewModels require `@HiltViewModel` annotation
- Modules use `@Module` + `@InstallIn(SingletonComponent::class)`
- Bind interfaces with `@Binds` in abstract modules

### Coroutines & Flow
- Use `viewModelScope` for ViewModel coroutines
- Repository functions return `Flow<T>` for reactive data or `suspend` for one-shot ops
- Apply `flowOn(Dispatchers.IO)` for database/network operations
- DAOs return `Flow<T>` for observed queries, `suspend` for mutations

### Compose Best Practices
- Keep composables small and focused
- Use `remember` for expensive computations
- Hoist state to minimum common ancestor
- `Modifier` parameter last with default value
- Collect StateFlow with `collectAsState()`

### State Management
- ViewModels expose `StateFlow<UiState>` (never expose `MutableStateFlow`)
- Update state with `_state.update { ... }`
- Use sealed classes for UI states: `Loading`, `Success(data)`, `Error(message)`

### Error Handling
- Use `Result<T>` for failable operations
- Catch exceptions at repository boundaries
- Map exceptions to domain errors in repositories
- Never let exceptions reach UI layer

## Version Catalog Usage
When adding dependencies, use version catalog references:
```kotlin
implementation(libs.hilt.android)
ksp(libs.hilt.compiler)
implementation(libs.room.runtime)
```
