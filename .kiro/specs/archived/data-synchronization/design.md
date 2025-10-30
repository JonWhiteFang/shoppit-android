# Design Document

## Overview

The Data Synchronization feature implements a robust, offline-first cloud sync system for Shoppit. It enables users to back up their meal library, meal plans, and shopping lists to a cloud backend and synchronize data across multiple devices. The design follows Clean Architecture principles, maintains the existing offline-first approach, and integrates seamlessly with the current Room-based local storage.

### Key Design Principles

1. **Offline-First**: Local database remains the single source of truth; sync is a background operation
2. **Non-Blocking**: Sync operations never block UI or user actions
3. **Conflict Resolution**: Automatic Last-Write-Wins strategy based on timestamps
4. **Security**: Encrypted token storage and secure API communication
5. **Resilience**: Retry logic with exponential backoff for network failures
6. **Minimal Disruption**: Integrates with existing architecture without major refactoring

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MealViewModel│  │PlannerViewModel│ │ShoppingViewModel│   │
│  └──────┬───────┘  └──────┬─────────┘ └──────┬───────┘      │
│         │                  │                   │              │
└─────────┼──────────────────┼───────────────────┼──────────────┘
          │                  │                   │
┌─────────┼──────────────────┼───────────────────┼──────────────┐
│         │    Domain Layer  │                   │              │
│  ┌──────▼───────┐  ┌──────▼─────────┐  ┌──────▼───────┐     │
│  │ Meal UseCase │  │ Planner UseCase│  │Shopping UseCase│    │
│  └──────┬───────┘  └──────┬─────────┘  └──────┬───────┘     │
│         │                  │                   │              │
│  ┌──────▼──────────────────▼───────────────────▼───────┐     │
│  │           Repository Interfaces                      │     │
│  └──────────────────────────┬───────────────────────────┘     │
└─────────────────────────────┼─────────────────────────────────┘
                              │
┌─────────────────────────────┼─────────────────────────────────┐
│         Data Layer          │                                 │
│  ┌──────────────────────────▼───────────────────────────┐     │
│  │         Repository Implementations                    │     │
│  │  (Enhanced with Sync Metadata)                       │     │
│  └──────┬───────────────────────────────────┬───────────┘     │
│         │                                   │                 │
│  ┌──────▼──────┐                    ┌──────▼──────┐          │
│  │  Room DAO   │                    │  Sync Engine│          │
│  │  (Local DB) │                    │             │          │
│  └─────────────┘                    └──────┬──────┘          │
│                                            │                 │
│                                     ┌──────▼──────┐          │
│                                     │ Sync Worker │          │
│                                     │(WorkManager)│          │
│                                     └──────┬──────┘          │
│                                            │                 │
│                                     ┌──────▼──────┐          │
│                                     │  Auth       │          │
│                                     │  Service    │          │
│                                     └──────┬──────┘          │
│                                            │                 │
│                                     ┌──────▼──────┐          │
│                                     │  Retrofit   │          │
│                                     │  API Client │          │
│                                     └──────┬──────┘          │
└────────────────────────────────────────────┼──────────────────┘
                                             │
                                      ┌──────▼──────┐
                                      │Cloud Backend│
                                      │   (REST)    │
                                      └─────────────┘
```

### Component Interaction Flow

**Write Operation (User Creates/Updates Data):**
```
User Action → ViewModel → UseCase → Repository → Room DAO → Local DB
                                                      ↓
                                              Sync Engine (queues change)
                                                      ↓
                                              Sync Worker (background)
                                                      ↓
                                              Cloud Backend
```

**Read Operation (User Views Data):**
```
User Action → ViewModel → UseCase → Repository → Room DAO → Local DB
                                                      ↑
                                              Sync Engine (background pull)
                                                      ↑
                                              Cloud Backend
```

## Components and Interfaces

### 1. Sync Engine

**Purpose**: Orchestrates all synchronization operations between local database and cloud backend.

**Interface:**
```kotlin
interface SyncEngine {
    // Sync operations
    suspend fun syncAll(): Result<SyncResult>
    suspend fun syncMeals(): Result<SyncResult>
    suspend fun syncMealPlans(): Result<SyncResult>
    suspend fun syncShoppingLists(): Result<SyncResult>
    
    // Queue management
    suspend fun queueChange(entityType: EntityType, entityId: Long, operation: SyncOperation)
    suspend fun getPendingChanges(): List<PendingChange>
    
    // Status monitoring
    fun observeSyncStatus(): Flow<SyncStatus>
    fun getLastSyncTime(): Long?
    
    // Manual control
    suspend fun forceSyncNow(): Result<SyncResult>
    suspend fun cancelSync()
}

data class SyncResult(
    val success: Boolean,
    val syncedEntities: Int,
    val failedEntities: Int,
    val conflicts: Int,
    val timestamp: Long
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

enum class EntityType {
    MEAL,
    MEAL_PLAN,
    SHOPPING_LIST_ITEM
}

enum class SyncOperation {
    CREATE,
    UPDATE,
    DELETE
}

data class PendingChange(
    val id: Long,
    val entityType: EntityType,
    val entityId: Long,
    val operation: SyncOperation,
    val timestamp: Long,
    val retryCount: Int
)
```

**Implementation Details:**
- Uses WorkManager for background sync scheduling
- Maintains a queue of pending changes in Room database
- Implements exponential backoff for retries (1s, 2s, 4s, 8s, 16s)
- Batches changes for efficient network usage
- Monitors network connectivity using ConnectivityManager

### 2. Authentication Service

**Purpose**: Manages user authentication, token storage, and session management.

**Interface:**
```kotlin
interface AuthService {
    // Authentication
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, name: String): Result<User>
    suspend fun signOut(): Result<Unit>
    
    // Token management
    suspend fun getAccessToken(): String?
    suspend fun refreshAccessToken(): Result<String>
    fun isAuthenticated(): Boolean
    
    // User info
    fun getCurrentUser(): Flow<User?>
    suspend fun updateUserProfile(name: String): Result<User>
}

data class User(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: Long
)
```

**Implementation Details:**
- Uses EncryptedSharedPreferences for token storage
- Implements automatic token refresh before expiration
- Stores tokens in Android Keystore for maximum security
- Provides reactive user state via Flow

### 3. Sync API Client

**Purpose**: Handles all HTTP communication with cloud backend.

**Interface:**
```kotlin
interface SyncApiService {
    // Meal sync
    @POST("api/v1/meals/sync")
    suspend fun syncMeals(@Body request: MealSyncRequest): Response<MealSyncResponse>
    
    @GET("api/v1/meals/changes")
    suspend fun getMealChanges(@Query("since") timestamp: Long): Response<MealChangesResponse>
    
    // Meal plan sync
    @POST("api/v1/meal-plans/sync")
    suspend fun syncMealPlans(@Body request: MealPlanSyncRequest): Response<MealPlanSyncResponse>
    
    @GET("api/v1/meal-plans/changes")
    suspend fun getMealPlanChanges(@Query("since") timestamp: Long): Response<MealPlanChangesResponse>
    
    // Shopping list sync
    @POST("api/v1/shopping-lists/sync")
    suspend fun syncShoppingLists(@Body request: ShoppingListSyncRequest): Response<ShoppingListSyncResponse>
    
    @GET("api/v1/shopping-lists/changes")
    suspend fun getShoppingListChanges(@Query("since") timestamp: Long): Response<ShoppingListChangesResponse>
    
    // Batch operations
    @POST("api/v1/sync/batch")
    suspend fun batchSync(@Body request: BatchSyncRequest): Response<BatchSyncResponse>
}
```

**DTOs:**
```kotlin
// Meal DTOs
data class MealDto(
    val id: String,
    val localId: Long?,
    val name: String,
    val ingredients: List<IngredientDto>,
    val notes: String?,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)

data class MealSyncRequest(
    val meals: List<MealDto>,
    val lastSyncTimestamp: Long?
)

data class MealSyncResponse(
    val synced: List<MealDto>,
    val conflicts: List<ConflictDto>,
    val serverTimestamp: Long
)

// Conflict resolution
data class ConflictDto(
    val entityType: String,
    val localVersion: Any,
    val serverVersion: Any,
    val resolution: String // "server_wins", "local_wins"
)
```

### 4. Sync Metadata Storage

**Purpose**: Tracks synchronization state for each entity.

**Room Entities:**
```kotlin
@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String, // "meal", "meal_plan", "shopping_list_item"
    val entityId: Long,
    val serverId: String?, // Cloud backend ID
    val lastSyncedAt: Long?,
    val localUpdatedAt: Long,
    val syncStatus: String, // "synced", "pending", "conflict", "error"
    val retryCount: Int = 0,
    val errorMessage: String?
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val operation: String, // "create", "update", "delete"
    val payload: String, // JSON serialized entity
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastAttemptAt: Long?
)

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE entityType = :type AND entityId = :id")
    suspend fun getMetadata(type: String, id: Long): SyncMetadataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: SyncMetadataEntity)
    
    @Query("SELECT * FROM sync_metadata WHERE syncStatus = 'pending'")
    fun observePendingSync(): Flow<List<SyncMetadataEntity>>
    
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllQueuedChanges(): List<SyncQueueEntity>
    
    @Insert
    suspend fun queueChange(change: SyncQueueEntity)
    
    @Delete
    suspend fun removeFromQueue(change: SyncQueueEntity)
}
```

### 5. Enhanced Entity Models

**Add sync fields to existing entities:**

```kotlin
// Update MealEntity
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ingredients: List<IngredientEntity>,
    val notes: String?,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long, // NEW: Track local modifications
    val serverId: String?, // NEW: Cloud backend ID
    val syncStatus: String = "pending" // NEW: "synced", "pending", "conflict"
)

// Similar updates for MealPlanEntity and ShoppingListItemEntity
```

### 6. Sync Worker

**Purpose**: Background task that performs periodic and on-demand synchronization.

**Implementation:**
```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val syncEngine: SyncEngine,
    private val authService: AuthService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        if (!authService.isAuthenticated()) {
            return Result.success()
        }
        
        return when (val result = syncEngine.syncAll()) {
            is kotlin.Result.Success -> Result.success()
            is kotlin.Result.Failure -> {
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
    
    companion object {
        const val MAX_RETRIES = 5
        const val WORK_NAME = "sync_work"
        
        fun schedule(workManager: WorkManager) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
```

### 7. Conflict Resolution Strategy

**Last-Write-Wins Implementation:**

```kotlin
class ConflictResolver {
    fun resolve(
        local: SyncableEntity,
        remote: SyncableEntity
    ): ConflictResolution {
        return when {
            local.updatedAt > remote.updatedAt -> {
                ConflictResolution.UseLocal(local)
            }
            local.updatedAt < remote.updatedAt -> {
                ConflictResolution.UseRemote(remote)
            }
            else -> {
                // Timestamps equal, prefer server version
                ConflictResolution.UseRemote(remote)
            }
        }
    }
}

sealed class ConflictResolution {
    data class UseLocal(val entity: SyncableEntity) : ConflictResolution()
    data class UseRemote(val entity: SyncableEntity) : ConflictResolution()
}

interface SyncableEntity {
    val id: Long
    val serverId: String?
    val updatedAt: Long
}
```

## Data Models

### Domain Models

```kotlin
// Sync status for UI
data class SyncState(
    val status: SyncStatus,
    val lastSyncTime: Long?,
    val pendingChanges: Int,
    val errorMessage: String?
)

// User authentication
data class AuthState(
    val isAuthenticated: Boolean,
    val user: User?,
    val error: String?
)
```

### Database Schema Updates

**Migration Strategy:**
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add sync columns to existing tables
        database.execSQL(
            "ALTER TABLE meals ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE meals ADD COLUMN serverId TEXT"
        )
        database.execSQL(
            "ALTER TABLE meals ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'pending'"
        )
        
        // Create sync metadata table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_metadata (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                entityId INTEGER NOT NULL,
                serverId TEXT,
                lastSyncedAt INTEGER,
                localUpdatedAt INTEGER NOT NULL,
                syncStatus TEXT NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0,
                errorMessage TEXT
            )
        """)
        
        // Create sync queue table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                entityId INTEGER NOT NULL,
                operation TEXT NOT NULL,
                payload TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                retryCount INTEGER NOT NULL DEFAULT 0,
                lastAttemptAt INTEGER
            )
        """)
    }
}
```

## Error Handling

### Error Types

```kotlin
sealed class SyncError : Exception() {
    data class NetworkError(override val message: String) : SyncError()
    data class AuthenticationError(override val message: String) : SyncError()
    data class ServerError(val code: Int, override val message: String) : SyncError()
    data class ConflictError(val conflicts: List<ConflictDto>) : SyncError()
    data class DatabaseError(override val message: String) : SyncError()
    object NoInternetError : SyncError()
}
```

### Error Recovery

1. **Network Errors**: Retry with exponential backoff (1s, 2s, 4s, 8s, 16s)
2. **Authentication Errors**: Attempt token refresh, then prompt re-login
3. **Server Errors (5xx)**: Retry with backoff, max 5 attempts
4. **Client Errors (4xx)**: Log error, don't retry, notify user
5. **Conflict Errors**: Apply resolution strategy, log for review
6. **Database Errors**: Log error, attempt recovery, notify user

### Retry Logic

```kotlin
class RetryPolicy {
    suspend fun <T> executeWithRetry(
        maxAttempts: Int = 5,
        initialDelay: Long = 1000,
        maxDelay: Long = 16000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    return Result.failure(e)
                }
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }
}
```

## Testing Strategy

### Unit Tests

1. **SyncEngine Tests**
   - Test sync queue management
   - Test conflict resolution logic
   - Test retry mechanism
   - Test status updates

2. **AuthService Tests**
   - Test token storage and retrieval
   - Test token refresh logic
   - Test authentication flows

3. **ConflictResolver Tests**
   - Test Last-Write-Wins logic
   - Test edge cases (equal timestamps)

4. **Repository Tests**
   - Test sync metadata updates
   - Test queue operations

### Integration Tests

1. **End-to-End Sync Flow**
   - Create meal locally → verify queued → verify synced
   - Modify meal on device A → verify appears on device B
   - Offline modifications → online → verify sync

2. **Conflict Resolution**
   - Modify same meal on two devices → verify resolution
   - Delete on one device, modify on another → verify handling

3. **Network Failure Scenarios**
   - Sync during network loss → verify queued
   - Network restored → verify auto-sync

### Instrumented Tests

1. **WorkManager Integration**
   - Test periodic sync scheduling
   - Test constraint-based execution
   - Test retry behavior

2. **Database Migration**
   - Test schema migration
   - Test data preservation

## Security Considerations

### Token Storage
- Use EncryptedSharedPreferences for token storage
- Store encryption keys in Android Keystore
- Implement token rotation every 7 days
- Clear tokens on sign-out

### API Communication
- Use HTTPS for all API calls
- Implement certificate pinning
- Add request signing for sensitive operations
- Rate limit API calls to prevent abuse

### Data Privacy
- Encrypt sensitive data before upload
- Implement user data deletion on account removal
- Comply with GDPR requirements
- Provide data export functionality

## Performance Optimization

### Batching Strategy
- Batch up to 50 entities per sync request
- Prioritize recent changes
- Use delta sync (only changed entities)

### Network Efficiency
- Compress request/response payloads (gzip)
- Use ETags for conditional requests
- Implement request deduplication
- Cache API responses when appropriate

### Database Optimization
- Index sync metadata tables
- Use transactions for batch operations
- Implement pagination for large datasets
- Clean up old sync queue entries

### Background Processing
- Use WorkManager for reliable background execution
- Respect battery optimization settings
- Implement adaptive sync frequency based on usage
- Defer non-critical syncs during low battery

## UI Integration

### Sync Status Indicator

```kotlin
@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (syncState.status) {
            SyncStatus.SYNCING -> {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Text("Syncing...")
            }
            SyncStatus.SUCCESS -> {
                Icon(Icons.Default.CloudDone, "Synced")
                Text("Synced ${formatTime(syncState.lastSyncTime)}")
            }
            SyncStatus.ERROR -> {
                Icon(Icons.Default.CloudOff, "Error", tint = Color.Red)
                Text("Sync failed")
                TextButton(onClick = onManualSync) {
                    Text("Retry")
                }
            }
            SyncStatus.OFFLINE -> {
                Icon(Icons.Default.CloudOff, "Offline")
                Text("${syncState.pendingChanges} pending")
            }
            SyncStatus.IDLE -> {
                Icon(Icons.Default.Cloud, "Idle")
                TextButton(onClick = onManualSync) {
                    Text("Sync now")
                }
            }
        }
    }
}
```

### Authentication Screen

```kotlin
@Composable
fun AuthScreen(
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (email: String, password: String, name: String) -> Unit,
    onSkip: () -> Unit
) {
    // Sign in/sign up form with skip option
}
```

## Deployment Considerations

### Backend Requirements
- RESTful API with JWT authentication
- PostgreSQL or similar relational database
- Redis for session management
- S3 or similar for potential file storage
- Load balancer for horizontal scaling

### API Endpoints
```
POST   /api/v1/auth/signup
POST   /api/v1/auth/signin
POST   /api/v1/auth/refresh
POST   /api/v1/auth/signout

GET    /api/v1/meals/changes?since=<timestamp>
POST   /api/v1/meals/sync
GET    /api/v1/meal-plans/changes?since=<timestamp>
POST   /api/v1/meal-plans/sync
GET    /api/v1/shopping-lists/changes?since=<timestamp>
POST   /api/v1/shopping-lists/sync

POST   /api/v1/sync/batch
```

### Monitoring
- Track sync success/failure rates
- Monitor API response times
- Alert on high error rates
- Track user adoption metrics

## Migration Path

### Phase 1: Foundation (Week 1)
- Add sync metadata tables
- Implement AuthService
- Create Retrofit API client
- Set up token storage

### Phase 2: Core Sync (Week 2)
- Implement SyncEngine
- Add sync queue management
- Implement conflict resolution
- Create SyncWorker

### Phase 3: Integration (Week 3)
- Update repositories with sync hooks
- Add UI for authentication
- Implement sync status indicator
- Add manual sync trigger

### Phase 4: Testing & Polish (Week 4)
- Comprehensive testing
- Performance optimization
- Error handling refinement
- Documentation

## Future Enhancements

1. **Selective Sync**: Allow users to choose what to sync
2. **Offline Conflict UI**: Show conflicts to users for manual resolution
3. **Sync Analytics**: Detailed sync statistics and history
4. **Multi-Account Support**: Support multiple user accounts
5. **Family Sharing**: Share meal plans with family members
6. **Real-time Sync**: WebSocket-based real-time updates
7. **Attachment Sync**: Sync meal photos and documents
