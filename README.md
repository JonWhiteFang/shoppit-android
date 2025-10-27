# 🛒 Shoppit - Smart Meal Planning & Shopping List

An offline-first Android application for meal planning and automated shopping list generation.

## Features

- 📝 **Meal Management** - Save meals with ingredients
- 📅 **Weekly Planning** - Plan meals for the week
- 🛒 **Smart Shopping Lists** - Auto-generate shopping lists from meal plans
- 📱 **Offline-First** - Works without internet, syncs when available
- 🎨 **Material Design 3** - Modern, beautiful UI

## Tech Stack

- **Language**: Kotlin 1.9.20
- **UI**: Jetpack Compose + Material3
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Database**: Room + SQLite
- **Async**: Coroutines + Flow
- **Testing**: JUnit, MockK, Compose Testing

## Getting Started

### Prerequisites

- JDK 17 or higher
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK with API level 34

### Setup

1. Clone the repository:
```bash
git clone <repository-url>
cd shoppit-android
```

2. Open in Android Studio and sync Gradle

3. Build and run:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

For detailed setup instructions, see [Getting Started Guide](docs/guides/getting-started.md).

## Project Structure

```
app/src/main/java/com/shoppit/app/
├── data/           # Data layer (Room, repositories)
├── domain/         # Business logic (use cases, entities)
├── presentation/   # UI layer (Compose, ViewModels)
├── di/             # Dependency injection modules
└── util/           # Utilities and helpers
```

## Architecture

Shoppit follows Clean Architecture principles with three distinct layers:

- **Presentation**: Jetpack Compose UI + ViewModels
- **Domain**: Business logic and entities (pure Kotlin)
- **Data**: Room database + repositories

For detailed architecture information, see [Architecture Documentation](docs/architecture/overview.md).

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Code Quality

```bash
# Lint checks
./gradlew lint

# Generate lint report
./gradlew lintDebug
```

### Git Workflow

1. Create feature branch from `develop`
2. Implement feature following Clean Architecture
3. Write tests for your changes
4. Create Pull Request

See [Git Workflow](docs/guides/git-workflow.md) for detailed guidelines.

## Documentation

Comprehensive documentation is available in the `docs/` directory:

- [Getting Started](docs/guides/getting-started.md) - Setup and first steps
- [Architecture Overview](docs/architecture/overview.md) - System design
- [Dependency Injection](docs/guides/dependency-injection.md) - Hilt setup
- [Testing Guide](docs/guides/testing.md) - Testing strategies
- [Code Style](docs/guides/code-style.md) - Coding conventions

## Contributing

1. Follow the established architecture patterns
2. Write tests for new features
3. Update documentation as needed
4. Follow conventional commit format
5. Ensure all tests pass before submitting PR

## License

[Add your license here]

## Contact

[Add contact information]
