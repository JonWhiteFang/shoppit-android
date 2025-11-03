# Design Document

## Overview

This design outlines a systematic approach to refining all steering documentation in the Shoppit Android project. The refinement will improve clarity, consistency, accuracy, and usability while ensuring all documents provide maximum value to developers and AI assistants.

## Architecture

### Document Categories

Steering documents are organized into the following categories:

1. **Always-Included Core Guidance**
   - `product.md` - Product requirements and domain model
   - `structure.md` - Project structure and architecture
   - `tech.md` - Technology stack and build system
   - `mandatory-security-workflow.md` - Security scanning requirements
   - `security.md` - Security best practices
   - `mcp-usage-guidelines.md` - MCP tool usage rules

2. **File-Specific Guidance**
   - `kotlin-best-practices.md` - Kotlin coding standards (*.kt, *.kts files)

3. **Manual/Reference Guidance**
   - `spec-archiving.md` - Spec archiving workflow
   - `testing-strategy.md` - Testing philosophy and patterns
   - `compose-patterns.md` - Jetpack Compose patterns
   - `data-layer-patterns.md` - Room and repository patterns
   - `error-handling.md` - Error handling across layers
   - `git-workflow.md` - Git branching and commit conventions
   - `navigation-accessibility.md` - Navigation and accessibility
   - `system-environment.md` - System commands and environment

### Refinement Workflow

```
┌─────────────────────────────────────────────────────────────┐
│                    Refinement Process                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Phase 1: Analysis                                           │
│  - Read current document                                     │
│  - Identify issues (outdated info, unclear sections, etc.)   │
│  - Check front matter accuracy                               │
│  - Verify code examples                                      │
│  - Check cross-references                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Phase 2: Refinement                                         │
│  - Update front matter if needed                             │
│  - Fix outdated information                                  │
│  - Improve clarity and conciseness                           │
│  - Enhance code examples                                     │
│  - Add missing cross-references                              │
│  - Ensure Windows command accuracy                           │
│  - Verify practical examples                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Phase 3: Validation                                         │
│  - Verify all code examples are syntactically correct        │
│  - Check all cross-references resolve                        │
│  - Ensure consistency with other documents                   │
│  - Validate against requirements                             │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### Document Analyzer

**Purpose:** Analyze each steering document to identify refinement opportunities

**Inputs:**
- Steering document content
- Project structure
- `libs.versions.toml` for version verification
- Other steering documents for cross-reference validation

**Outputs:**
- List of issues found
- Recommendations for improvements
- Priority level for each issue

**Analysis Checks:**
1. Front matter presence and accuracy
2. Code example syntax and correctness
3. Cross-reference validity
4. Version information currency
5. Windows command format
6. Clarity and conciseness
7. Practical example presence
8. Security emphasis adequacy
9. Testing guidance completeness
10. Structural consistency

### Document Refiner

**Purpose:** Apply refinements to steering documents based on analysis

**Operations:**
1. **Front Matter Updates**
   - Add missing front matter
   - Correct inclusion rules
   - Update file match patterns

2. **Content Improvements**
   - Simplify verbose sections
   - Clarify ambiguous instructions
   - Remove redundant information
   - Add missing context

3. **Code Example Enhancements**
   - Fix syntax errors
   - Add missing imports
   - Provide more context
   - Ensure project convention compliance

4. **Cross-Reference Additions**
   - Link related documents
   - Reference project documentation
   - Add navigation aids

5. **Windows-Specific Corrections**
   - Update Gradle commands to `.\gradlew.bat`
   - Fix file path separators
   - Correct PowerShell syntax
   - Update environment variable references

6. **Version Updates**
   - Sync dependency versions with `libs.versions.toml`
   - Update API levels
   - Correct tool versions

### Validation Engine

**Purpose:** Ensure refined documents meet all requirements

**Validation Steps:**
1. Parse front matter YAML
2. Verify code block syntax
3. Check cross-reference links
4. Validate file paths
5. Ensure structural consistency
6. Confirm requirement coverage

## Data Models

### Document Metadata

```kotlin
data class SteeringDocument(
    val filename: String,
    val frontMatter: FrontMatter?,
    val content: String,
    val sections: List<Section>,
    val codeExamples: List<CodeExample>,
    val crossReferences: List<CrossReference>
)

data class FrontMatter(
    val inclusion: InclusionRule,
    val fileMatchPattern: String? = null
)

enum class InclusionRule {
    ALWAYS,
    MANUAL,
    FILE_MATCH
}

data class Section(
    val heading: String,
    val level: Int,
    val content: String
)

data class CodeExample(
    val language: String,
    val code: String,
    val context: String?
)

data class CrossReference(
    val text: String,
    val target: String,
    val isValid: Boolean
)
```

### Refinement Issue

```kotlin
data class RefinementIssue(
    val type: IssueType,
    val severity: Severity,
    val location: String,
    val description: String,
    val recommendation: String
)

enum class IssueType {
    MISSING_FRONT_MATTER,
    INCORRECT_INCLUSION_RULE,
    OUTDATED_VERSION,
    BROKEN_CROSS_REFERENCE,
    SYNTAX_ERROR,
    UNCLEAR_INSTRUCTION,
    MISSING_EXAMPLE,
    WINDOWS_COMMAND_ERROR,
    REDUNDANT_CONTENT,
    MISSING_CONTEXT
}

enum class Severity {
    CRITICAL,  // Blocks usage
    HIGH,      // Causes confusion
    MEDIUM,    // Reduces clarity
    LOW        // Minor improvement
}
```

## Error Handling

### Analysis Errors

- **File Read Errors**: Log and skip document, report at end
- **Parse Errors**: Note location, attempt to continue analysis
- **Validation Errors**: Collect all errors, report comprehensively

### Refinement Errors

- **Write Errors**: Abort refinement, preserve original
- **Syntax Errors**: Flag for manual review
- **Merge Conflicts**: Preserve both versions, mark for resolution

### Recovery Strategy

1. Always preserve original document before refinement
2. Create backup in `.kiro/specs/steering-docs-refinement/backups/`
3. If refinement fails, restore from backup
4. Log all errors for review
5. Continue with next document on error

## Testing Strategy

### Document Analysis Testing

- Verify issue detection accuracy
- Test front matter parsing
- Validate code example extraction
- Check cross-reference resolution

### Refinement Testing

- Verify front matter updates
- Test content improvements
- Validate code example fixes
- Check cross-reference additions
- Ensure Windows command corrections

### Integration Testing

- Test complete refinement workflow
- Verify backup and restore
- Check error handling
- Validate final document quality

## Refinement Priorities

### Priority 1: Critical Corrections (Must Fix)

1. **Mandatory Security Workflow** (`mandatory-security-workflow.md`)
   - Ensure absolute clarity on security requirements
   - Verify all commands are correct
   - Emphasize consequences of skipping scans

2. **MCP Usage Guidelines** (`mcp-usage-guidelines.md`)
   - Ensure Filesystem MCP mandate is unmissable
   - Verify all tool mappings are correct
   - Add enforcement reminders

3. **System Environment** (`system-environment.md`)
   - Verify all Windows commands are correct
   - Update any outdated paths
   - Ensure Gradle commands use `.bat` extension

### Priority 2: High-Value Improvements (Should Fix)

4. **Structure** (`structure.md`)
   - Verify file naming conventions match actual project
   - Update layer organization if needed
   - Ensure architectural rules are clear

5. **Tech Stack** (`tech.md`)
   - Sync all versions with `libs.versions.toml`
   - Update Gradle commands for Windows
   - Verify implementation notes are current

6. **Testing Strategy** (`testing-strategy.md`)
   - Ensure examples match project patterns
   - Verify test organization matches actual structure
   - Add any missing test patterns

7. **Compose Patterns** (`compose-patterns.md`)
   - Verify all examples use current Compose APIs
   - Ensure state management patterns are clear
   - Add any missing common patterns

8. **Data Layer Patterns** (`data-layer-patterns.md`)
   - Verify Room patterns match project usage
   - Ensure repository patterns are complete
   - Check mapper examples are accurate

9. **Error Handling** (`error-handling.md`)
   - Verify AppError hierarchy matches project
   - Ensure all layers are covered
   - Check UI error display examples

### Priority 3: Enhancements (Nice to Have)

10. **Git Workflow** (`git-workflow.md`)
    - Add more practical examples
    - Enhance troubleshooting section
    - Clarify merge strategies

11. **Navigation & Accessibility** (`navigation-accessibility.md`)
    - Add more accessibility examples
    - Enhance keyboard navigation guidance
    - Clarify deep link configuration

12. **Kotlin Best Practices** (`kotlin-best-practices.md`)
    - Add more idiomatic examples
    - Enhance documentation guidelines
    - Clarify naming conventions

13. **Product Requirements** (`product.md`)
    - Update feature status
    - Clarify business rules
    - Enhance domain model descriptions

14. **Security** (`security.md`)
    - Enhance workflow integration examples
    - Add more common security issues
    - Improve SECURITY_ISSUES.md template

15. **Spec Archiving** (`spec-archiving.md`)
    - Clarify archiving triggers
    - Enhance examples
    - Add troubleshooting section

## Document-Specific Refinement Plans

### 1. mandatory-security-workflow.md

**Issues to Address:**
- Ensure Windows path format in all examples
- Add visual emphasis to critical requirements
- Verify Snyk command syntax
- Add completion checklist

**Refinements:**
- Bold all "MUST" and "REQUIRED" statements
- Add emoji indicators for critical steps (🔒)
- Ensure all path examples use Windows format
- Add "Remember" section at end with key points

### 2. mcp-usage-guidelines.md

**Issues to Address:**
- Ensure Filesystem MCP mandate is unmissable
- Verify all tool mappings
- Add enforcement section
- Clarify decision tree

**Refinements:**
- Add warning box at top about mandatory MCP usage
- Create comparison table: Built-in vs MCP tools
- Enhance decision tree with visual formatting
- Add "Before ANY file operation" reminder

### 3. system-environment.md

**Issues to Address:**
- Verify all commands use Windows format
- Update any outdated paths
- Ensure environment variables are correct
- Check Gradle commands

**Refinements:**
- Verify all `.\gradlew.bat` usage
- Check all backslash path separators
- Update PowerShell examples
- Verify ADB path is current

### 4. structure.md

**Issues to Address:**
- Verify file naming conventions
- Check layer organization
- Ensure architectural rules are clear
- Update feature checklist

**Refinements:**
- Add front matter: `inclusion: always`
- Verify all file naming examples
- Check package structure matches project
- Update feature implementation checklist

### 5. tech.md

**Issues to Address:**
- Sync versions with libs.versions.toml
- Update Gradle commands
- Verify implementation notes
- Check dependency injection examples

**Refinements:**
- Add front matter: `inclusion: always`
- Update all version numbers
- Fix Gradle command format
- Verify Hilt examples

### 6-15. Remaining Documents

Similar analysis and refinement plans for each document, focusing on:
- Front matter accuracy
- Code example correctness
- Cross-reference completeness
- Windows command format
- Clarity and conciseness
- Practical examples

## Implementation Approach

### Phase 1: Preparation

1. Create backup directory
2. Read all steering documents
3. Read `libs.versions.toml` for version verification
4. Analyze project structure for path verification

### Phase 2: Analysis

For each document:
1. Parse front matter
2. Extract code examples
3. Identify cross-references
4. Check for issues
5. Generate refinement recommendations

### Phase 3: Refinement

For each document (in priority order):
1. Create backup
2. Apply refinements
3. Validate changes
4. Save refined document

### Phase 4: Verification

1. Check all cross-references resolve
2. Verify code examples are syntactically correct
3. Ensure consistency across documents
4. Validate against requirements

## Success Criteria

A steering document is successfully refined when:

1. ✅ Front matter is present and accurate
2. ✅ All code examples are syntactically correct
3. ✅ All cross-references resolve correctly
4. ✅ Windows commands use correct format
5. ✅ Version information is current
6. ✅ Content is clear and concise
7. ✅ Practical examples are present
8. ✅ Security requirements are emphasized
9. ✅ Testing guidance is complete
10. ✅ Structure is consistent with other documents

## Maintenance

### Keeping Documents Current

- Review steering docs when major dependencies update
- Update when architectural patterns change
- Revise when new features are added
- Refresh when tooling changes

### Version Tracking

- Note last review date in document
- Track major changes in CHANGELOG.md
- Link to relevant PRs for significant updates

## Notes

- Refinement should preserve the voice and style of original documents
- Focus on clarity and accuracy over perfection
- Prioritize high-impact improvements
- Maintain backward compatibility where possible
- Document any breaking changes clearly
