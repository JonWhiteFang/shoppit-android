# Design Document

## Overview

This design document outlines the enhanced data persistence and caching architecture for the Shoppit application. The solution builds upon the existing Room database implementation to add robust migration support, in-memory caching, transaction management, data validation, backup/recovery capabilities, and performance optimizations.

The design follows the offline-first architecture pattern, ensuring the app remains fully functional without network connectivity while providing optimal performance through intelligent caching strategies.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Presentation Layer                       │
│                    (ViewModels, UI)                          │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Domain Layer                            │
│              (Use Cases, Repositories)                       │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Repository Implementation                   │   │
│  │  ┌────────────────┐      ┌────────────────────────┐  │   │
│  │  │  Cache Manager │◄────►│  Database Manager      │  │   │
│  │  │  (In-Memory)   │      │  (Room + Migrations)   │  │   │
│  │  └────────────────┘      └────────────────────────┘  │   │
│  │           ▲                         ▲                 │   │
│  │           │                         │                 │   │
│  │           ▼                         ▼                 │   │
│  │  ┌────────────────┐      ┌────────────────────────┐  │   │
│  │  │   Validator    │      │  Transaction Manager   │  │   │
│  │  └────────────────┘      └────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   SQLite Database                            │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **Read Operations**: Repository → Cache Manager → (if miss) → Database Manager → SQLite
2. **Write Operations**: Repository → Validator → Transaction Manager → Database Manager → SQLite → Cache Invalidation
3. **Migration**: App Start → Database Manager → Migration Handler → SQLite → Validation

## Components and Interfaces

### 1. Database Migration System

#### Migration Handler
```kotlin
interface MigrationHandler {
    fun getMigrations(): List<Migration>
    fun validateMigration(fromVersion: Int, toVersion: Int): Boolean
    fun onMigrationComplete(fromVersion: Int, toVersion: Int)
    fun onMigrationFailed(fromVersion: Int, toVersion: Int, error: Throwable)
}
```

**Implementation Details:**
- Create migration classes for each schema version change
- Implement validation queries to verify data integrity post-migration
- Log all migration operations using Timber
- Provide fallback strategy with user notification for failed migrations

**Migration Strategy:**
- Version 2 → 3: Add meal plan tables (meal_plans, meal_plan_items)
- Version 3 → 4: Add shopping list tables (shopping_lists, shopping_list_items)
- Version 4 → 5: Add indices for performance optimization
- Each migration includes rollback SQL for testing

### 2. Cache Management System

#### Cache Manager Interface
```kotlin
interface CacheManager<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun invalidate(key: K)
    fun invalidateAll()
    fun size(): Int
    fun clear()
}
```

#### LRU Cache Implementation
```kotlin
class LruCacheManager<K, V>(
    private val maxSize: Int,
    private val onEviction: ((K, V) -> Unit)? = null
) : CacheManager<K, V>
```

**Caching Strategy:**
- **Meal List Cache**: Cache all meals with 5-minute TTL
- **Meal Detail Cache**: Cache individual meals with 10-minute TTL
- **Cache Size**: Maximum 100 meal objects in memory
- **Eviction Policy**: LRU (Least Recently Used)
- **Invalidation**: On any write operation (add, update, delete)

**Cache Warming:**
- Pre-load frequently accessed meals on app start
- Background refresh of stale cache entries
- Predictive caching based on user patterns

### 3. Transaction Management

#### Transaction Manager Interface
```kotlin
interface TransactionManager {
    suspend fun <T> executeInTransaction(block: suspend () -> T): Result<T>
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        block: suspend () -> T
    ): Result<T>
}
```

**Transaction Scenarios:**
- Adding meal with multiple ingredients (atomic operation)
- Updating meal plan with multiple meal assignments
- Generating shopping list from multiple meal plans
- Bulk operations (import/export)

**Concurrency Handling:**
- Use Room's @Transaction annotation for database operations
- Implement optimistic locking for conflict resolution
- Queue concurrent writes to prevent database locks
- Timeout mechanism for long-running transactions (30 seconds)

### 4. Data Validation System

#### Validator Interface
```kotlin
interface DataValidator<T> {
    fun validate(data: T): ValidationResult
    fun validateBatch(data: List<T>): List<ValidationResult>
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<ValidationError>) : ValidationResult()
}

data class ValidationError(
    val field: String,
    val message: String,
    val code: String
)
```

**Validation Rules:**
- **Meal Validation**: Name not empty, at least one ingredient, valid ingredient quantities
- **Ingredient Validation**: Name not empty, quantity positive, unit valid
- **Meal Plan Validation**: Valid date, meal type enum, meal reference exists
- **Shopping List Validation**: At least one item, valid quantities

**Database Constraints:**
- Foreign key constraints enabled
- Unique constraints on business keys
- Check constraints for valid enums
- Not null constraints on required fields

### 5. Backup and Recovery System

#### Backup Manager Interface
```kotlin
interface BackupManager {
    suspend fun createBackup(): Result<BackupMetadata>
    suspend fun restoreBackup(backupId: String): Result<Unit>
    suspend fun listBackups(): Result<List<BackupMetadata>>
    suspend fun deleteBackup(backupId: String): Result<Unit>
    suspend fun exportToFile(destination: Uri): Result<Unit>
    suspend fun importFromFile(source: Uri): Result<Unit>
}

data class BackupMetadata(
    val id: String,
    val timestamp: Long,
    val version: Int,
    val size: Long,
    val checksum: String
)
```

**Backup Strategy:**
- **Automatic Checkpoints**: Every 24 hours or after 1000 transactions
- **Manual Backup**: User-initiated export to external storage
- **Backup Format**: JSON for portability, encrypted for security
- **Retention**: Keep last 7 automatic backups, unlimited manual backups

**Recovery Mechanisms:**
- Detect corruption using SQLite PRAGMA integrity_check
- Automatic recovery from last valid checkpoint
- Manual restore from user-selected backup
- Data validation after restore

### 6. Performance Optimization

#### Index Strategy
```sql
-- Frequently queried columns
CREATE INDEX idx_meals_name ON meals(name);
CREATE INDEX idx_meal_plans_date ON meal_plans(date);
CREATE INDEX idx_shopping_lists_created_at ON shopping_lists(created_at);

-- Composite indices for common queries
CREATE INDEX idx_meal_plans_date_meal_type ON meal_plans(date, meal_type);
CREATE INDEX idx_shopping_list_items_list_checked ON shopping_list_items(list_id, is_checked);
```

#### Query Optimization
- Use EXPLAIN QUERY PLAN to analyze slow queries
- Implement pagination for large result sets (50 items per page)
- Use projection to fetch only required columns
- Batch insert operations (100 items per batch)

#### Performance Monitoring
```kotlin
interface PerformanceMonitor {
    fun trackQuery(query: String, duration: Long)
    fun trackTransaction(operation: String, duration: Long)
    fun getSlowQueries(threshold: Long = 100): List<QueryMetrics>
    fun getCacheHitRate(): Double
}

data class QueryMetrics(
    val query: String,
    val avgDuration: Long,
    val executionCount: Int,
    val lastExecuted: Long
)
```

**Performance Thresholds:**
- Query execution: < 100ms (warning), < 50ms (target)
- Transaction execution: < 500ms (warning), < 200ms (target)
- Cache hit rate: > 80% (target)
- Database size: < 100MB (warning), < 50MB (target)

### 7. Data Cleanup and Maintenance

#### Maintenance Manager Interface
```kotlin
interface MaintenanceManager {
    suspend fun cleanupOrphanedData(): Result<CleanupReport>
    suspend fun vacuum(): Result<VacuumReport>
    suspend fun archiveOldData(retentionDays: Int): Result<ArchiveReport>
    suspend fun analyzeDatabaseSize(): Result<SizeReport>
}

data class CleanupReport(
    val orphanedRecords: Int,
    val deletedRecords: Int,
    val spaceReclaimed: Long
)

data class VacuumReport(
    val sizeBefore: Long,
    val sizeAfter: Long,
    val spaceReclaimed: Long,
    val duration: Long
)
```

**Cleanup Rules:**
- Remove orphaned ingredients (not linked to any meal)
- Remove completed shopping lists older than 30 days
- Remove meal plans older than 90 days
- Archive deleted meals (soft delete with 30-day retention)

**Maintenance Schedule:**
- Orphan cleanup: Daily at 3 AM
- Vacuum operation: Weekly on Sunday at 3 AM
- Archive operation: Monthly on 1st at 3 AM
- Size analysis: On app start

## Data Models

### Enhanced Entity Definitions

```kotlin
@Entity(
    tableName = "meals",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at"]),
        Index(value = ["updated_at"])
    ]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "ingredients")
    val ingredients: List<IngredientEntity>,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    
    @ColumnInfo(name = "version")
    val version: Int = 1
)
```

### Cache Entry Model

```kotlin
data class CacheEntry<T>(
    val data: T,
    val timestamp: Long,
    val ttl: Long,
    val accessCount: Int = 0
) {
    fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttl
    fun isStale(staleTtl: Long): Boolean = System.currentTimeMillis() - timestamp > staleTtl
}
```

## Error Handling

### Error Hierarchy

```kotlin
sealed class PersistenceError : Exception() {
    data class MigrationFailed(
        val fromVersion: Int,
        val toVersion: Int,
        override val cause: Throwable
    ) : PersistenceError()
    
    data class TransactionFailed(
        val operation: String,
        override val cause: Throwable
    ) : PersistenceError()
    
    data class ValidationFailed(
        val errors: List<ValidationError>
    ) : PersistenceError()
    
    data class CorruptionDetected(
        val details: String
    ) : PersistenceError()
    
    data class BackupFailed(
        val reason: String,
        override val cause: Throwable?
    ) : PersistenceError()
    
    data class CacheFull(
        val currentSize: Int,
        val maxSize: Int
    ) : PersistenceError()
}
```

### Error Recovery Strategies

1. **Migration Failure**: Fallback to destructive migration with user consent
2. **Transaction Failure**: Retry with exponential backoff (3 attempts)
3. **Validation Failure**: Return detailed errors to user
4. **Corruption**: Restore from last valid backup
5. **Cache Full**: Evict LRU entries, continue operation
6. **Backup Failure**: Log error, continue without backup

## Testing Strategy

### Unit Tests

1. **Migration Tests**: Test each migration path with sample data
2. **Cache Tests**: Test LRU eviction, TTL expiration, invalidation
3. **Transaction Tests**: Test rollback, retry logic, concurrency
4. **Validation Tests**: Test all validation rules and error cases
5. **Backup Tests**: Test export/import, corruption detection

### Instrumented Tests

1. **Database Tests**: Test actual Room operations with in-memory database
2. **Migration Tests**: Test real migration scenarios with SQLite
3. **Performance Tests**: Measure query execution times
4. **Concurrency Tests**: Test multiple threads accessing database
5. **Integration Tests**: Test complete flows (add meal → cache → database)

### Performance Benchmarks

- Measure cache hit rate under various scenarios
- Benchmark query performance with different data sizes
- Test transaction throughput under load
- Measure migration time for large databases

## Implementation Phases

### Phase 1: Migration System (Priority: High)
- Implement MigrationHandler interface
- Create migrations for future schema changes
- Add migration validation and logging
- Test migration paths

### Phase 2: Cache Management (Priority: High)
- Implement LruCacheManager
- Integrate cache into repository layer
- Add cache warming on app start
- Implement cache invalidation logic

### Phase 3: Transaction Management (Priority: Medium)
- Implement TransactionManager
- Add retry logic with exponential backoff
- Implement optimistic locking
- Add transaction timeout handling

### Phase 4: Data Validation (Priority: Medium)
- Implement DataValidator for each entity
- Add database constraints
- Integrate validation into repository layer
- Add validation error reporting

### Phase 5: Backup and Recovery (Priority: Low)
- Implement BackupManager
- Add automatic checkpoint mechanism
- Implement corruption detection
- Add export/import functionality

### Phase 6: Performance Optimization (Priority: Medium)
- Add database indices
- Implement PerformanceMonitor
- Optimize slow queries
- Add batch operations

### Phase 7: Maintenance System (Priority: Low)
- Implement MaintenanceManager
- Add cleanup jobs
- Implement vacuum operation
- Add size monitoring

## Configuration

### Database Configuration

```kotlin
@Provides
@Singleton
fun provideAppDatabase(
    @ApplicationContext context: Context,
    migrationHandler: MigrationHandler
): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        DATABASE_NAME
    )
        .addMigrations(*migrationHandler.getMigrations().toTypedArray())
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Create indices
                db.execSQL("CREATE INDEX idx_meals_name ON meals(name)")
                // ... other indices
            }
        })
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
}
```

### Cache Configuration

```kotlin
object CacheConfig {
    const val MEAL_LIST_CACHE_SIZE = 100
    const val MEAL_LIST_TTL_MS = 5 * 60 * 1000L // 5 minutes
    const val MEAL_DETAIL_TTL_MS = 10 * 60 * 1000L // 10 minutes
    const val CACHE_WARMING_ENABLED = true
    const val CACHE_WARMING_LIMIT = 50
}
```

### Performance Configuration

```kotlin
object PerformanceConfig {
    const val SLOW_QUERY_THRESHOLD_MS = 100L
    const val TRANSACTION_TIMEOUT_MS = 30_000L
    const val BATCH_SIZE = 100
    const val PAGE_SIZE = 50
    const val TARGET_CACHE_HIT_RATE = 0.8
}
```

## Security Considerations

1. **Data Encryption**: Use SQLCipher for database encryption (optional, future enhancement)
2. **Backup Encryption**: Encrypt exported backups using Android Keystore
3. **Access Control**: Ensure database file has proper permissions
4. **SQL Injection**: Use parameterized queries (Room handles this)
5. **Data Sanitization**: Validate and sanitize all user inputs

## Monitoring and Observability

### Metrics to Track

1. **Cache Metrics**: Hit rate, miss rate, eviction count, size
2. **Query Metrics**: Execution time, frequency, slow queries
3. **Transaction Metrics**: Success rate, rollback count, duration
4. **Database Metrics**: Size, growth rate, fragmentation
5. **Migration Metrics**: Success rate, duration, failure reasons

### Logging Strategy

- **Debug**: Cache hits/misses, query execution
- **Info**: Migration start/complete, backup operations
- **Warning**: Slow queries, cache full, approaching size limits
- **Error**: Migration failures, transaction failures, corruption detected

## Dependencies

- Room 2.6.0 (existing)
- Kotlin Coroutines 1.7.3 (existing)
- Timber 5.0.1 (existing)
- Hilt 2.48 (existing)
- AndroidX Security Crypto 1.1.0-alpha06 (existing)

No new dependencies required.
