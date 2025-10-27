# Shoppit Documentation

Welcome to the Shoppit Android application documentation. This directory contains comprehensive guides, architecture documentation, and quick references for developers working on the project.

📋 **[Complete Documentation Index](INDEX.md)** - Master navigation hub with searchable index of all documentation

## 📚 Documentation Overview

### Quick Start Paths

**New to the project?**
1. [Getting Started Guide](guides/getting-started.md) - Setup and first steps
2. [Architecture Overview](architecture/overview.md) - Understand the system design
3. [Code Style Guide](guides/code-style.md) - Learn our conventions

**Building a feature?**
1. [Architecture Overview](architecture/overview.md) - Design principles
2. [Data Flow](architecture/data-flow.md) - Repository patterns
3. [Dependency Injection Guide](guides/dependency-injection.md) - DI setup
4. [Compose Patterns Guide](guides/compose-patterns.md) - UI best practices

**Writing tests?**
1. [Testing Guide](guides/testing.md) - Comprehensive testing strategies
2. [Dependency Injection Guide](guides/dependency-injection.md) - Testing with Hilt
3. [Compose Patterns Guide](guides/compose-patterns.md) - UI testing

## 📖 Documentation Categories

### Architecture & Design

Understand the system design and architectural patterns:

- **[Architecture Overview](architecture/overview.md)** - High-level architecture and Clean Architecture principles
- **[Detailed Design](architecture/detailed-design.md)** - Comprehensive architecture specification with package structure
- **[Data Flow](architecture/data-flow.md)** - Data movement patterns and repository patterns
- **[State Management](architecture/state-management.md)** - ViewModel state patterns and Compose state management

### Development Guides

Step-by-step guides for common development tasks:

- **[Getting Started](guides/getting-started.md)** - Project setup, IDE configuration, and first steps
- **[Dependency Injection](guides/dependency-injection.md)** - Hilt DI configuration, modules, and usage patterns
- **[Testing Guide](guides/testing.md)** - Unit, integration, and UI testing strategies
- **[Git Workflow](guides/git-workflow.md)** - Branching strategy, commit conventions, and PR process
- **[Code Style](guides/code-style.md)** - Kotlin conventions, file naming, and coding standards
- **[Compose Patterns](guides/compose-patterns.md)** - Compose best practices, state management, and performance
- **[MCP Usage](guides/mcp-usage.md)** - MCP tool guidelines and decision tree

### Technical Reference

Quick lookup references for daily development:

- **[Hilt Quick Reference](reference/hilt-quick-reference.md)** - DI patterns and common issues
- **[Database Schema](reference/database-schema.md)** - Room entities, DAOs, and queries
- **[API Reference](reference/api-reference.md)** - Use cases, repositories, and domain models
- **[Gradle Commands](reference/gradle-commands.md)** - Build, test, and code quality commands

### Setup Documentation

Implementation details and setup guides:

- **[Hilt Implementation Summary](setup/hilt-implementation-summary.md)** - Complete Hilt setup documentation

## 🎯 Common Tasks

### I want to...

- **Set up the project** → [Getting Started Guide](guides/getting-started.md)
- **Add a new feature** → [Architecture Overview](architecture/overview.md) + [Data Flow](architecture/data-flow.md)
- **Build UI with Compose** → [Compose Patterns Guide](guides/compose-patterns.md)
- **Use Hilt DI** → [Dependency Injection Guide](guides/dependency-injection.md)
- **Write tests** → [Testing Guide](guides/testing.md)
- **Work with database** → [Database Schema](reference/database-schema.md)
- **Contribute code** → [Git Workflow](guides/git-workflow.md) + [Contributing Guide](../CONTRIBUTING.md)
- **Use MCP tools** → [MCP Usage Guide](guides/mcp-usage.md)

For a complete task-based index, see the [Documentation Index](INDEX.md).

## 📋 Documentation by Role

### New Developer
Start here to get up to speed:
1. [Getting Started Guide](guides/getting-started.md)
2. [Architecture Overview](architecture/overview.md)
3. [Code Style Guide](guides/code-style.md)
4. [Contributing Guide](../CONTRIBUTING.md)

### Feature Developer
Essential reading for building features:
1. [Architecture Overview](architecture/overview.md)
2. [Detailed Design](architecture/detailed-design.md)
3. [Data Flow](architecture/data-flow.md)
4. [Dependency Injection Guide](guides/dependency-injection.md)
5. [Compose Patterns Guide](guides/compose-patterns.md)

### UI Developer
Focus on UI development:
1. [Compose Patterns Guide](guides/compose-patterns.md)
2. [State Management](architecture/state-management.md)
3. [Code Style Guide](guides/code-style.md)
4. [Testing Guide](guides/testing.md) - UI testing section

### Reviewer
Review checklist and standards:
1. [Contributing Guide](../CONTRIBUTING.md)
2. [Architecture Overview](architecture/overview.md)
3. [Code Style Guide](guides/code-style.md)
4. [Testing Guide](guides/testing.md)

## 🔍 Finding Information

### By Topic
- **Architecture** → [architecture/](architecture/)
- **Dependency Injection** → [guides/dependency-injection.md](guides/dependency-injection.md) + [reference/hilt-quick-reference.md](reference/hilt-quick-reference.md)
- **Testing** → [guides/testing.md](guides/testing.md)
- **UI Development** → [guides/compose-patterns.md](guides/compose-patterns.md)
- **Data Layer** → [architecture/data-flow.md](architecture/data-flow.md) + [reference/database-schema.md](reference/database-schema.md)
- **Workflow** → [guides/git-workflow.md](guides/git-workflow.md) + [../CONTRIBUTING.md](../CONTRIBUTING.md)

### By File Type
- **Guides** → [guides/](guides/) - How-to instructions
- **Architecture** → [architecture/](architecture/) - System design
- **Reference** → [reference/](reference/) - Quick lookup
- **Setup** → [setup/](setup/) - Implementation details

## 📝 Contributing to Documentation

When adding features or making changes:

1. **Update relevant documentation** - Keep docs in sync with code
2. **Add examples** - Include code examples from the Shoppit domain
3. **Update INDEX.md** - Add new documents to the index
4. **Follow conventions** - Use consistent formatting and structure
5. **Test examples** - Ensure code examples compile and work

See [Keeping Documentation Updated](INDEX.md#-keeping-documentation-updated) in the index for detailed guidelines.

## 🔗 External Resources

### Official Documentation
- [Kotlin](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt](https://dagger.dev/hilt/)
- [Room](https://developer.android.com/training/data-storage/room)
- [Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)

### Best Practices
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Dependency Injection](https://developer.android.com/training/dependency-injection)
- [Testing](https://developer.android.com/training/testing)

## 📧 Getting Help

Can't find what you need?

1. **Search the documentation** - Use the [Documentation Index](INDEX.md)
2. **Check the code** - Look for KDoc comments
3. **Review examples** - Check existing code for patterns
4. **Ask the team** - Team chat or standup
5. **Open an issue** - For documentation improvements

## 🎯 Documentation Goals

Our documentation aims to:

- ✅ Help new developers get started quickly
- ✅ Provide clear examples and patterns
- ✅ Explain architectural decisions
- ✅ Enable self-service problem solving
- ✅ Maintain consistency across the codebase
- ✅ Support multiple learning paths

---

**For the complete documentation index with task-based navigation, role-based paths, and topic organization, see [INDEX.md](INDEX.md).**
