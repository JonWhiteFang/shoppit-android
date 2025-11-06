# Code Quality Analysis System - Usage Guide

## Overview

The Code Quality Analysis System is a comprehensive static analysis tool for the Shoppit Android project. It analyzes Kotlin code to identify issues across multiple categories including architecture violations, code smells, performance problems, security vulnerabilities, and more.

## Quick Start

### Running Complete Analysis

To analyze the entire codebase:

```powershell
.\gradlew.bat analyzeCodeQuality
```

This will:
1. Scan all Kotlin files in `app/src/main/java`
2. Run all registered analyzers
3. Run Detekt static analysis
4. Generate a comprehensive report at `.kiro/specs/code-quality-analysis/analysis-report.md`
5. Create/update the baseline for tracking improvements

### Running Incremental Analysis

To analyze specific files or directories:

```powershell
# Analyze a specific directory
.\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/main/java/com/shoppit/app/ui

# Analyze multiple paths (comma-separated)
.\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/main/java/com/shoppit/app/ui,app/src/main/java/com/shoppit/app/data
```

### Running Specific Analyzers

To run only specific analyzers:

```powershell
# Run only security and architecture analyzers
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture

# Run only Detekt
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=detekt
```

Available analyzers:
- `architecture` - Architecture and layer violations
- `compose` - Jetpack Compose best practices
- `state-management` - State management patterns
- `error-handling` - Error handling and exception management
- `dependency-injection` - Hilt dependency injection
- `database` - Room database patterns
- `performance` - Performance optimizations
- `naming` - Naming conventions
- `test-coverage` - Test coverage validation
- `documentation` - Documentation completeness
- `security` - Security vulnerabilities
- `detekt` - Detekt static analysis

### Generating Baseline

To create or update the baseline:

```powershell
.\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true
```

The baseline is used to track improvements over time and identify new issues vs. existing ones.

### Custom Output Path

To save the report to a custom location:

```powershell
.\gradlew.bat analyzeCodeQuality -Panalysis.output=custom/output/path
```

### Combining Options

You can combine multiple options:

```powershell
# Analyze UI layer with security and architecture analyzers, generate baseline
.\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/main/java/com/shoppit/app/ui -Panalysis.analyzers=security,architecture -Panalysis.baseline=true
```

## Command-Line Options

### `-Panalysis.path`

Specifies the path(s) to analyze.

- **Type**: String (comma-separated for multiple paths)
- **Default**: `app/src/main/java`
- **Examples**:
  - Single path: `-Panalysis.path=app/src/main/java/com/shoppit/app/ui`
  - Multiple paths: `-Panalysis.path=app/src/main/java/com/shoppit/app/ui,app/src/main/java/com/shoppit/app/data`

### `-Panalysis.analyzers`

Specifies which analyzers to run.

- **Type**: String (comma-separated)
- **Default**: All analyzers
- **Valid values**: `architecture`, `compose`, `state-management`, `error-handling`, `dependency-injection`, `database`, `performance`, `naming`, `test-coverage`, `documentation`, `security`, `detekt`
- **Examples**:
  - Single analyzer: `-Panalysis.analyzers=security`
  - Multiple analyzers: `-Panalysis.analyzers=security,architecture,performance`

### `-Panalysis.baseline`

Generates or updates the baseline.

- **Type**: Boolean
- **Default**: `false`
- **Example**: `-Panalysis.baseline=true`

### `-Panalysis.output`

Specifies the output directory for the report.

- **Type**: String
- **Default**: `.kiro/specs/code-quality-analysis`
- **Example**: `-Panalysis.output=custom/output/path`

## Understanding the Report

The analysis report is generated in Markdown format and includes:

### Executive Summary

- **Total Findings**: Number of issues found
- **By Priority**: Breakdown by CRITICAL, HIGH, MEDIUM, LOW
- **By Category**: Breakdown by analysis category
- **Key Metrics**: Average complexity, function length, class size, etc.
- **Trend Data**: Comparison with baseline (if available)

### Findings by Priority

Issues are grouped by priority level:

- **CRITICAL**: Security vulnerabilities, critical bugs (fix immediately)
- **HIGH**: Architecture violations, major code smells (fix before release)
- **MEDIUM**: Performance issues, moderate code smells (fix when convenient)
- **LOW**: Style issues, minor improvements (nice to have)

Each finding includes:
- **File and Location**: Where the issue was found
- **Category**: Type of issue
- **Effort**: Estimated effort to fix (TRIVIAL, SMALL, MEDIUM, LARGE)
- **Description**: What the issue is
- **Code Snippet**: The problematic code
- **Recommendation**: How to fix it
- **Before/After Examples**: (when applicable)

### Improvement Recommendations

Actionable recommendations grouped by effort level:

- **Quick Wins (Trivial/Small)**: Easy fixes with high impact
- **Medium Effort**: Moderate refactoring required
- **Large Effort**: Significant refactoring or redesign

## Interpreting Findings

### Priority Levels

#### CRITICAL
- **What**: Security vulnerabilities, data loss risks, critical bugs
- **Action**: Fix immediately before any release
- **Examples**: Hardcoded secrets, SQL injection, exposed sensitive data

#### HIGH
- **What**: Architecture violations, major code smells, potential bugs
- **Action**: Fix before next release
- **Examples**: Domain layer with Android imports, exposed mutable state, missing error handling

#### MEDIUM
- **What**: Performance issues, moderate code smells, maintainability concerns
- **Action**: Fix when convenient, prioritize for next sprint
- **Examples**: Inefficient list operations, missing remember in Compose, high complexity

#### LOW
- **What**: Style issues, minor improvements, documentation gaps
- **Action**: Fix during regular refactoring, nice to have
- **Examples**: Missing KDoc, naming convention violations, magic numbers

### Effort Levels

#### TRIVIAL
- **Time**: < 5 minutes
- **Examples**: Remove unused imports, add constants, fix formatting

#### SMALL
- **Time**: 5-30 minutes
- **Examples**: Add KDoc comments, rename variables, extract constants

#### MEDIUM
- **Time**: 30 minutes - 2 hours
- **Examples**: Refactor function, add error handling, optimize performance

#### LARGE
- **Time**: > 2 hours
- **Examples**: Split large class, redesign architecture, major refactoring

## Best Practices

### Regular Analysis

Run analysis regularly to catch issues early:

```powershell
# Before committing
.\gradlew.bat analyzeCodeQuality -Panalysis.path=path/to/changed/files

# Before creating PR
.\gradlew.bat analyzeCodeQuality

# Weekly full analysis
.\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true
```

### Incremental Adoption

Start with high-priority issues:

```powershell
# Focus on security first
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security

# Then architecture
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=architecture

# Gradually add more analyzers
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture,error-handling
```

### Baseline Management

Use baselines to track progress:

```powershell
# Create initial baseline
.\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true

# After fixing issues, update baseline
.\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true

# Compare current state with baseline
.\gradlew.bat analyzeCodeQuality
```

### CI/CD Integration

Integrate into your CI/CD pipeline:

```yaml
# GitHub Actions example
- name: Run Code Quality Analysis
  run: .\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture

- name: Check for Critical Issues
  run: |
    if (Select-String -Path .kiro/specs/code-quality-analysis/analysis-report.md -Pattern "CRITICAL") {
      Write-Error "Critical issues found!"
      exit 1
    }
```

## Troubleshooting

### Analysis Fails to Start

**Problem**: Task fails with "Analysis path does not exist"

**Solution**: Verify the path exists and use absolute or relative path from project root:
```powershell
# Check current directory
pwd

# Use correct path
.\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/main/java
```

### Invalid Analyzer Name

**Problem**: Task fails with "Invalid analyzer(s)"

**Solution**: Check analyzer name spelling and use valid analyzer IDs:
```powershell
# Valid
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture

# Invalid (typo)
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=securtiy,architecure
```

### Detekt Configuration Not Found

**Problem**: Detekt warnings about missing configuration

**Solution**: Ensure `app/detekt-config.yml` exists. If not, create it or disable Detekt:
```powershell
# Check if config exists
Test-Path app/detekt-config.yml

# If missing, copy from template or disable Detekt
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=architecture,compose,security
```

### Out of Memory

**Problem**: Analysis fails with OutOfMemoryError

**Solution**: Increase Gradle heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### Slow Analysis

**Problem**: Analysis takes too long

**Solution**: Use incremental analysis or filter analyzers:
```powershell
# Analyze only changed files
.\gradlew.bat analyzeCodeQuality -Panalysis.path=path/to/changed/files

# Run fewer analyzers
.\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture
```

## Advanced Usage

### Custom Analyzer Configuration

To customize analyzer behavior, modify the analyzer classes in:
```
app/src/main/java/com/shoppit/app/analysis/analyzers/
```

### Custom Report Format

To customize the report format, modify:
```
app/src/main/java/com/shoppit/app/analysis/reporting/ReportGeneratorImpl.kt
```

### Programmatic Usage

To run analysis programmatically from code:

```kotlin
import com.shoppit.app.analysis.core.AnalysisOrchestratorImpl
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val orchestrator = AnalysisOrchestratorImpl(
        fileScanner = FileScannerImpl(),
        resultAggregator = ResultAggregatorImpl(),
        reportGenerator = ReportGeneratorImpl(),
        baselineManager = BaselineManagerImpl(),
        detektIntegration = DetektIntegration()
    )
    
    // Run complete analysis
    val result = orchestrator.analyzeAll()
    
    println("Found ${result.findings.size} issues in ${result.filesAnalyzed} files")
    println("Execution time: ${result.executionTime}")
}
```

## FAQ

### Q: How often should I run analysis?

**A**: Run incremental analysis before each commit, full analysis before PRs, and baseline updates weekly.

### Q: Should I fix all issues immediately?

**A**: No. Prioritize CRITICAL and HIGH issues. Address MEDIUM and LOW issues gradually during regular refactoring.

### Q: Can I ignore specific findings?

**A**: Yes. Document accepted risks in the baseline or add suppressions in code with justification.

### Q: How do I add a custom analyzer?

**A**: Create a new class implementing `CodeAnalyzer` interface and register it in `AnalysisOrchestratorImpl`.

### Q: Can I run analysis on test code?

**A**: Yes. Specify the test directory path:
```powershell
.\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/test/java
```

### Q: How do I export findings to other formats?

**A**: Currently only Markdown is supported. Extend `ReportGenerator` to add JSON, HTML, or other formats.

## Support

For issues or questions:
1. Check this guide and troubleshooting section
2. Review the design document: `.kiro/specs/code-quality-analysis/design.md`
3. Check analyzer documentation: `.kiro/specs/code-quality-analysis/ANALYZER_REFERENCE.md`
4. Contact the development team

## Next Steps

1. Run your first analysis: `.\gradlew.bat analyzeCodeQuality`
2. Review the generated report
3. Fix CRITICAL and HIGH priority issues
4. Create a baseline: `.\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true`
5. Integrate into your workflow
6. Track improvements over time
