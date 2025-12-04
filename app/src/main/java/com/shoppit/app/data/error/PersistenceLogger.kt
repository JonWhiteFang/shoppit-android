package com.shoppit.app.data.error

import timber.log.Timber

/**
 * Centralized logging utility for all persistence layer operations.
 * Provides consistent logging format and severity levels.
 */
object PersistenceLogger {
    
    private const val TAG_PREFIX = "Persistence"
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    // Migration logging
    fun logMigrationStart(fromVersion: Int, toVersion: Int) {
        Timber.tag("$TAG_PREFIX.Migration").i("Starting migration from v$fromVersion to v$toVersion")
    }
    
    fun logMigrationSuccess(fromVersion: Int, toVersion: Int, durationMs: Long) {
        Timber.tag("$TAG_PREFIX.Migration").i(
            "Migration completed successfully: v$fromVersion → v$toVersion (${durationMs}ms)"
        )
    }
    
    fun logMigrationFailure(fromVersion: Int, toVersion: Int, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Migration").e(
            error,
            "Migration failed: v$fromVersion → v$toVersion"
        )
    }
    
    // Cache logging
    fun logCacheHit(key: String) {
        Timber.tag("$TAG_PREFIX.Cache").d("Cache HIT: ${sanitize(key)}")
    }
    
    fun logCacheMiss(key: String) {
        Timber.tag("$TAG_PREFIX.Cache").d("Cache MISS: ${sanitize(key)}")
    }
    
    fun logCacheEviction(key: String, reason: String) {
        Timber.tag("$TAG_PREFIX.Cache").d("Cache eviction: ${sanitize(key)} (reason: ${sanitize(reason)})")
    }
    
    fun logCacheInvalidation(key: String) {
        Timber.tag("$TAG_PREFIX.Cache").d("Cache invalidated: ${sanitize(key)}")
    }
    
    fun logCacheStats(hitRate: Double, size: Int, maxSize: Int) {
        Timber.tag("$TAG_PREFIX.Cache").i(
            "Cache stats - Hit rate: %.2f%%, Size: %d/%d".format(hitRate * 100, size, maxSize)
        )
    }
    
    // Transaction logging
    fun logTransactionStart(operation: String) {
        Timber.tag("$TAG_PREFIX.Transaction").d("Transaction started: ${sanitize(operation)}")
    }
    
    fun logTransactionCommit(operation: String, durationMs: Long) {
        Timber.tag("$TAG_PREFIX.Transaction").d(
            "Transaction committed: ${sanitize(operation)} (${durationMs}ms)"
        )
    }
    
    fun logTransactionRollback(operation: String, reason: String) {
        Timber.tag("$TAG_PREFIX.Transaction").w(
            "Transaction rolled back: ${sanitize(operation)} (reason: ${sanitize(reason)})"
        )
    }
    
    fun logTransactionRetry(operation: String, attempt: Int, maxAttempts: Int) {
        Timber.tag("$TAG_PREFIX.Transaction").w(
            "Transaction retry: ${sanitize(operation)} (attempt $attempt/$maxAttempts)"
        )
    }
    
    // Query logging
    fun logQueryExecution(query: String, durationMs: Long) {
        val sanitizedQuery = sanitize(query)
        if (durationMs > 100) {
            Timber.tag("$TAG_PREFIX.Query").w("SLOW QUERY (${durationMs}ms): $sanitizedQuery")
        } else {
            Timber.tag("$TAG_PREFIX.Query").d("Query executed (${durationMs}ms): $sanitizedQuery")
        }
    }
    
    fun logQueryFailure(query: String, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Query").e(error, "Query failed: ${sanitize(query)}")
    }
    
    // Validation logging
    fun logValidationStart(entityType: String) {
        Timber.tag("$TAG_PREFIX.Validation").d("Validating $entityType")
    }
    
    fun logValidationSuccess(entityType: String) {
        Timber.tag("$TAG_PREFIX.Validation").d("Validation passed: $entityType")
    }
    
    fun logValidationFailure(entityType: String, errors: List<ValidationError>) {
        Timber.tag("$TAG_PREFIX.Validation").w(
            "Validation failed for $entityType: ${errors.size} errors - ${errors.joinToString(", ") { it.field }}"
        )
    }
    
    // Backup logging
    fun logBackupStart(type: String) {
        Timber.tag("$TAG_PREFIX.Backup").i("Starting $type backup")
    }
    
    fun logBackupSuccess(type: String, sizeBytes: Long, durationMs: Long) {
        Timber.tag("$TAG_PREFIX.Backup").i(
            "Backup completed: $type (${sizeBytes / 1024}KB, ${durationMs}ms)"
        )
    }
    
    fun logBackupFailure(type: String, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Backup").e(error, "Backup failed: $type")
    }
    
    fun logRestoreStart(backupId: String) {
        Timber.tag("$TAG_PREFIX.Backup").i("Starting restore from backup: $backupId")
    }
    
    fun logRestoreSuccess(backupId: String, durationMs: Long) {
        Timber.tag("$TAG_PREFIX.Backup").i(
            "Restore completed: $backupId (${durationMs}ms)"
        )
    }
    
    fun logRestoreFailure(backupId: String, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Backup").e(error, "Restore failed: $backupId")
    }
    
    // Corruption logging
    fun logCorruptionDetected(details: String) {
        Timber.tag("$TAG_PREFIX.Corruption").e("DATABASE CORRUPTION DETECTED: ${sanitize(details)}")
    }
    
    fun logCorruptionRecoveryStart() {
        Timber.tag("$TAG_PREFIX.Corruption").i("Starting corruption recovery")
    }
    
    fun logCorruptionRecoverySuccess() {
        Timber.tag("$TAG_PREFIX.Corruption").i("Corruption recovery completed successfully")
    }
    
    fun logCorruptionRecoveryFailure(error: Throwable) {
        Timber.tag("$TAG_PREFIX.Corruption").e(error, "Corruption recovery failed")
    }
    
    // Maintenance logging
    fun logMaintenanceStart(operation: String) {
        Timber.tag("$TAG_PREFIX.Maintenance").i("Starting maintenance: ${sanitize(operation)}")
    }
    
    fun logMaintenanceSuccess(operation: String, details: String) {
        Timber.tag("$TAG_PREFIX.Maintenance").i("Maintenance completed: ${sanitize(operation)} - ${sanitize(details)}")
    }
    
    fun logMaintenanceFailure(operation: String, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Maintenance").e(error, "Maintenance failed: ${sanitize(operation)}")
    }
    
    fun logCleanupResult(orphanedRecords: Int, deletedRecords: Int, spaceReclaimed: Long) {
        Timber.tag("$TAG_PREFIX.Maintenance").i(
            "Cleanup completed: $orphanedRecords orphaned, $deletedRecords deleted, ${spaceReclaimed / 1024}KB reclaimed"
        )
    }
    
    fun logVacuumResult(sizeBefore: Long, sizeAfter: Long, durationMs: Long) {
        val spaceReclaimed = sizeBefore - sizeAfter
        Timber.tag("$TAG_PREFIX.Maintenance").i(
            "Vacuum completed: ${spaceReclaimed / 1024}KB reclaimed (${durationMs}ms)"
        )
    }
    
    // Performance logging
    fun logPerformanceMetrics(
        avgQueryTime: Long,
        cacheHitRate: Double,
        dbSize: Long,
        slowQueryCount: Int
    ) {
        Timber.tag("$TAG_PREFIX.Performance").i(
            "Performance metrics - Avg query: ${avgQueryTime}ms, Cache hit: %.2f%%, DB size: ${dbSize / 1024}KB, Slow queries: $slowQueryCount"
                .format(cacheHitRate * 100)
        )
    }
    
    fun logDatabaseSizeWarning(currentSize: Long, threshold: Long) {
        Timber.tag("$TAG_PREFIX.Performance").w(
            "Database size approaching limit: ${currentSize / 1024}KB / ${threshold / 1024}KB"
        )
    }
    
    // General operation logging
    fun logOperationStart(operation: String, details: String = "") {
        val sanitizedOp = sanitize(operation)
        val message = if (details.isNotEmpty()) "$sanitizedOp - ${sanitize(details)}" else sanitizedOp
        Timber.tag("$TAG_PREFIX.Operation").d("Starting: $message")
    }
    
    fun logOperationSuccess(operation: String, durationMs: Long) {
        Timber.tag("$TAG_PREFIX.Operation").d("Completed: ${sanitize(operation)} (${durationMs}ms)")
    }
    
    fun logOperationFailure(operation: String, error: Throwable) {
        Timber.tag("$TAG_PREFIX.Operation").e(error, "Failed: ${sanitize(operation)}")
    }
}
