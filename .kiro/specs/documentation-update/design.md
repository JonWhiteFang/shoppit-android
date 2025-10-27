# Design Document

## Overview

This design outlines the comprehensive update of the Shoppit project documentation system. The update will transform the current documentation into a complete, consistent, and maintainable knowledge base that aligns with all project standards defined in steering rules. The design focuses on creating a hierarchical documentation structure with clear navigation, comprehensive coverage of all topics, and practical examples throughout.

## Architecture

### Documentation Structure

The documentation system follows a hierarchical organization:

```
Root Level (Quick Start & Overview)
├── README.md (Project overview, quick start)
├── CONTRIBUTING.md (Contribution guidelines)
└── CHANGELOG.md (Version history)

Documentation Directory (Detailed Information)
├── INDEX.md (Master navigation and search)
├── README.md (Documentation overview)
├── architecture/ (System design)
│   ├── overview.md (High-level architecture)
│   ├── detailed-design.md (Comprehensive design)
│   ├── data-flow.md (Data movement patterns)
│   └── state-management.md (State handling patterns)
├── guides/ (How-to instructions)
│   ├── getting-started.md (Setup and first steps)
│   ├── dependency-injection.md (Hilt DI guide)
│   ├── testing.md (Testing strategies)
│   ├── git-workflow.md (Git and PR guidelines)
│   ├── code-style.md (Coding conventions)
│   ├── compose-patterns.md (Compose best practices)
│   └── mcp-usage.md (MCP tool guidelines)
├── reference/ (Quick lookup)
│   ├── hilt-quick-reference.md (DI patterns)
│   ├── database-schema.md (Room schema)
│   ├── api-reference.md (Use cases and repositories)
│   └── gradle-commands.md (Common commands)
└── setup/ (Implementation details)
    └── hilt-implementation-summary.md (DI setup)
```

### Information Flow

Documentation follows a progressive disclosure pattern:

1. **Entry Point**: README.md provides high-level overview and links to detailed docs
2. **Navigation Hub**: docs/INDEX.md provides comprehensive navigation by task, role, and topic
3. **Category Pages**: docs/README.md and category-specific overviews guide to specific documents
4. **Detailed Guides**: Individual markdown files provide in-depth information
5. **Quick References**: Reference documents provide fast lookup for common patterns

### Cross-Referencing Strategy

- Root README links to docs/guides/getting-started.md for detailed setup
- docs/INDEX.md provides multiple navigation paths (by task, role, topic)
- Each guide links to related guides and references
- Architecture docs link to implementation guides
- Guides link to quick references for patterns
- All docs link back to INDEX.md for navigation

## Components and Interfaces

### Root Documentation Components

#### README.md
**Purpose**: Project overview and quick start
**Sections**:
- Project description and features
- Tech stack summary (from tech.md)
- Quick start instructions
- Project structure overview
- Links to detailed documentation
- Development commands
- Contributing guidelines link

#### CONTRIBUTING.md
**Purpose**: Contribution guidelines and workflow
**Sections**:
- Code of conduct
- Development workflow
- Branch strategy (from git-workflow.md)
- Commit message format (from git-workflow.md)
- Pull request process
- Code review guidelines
- Testing requirements

#### CHANGELOG.md
**Purpose**: Version history
**Sections**:
- Unreleased changes
- Version entries with dates
- Categories: Added, Changed, Deprecated, Removed, Fixed, Security

### Documentation Index Component

#### docs/INDEX.md
**Purpose**: Master navigation hub
**Sections**:
- Quick start paths
- Documentation by task ("I want to...")
- Documentation by role (New Developer, Feature Developer, Architect, Reviewer)
- Documentation by topic (Architecture, DI, Testing, etc.)
- Documentation status table
- External resources
- Getting help section

### Architecture Documentation Components

#### architecture/overview.md
**Purpose**: High-level architecture introduction
**Sections**:
- Clean Architecture principles
- Layer responsibilities (from structure.md)
- Dependency rules
- Data flow overview
- Key architectural decisions
- Diagrams (Mermaid)

#### architecture/detailed-design.md
**Purpose**: Comprehensive architecture specification
**Sections**:
- Detailed layer descriptions
- Package structure (from structure.md)
- File naming conventions (from structure.md)
- State management patterns
- Error handling strategy
- Coroutines and Flow usage
- Testing architecture

#### architecture/data-flow.md (NEW)
**Purpose**: Data movement patterns
**Sections**:
- UI to data layer flow
- Repository patterns
- Flow and StateFlow usage
- Offline-first strategy
- Data synchronization
- Caching patterns

#### architecture/state-management.md (NEW)
**Purpose**: State handling patterns
**Sections**:
- ViewModel state patterns
- UI state classes
- State hoisting
- Side effects
- Compose state management
- Testing state

### Guide Documentation Components

#### guides/getting-started.md
**Purpose**: Project setup and first steps
**Sections**:
- Prerequisites
- Project setup steps
- IDE configuration
- Running the app
- Project structure walkthrough
- First feature implementation
- Troubleshooting

#### guides/dependency-injection.md
**Purpose**: Hilt DI guide
**Sections**:
- Hilt overview
- Module creation
- Providing dependencies
- Injection patterns
- Scopes and components
- Testing with Hilt
- Common patterns
- Troubleshooting

#### guides/testing.md (NEW)
**Purpose**: Comprehensive testing guide
**Sections**:
- Testing philosophy (from testing-strategy.md)
- Test coverage guidelines (from testing-strategy.md)
- Unit testing patterns (from testing-strategy.md)
- Instrumented testing patterns (from testing-strategy.md)
- ViewModel testing (from testing-strategy.md)
- Use case testing (from testing-strategy.md)
- Repository testing (from testing-strategy.md)
- Compose UI testing (from testing-strategy.md)
- Test data builders (from testing-strategy.md)
- Mocking guidelines (from testing-strategy.md)
- Coroutine testing (from testing-strategy.md)
- Running tests
- CI/CD integration

#### guides/git-workflow.md (NEW)
**Purpose**: Git and PR guidelines
**Sections**:
- Branch strategy (from git-workflow.md)
- Branch naming conventions (from git-workflow.md)
- Commit message format (from git-workflow.md)
- Workflow steps (from git-workflow.md)
- Pull request guidelines (from git-workflow.md)
- Merge strategies (from git-workflow.md)
- Git hooks (from git-workflow.md)
- Useful commands (from git-workflow.md)
- Troubleshooting (from git-workflow.md)

#### guides/code-style.md (NEW)
**Purpose**: Coding conventions
**Sections**:
- Kotlin conventions (from structure.md)
- File naming conventions (from structure.md)
- Package organization (from structure.md)
- Code formatting
- Documentation standards
- Naming conventions
- Best practices
- Anti-patterns to avoid

#### guides/compose-patterns.md (NEW)
**Purpose**: Compose best practices
**Sections**:
- State management patterns (from compose-patterns.md)
- Composable structure (from compose-patterns.md)
- Performance optimization (from compose-patterns.md)
- Navigation patterns (from compose-patterns.md)
- Reusable components (from compose-patterns.md)
- Dialog patterns (from compose-patterns.md)
- Preview patterns (from compose-patterns.md)
- Side effects (from compose-patterns.md)
- Testing composables (from compose-patterns.md)
- Common pitfalls (from compose-patterns.md)

#### guides/mcp-usage.md (NEW)
**Purpose**: MCP tool guidelines
**Sections**:
- MCP overview
- File operations (from mcp-usage-guidelines.md)
- Documentation queries (from mcp-usage-guidelines.md)
- Information research (from mcp-usage-guidelines.md)
- Complex problem solving (from mcp-usage-guidelines.md)
- GitHub operations (from mcp-usage-guidelines.md)
- Decision tree (from mcp-usage-guidelines.md)
- Best practices (from mcp-usage-guidelines.md)

### Reference Documentation Components

#### reference/hilt-quick-reference.md
**Purpose**: Quick DI pattern lookup
**Sections**:
- Module annotations
- Providing dependencies
- Injection patterns
- Scopes
- Testing patterns
- Common issues

#### reference/database-schema.md (NEW)
**Purpose**: Room database schema
**Sections**:
- Entity definitions
- Relationships
- DAOs
- Migrations
- Queries
- Indexes

#### reference/api-reference.md (NEW)
**Purpose**: Use cases and repositories
**Sections**:
- Use case catalog
- Repository interfaces
- Domain models
- Error types
- Validators

#### reference/gradle-commands.md (NEW)
**Purpose**: Common Gradle commands
**Sections**:
- Build commands (from tech.md)
- Test commands (from tech.md)
- Code quality commands (from tech.md)
- Development commands (from tech.md)
- Troubleshooting commands

## Data Models

### Documentation File Structure

```kotlin
data class DocumentationFile(
    val path: String,              // e.g., "docs/guides/testing.md"
    val title: String,             // e.g., "Testing Guide"
    val category: DocumentCategory, // ARCHITECTURE, GUIDE, REFERENCE, SETUP
    val status: DocumentStatus,    // COMPLETE, IN_PROGRESS, PENDING
    val sections: List<Section>,   // Hierarchical sections
    val crossReferences: List<String>, // Links to other docs
    val lastUpdated: String        // Date of last update
)

enum class DocumentCategory {
    ROOT,           // README.md, CONTRIBUTING.md, CHANGELOG.md
    ARCHITECTURE,   // docs/architecture/
    GUIDE,          // docs/guides/
    REFERENCE,      // docs/reference/
    SETUP          // docs/setup/
}

enum class DocumentStatus {
    COMPLETE,      // ✅ Complete and up-to-date
    IN_PROGRESS,   // 🚧 In progress or planned
    PENDING,       // ⚠️ Needs creation or update
    OUTDATED       // ⚠️ Needs update
}

data class Section(
    val level: Int,           // Heading level (1-6)
    val title: String,        // Section title
    val content: String,      // Section content
    val codeExamples: List<CodeExample>,
    val subsections: List<Section>
)

data class CodeExample(
    val language: String,     // kotlin, bash, gradle, etc.
    val code: String,         // Code content
    val description: String   // What the code demonstrates
)
```

### Navigation Structure

```kotlin
data class NavigationPath(
    val pathType: PathType,
    val title: String,
    val description: String,
    val documents: List<String>  // Ordered list of document paths
)

enum class PathType {
    QUICK_START,    // For new developers
    BY_TASK,        // "I want to..." paths
    BY_ROLE,        // New Developer, Feature Developer, etc.
    BY_TOPIC        // Architecture, Testing, etc.
}
```

## Error Handling

### Documentation Validation

**Missing Links Detection**:
- Scan all markdown files for broken internal links
- Report links to non-existent files
- Suggest corrections for typos in links

**Consistency Validation**:
- Check heading level hierarchy (no skipped levels)
- Verify code block language tags
- Ensure consistent terminology usage
- Validate cross-reference patterns

**Completeness Validation**:
- Check that all files in INDEX.md exist
- Verify all steering rule topics are covered
- Ensure all "pending" items have creation plans

### Update Conflicts

**Concurrent Updates**:
- Document files are independent, minimal conflict risk
- INDEX.md updates require coordination
- Use clear section boundaries to minimize conflicts

**Version Mismatches**:
- Code examples reference current versions from libs.versions.toml
- Include version numbers in examples where relevant
- Update CHANGELOG.md with documentation changes

## Testing Strategy

### Documentation Testing

**Link Validation**:
- Automated script to check all internal links
- Verify external links are accessible
- Test relative path resolution

**Code Example Validation**:
- Extract code examples from documentation
- Compile Kotlin examples against project
- Verify bash commands are syntactically correct
- Test Gradle commands execute successfully

**Rendering Validation**:
- Preview all markdown files in viewer
- Verify Mermaid diagrams render correctly
- Check code block syntax highlighting
- Ensure tables format properly

**Content Review**:
- Peer review for technical accuracy
- Check examples match current patterns
- Verify instructions work on clean setup
- Test navigation paths are logical

### Manual Testing Checklist

**New Developer Path**:
1. Follow README.md quick start
2. Complete getting-started.md setup
3. Implement first feature using guides
4. Verify all links work
5. Confirm examples compile

**Feature Developer Path**:
1. Navigate using INDEX.md
2. Find relevant architecture docs
3. Follow DI guide for new component
4. Use quick references during coding
5. Verify patterns match codebase

**Documentation Maintenance**:
1. Update a guide with new pattern
2. Verify cross-references update
3. Check INDEX.md reflects changes
4. Update status table
5. Confirm no broken links

## Implementation Phases

### Phase 1: Foundation (Root & Index)
- Update README.md with accurate tech stack and structure
- Create comprehensive CONTRIBUTING.md
- Enhance docs/INDEX.md with all navigation paths
- Update docs/README.md overview

### Phase 2: Architecture Documentation
- Update architecture/overview.md with current patterns
- Enhance architecture/detailed-design.md
- Create architecture/data-flow.md
- Create architecture/state-management.md

### Phase 3: Core Guides
- Update guides/getting-started.md
- Update guides/dependency-injection.md
- Create guides/testing.md from testing-strategy.md
- Create guides/git-workflow.md from git-workflow.md

### Phase 4: Advanced Guides
- Create guides/code-style.md from structure.md
- Create guides/compose-patterns.md from compose-patterns.md
- Create guides/mcp-usage.md from mcp-usage-guidelines.md

### Phase 5: Reference Documentation
- Update reference/hilt-quick-reference.md
- Create reference/database-schema.md
- Create reference/api-reference.md
- Create reference/gradle-commands.md

### Phase 6: Validation & Polish
- Run link validation
- Test code examples
- Peer review all updates
- Update status table in INDEX.md

## Design Decisions

### Markdown Over Other Formats
**Decision**: Use markdown for all documentation
**Rationale**: 
- Version control friendly
- Readable in plain text
- Supported by GitHub, IDEs, and documentation tools
- Easy to maintain and update

### Mermaid for Diagrams
**Decision**: Use Mermaid syntax for diagrams
**Rationale**:
- Version controllable (text-based)
- Renders in GitHub and many markdown viewers
- Easy to update without external tools
- Consistent styling

### Progressive Disclosure
**Decision**: Organize docs from simple to complex
**Rationale**:
- New developers can start quickly
- Advanced topics don't overwhelm beginners
- Clear learning path
- Multiple entry points for different needs

### Steering Rules as Source of Truth
**Decision**: Base documentation on steering rules
**Rationale**:
- Ensures consistency between rules and docs
- Single source of truth for standards
- Easier to maintain alignment
- Steering rules already comprehensive

### Task-Based Navigation
**Decision**: Include "I want to..." navigation in INDEX.md
**Rationale**:
- Matches how developers think
- Faster to find relevant information
- Reduces cognitive load
- Complements topic-based organization

### Code Examples from Shoppit Domain
**Decision**: Use meal planning examples in all code samples
**Rationale**:
- Consistent context across documentation
- Examples are immediately relevant
- Easier to understand in project context
- Demonstrates real patterns from codebase

### Separate Quick References
**Decision**: Maintain separate quick reference documents
**Rationale**:
- Fast lookup during development
- Reduces need to read full guides
- Complements detailed guides
- Optimized for different use cases

## Success Criteria

The documentation update will be considered successful when:

1. **Completeness**: All topics from steering rules are documented
2. **Consistency**: All docs follow same structure and formatting
3. **Accuracy**: All code examples compile and instructions work
4. **Navigation**: Developers can find information in under 2 minutes
5. **Validation**: All links work and code examples are tested
6. **Status**: Documentation status table shows all sections complete
7. **Feedback**: New developers can set up project using only documentation
8. **Maintenance**: Clear guidelines exist for keeping docs updated
