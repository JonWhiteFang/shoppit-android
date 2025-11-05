package com.shoppit.app.analysis.models

/**
 * Categories of code quality analysis.
 * Each category represents a specific aspect of code quality.
 */
enum class AnalysisCategory {
    /**
     * Code smells and anti-patterns.
     * Examples: Long methods, large classes, high complexity.
     */
    CODE_SMELL,
    
    /**
     * Architecture pattern violations.
     * Examples: Layer boundary violations, dependency flow issues.
     */
    ARCHITECTURE,
    
    /**
     * Jetpack Compose best practices.
     * Examples: Missing Modifier parameters, improper state management.
     */
    COMPOSE,
    
    /**
     * State management patterns.
     * Examples: Exposed mutable state, improper state updates.
     */
    STATE_MANAGEMENT,
    
    /**
     * Error handling patterns.
     * Examples: Missing error handling, empty catch blocks.
     */
    ERROR_HANDLING,
    
    /**
     * Dependency injection patterns.
     * Examples: Missing Hilt annotations, manual instantiation.
     */
    DEPENDENCY_INJECTION,
    
    /**
     * Database patterns and best practices.
     * Examples: Missing Flow returns, synchronous operations.
     */
    DATABASE,
    
    /**
     * Performance optimization opportunities.
     * Examples: Inefficient iterations, unnecessary allocations.
     */
    PERFORMANCE,
    
    /**
     * Naming convention violations.
     * Examples: Incorrect casing, inconsistent naming.
     */
    NAMING,
    
    /**
     * Test coverage analysis.
     * Examples: Missing test files, untested components.
     */
    TEST_COVERAGE,
    
    /**
     * Documentation quality.
     * Examples: Missing KDoc, undocumented complexity.
     */
    DOCUMENTATION,
    
    /**
     * Security vulnerabilities and risks.
     * Examples: Hardcoded secrets, SQL injection risks.
     */
    SECURITY
}
