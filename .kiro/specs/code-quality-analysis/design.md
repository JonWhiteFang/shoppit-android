# Design Document

## Overview

The Code Quality Analysis System is a comprehensive static analysis tool designed specifically for the Shoppit Android application. It combines automated code analysis, pattern detection, and best practice validation to provide actionable recommendations for improving code quality, maintainability, and performance.

The system is built around a modular architecture that separates analysis concerns into specialized analyzers, each focusing on specific aspects of code quality. Results are aggregated, prioritized, and presented in a structured report format that enables developers to systematically address issues.

### Key Design Principles

1. **Modularity**: Each analyzer is independent and can be run separately or as part of a complete analysis
2. **Extensibility**: New analyzers can be added without modifying existing code
3. **Integration**: Leverages existing tools (Detekt, Snyk) rather than reimplementing functionality
4. **Actionability**: Every finding includes specific, actionable recommendations with code examples
5. **Prioritization**: Issues are classified by impact to guide remediation efforts
6. **Traceability**: All findings include file path, line number, and context for easy location

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Analysis Orchestrator                     │
│  - Coordinates analyzer execution                            │
│  - Manages analysis lifecycle                                │
│  - Aggregates results                                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      File Scanner                            │
│  - Discovers Kotlin files                                    │
│  - Filters excluded paths                                    │
│  - Provides file metadata                                    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Analyzer Pipeline                         │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Code Smell       │  │ Architecture     │                │
│  │ Analyzer         │  │ Analyzer         │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Compose          │  │ State Management │                │
│  │ Analyzer         │  │ Analyzer         │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Error Handling   │  │ DI Analyzer      │                │
│  │ Analyzer         │  │                  │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Database         │  │ Performance      │                │
│  │ Analyzer         │  │ Analyzer         │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Naming           │  │ Test Coverage    │                │
│  │ Analyzer         │  │ Analyzer         │                │
│  └──────────────────┘  └──────────────────┘                │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Documentation    │  │ Security         │                │
│  │ Analyzer         │  │ Analyzer         │                │
│  └──────────────────┘  └──────────────────┘                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Result Aggregator                         │
│  - Collects findings from all analyzers                      │
│  - Deduplicates issues                                       │
│  - Assigns priorities                                        │
│  - Calculates metrics                                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Report Generator                          │
│  - Formats findings as markdown                              │
│  - Groups by category and priority                           │
│  - Includes code examples                                    │
│  - Generates executive summary                               │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Baseline Manager                          │
│  - Stores historical metrics                                 │
│  - Compares current vs baseline                              │
│  - Tracks improvements/regressions                           │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
User Request
    │
    ▼
Analysis Orchestrator
    │
    ├─→ File Scanner ──→ List of Kotlin files
    │
    ├─→ Analyzer Pipeline
    │   │
    │   ├─→ Parse file with Kotlin PSI
    │   │
    │   ├─→ Run each analyzer
    │   │   │
    │   │   ├─→ Code Smell Analyzer
    │   │   ├─→ Architecture Analyzer
    │   │   ├─→ Compose Analyzer
    │   │   └─→ ... (other analyzers)
    │   │
    │   └─→ Collect findings
    │
    ├─→ Result Aggregator
    │   │
    │   ├─→ Deduplicate
    │   ├─→ Prioritize
    │   └─→ Calculate metrics
    │
    ├─→ Report Generator
    │   │
    │   └─→ Generate markdown report
    │
    └─→ Baseline Manager
        │
        ├─→ Compare with baseline
        └─→ Update history
```

## Components and Interfaces

### 1. Analysis Orchestrator

**Responsibility**: Coordinates the entire analysis process from file discovery to report generation.

**Interface**:
```kotlin
interface AnalysisOrchestrator {
    /**
     * Runs complete analysis on the entire codebase
     */
    suspend fun analyzeAll(): AnalysisResult
    
    /**
     * Runs analysis on specific files or directories
     */
    suspend fun analyzeIncremental(paths: List<String>): AnalysisResult
    
    /**
     * Runs specific analyzers only
     */
    suspend fun analyzeWithFilters(
        paths: List<String>? = null,
        analyzers: List<AnalyzerType>
    ): AnalysisResult
}

data class AnalysisResult(
    val findings: List<Finding>,
    val metrics: AnalysisMetrics,
    val executionTime: Duration,
    val filesAnalyzed: Int
)
```

**Implementation Details**:
- Manages analyzer lifecycle (initialization, execution, cleanup)
- Handles errors gracefully (continues analysis if one analyzer fails)
- Provides progress reporting for long-running analyses
- Supports cancellation for interactive use

### 2. File Scanner

**Responsibility**: Discovers and filters Kotlin source files for analysis.

**Interface**:
```kotlin
interface FileScanner {
    /**
     * Scans directory recursively for Kotlin files
     */
    fun scanDirectory(path: String): List<FileInfo>
    
    /**
     * Filters files based on exclusion patterns
     */
    fun filterFiles(files: List<FileInfo>): List<FileInfo>
    
    /**
     * Checks if file should be analyzed
     */
    fun shouldAnalyze(file: FileInfo): Boolean
}

data class FileInfo(
    val path: String,
    val relativePath: String,
    val size: Long,
    val lastModified: Long,
    val layer: CodeLayer? // Data, Domain, UI, or null
)

enum class CodeLayer {
    DATA, DOMAIN, UI, DI, TEST
}
```

**Implementation Details**:
- Respects `.gitignore` patterns
- Excludes build directories (`build/`, `.gradle/`)
- Excludes generated code directories
- Detects code layer based on package structure
- Caches file metadata for performance

### 3. Analyzer Interface

**Responsibility**: Defines common interface for all code analyzers.

**Interface**:
```kotlin
interface CodeAnalyzer {
    /**
     * Unique identifier for this analyzer
     */
    val id: String
    
    /**
     * Human-readable name
     */
    val name: String
    
    /**
     * Category of analysis
     */
    val category: AnalysisCategory
    
    /**
     * Analyzes a single file
     */
    suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding>
    
    /**
     * Checks if this analyzer applies to the given file
     */
    fun appliesTo(file: FileInfo): Boolean
}

enum class AnalysisCategory {
    CODE_SMELL,
    ARCHITECTURE,
    COMPOSE,
    STATE_MANAGEMENT,
    ERROR_HANDLING,
    DEPENDENCY_INJECTION,
    DATABASE,
    PERFORMANCE,
    NAMING,
    TEST_COVERAGE,
    DOCUMENTATION,
    SECURITY
}
```

### 4. Finding Model

**Responsibility**: Represents a single code quality issue or recommendation.

**Data Model**:
```kotlin
data class Finding(
    val id: String, // Unique identifier
    val analyzer: String, // Analyzer that found this issue
    val category: AnalysisCategory,
    val priority: Priority,
    val title: String,
    val description: String,
    val file: String,
    val lineNumber: Int,
    val columnNumber: Int? = null,
    val codeSnippet: String,
    val recommendation: String,
    val beforeExample: String? = null,
    val afterExample: String? = null,
    val autoFixable: Boolean = false,
    val autoFix: String? = null,
    val effort: Effort,
    val references: List<String> = emptyList(), // Links to docs
    val relatedFindings: List<String> = emptyList() // IDs of related findings
)

enum class Priority {
    CRITICAL, // Security issues, data loss risks
    HIGH,     // Architecture violations, major bugs
    MEDIUM,   // Performance issues, code smells
    LOW       // Style issues, minor improvements
}

enum class Effort {
    TRIVIAL,  // < 5 minutes
    SMALL,    // 5-30 minutes
    MEDIUM,   // 30 minutes - 2 hours
    LARGE     // > 2 hours
}
```

### 5. Specific Analyzers

#### Code Smell Analyzer

**Detects**:
- Long functions (> 50 lines)
- Large classes (> 300 lines)
- High cyclomatic complexity (> 15)
- Deep nesting (> 4 levels)
- Too many parameters (> 5)
- Duplicate code blocks
- Magic numbers
- Long parameter lists

**Implementation**:
```kotlin
class CodeSmellAnalyzer : CodeAnalyzer {
    override val id = "code-smell"
    override val category = AnalysisCategory.CODE_SMELL
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                // Check function length
                val lineCount = function.text.lines().size
                if (lineCount > 50) {
                    findings.add(createLongFunctionFinding(function, lineCount))
                }
                
                // Check parameter count
                if (function.valueParameters.size > 5) {
                    findings.add(createTooManyParametersFinding(function))
                }
                
                // Check cyclomatic complexity
                val complexity = calculateComplexity(function)
                if (complexity > 15) {
                    findings.add(createHighComplexityFinding(function, complexity))
                }
            }
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                
                val lineCount = klass.text.lines().size
                if (lineCount > 300) {
                    findings.add(createLargeClassFinding(klass, lineCount))
                }
            }
        })
        
        return findings
    }
}
```

#### Architecture Analyzer

**Validates**:
- Domain layer has no Android imports
- Repository implementations in data layer
- ViewModels expose StateFlow, not MutableStateFlow
- Use cases have single operator function
- Dependency flow (UI → Domain ← Data)
- Proper layer separation

**Implementation**:
```kotlin
class ArchitectureAnalyzer : CodeAnalyzer {
    override val id = "architecture"
    override val category = AnalysisCategory.ARCHITECTURE
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        when (file.layer) {
            CodeLayer.DOMAIN -> {
                // Check for Android imports
                ast.importDirectives.forEach { import ->
                    if (import.importPath?.pathStr?.startsWith("android.") == true) {
                        findings.add(createAndroidImportInDomainFinding(import))
                    }
                }
            }
            
            CodeLayer.UI -> {
                // Check ViewModels
                ast.classes.filter { it.isViewModel() }.forEach { viewModel ->
                    // Check StateFlow exposure
                    viewModel.declarations.forEach { declaration ->
                        if (declaration is KtProperty && declaration.isPublic()) {
                            val type = declaration.typeReference?.text
                            if (type?.contains("MutableStateFlow") == true) {
                                findings.add(createMutableStateFlowExposedFinding(declaration))
                            }
                        }
                    }
                }
            }
            
            else -> {}
        }
        
        return findings
    }
}
```

#### Compose Analyzer

**Validates**:
- Composable functions have Modifier parameter
- State mutations use `update { }`
- Expensive computations wrapped in `remember`
- LazyColumn items have stable keys
- No nested LazyColumns
- Proper state hoisting

**Implementation**:
```kotlin
class ComposeAnalyzer : CodeAnalyzer {
    override val id = "compose"
    override val category = AnalysisCategory.COMPOSE
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                if (function.hasAnnotation("Composable")) {
                    // Check for Modifier parameter
                    val hasModifier = function.valueParameters.any { 
                        it.name == "modifier" && it.typeReference?.text == "Modifier"
                    }
                    
                    if (!hasModifier && !function.isPrivate()) {
                        findings.add(createMissingModifierParameterFinding(function))
                    }
                    
                    // Check for remember usage
                    checkRememberUsage(function, findings)
                    
                    // Check for nested LazyColumns
                    checkNestedLazyColumns(function, findings)
                }
            }
        })
        
        return findings
    }
}
```

#### State Management Analyzer

**Validates**:
- Private mutable state not exposed
- State updates use `_state.update { }`
- Sealed classes for mutually exclusive states
- `flowOn(Dispatchers.IO)` for database operations
- ViewModels use `viewModelScope`

#### Error Handling Analyzer

**Validates**:
- Repository functions catch and map exceptions
- Result type used for failable operations
- No exceptions reach UI layer
- No empty catch blocks
- Proper error types (AppError)

#### Dependency Injection Analyzer

**Validates**:
- ViewModels annotated with `@HiltViewModel`
- Constructor injection with `@Inject constructor()`
- Modules use `@Module` and `@InstallIn`
- `@Binds` used for interface binding

#### Database Analyzer

**Validates**:
- DAO query functions return Flow
- DAO mutations are suspend functions
- `flowOn(Dispatchers.IO)` applied
- Foreign keys with CASCADE
- Parameterized queries

#### Performance Analyzer

**Detects**:
- Inefficient list iterations
- String concatenation in loops
- Unnecessary object allocations
- Unstable Compose parameters
- Missing `@Immutable` or `@Stable` annotations

#### Naming Analyzer

**Validates**:
- File naming conventions
- Class names (PascalCase)
- Function names (camelCase)
- Constants (UPPER_SNAKE_CASE)
- Private mutable state (underscore prefix)

#### Test Coverage Analyzer

**Validates**:
- ViewModels have test files
- Use cases have test files
- Repositories have test files
- Test naming convention

#### Documentation Analyzer

**Validates**:
- Public API has KDoc comments
- Complex algorithms have inline comments
- Data class properties documented
- Sealed class subclasses documented

#### Security Analyzer

**Detects**:
- Hardcoded secrets
- Logging sensitive information
- SQL injection risks
- Insecure data storage

### 6. Result Aggregator

**Responsibility**: Collects, deduplicates, and prioritizes findings.

**Interface**:
```kotlin
interface ResultAggregator {
    /**
     * Aggregates findings from multiple analyzers
     */
    fun aggregate(findings: List<Finding>): AggregatedResult
    
    /**
     * Deduplicates similar findings
     */
    fun deduplicate(findings: List<Finding>): List<Finding>
    
    /**
     * Calculates overall metrics
     */
    fun calculateMetrics(findings: List<Finding>): AnalysisMetrics
}

data class AggregatedResult(
    val findings: List<Finding>,
    val metrics: AnalysisMetrics,
    val byCategory: Map<AnalysisCategory, List<Finding>>,
    val byPriority: Map<Priority, List<Finding>>,
    val byFile: Map<String, List<Finding>>
)

data class AnalysisMetrics(
    val totalFiles: Int,
    val totalFindings: Int,
    val findingsByPriority: Map<Priority, Int>,
    val findingsByCategory: Map<AnalysisCategory, Int>,
    val averageComplexity: Double,
    val averageFunctionLength: Double,
    val averageClassLength: Double,
    val testCoveragePercentage: Double,
    val documentationCoveragePercentage: Double
)
```

### 7. Report Generator

**Responsibility**: Formats analysis results as markdown report.

**Interface**:
```kotlin
interface ReportGenerator {
    /**
     * Generates complete analysis report
     */
    fun generate(result: AggregatedResult, baseline: Baseline?): String
    
    /**
     * Generates executive summary
     */
    fun generateSummary(metrics: AnalysisMetrics, baseline: Baseline?): String
    
    /**
     * Generates findings section
     */
    fun generateFindings(findings: List<Finding>): String
}
```

**Report Structure**:
```markdown
# Code Quality Analysis Report

Generated: [timestamp]
Files Analyzed: [count]
Total Findings: [count]

## Executive Summary

### Key Metrics
- Critical Issues: [count] (↑/↓ from baseline)
- High Priority: [count] (↑/↓ from baseline)
- Medium Priority: [count] (↑/↓ from baseline)
- Low Priority: [count] (↑/↓ from baseline)

### Metrics
- Average Cyclomatic Complexity: [value]
- Average Function Length: [value] lines
- Average Class Length: [value] lines
- Test Coverage: [percentage]%
- Documentation Coverage: [percentage]%

### Top Issues
1. [Most critical issue]
2. [Second most critical]
3. [Third most critical]

## Findings by Category

### Critical Priority

#### Security
[Findings...]

### High Priority

#### Architecture
[Findings...]

#### Error Handling
[Findings...]

### Medium Priority

#### Performance
[Findings...]

#### Code Smells
[Findings...]

### Low Priority

#### Naming
[Findings...]

#### Documentation
[Findings...]

## Detailed Findings

### [Category] - [Priority]

#### [Finding Title]

**File**: `path/to/file.kt:123`

**Description**: [Detailed description]

**Current Code**:
```kotlin
[Code snippet]
```

**Recommendation**: [Specific recommendation]

**Improved Code**:
```kotlin
[After example]
```

**Effort**: [TRIVIAL/SMALL/MEDIUM/LARGE]

**References**:
- [Link to documentation]
- [Link to best practices]

---

## Improvement Recommendations

### Quick Wins (< 1 hour total)
1. [Auto-fixable issues]
2. [Trivial effort items]

### Short Term (1-4 hours)
1. [Small effort items]

### Medium Term (1-2 days)
1. [Medium effort items]

### Long Term (> 2 days)
1. [Large effort items]

## Baseline Comparison

### Improvements
- [Category]: [count] issues resolved
- [Metric]: Improved by [percentage]%

### Regressions
- [Category]: [count] new issues
- [Metric]: Degraded by [percentage]%

## Next Steps

1. Address all Critical priority issues immediately
2. Create tasks for High priority issues
3. Schedule refactoring for Medium priority issues
4. Consider Low priority issues for future improvements
```

### 8. Baseline Manager

**Responsibility**: Manages historical analysis data and tracks improvements.

**Interface**:
```kotlin
interface BaselineManager {
    /**
     * Loads existing baseline
     */
    fun loadBaseline(): Baseline?
    
    /**
     * Saves current analysis as baseline
     */
    fun saveBaseline(metrics: AnalysisMetrics, findings: List<Finding>)
    
    /**
     * Compares current analysis with baseline
     */
    fun compare(current: AnalysisMetrics, baseline: Baseline): Comparison
    
    /**
     * Saves analysis to history
     */
    fun saveToHistory(result: AggregatedResult)
}

data class Baseline(
    val timestamp: Long,
    val metrics: AnalysisMetrics,
    val findingIds: Set<String> // IDs of all findings
)

data class Comparison(
    val improved: Map<String, Double>, // Metric name -> improvement %
    val regressed: Map<String, Double>, // Metric name -> regression %
    val resolved: List<String>, // Finding IDs resolved
    val newIssues: List<String> // New finding IDs
)
```

## Data Models

### Configuration

```kotlin
data class AnalysisConfig(
    val includePaths: List<String> = listOf("app/src/main/java"),
    val excludePaths: List<String> = listOf("**/build/**", "**/.gradle/**"),
    val enabledAnalyzers: List<String> = listOf("all"),
    val minPriority: Priority = Priority.LOW,
    val detektConfigPath: String = "detekt-config.yml",
    val failOnCritical: Boolean = true,
    val generateBaseline: Boolean = true,
    val outputPath: String = ".kiro/specs/code-quality-analysis"
)
```

### Detekt Integration

```kotlin
interface DetektIntegration {
    /**
     * Runs Detekt analysis
     */
    suspend fun runDetekt(config: String): List<Finding>
    
    /**
     * Converts Detekt findings to our Finding model
     */
    fun convertFindings(detektFindings: List<DetektFinding>): List<Finding>
}
```

## Error Handling

### Error Types

```kotlin
sealed class AnalysisError : Exception() {
    data class FileNotFound(val path: String) : AnalysisError()
    data class ParseError(val file: String, val cause: Throwable) : AnalysisError()
    data class AnalyzerError(val analyzer: String, val cause: Throwable) : AnalysisError()
    data class ConfigurationError(val message: String) : AnalysisError()
}
```

### Error Handling Strategy

1. **File-level errors**: Log error, skip file, continue analysis
2. **Analyzer errors**: Log error, skip analyzer for that file, continue
3. **Configuration errors**: Fail fast with clear error message
4. **Parse errors**: Log error, skip file, continue analysis

All errors are collected and reported in the final report.

## Testing Strategy

### Unit Tests

1. **Analyzer Tests**: Test each analyzer with sample code snippets
2. **Aggregator Tests**: Test deduplication and prioritization logic
3. **Report Generator Tests**: Test markdown generation
4. **Baseline Manager Tests**: Test comparison logic

### Integration Tests

1. **End-to-end analysis**: Run on sample project
2. **Detekt integration**: Verify Detekt findings are incorporated
3. **Report generation**: Verify complete report structure

### Test Data

Create sample Kotlin files demonstrating:
- Each type of code smell
- Architecture violations
- Compose anti-patterns
- State management issues
- Error handling problems

## Performance Considerations

### Optimization Strategies

1. **Parallel Analysis**: Analyze files in parallel using coroutines
2. **Caching**: Cache parsed ASTs for files that haven't changed
3. **Incremental Analysis**: Only analyze changed files
4. **Lazy Loading**: Load analyzers on demand
5. **Streaming Results**: Stream findings as they're discovered

### Performance Targets

- Analyze 100 files in < 30 seconds
- Memory usage < 500MB for typical project
- Support projects with 1000+ files

## Integration Points

### Detekt Integration

```kotlin
// detekt-config.yml
build:
  maxIssues: 0
  excludeCorrectable: false

processors:
  active: true

console-reports:
  active: true

output-reports:
  active: true
  exclude:
    - 'TxtOutputReport'
    - 'HtmlOutputReport'

complexity:
  active: true
  LongMethod:
    active: true
    threshold: 50
  ComplexMethod:
    active: true
    threshold: 15
  TooManyFunctions:
    active: true
    thresholdInFiles: 15
    thresholdInClasses: 15

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers: [-1, 0, 1, 2]
  WildcardImport:
    active: true

compose:
  active: true
  ComposableNaming:
    active: true
  ModifierMissing:
    active: true
```

### Snyk Integration

After analysis completes, recommend running:
```powershell
pwd
snyk_code_scan(path = "absolute-path")
snyk_sca_scan(path = "absolute-path", all_projects = true)
```

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Code Quality Analysis
  run: |
    ./gradlew.bat analyzeCodeQuality
    
- name: Upload Analysis Report
  uses: actions/upload-artifact@v3
  with:
    name: code-quality-report
    path: .kiro/specs/code-quality-analysis/analysis-report.md
    
- name: Fail on Critical Issues
  run: |
    if grep -q "Critical Issues: [1-9]" .kiro/specs/code-quality-analysis/analysis-report.md; then
      exit 1
    fi
```

## Extensibility

### Adding New Analyzers

1. Implement `CodeAnalyzer` interface
2. Register analyzer in `AnalyzerRegistry`
3. Add tests for new analyzer
4. Update documentation

Example:
```kotlin
class CustomAnalyzer : CodeAnalyzer {
    override val id = "custom"
    override val name = "Custom Analyzer"
    override val category = AnalysisCategory.CODE_SMELL
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        // Custom analysis logic
        return emptyList()
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        return true // or custom logic
    }
}

// Register
AnalyzerRegistry.register(CustomAnalyzer())
```

### Custom Rules

Support loading custom rules from configuration:

```yaml
# custom-rules.yml
rules:
  - id: custom-rule-1
    name: "Custom Rule"
    pattern: "regex pattern"
    message: "Custom message"
    priority: HIGH
    category: CODE_SMELL
```

## Deliverables

1. **Analysis Tool**: Kotlin implementation of all analyzers
2. **Report Generator**: Markdown report generation
3. **Baseline Manager**: Historical tracking
4. **Detekt Configuration**: Project-specific Detekt config
5. **Documentation**: Usage guide and analyzer documentation
6. **Tests**: Comprehensive test suite
7. **CI/CD Integration**: GitHub Actions workflow

## Success Criteria

1. Analyzes entire Shoppit codebase in < 2 minutes
2. Generates actionable report with specific recommendations
3. Detects all major architecture violations
4. Identifies performance optimization opportunities
5. Tracks improvements over time
6. Integrates seamlessly with existing tools
7. Provides clear, prioritized action items
