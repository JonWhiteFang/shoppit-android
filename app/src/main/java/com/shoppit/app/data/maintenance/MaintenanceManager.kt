package com.shoppit.app.data.maintenance

/**
 * Interface for managing database maintenance operations.
 * Handles cleanup, vacuum, archiving, and size monitoring.
 */
interface MaintenanceManager {
    /**
     * Cleans up orphaned data that is no longer referenced.
     *
     * @return Result with cleanup report or error
     */
    suspend fun cleanupOrphanedData(): Result<CleanupReport>
    
    /**
     * Performs SQLite VACUUM operation to reclaim unused space.
     *
     * @return Result with vacuum report or error
     */
    suspend fun vacuum(): Result<VacuumReport>
    
    /**
     * Archives old data based on retention policy.
     *
     * @param retentionDays Number of days to retain data (default: 90)
     * @return Result with archive report or error
     */
    suspend fun archiveOldData(retentionDays: Int = 90): Result<ArchiveReport>
    
    /**
     * Analyzes database size and provides breakdown.
     *
     * @return Result with size report or error
     */
    suspend fun analyzeDatabaseSize(): Result<SizeReport>
}

/**
 * Report of cleanup operation results.
 *
 * @property orphanedRecords Number of orphaned records found
 * @property deletedRecords Number of records deleted
 * @property spaceReclaimed Space reclaimed in bytes
 */
data class CleanupReport(
    val orphanedRecords: Int,
    val deletedRecords: Int,
    val spaceReclaimed: Long
)

/**
 * Report of vacuum operation results.
 *
 * @property sizeBefore Database size before vacuum in bytes
 * @property sizeAfter Database size after vacuum in bytes
 * @property spaceReclaimed Space reclaimed in bytes
 * @property duration Operation duration in milliseconds
 */
data class VacuumReport(
    val sizeBefore: Long,
    val sizeAfter: Long,
    val spaceReclaimed: Long,
    val duration: Long
)

/**
 * Report of archive operation results.
 *
 * @property archivedRecords Number of records archived
 * @property retentionDays Retention period used
 * @property spaceReclaimed Space reclaimed in bytes
 */
data class ArchiveReport(
    val archivedRecords: Int,
    val retentionDays: Int,
    val spaceReclaimed: Long
)

/**
 * Report of database size analysis.
 *
 * @property totalSize Total database size in bytes
 * @property tablesSizes Map of table names to their sizes in bytes
 * @property indexesSize Total size of indices in bytes
 * @property freePages Number of free pages
 */
data class SizeReport(
    val totalSize: Long,
    val tablesSizes: Map<String, Long>,
    val indexesSize: Long,
    val freePages: Int
)
