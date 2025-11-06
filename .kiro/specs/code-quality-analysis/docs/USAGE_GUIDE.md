# Code Quality Analysis System - Usage Guide

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Running Analysis](#running-analysis)
4. [Command-Line Options](#command-line-options)
5. [Configuration](#configuration)
6. [Understanding Reports](#understanding-reports)
7. [Working with Baselines](#working-with-baselines)
8. [Incremental Analysis](#incremental-analysis)
9. [CI/CD Integration](#cicd-integration)
10. [Troubleshooting](#troubleshooting)

## Overview

The Code Quality Analysis System is a comprehensive static analysis tool designed for the Shoppit Android application. It analyzes Kotlin source code to identify:

- Code smells and anti-patterns
- Architecture violations
- Compose best practice violations
- State management issues
- Error handling problems
- Dependency injection issues
- Database pattern violations
- Performance optimization opportunities
- Naming convention violations
- Missing test coverage
- Documentation gaps
- Security vulnerabilities

The system generates detailed, actionable reports with specific recommendations and code examples to help you systematically improve code quality.

## Getting Started

### Prerequisites

- Gradle 8.9 or higher
- Java 17
- Kotlin 2.1.0
- Android SDK (API 24-34)

### Installation

The analysis system is integrated into the Shoppit project's Gradle build. No additional installation is required.

### First Run

To run your first analysis:

```powershell
# Navigate to project root
cd D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android

# Run complete analysis
.\gradlew.bat analyzeCodeQuality
```

This will:
1. Scan all Kotlin files in `app/src/main/java/com/shoppit/app/`
2. Run all analyzers
3. Generate a report at `.kiro/specs/code-quality-analysis/analysis-report.md`
4. Create a baseline at `.kiro/specs/code-quality-analysis/baseline.json`

## Running Analysis

### Complete Analysis

Analyze the entire codebase:

```powershell
.\gradlew.bat analyzeCodeQuality
```

### Incremental Analysis

Analyze specific files or directories:

```powershell
# Analyze specific directory
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui/meal

# Analyze specific file
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt

# Analyze multiple paths
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui,app/src/main/java/com/shoppit/app/data
```

### Filtered Analysis

Run specific analyzers only:

```powershell
# Run only architecture analyzer
.\gradlew.bat analyzeCodeQuality --analyzers=architecture

# Run multiple analyzers
.\gradlew.bat analyzeCodeQuality --analyzers=architecture,compose,state-management

# Available analyzers:
# - code-smell
# - architecture
# - compose
# - state-management
# - error-handling
# - dependency-injection
# - database
# - performance
# - naming
# - test-coverage
# - documentation
# - security
```

### Severity Filtering

Filter results by minimum severity:

```powershell
# Show only high and critical issues
.\gradlew.bat analyzeCodeQuality --severity-threshold=high

# Show only critical issues
.\gradlew.bat analyzeCodeQuality --severity-threshold=critical

# Available thresholds: low, medium, high, critical
```

## Command-Line Options

### Basic Options

| Option | Description | Example |
|--------|-------------|---------|
| `--path` | Specify files/directories to analyze | `--path=app/src/main/java/com/shoppit/app/ui` |
| `--analyzers` | Run specific analyzers only | `--analyzers=architecture,compose` |
| `--severity-threshold` | Minimum severity to report | `--severity-threshold=high` |
| `--output` | Custom output path for report | `--output=reports/quality.md` |

### Baseline Options

| Option | Description | Example |
|--------|-------------|---------|
| `--baseline` | Generate new baseline | `--baseline` |
| `--no-baseline` | Skip baseline comparison | `--no-baseline` |
| `--baseline-file` | Use custom baseline file | `--baseline-file=baseline-v1.json` |

### Advanced Options

| Option | Description | Example |
|--------|-------------|---------|
| `--fail-on-critical` | Exit with error if critical issues found | `--fail-on-critical` |
| `--no-detekt` | Skip Detekt integration | `--no-detekt` |
| `--parallel` | Number of parallel analyzers | `--parallel=4` |
| `--verbose` | Enable verbose logging | `--verbose` |
| `--dry-run` | Show what would be analyzed without running | `--dry-run` |

### Examples

```powershell
# Analyze UI layer with high severity threshold
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui --severity-threshold=high

# Run architecture and compose analyzers only
.\gradlew.bat analyzeCodeQuality --analyzers=architecture,compose --output=reports/arch-compose.md

# Generate new baseline after major refactoring
.\gradlew.bat analyzeCodeQuality --baseline

# CI/CD mode: fail on critical issues
.\gradlew.bat analyzeCodeQuality --fail-on-critical --severity-threshold=high
```

## Configuration

### Configuration File

Create a configuration file at `.kiro/specs/code-quality-analysis/config.yml`:

```yaml
# Analysis Configuration

# Paths to include in analysis
includePaths:
  - app/src/main/java/com/shoppit/app

# Paths to exclude from analysis
excludePaths:
  - "**/build/**"
  - "**/.gradle/**"
  - "**/generated/**"

# Enabled analyzers (or "all")
enabledAnalyzers:
  - all

# Minimum priority to report
minPriority: LOW  # LOW, MEDIUM, HIGH, CRITICAL

# Detekt configuration
detektConfigPath: app/detekt-config.yml

# Fail build on critical issues
failOnCritical: true

# Generate baseline on first run
generateBaseline: true

# Output directory
outputPath: .kiro/specs/code-quality-analysis

# Performance settings
parallel: true
maxParallelAnalyzers: 4
cacheEnabled: true
```

### Analyzer-Specific Configuration

Configure individual analyzers:

```yaml
analyzers:
  code-smell:
    maxFunctionLength: 50
    maxClassLength: 300
    maxParameters: 5
    maxComplexity: 15
    maxNestingDepth: 4
    
  architecture:
    enforceLayerSeparation: true
    allowedAndroidImportsInDomain: []
    
  compose:
    requireModifierParameter: true
    enforceRememberUsage: true
    detectNestedLazyColumns: true
    
  performance:
    detectInefficinetIterations: true
    detectStringConcatenation: true
    
  naming:
    fileNamingPattern: "[A-Z][a-zA-Z0-9]*\\.kt"
    classNamingPattern: "[A-Z][a-zA-Z0-9]*"
    functionNamingPattern: "[a-z][a-zA-Z0-9]*"
```

### Environment Variables

Override configuration with environment variables:

```powershell
# Set minimum priority
$env:ANALYSIS_MIN_PRIORITY = "HIGH"

# Set output path
$env:ANALYSIS_OUTPUT_PATH = "reports"

# Enable verbose logging
$env:ANALYSIS_VERBOSE = "true"

# Run analysis
.\gradlew.bat analyzeCodeQuality
```

## Understanding Reports

### Report Structure

The generated report (`analysis-report.md`) contains:

1. **Executive Summary**: Key metrics and top issues
2. **Findings by Category**: Issues grouped by analysis category
3. **Detailed Findings**: Complete information for each issue
4. **Improvement Recommendations**: Prioritized action items
5. **Baseline Comparison**: Changes since last analysis
6. **Next Steps**: Recommended actions

### Reading Findings

Each finding includes:

```markdown
#### [Finding Title]

**File**: `path/to/file.kt:123`
**Priority**: HIGH
**Category**: Architecture
**Effort**: SMALL (5-30 minutes)

**Description**: 
Detailed explanation of the issue and why it matters.

**Current Code**:
```kotlin
// Problematic code
class MealViewModel {
    val uiState = MutableStateFlow<UiState>(Loading)  // ❌ Exposed mutable state
}
```

**Recommendation**:
Expose immutable StateFlow instead of MutableStateFlow to prevent external modifications.

**Improved Code**:
```kotlin
// Fixed code
class MealViewModel {
    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()  // ✅ Immutable exposure
}
```

**References**:
- [State Management Patterns](docs/compose-patterns.md#state-management)
- [ViewModel Best Practices](https://developer.android.com/topic/libraries/architecture/viewmodel)
```

### Priority Levels

| Priority | Description | Action Required |
|----------|-------------|-----------------|
| **CRITICAL** | Security issues, data loss risks | Fix immediately |
| **HIGH** | Architecture violations, major bugs | Fix before release |
| **MEDIUM** | Performance issues, code smells | Schedule for refactoring |
| **LOW** | Style issues, minor improvements | Fix when convenient |

### Effort Estimates

| Effort | Time Estimate | Description |
|--------|---------------|-------------|
| **TRIVIAL** | < 5 minutes | Simple fixes, auto-fixable |
| **SMALL** | 5-30 minutes | Straightforward refactoring |
| **MEDIUM** | 30 min - 2 hours | Moderate refactoring |
| **LARGE** | > 2 hours | Significant redesign |

### Metrics Interpretation

**Cyclomatic Complexity**:
- < 10: Simple, easy to test
- 10-15: Moderate complexity
- 15-20: High complexity, consider refactoring
- > 20: Very high complexity, refactor required

**Function Length**:
- < 30 lines: Good
- 30-50 lines: Acceptable
- 50-100 lines: Consider splitting
- > 100 lines: Refactor required

**Class Length**:
- < 200 lines: Good
- 200-300 lines: Acceptable
- 300-500 lines: Consider splitting
- > 500 lines: Refactor required

## Working with Baselines

### Creating a Baseline

Generate a baseline to track improvements:

```powershell
.\gradlew.bat analyzeCodeQuality --baseline
```

This creates `.kiro/specs/code-quality-analysis/baseline.json` with:
- Current metrics (complexity, function length, etc.)
- List of all finding IDs
- Timestamp

### Comparing with Baseline

Subsequent runs automatically compare with the baseline:

```powershell
.\gradlew.bat analyzeCodeQuality
```

The report shows:
- **Improvements**: Issues resolved since baseline
- **Regressions**: New issues introduced
- **Metric changes**: Percentage improvements/degradations

### Updating Baseline

After major refactoring, update the baseline:

```powershell
# Update baseline with current state
.\gradlew.bat analyzeCodeQuality --baseline

# Or use a specific name
.\gradlew.bat analyzeCodeQuality --baseline --baseline-file=baseline-v2.json
```

### Baseline History

Historical baselines are saved in `.kiro/specs/code-quality-analysis/history/`:

```
history/
├── baseline-2024-01-15.json
├── baseline-2024-02-01.json
└── baseline-2024-03-01.json
```

View trends over time:

```powershell
# Generate trend report
.\gradlew.bat analyzeCodeQuality --trend-report
```

## Incremental Analysis

### When to Use Incremental Analysis

Use incremental analysis when:
- Working on a specific feature
- Reviewing a pull request
- Debugging specific issues
- Quick feedback during development

### Feature-Based Analysis

Analyze a specific feature:

```powershell
# Analyze meal feature
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui/meal,app/src/main/java/com/shoppit/app/domain/meal,app/src/main/java/com/shoppit/app/data/meal
```

### Layer-Based Analysis

Analyze a specific layer:

```powershell
# Analyze UI layer
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui

# Analyze domain layer
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/domain

# Analyze data layer
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/data
```

### Changed Files Only

Analyze only files changed in current branch:

```powershell
# Get changed files
$changedFiles = git diff --name-only develop...HEAD | Where-Object { $_ -like "*.kt" }

# Analyze changed files
.\gradlew.bat analyzeCodeQuality --path=$($changedFiles -join ',')
```

## CI/CD Integration

See [CI/CD Integration Guide](CI_CD_INTEGRATION.md) for detailed instructions.

### Quick Setup

Add to `.github/workflows/code-quality.yml`:

```yaml
name: Code Quality Analysis

on:
  pull_request:
    branches: [ develop, main ]
  push:
    branches: [ develop, main ]

jobs:
  analyze:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'oracle'
      
      - name: Run Code Quality Analysis
        run: ./gradlew analyzeCodeQuality --fail-on-critical
      
      - name: Upload Report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: code-quality-report
          path: .kiro/specs/code-quality-analysis/analysis-report.md
```

## Troubleshooting

### Common Issues

**Issue**: Analysis fails with "Out of memory"

**Solution**:
```powershell
# Increase Gradle heap size
$env:GRADLE_OPTS = "-Xmx4g"
.\gradlew.bat analyzeCodeQuality
```

**Issue**: Analysis is very slow

**Solution**:
```powershell
# Enable parallel analysis
.\gradlew.bat analyzeCodeQuality --parallel=4

# Or use incremental analysis
.\gradlew.bat analyzeCodeQuality --path=app/src/main/java/com/shoppit/app/ui
```

**Issue**: Too many false positives

**Solution**:
```powershell
# Increase severity threshold
.\gradlew.bat analyzeCodeQuality --severity-threshold=high

# Or disable specific analyzers
.\gradlew.bat analyzeCodeQuality --analyzers=architecture,security
```

**Issue**: Report not generated

**Solution**:
```powershell
# Check output directory exists
New-Item -ItemType Directory -Force -Path .kiro/specs/code-quality-analysis

# Run with verbose logging
.\gradlew.bat analyzeCodeQuality --verbose
```

### Getting Help

1. Check the [Analyzer Documentation](ANALYZER_REFERENCE.md)
2. Review [Example Reports](examples/)
3. See [CI/CD Integration Guide](CI_CD_INTEGRATION.md)
4. Check project documentation in `docs/`

### Reporting Issues

When reporting issues, include:
- Gradle version: `.\gradlew.bat --version`
- Command used: `.\gradlew.bat analyzeCodeQuality --verbose`
- Error message or unexpected behavior
- Sample code that triggers the issue (if applicable)

## Best Practices

### Regular Analysis

Run analysis regularly:
- **Daily**: During active development
- **Before commits**: On changed files
- **Before PRs**: On feature branches
- **Weekly**: Complete codebase analysis

### Addressing Findings

Prioritize fixes:
1. **Critical**: Fix immediately
2. **High**: Fix before release
3. **Medium**: Schedule for next sprint
4. **Low**: Fix during refactoring

### Baseline Management

- Create baseline after major releases
- Update baseline after significant refactoring
- Keep historical baselines for trend analysis
- Document baseline changes in CHANGELOG

### Team Workflow

1. Run analysis locally before committing
2. Address critical and high priority issues
3. Document accepted risks for medium/low issues
4. Review analysis reports in code reviews
5. Track metrics over time

## Next Steps

- Read the [Analyzer Reference](ANALYZER_REFERENCE.md) to understand what each analyzer checks
- Review [Example Reports](examples/) to see sample findings
- Set up [CI/CD Integration](CI_CD_INTEGRATION.md) for automated analysis
- Explore [Advanced Configuration](ADVANCED_CONFIGURATION.md) options
