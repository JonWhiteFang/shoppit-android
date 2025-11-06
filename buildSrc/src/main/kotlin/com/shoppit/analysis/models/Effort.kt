package com.shoppit.analysis.models

/**
 * Estimated effort required to fix a code quality issue.
 */
enum class Effort {
    /**
     * Trivial effort - less than 5 minutes.
     * Examples: Formatting fixes, simple renames.
     */
    TRIVIAL,
    
    /**
     * Small effort - 5 to 30 minutes.
     * Examples: Simple refactoring, adding missing annotations.
     */
    SMALL,
    
    /**
     * Medium effort - 30 minutes to 2 hours.
     * Examples: Extracting methods, restructuring classes.
     */
    MEDIUM,
    
    /**
     * Large effort - more than 2 hours.
     * Examples: Major refactoring, architectural changes.
     */
    LARGE
}
