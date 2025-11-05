package com.shoppit.app.analysis.models

/**
 * Priority level for code quality findings.
 * Used to classify the severity and urgency of issues.
 */
enum class Priority {
    /**
     * Critical issues that must be fixed immediately.
     * Examples: Security vulnerabilities, data loss risks.
     */
    CRITICAL,
    
    /**
     * High priority issues that should be fixed soon.
     * Examples: Architecture violations, major bugs.
     */
    HIGH,
    
    /**
     * Medium priority issues that should be addressed.
     * Examples: Performance issues, code smells.
     */
    MEDIUM,
    
    /**
     * Low priority issues that can be fixed when convenient.
     * Examples: Style issues, minor improvements.
     */
    LOW
}
