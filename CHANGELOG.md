# Changelog

All notable changes to the Shoppit project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Meal planning feature documentation
  - Database schema updated to version 3 with MealPlanEntity
  - MealPlanDao with queries for weekly and daily meal plans
  - Meal planning API reference with use cases and domain models
  - Updated documentation index with meal planning references
- Initial project setup with Clean Architecture structure
- Hilt dependency injection configuration
  - DatabaseModule for Room database dependencies
  - RepositoryModule for repository bindings
  - UseCaseModule for use case dependencies
- Hilt testing infrastructure
  - HiltTestRunner for instrumented tests
  - TestDatabaseModule with in-memory database
  - Base test classes (ViewModelTest, RepositoryTest, DatabaseTest)
- Room database foundation
  - AppDatabase with TypeConverters for LocalDateTime and LocalDate
  - Placeholder entity for compilation
  - Database versioning strategy
- Error handling framework
  - AppError sealed class for typed errors (NetworkError, DatabaseError, ValidationError, UnknownError)
  - AppResult type alias for Result pattern
  - Base UseCase abstract class with error handling
- Logging infrastructure with Timber
  - Debug logging for development
  - Release logging with custom ReleaseTree
- Material3 theme configuration
  - Color scheme (light and dark themes)
  - Typography system
  - Theme composable with dynamic color support
- Comprehensive documentation system
  - Documentation index with navigation by task, role, and topic
  - Getting Started guide with setup instructions
  - Dependency Injection guide with Hilt patterns
  - Architecture overview with Clean Architecture principles
  - Detailed design documentation
  - Testing guide with unit, integration, and UI test patterns
  - Git workflow guide with branching and commit conventions
  - Code style guide with Kotlin conventions
  - Compose patterns guide with best practices
  - MCP usage guide for tool integration
  - Quick reference guides for Hilt, database schema, API, and Gradle commands
  - Updated README with accurate tech stack and project structure
  - Enhanced CONTRIBUTING guide with detailed workflow

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

## Project Setup Phase - 2024

### Completed Tasks

#### Task 1: Project Structure Setup
- ✅ Created Clean Architecture package structure
- ✅ Set up presentation, domain, and data layers
- ✅ Configured package organization

#### Task 2: Gradle Configuration
- ✅ Configured version catalog with all dependencies
- ✅ Set up Kotlin, Compose, Hilt, and Room
- ✅ Configured KSP for annotation processing
- ✅ Set up testing dependencies

#### Task 3: Database Foundation
- ✅ Created AppDatabase with Room
- ✅ Implemented TypeConverters for LocalDateTime and LocalDate
- ✅ Set up database versioning
- ✅ Created placeholder entity for compilation

#### Task 4: Hilt Dependency Injection
- ✅ Implemented DatabaseModule
- ✅ Created RepositoryModule structure
- ✅ Set up UseCaseModule
- ✅ Configured HiltTestRunner
- ✅ Implemented TestDatabaseModule
- ✅ Added KSP processor for androidTest

#### Task 5: Error Handling & Utilities
- ✅ Created AppError sealed class
- ✅ Implemented AppResult type alias
- ✅ Created base UseCase abstract class
- ✅ Set up Timber logging
- ✅ Implemented ReleaseTree for production logging

#### Task 6: UI Foundation
- ✅ Configured Material3 theme
- ✅ Set up MainActivity with Compose
- ✅ Created theme files (Color, Type, Theme)

#### Task 7: Testing Infrastructure
- ✅ Created MainDispatcherRule for coroutine testing
- ✅ Implemented ViewModelTest base class
- ✅ Created RepositoryTest base class
- ✅ Set up DatabaseTest base class
- ✅ Configured Hilt testing support

#### Task 8: Documentation
- ✅ Created comprehensive documentation structure
- ✅ Written Getting Started guide
- ✅ Documented Dependency Injection setup
- ✅ Created Architecture overview
- ✅ Added Hilt quick reference
- ✅ Updated project README

---

## Version History

### [0.1.0] - Initial Setup
- Project foundation established
- Clean Architecture implemented
- Hilt DI configured
- Testing infrastructure ready
- Documentation complete
