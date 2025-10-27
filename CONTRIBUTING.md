# Contributing to Shoppit

Thank you for your interest in contributing to Shoppit! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Pull Request Process](#pull-request-process)

## Code of Conduct

- Be respectful and inclusive
- Focus on constructive feedback
- Help others learn and grow
- Maintain professional communication

## Getting Started

1. **Fork the repository** and clone your fork
2. **Set up the development environment** following the [Getting Started Guide](docs/guides/getting-started.md)
3. **Create a feature branch** from `develop`
4. **Make your changes** following our coding standards
5. **Write tests** for your changes
6. **Update documentation** as needed
7. **Submit a pull request**

## Development Workflow

### Branch Strategy

We use a Git Flow-inspired workflow:

- **main**: Production-ready code, always deployable
- **develop**: Integration branch for features, next release candidate
- **feature/**: New features and enhancements
- **bugfix/**: Bug fixes for develop branch
- **hotfix/**: Urgent fixes for production

### Branch Naming

Follow this naming convention:

- `feature/<issue-number>-<short-description>` - New features
- `bugfix/<issue-number>-<short-description>` - Bug fixes
- `hotfix/<version>-<short-description>` - Urgent production fixes
- `refactor/<short-description>` - Code refactoring
- `docs/<short-description>` - Documentation updates

**Examples:**
```bash
feature/123-meal-list-ui
bugfix/456-fix-ingredient-duplication
hotfix/1.2.1-critical-data-loss
refactor/repository-error-handling
docs/update-testing-guide
```

For complete workflow details, see [Git Workflow Guide](docs/guides/git-workflow.md).

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation changes
- `style` - Code style changes (formatting, no logic change)
- `refactor` - Code refactoring
- `perf` - Performance improvements
- `test` - Adding or updating tests
- `build` - Build system changes
- `ci` - CI/CD changes
- `chore` - Other changes

**Scopes:**
- `meal` - Meal management
- `planner` - Meal planner
- `shopping` - Shopping list
- `data` - Data layer
- `ui` - UI layer
- `domain` - Domain layer
- `di` - Dependency injection

**Examples:**
```bash
feat(meal): add ingredient quantity validation

Implement validation to ensure ingredient quantities are positive numbers.
Reject empty or negative values with appropriate error messages.

Closes #123

fix(shopping): prevent duplicate ingredients in aggregation

Fixed issue where ingredients with same name but different casing
were not being aggregated correctly.

Fixes #456
```

## Coding Standards

### Architecture

Follow **Clean Architecture** principles:

1. **Presentation Layer** - UI and ViewModels
   - Use Jetpack Compose for UI
   - Expose immutable StateFlow from ViewModels
   - Handle user actions in ViewModels

2. **Domain Layer** - Business logic (pure Kotlin)
   - No Android dependencies
   - Define entities and use cases
   - Repository interfaces only

3. **Data Layer** - Data sources and repositories
   - Implement repository interfaces
   - Handle data persistence with Room
   - Map between data and domain models

### Kotlin Style

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use trailing commas in multi-line lists
- Prefer `val` over `var`
- Use expression bodies for single-expression functions

```kotlin
// Good
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
) : Repository {
    
    override fun getMeals(): Flow<List<Meal>> = 
        mealDao.getAllMeals().map { it.toDomain() }
}

// Bad
class MealRepository @Inject constructor(private val mealDao: MealDao) : Repository {
    override fun getMeals(): Flow<List<Meal>> {
        return mealDao.getAllMeals().map { entity -> entity.toDomain() }
    }
}
```

### Compose Guidelines

- Keep composables small and focused
- Use `remember` for expensive computations
- Hoist state to minimum common ancestor
- Always include `modifier: Modifier = Modifier` parameter
- Use `@Preview` for all composables

```kotlin
@Composable
fun MealCard(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Text(text = meal.name)
    }
}

@Preview
@Composable
private fun MealCardPreview() {
    ShoppitTheme {
        MealCard(
            meal = Meal(id = 1, name = "Pasta"),
            onClick = {},
        )
    }
}
```

### Dependency Injection

- Use constructor injection whenever possible
- Inject interfaces, not implementations
- Use appropriate scopes (`@Singleton`, `ViewModelComponent`)
- Keep modules focused and well-organized

```kotlin
// Good
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
) : ViewModel()

// Bad - injecting implementation
class MealViewModel @Inject constructor(
    private val repository: MealRepositoryImpl,
) : ViewModel()
```

## Testing Requirements

### Test Coverage

All new code must include tests:

- **Domain Layer**: 80%+ coverage
  - Test all use cases
  - Test validation logic
  - Test error handling

- **Data Layer**: 60%+ coverage
  - Test repository implementations
  - Test data mapping
  - Test DAO operations (instrumented)

- **Presentation Layer**: 40%+ coverage
  - Test ViewModel state transitions
  - Test user action handling
  - Test critical UI flows

### Test Structure

```kotlin
@ExperimentalCoroutinesApi
class MealViewModelTest : ViewModelTest() {
    
    private lateinit var viewModel: MealViewModel
    private lateinit var getMealsUseCase: GetMealsUseCase
    
    @Before
    fun setUp() {
        getMealsUseCase = mockk()
        viewModel = MealViewModel(getMealsUseCase)
    }
    
    @Test
    fun `loads meals successfully`() = runTest {
        // Given
        val meals = listOf(Meal(id = 1, name = "Pasta"))
        coEvery { getMealsUseCase() } returns flowOf(Result.success(meals))
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MealUiState.Success)
        assertEquals(meals, (state as MealUiState.Success).meals)
    }
}
```

### Running Tests

Before submitting a PR, ensure all tests pass:

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# Lint checks
./gradlew lint
```

## Documentation

### Code Documentation

- Add KDoc comments for public APIs
- Document complex logic with inline comments
- Include usage examples in class documentation

```kotlin
/**
 * Repository for managing meal data.
 * 
 * Provides access to meal CRUD operations with offline-first approach.
 * All operations return Flow for reactive updates or Result for one-shot operations.
 * 
 * Usage:
 * ```
 * class GetMealsUseCase @Inject constructor(
 *     private val repository: MealRepository
 * ) {
 *     operator fun invoke(): Flow<Result<List<Meal>>> = 
 *         repository.getMealsFlow()
 * }
 * ```
 */
interface MealRepository {
    /**
     * Returns a flow of all meals, updated reactively.
     * 
     * @return Flow emitting Result with list of meals or error
     */
    fun getMealsFlow(): Flow<Result<List<Meal>>>
}
```

### Documentation Updates

When adding features, update relevant documentation:

- Update README if adding major features
- Update architecture docs if changing structure
- Update guides if adding new patterns
- Add examples to quick reference guides

## Pull Request Process

### Before Submitting

1. ✅ All tests pass
2. ✅ Code follows style guidelines
3. ✅ Documentation is updated
4. ✅ Commit messages follow conventions
5. ✅ Branch is up to date with `develop`

### PR Title Format

```
[Type] Short description (#issue-number)

feat: Add meal planner calendar view (#123)
fix: Prevent duplicate ingredients in shopping list (#456)
refactor: Extract repository mappers to separate files
docs: Update testing guide with ViewModel examples
```

### PR Template

```markdown
## Description
Brief description of changes and motivation.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Refactoring (no functional changes)
- [ ] Documentation update

## Related Issues
Closes #123
Related to #456

## Changes Made
- Added meal plan calendar view
- Implemented date selection logic
- Created MealPlanViewModel with state management
- Added unit tests for ViewModel

## Testing
- [ ] Unit tests added/updated
- [ ] Instrumented tests added/updated
- [ ] Manual testing completed
- [ ] All tests passing

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Tests added/updated and passing
- [ ] Branch is up to date with develop
```

### Review Process

1. **Automated Checks**: CI/CD runs tests and lint
2. **Code Review**: Team member reviews code
3. **Feedback**: Address review comments
4. **Approval**: Reviewer approves PR
5. **Merge**: Squash and merge to `develop`

### After Merge

- Delete your feature branch
- Update local `develop` branch
- Close related issues

## Questions?

- Check the [documentation](docs/README.md)
- Ask in team chat
- Open a discussion on GitHub

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
