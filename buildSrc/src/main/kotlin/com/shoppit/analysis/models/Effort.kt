package com.shoppit.analysis.models

/**
 * Estimated effort to fix an issue.
 */
enum class Effort {
    TRIVIAL,    // < 15 minutes
    EASY,       // 15-30 minutes
    MEDIUM,     // 30 minutes - 2 hours
    HARD,       // 2-8 hours
    VERY_HARD   // > 8 hours
}
