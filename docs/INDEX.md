# Shoppit Documentation Index

Complete index of all documentation for the Shoppit Android project.

## 📖 Quick Start

New to the project? Start here:

1. [README](../README.md) - Project overview and quick start
2. [Getting Started Guide](guides/getting-started.md) - Detailed setup instructions
3. [Architecture Overview](architecture/overview.md) - Understand the system design
4. [Contributing Guide](../CONTRIBUTING.md) - How to contribute

## 📚 Documentation Structure

### Root Level Documents

| Document | Description |
|----------|-------------|
| [README.md](../README.md) | Project overview, features, tech stack, and quick start |
| [CHANGELOG.md](../CHANGELOG.md) | Version history and changes |
| [CONTRIBUTING.md](../CONTRIBUTING.md) | Contribution guidelines, workflow, and PR process |

### Architecture Documentation

Located in `docs/architecture/`

| Document | Description | Audience |
|----------|-------------|----------|
| [Overview](architecture/overview.md) | High-level architecture and Clean Architecture principles | All developers |
| [Detailed Design](architecture/detailed-design.md) | Comprehensive architecture specification | Architects, senior developers |
| [Data Flow](architecture/data-flow.md) | Data movement patterns and repository patterns | Feature developers |
| [State Management](architecture/state-management.md) | ViewModel state patterns and Compose state | UI developers |

**Topics Covered:**
- Clean Architecture layers and dependency rules
- Package structure and file naming conventions
- Data flow from UI to database
- State management with StateFlow and Compose
- Error handling strategies
- Coroutines and Flow usage
- Testing architecture

### Development Guides

Located in `docs/guides/`

| Document | Description | When to Read |
|----------|-------------|--------------|
| [Getting Started](guides/getting-started.md) | Project setup and first steps | First time setup |
| [Dependency Injection](guides/dependency-injection.md) | Hilt DI configuration and usage | When using DI |
| [Testing Guide](guides/testing.md) | Testing strategies and patterns | When writing tests |
| [Git Workflow](guides/git-workflow.md) | Branching, commits, and PRs | Before contributing |
| [Code Style](guides/code-style.md) | Coding conventions and standards | Daily development |
| [Compose Patterns](guides/compose-patterns.md) | Compose best practices | When building UI |
| [MCP Usage](guides/mcp-usage.md) | MCP tool guidelines | When using MCP tools |

**Topics Covered:**
- Project setup and IDE configuration
- Hilt modules, scopes, and injection patterns
- Unit, integration, and UI testing
- Branch strategy and commit conventions
- Kotlin conventions and file naming
- State management and performance optimization
- File operations and documentation queries

### Technical Reference

Located in `docs/reference/`

| Document | Description | Use Case |
|----------|-------------|----------|
| [Hilt Quick Reference](reference/hilt-quick-reference.md) | Quick lookup for Hilt patterns | Daily development |
| [Database Schema](reference/database-schema.md) | Room entities, DAOs, and queries | Database work |
| [API Reference](reference/api-reference.md) | Use cases and repository interfaces | Domain layer work |
| [Meal Planning API](reference/meal-planning-api.md) | Meal planning use cases and models | Meal planning feature |
| [Gradle Commands](reference/gradle-commands.md) | Common Gradle commands | Build and test |

**Topics Covered:**
- Module annotations and dependency provision
- Entity definitions and relationships (including meal plans)
- Use case catalog and domain models
- Build, test, and code quality commands

### Setup Documentation

Located in `docs/setup/`

| Document | Description | Audience |
|----------|-------------|----------|
| [Hilt Implementation Summary](setup/hilt-implementation-summary.md) | Complete Hilt setup documentation | Developers, reviewers |

**Topics Covered:**
- Implemented components
- Usage examples
- Verification steps
- Next steps

## 🎯 Documentation by Task

### I want to...

#### Set up the project
1. Read [README](../README.md) for overview and tech stack
2. Follow [Getting Started Guide](guides/getting-started.md) for detailed setup
3. Review [Architecture Overview](architecture/overview.md) to understand the structure
4. Check [Code Style Guide](guides/code-style.md) for conventions

#### Add a new feature
1. Review [Architecture Overview](architecture/overview.md) for layer responsibilities
2. Check [Detailed Design](architecture/detailed-design.md) for patterns
3. Read [Data Flow](architecture/data-flow.md) for repository patterns
4. Use [Dependency Injection Guide](guides/dependency-injection.md) for DI setup
5. Follow [Git Workflow](guides/git-workflow.md) for branching
6. Reference [Hilt Quick Reference](reference/hilt-quick-reference.md) during development

#### Build UI with Compose
1. Read [Compose Patterns Guide](guides/compose-patterns.md) for best practices
2. Check [State Management](architecture/state-management.md) for ViewModel patterns
3. Use [Code Style Guide](guides/code-style.md) for composable conventions
4. Review [Testing Guide](guides/testing.md) for UI testing

#### Use Hilt DI
1. Read [Dependency Injection Guide](guides/dependency-injection.md) for comprehensive guide
2. Use [Hilt Quick Reference](reference/hilt-quick-reference.md) for quick patterns
3. Check [Hilt Implementation Summary](setup/hilt-implementation-summary.md) for examples

#### Write tests
1. Read [Testing Guide](guides/testing.md) for comprehensive testing strategies
2. Check [Dependency Injection Guide](guides/dependency-injection.md) for testing with Hilt
3. Use [Hilt Quick Reference](reference/hilt-quick-reference.md) for testing patterns
4. Review [Compose Patterns Guide](guides/compose-patterns.md) for UI testing

#### Work with database
1. Check [Database Schema](reference/database-schema.md) for entities and DAOs
2. Review [Data Flow](architecture/data-flow.md) for repository patterns
3. Read [API Reference](reference/api-reference.md) for domain models
4. See [Meal Planning API](reference/meal-planning-api.md) for meal planning models

#### Understand the architecture
1. Start with [Architecture Overview](architecture/overview.md)
2. Deep dive into [Detailed Design](architecture/detailed-design.md)
3. Review [Data Flow](architecture/data-flow.md) for data movement
4. Check [State Management](architecture/state-management.md) for state patterns

#### Contribute to the project
1. Read [Contributing Guide](../CONTRIBUTING.md) for workflow and guidelines
2. Follow [Git Workflow](guides/git-workflow.md) for branching and commits
3. Check [Code Style Guide](guides/code-style.md) for conventions
4. Review [Testing Guide](guides/testing.md) for test requirements
5. Check [CHANGELOG](../CHANGELOG.md) for recent changes

#### Use MCP tools
1. Read [MCP Usage Guide](guides/mcp-usage.md) for tool guidelines
2. Check decision tree for tool selection
3. Review best practices for each tool type

#### Run builds and tests
1. Check [Gradle Commands](reference/gradle-commands.md) for common commands
2. Review [Testing Guide](guides/testing.md) for test execution
3. See [Getting Started Guide](guides/getting-started.md) for initial setup

#### Troubleshoot issues
1. Check [Getting Started Guide](guides/getting-started.md) - Troubleshooting section
2. Review [Dependency Injection Guide](guides/dependency-injection.md) - Troubleshooting section
3. Use [Hilt Quick Reference](reference/hilt-quick-reference.md) - Common issues
4. Check [Git Workflow](guides/git-workflow.md) - Troubleshooting section

## 📋 Documentation by Role

### New Developer
**Priority Reading:**
1. [README](../README.md) - Project overview
2. [Getting Started Guide](guides/getting-started.md) - Setup instructions
3. [Architecture Overview](architecture/overview.md) - System design
4. [Code Style Guide](guides/code-style.md) - Conventions
5. [Contributing Guide](../CONTRIBUTING.md) - Workflow

**Next Steps:**
- [Dependency Injection Guide](guides/dependency-injection.md)
- [Testing Guide](guides/testing.md)
- [Git Workflow](guides/git-workflow.md)

### Feature Developer
**Essential Reading:**
1. [Architecture Overview](architecture/overview.md) - Design principles
2. [Detailed Design](architecture/detailed-design.md) - Comprehensive patterns
3. [Data Flow](architecture/data-flow.md) - Repository patterns
4. [State Management](architecture/state-management.md) - ViewModel patterns
5. [Dependency Injection Guide](guides/dependency-injection.md) - DI setup
6. [Compose Patterns Guide](guides/compose-patterns.md) - UI best practices

**Quick References:**
- [Hilt Quick Reference](reference/hilt-quick-reference.md)
- [Database Schema](reference/database-schema.md)
- [API Reference](reference/api-reference.md)
- [Meal Planning API](reference/meal-planning-api.md)
- [Gradle Commands](reference/gradle-commands.md)

### UI Developer
**Essential Reading:**
1. [Compose Patterns Guide](guides/compose-patterns.md) - Best practices
2. [State Management](architecture/state-management.md) - ViewModel patterns
3. [Code Style Guide](guides/code-style.md) - Composable conventions
4. [Testing Guide](guides/testing.md) - UI testing

**Quick References:**
- [Hilt Quick Reference](reference/hilt-quick-reference.md)
- [API Reference](reference/api-reference.md)
- [Meal Planning API](reference/meal-planning-api.md)

### Architect/Tech Lead
**Comprehensive Reading:**
1. [Detailed Design](architecture/detailed-design.md) - Complete architecture
2. [Architecture Overview](architecture/overview.md) - Principles
3. [Data Flow](architecture/data-flow.md) - Data patterns
4. [State Management](architecture/state-management.md) - State patterns
5. [Hilt Implementation Summary](setup/hilt-implementation-summary.md) - DI setup
6. [CHANGELOG](../CHANGELOG.md) - Project history

**All Guides:**
- Review all guides for consistency
- Ensure patterns are followed

**All References:**
- [Database Schema](reference/database-schema.md)
- [API Reference](reference/api-reference.md)
- [Meal Planning API](reference/meal-planning-api.md)

### Reviewer
**Review Checklist:**
1. [Contributing Guide](../CONTRIBUTING.md) - PR requirements
2. [Architecture Overview](architecture/overview.md) - Design patterns
3. [Code Style Guide](guides/code-style.md) - Conventions
4. [Testing Guide](guides/testing.md) - Test requirements
5. [Git Workflow](guides/git-workflow.md) - Commit format

**Quick References:**
- [Hilt Quick Reference](reference/hilt-quick-reference.md)
- [Database Schema](reference/database-schema.md)
- [API Reference](reference/api-reference.md)

## 🔍 Documentation by Topic

### Architecture
- [Architecture Overview](architecture/overview.md) - Clean Architecture principles
- [Detailed Design](architecture/detailed-design.md) - Comprehensive specification
- [Data Flow](architecture/data-flow.md) - Data movement patterns
- [State Management](architecture/state-management.md) - State handling

### Dependency Injection
- [Dependency Injection Guide](guides/dependency-injection.md) - Comprehensive Hilt guide
- [Hilt Quick Reference](reference/hilt-quick-reference.md) - Quick patterns
- [Hilt Implementation Summary](setup/hilt-implementation-summary.md) - Setup details

### Testing
- [Testing Guide](guides/testing.md) - Comprehensive testing strategies
- [Dependency Injection Guide](guides/dependency-injection.md) - Testing with Hilt
- [Hilt Quick Reference](reference/hilt-quick-reference.md) - Testing patterns
- [Compose Patterns Guide](guides/compose-patterns.md) - UI testing

### UI Development
- [Compose Patterns Guide](guides/compose-patterns.md) - Best practices
- [State Management](architecture/state-management.md) - ViewModel patterns
- [Code Style Guide](guides/code-style.md) - Composable conventions

### Data Layer
- [Database Schema](reference/database-schema.md) - Room entities and DAOs
- [Data Flow](architecture/data-flow.md) - Repository patterns
- [API Reference](reference/api-reference.md) - Domain interfaces
- [Meal Planning API](reference/meal-planning-api.md) - Meal planning models

### Development Workflow
- [Getting Started Guide](guides/getting-started.md) - Setup and first steps
- [Git Workflow](guides/git-workflow.md) - Branching and commits
- [Contributing Guide](../CONTRIBUTING.md) - PR process
- [CHANGELOG](../CHANGELOG.md) - Version history

### Code Quality
- [Code Style Guide](guides/code-style.md) - Conventions and standards
- [Testing Guide](guides/testing.md) - Test coverage and patterns
- [Compose Patterns Guide](guides/compose-patterns.md) - UI best practices

### Tools & Build
- [Gradle Commands](reference/gradle-commands.md) - Build and test commands
- [MCP Usage Guide](guides/mcp-usage.md) - MCP tool guidelines
- [Getting Started Guide](guides/getting-started.md) - IDE setup

### Project Setup
- [README](../README.md) - Overview and quick start
- [Getting Started Guide](guides/getting-started.md) - Detailed setup
- [Hilt Implementation Summary](setup/hilt-implementation-summary.md) - DI setup

## 📝 Keeping Documentation Updated

When making changes to the project:

### Adding Features
- Update [CHANGELOG](../CHANGELOG.md) with new features
- Add examples to relevant guides
- Update [README](../README.md) if adding major features
- Update [API Reference](reference/api-reference.md) for new use cases
- Update [Meal Planning API](reference/meal-planning-api.md) for meal planning features
- Update [Database Schema](reference/database-schema.md) for new entities

### Changing Architecture
- Update [Architecture Overview](architecture/overview.md)
- Update [Detailed Design](architecture/detailed-design.md)
- Update [Data Flow](architecture/data-flow.md) if data patterns change
- Update [State Management](architecture/state-management.md) if state patterns change
- Document in [CHANGELOG](../CHANGELOG.md)

### Adding DI Components
- Update [Dependency Injection Guide](guides/dependency-injection.md)
- Add to [Hilt Quick Reference](reference/hilt-quick-reference.md)
- Document in [Hilt Implementation Summary](setup/hilt-implementation-summary.md)

### Changing Workflow
- Update [Contributing Guide](../CONTRIBUTING.md)
- Update [Git Workflow](guides/git-workflow.md)
- Update [Getting Started Guide](guides/getting-started.md)

### Adding UI Patterns
- Update [Compose Patterns Guide](guides/compose-patterns.md)
- Update [State Management](architecture/state-management.md) if relevant
- Add examples to [Code Style Guide](guides/code-style.md)

### Updating Tests
- Update [Testing Guide](guides/testing.md) with new patterns
- Update [Dependency Injection Guide](guides/dependency-injection.md) for DI testing
- Update [Compose Patterns Guide](guides/compose-patterns.md) for UI testing

## 🔗 External Resources

### Official Documentation
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Material Design 3](https://m3.material.io/)

### Best Practices
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Dependency Injection Best Practices](https://developer.android.com/training/dependency-injection)
- [Testing Guide](https://developer.android.com/training/testing)
- [Compose Performance](https://developer.android.com/jetpack/compose/performance)

### Tools
- [Gradle Documentation](https://docs.gradle.org/)
- [Android Studio](https://developer.android.com/studio)
- [Git Documentation](https://git-scm.com/doc)

## 📧 Getting Help

If you can't find what you're looking for:

1. **Search the documentation** - Use Ctrl+F or search in your IDE
2. **Check the code** - Most files have detailed KDoc comments
3. **Review examples** - Check existing code for patterns
4. **Ask the team** - Team chat or during standup
5. **Open an issue** - For documentation improvements or questions

## 🎯 Documentation Goals

Our documentation aims to:

- ✅ Help new developers get started quickly
- ✅ Provide clear examples and patterns
- ✅ Explain architectural decisions
- ✅ Enable self-service problem solving
- ✅ Maintain consistency across the codebase
- ✅ Support multiple learning paths (by task, role, topic)
- ✅ Provide quick references for daily development

## 📊 Documentation Status

| Category | Status | Last Updated |
|----------|--------|--------------|
| Root Documentation | ✅ Complete | 2024 |
| Getting Started | ✅ Complete | 2024 |
| Architecture Overview | ✅ Complete | 2024 |
| Detailed Design | ✅ Complete | 2024 |
| Data Flow | ✅ Complete | 2024 |
| State Management | ✅ Complete | 2024 |
| Dependency Injection | ✅ Complete | 2024 |
| Testing Guide | ✅ Complete | 2024 |
| Git Workflow | ✅ Complete | 2024 |
| Code Style | ✅ Complete | 2024 |
| Compose Patterns | ✅ Complete | 2024 |
| MCP Usage | ✅ Complete | 2024 |
| Hilt Quick Reference | ✅ Complete | 2024 |
| Database Schema | ✅ Complete | 2024 |
| API Reference | ✅ Complete | 2024 |
| Meal Planning API | ✅ Complete | 2024 |
| Gradle Commands | ✅ Complete | 2024 |
| Contributing Guide | ✅ Complete | 2024 |

Legend:
- ✅ Complete and up-to-date
- 🚧 In progress or planned
- ⚠️ Needs update

---

**Last Updated:** 2024
**Maintained By:** Shoppit Development Team
