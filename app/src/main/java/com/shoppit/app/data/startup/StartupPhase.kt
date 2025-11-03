package com.shoppit.app.data.startup

/**
 * Enum representing different phases of application startup.
 * Used to track and measure initialization performance.
 *
 * Requirements:
 * - 1.4: Defer non-critical initialization tasks to background threads
 * - 1.5: Log startup phase durations to identify bottlenecks
 */
enum class StartupPhase {
    /**
     * Application onCreate() method execution.
     * Includes Hilt initialization and critical setup.
     */
    APP_CREATION,
    
    /**
     * Hilt dependency injection initialization.
     * Includes component creation and module setup.
     */
    HILT_INITIALIZATION,
    
    /**
     * Database initialization and migration.
     * Includes Room database setup.
     */
    DATABASE_INITIALIZATION,
    
    /**
     * Critical services that must be initialized before app is usable.
     * Includes essential components needed for first screen.
     */
    CRITICAL_SERVICES,
    
    /**
     * Non-critical services that can be initialized in background.
     * Includes WorkManager, analytics, crash reporting, etc.
     */
    DEFERRED_SERVICES,
    
    /**
     * Time from app launch to first frame rendered.
     * Measures complete startup performance.
     */
    FIRST_FRAME
}
