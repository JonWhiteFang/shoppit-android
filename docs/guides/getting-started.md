# Getting Started with Shoppit

This guide will help you set up the Shoppit Android project and start developing.

## Prerequisites

### Required Software

- **JDK 17** or higher
- **Android Studio** Hedgehog (2023.1.1) or newer
- **Android SDK** with API level 34
- **Git** for version control

### Recommended Tools

- **Gradle 8.9** (included via wrapper)
- **KSP** (configured in project)
- **Android Emulator** or physical device for testing

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
│   │   │   │   ├── domain/         # Business logic
│   │   │   │   ├── presentation/   # UI layer
│   │   │   │   ├── di/             # Dependency injection
│   │   │   │   └── util/           # Utilities
│   │   │   └── res/                # Resources
│   │   ├── test/                   # Unit tests
│   │   └── androidTest/            # Instrumented tests
│   └── build.gradle.kts
├── docs/                           # Documentation
├── gradle/                         # Gradle wrapper
└── build.gradle.kts               # Root build file
```

## Architecture Overview

Shoppit follows **Clean Architecture** with three main layers:

### 1. Presentation Layer (`presentation/`)
- **UI**: Jetpack Compose screens
- **ViewModels**: State management with StateFlow
- **Navigation**: Compose Navigation

### 2. Domain Layer (`domain/`)
- **Entities**: Core business models
- **Use Cases**: Business logic operations
- **Repository Interfaces**: Data access contracts
- **Error Handling**: Result types and error classes

### 3. Data Layer (`data/`)
- **Local**: Room database, DAOs, entities
- **Remote**: Retrofit services, DTOs (future)
- **Repositories**: Implementation of domain interfaces
- **Mappers**: Data transformation utilities

### Dependency Injection (`di/`)
- **DatabaseModule**: Database and DAO providers
- **RepositoryModule**: Repository bindings
- **UseCaseModule**: Use case providers

See [Architecture Overview](../architecture/overview.md) for detailed information.

## Key Technologies

### Core Stack
- **Kotlin 1.9.20** - Programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material3** - Design system
- **Hilt** - Dependency injection
- **Room** - Local database
- **Coroutines & Flow** - Asynchronous programming

### Testing
- **JUnit 4** - Unit testing framework
- **MockK** - Mocking library
- **Compose Testing** - UI testing
- **Hilt Testing** - DI testing support

See [Build Configuration](../setup/build-configuration.md) for complete dependency list.

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feature/123-meal-list-ui
```

### 2. Implement the Feature

Follow the Clean Architecture pattern:

1. **Define domain model** in `domain/entity/`
2. **Create repository interface** in `domain/repository/`
3. **Implement use case** in `domain/usecase/`
4. **Create Room entity and DAO** in `data/local/`
5. **Implement repository** in `data/repository/`
6. **Create ViewModel** in `presentation/viewmodel/`
7. **Build UI** in `presentation/ui/`
8. **Wire up DI** in `di/` modules

### 3. Write Tests

- **Unit tests** for use cases and ViewModels
- **Integration tests** for repositories and DAOs
- **UI tests** for critical user flows

See [Testing Guide](testing.md) for detailed patterns.

### 4. Commit Changes

Follow conventional commit format:

```bash
git add .
git commit -m "feat(meal): add meal list screen with filtering"
```

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

1. **Explore the codebase** - Start with `ShoppitApplication.kt`
2. **Read the architecture docs** - [Architecture Overview](../architecture/overview.md)
3. **Learn about DI** - [Dependency Injection Guide](dependency-injection.md)
4. **Write your first test** - [Testing Guide](testing.md)
5. **Check coding standards** - [Code Style Guide](code-style.md)

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
