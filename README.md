# 🛒 Shoppit - Smart Meal Planning & Shopping List

An offline-first Android application for meal planning and automated shopping list generation.

## Features

- 📝 **Meal Management** - Save meals with ingredients ✅ Implemented
- 📅 **Weekly Planning** - Plan meals for the week 🚧 In Progress
- 🛒 **Smart Shopping Lists** - Auto-generate shopping lists from meal plans 📋 Planned
- 📱 **Offline-First** - Works without internet, syncs when available ✅ Implemented
- 🎨 **Material Design 3** - Modern, beautiful UI ✅ Implemented

## Tech Stack

- **Language**: Kotlin 2.0.21, Java 17
- **Build System**: Gradle with Kotlin DSL, AGP 8.7.3
- **UI**: Jetpack Compose (BOM 2023.10.01) + Material3
- **Architecture**: Clean Architecture + MVVM + Offline-First
- **DI**: Hilt 2.48 with KSP annotation processing
- **Database**: Room 2.6.0 with SQLite
- **Network**: Retrofit 2.9.0 + OkHttp 4.12.0
- **Async**: Kotlin Coroutines 1.7.3 + Flow
- **Navigation**: Navigation Compose 2.7.4
- **Logging**: Timber 5.0.1
- **Testing**: JUnit 4.13.2, MockK 1.13.8, Compose UI Testing, Espresso 3.5.1

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
├── data/           # Data sources, repositories, mappers
│   ├── local/      # Room database, DAOs, entities
│   ├── remote/     # Retrofit APIs, DTOs
│   ├── repository/ # Repository implementations
│   └── mapper/     # Entity/DTO/model conversions
├── domain/         # Business logic (pure Kotlin, no Android deps)
│   ├── model/      # Domain entities (Meal, Ingredient, etc.)
│   ├── repository/ # Repository interfaces
│   ├── usecase/    # Single-responsibility use cases
│   └── validator/  # Input validation logic
├── ui/             # Compose screens, ViewModels, navigation
│   ├── meal/       # Meal management feature
│   ├── planner/    # Meal planner feature
│   ├── shopping/   # Shopping list feature
│   ├── common/     # Reusable composables
│   ├── navigation/ # NavHost setup
│   └── theme/      # Material3 theme
└── di/             # Hilt dependency injection modules
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

We follow a feature branch workflow with conventional commits:

1. Create feature branch from `develop`: `feature/123-meal-planner-ui`
2. Implement feature following Clean Architecture
3. Write tests for your changes
4. Commit with conventional format: `feat(planner): add meal calendar view`
5. Create Pull Request to `develop`

See [Git Workflow Guide](docs/guides/git-workflow.md) for detailed guidelines on branching, commits, and PRs.

## Documentation

Comprehensive documentation is available in the `docs/` directory:

- **[Documentation Index](docs/INDEX.md)** - Master navigation hub for all documentation
- **Getting Started**
  - [Getting Started Guide](docs/guides/getting-started.md) - Setup and first steps
  - [Project Structure](docs/architecture/overview.md) - Understanding the codebase
- **Architecture & Design**
  - [Architecture Overview](docs/architecture/overview.md) - Clean Architecture implementation
  - [Detailed Design](docs/architecture/detailed-design.md) - Comprehensive architecture specification
  - [Data Flow](docs/architecture/data-flow.md) - Data movement patterns
  - [State Management](docs/architecture/state-management.md) - State handling patterns
- **Development Guides**
  - [Dependency Injection](docs/guides/dependency-injection.md) - Hilt DI guide
  - [Testing Guide](docs/guides/testing.md) - Testing strategies and patterns
  - [Git Workflow](docs/guides/git-workflow.md) - Branching, commits, and PRs
  - [Code Style](docs/guides/code-style.md) - Coding conventions
  - [Compose Patterns](docs/guides/compose-patterns.md) - Compose best practices
  - [MCP Usage](docs/guides/mcp-usage.md) - MCP tool guidelines
- **Quick References**
  - [Hilt Quick Reference](docs/reference/hilt-quick-reference.md) - DI patterns
  - [Database Schema](docs/reference/database-schema.md) - Room schema
  - [API Reference](docs/reference/api-reference.md) - Use cases and repositories
  - [Gradle Commands](docs/reference/gradle-commands.md) - Common commands

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Development workflow and branch strategy
- Commit message conventions
- Pull request process
- Code review guidelines
- Testing requirements

Quick guidelines:
1. Follow Clean Architecture patterns
2. Write tests for new features (80%+ domain, 60%+ data, 40%+ UI)
3. Update documentation as needed
4. Follow conventional commit format (`feat:`, `fix:`, `docs:`, etc.)
5. Ensure all tests pass before submitting PR

## License

[Add your license here]

## Contact

[Add contact information]
