# Implementation Plan

- [x] 1. Set up authentication infrastructure
  - Create AuthService interface and implementation with sign-in, sign-up, and token management
  - Implement secure token storage using EncryptedSharedPreferences
  - Create User domain model and AuthState data classes
  - _Requirements: 1.1, 1.2, 1.4, 8.1, 8.2, 8.3_

- [x] 2. Create sync metadata database schema
  - Add SyncMetadataEntity and SyncQueueEntity to Room database
  - Create SyncMetadataDao with queries for metadata and queue management
  - Implement database migration to add sync fields to existing entities (updatedAt, serverId, syncStatus)
  - Update AppDatabase to include new tables and migration
  - _Requirements: 2.1, 2.2, 2.3, 2.5_

- [x] 3. Implement Retrofit API client for sync
  - Create SyncApiService interface with endpoints for meals, meal plans, and shopping lists
  - Define DTOs for sync requests and responses (MealDto, MealSyncRequest, MealSyncResponse, etc.)
  - Create ConflictDto for conflict resolution data
  - Set up Retrofit instance with authentication interceptor
  - _Requirements: 2.4, 3.1, 3.2, 8.4_

- [x] 4. Build core SyncEngine
  - Create SyncEngine interface with sync operations, queue management, and status monitoring
  - Implement SyncEngineImpl with sync logic for meals, meal plans, and shopping lists
  - Add queue management methods to track pending changes
  - Implement sync status Flow for UI observation
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4_

- [x] 5. Implement conflict resolution
  - Create ConflictResolver class with Last-Write-Wins strategy
  - Implement conflict detection logic comparing local and remote timestamps
  - Add conflict logging for user review
  - Handle edge cases where timestamps are identical (prefer server version)
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [x] 6. Create SyncWorker for background sync
  - Implement SyncWorker using CoroutineWorker
  - Add periodic sync scheduling with WorkManager (15-minute intervals)
  - Configure network constraints and backoff policy
  - Implement retry logic with exponential backoff
  - _Requirements: 2.4, 4.1, 4.2, 4.4_

- [x] 7. Enhance repositories with sync hooks
  - Update MealRepositoryImpl to queue changes on create/update/delete
  - Update MealPlanRepositoryImpl to queue changes on create/update/delete
  - Update ShoppingListRepositoryImpl to queue changes on create/update/delete
  - Add sync metadata updates after successful operations
  - _Requirements: 2.1, 2.2, 2.3, 4.1_

- [x] 8. Implement retry and error handling
  - Create RetryPolicy class with exponential backoff logic
  - Define SyncError sealed class for different error types
  - Implement error recovery strategies for network, auth, and server errors
  - Add error logging and user notifications
  - _Requirements: 4.4, 8.5_

- [x] 9. Create authentication UI
  - Build AuthScreen composable with sign-in and sign-up forms
  - Create AuthViewModel to manage authentication state
  - Add "Skip" option for users who want offline-only mode
  - Implement form validation and error display
  - _Requirements: 1.1, 1.2, 1.4, 1.5_

- [x] 10. Build sync status UI components
  - Create SyncStatusIndicator composable showing current sync state
  - Add sync status to app bar or bottom navigation
  - Implement manual sync trigger button
  - Display pending changes count when offline
  - Show last sync time and success/error states
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 11. Integrate sync with navigation
  - Add authentication check on app startup
  - Navigate to AuthScreen if not authenticated (with skip option)
  - Initialize SyncWorker after successful authentication
  - Add sync status to main navigation screens
  - _Requirements: 1.1, 1.3, 1.5_

- [x] 12. Create Hilt modules for sync components
  - Create SyncModule providing SyncEngine, AuthService, and SyncApiService
  - Create NetworkModule providing Retrofit with authentication interceptor
  - Update existing modules to inject sync dependencies
  - Configure WorkManager injection for SyncWorker
  - _Requirements: All requirements (dependency injection setup)_

- [ ] 13. Write unit tests for sync components
  - Test SyncEngine sync operations and queue management
  - Test ConflictResolver Last-Write-Wins logic
  - Test AuthService token management and refresh
  - Test RetryPolicy exponential backoff
  - _Requirements: All requirements (testing)_

- [ ] 14. Write integration tests for sync flows
  - Test end-to-end sync flow (create → queue → sync → verify)
  - Test conflict resolution with concurrent modifications
  - Test offline queue and auto-sync on reconnection
  - Test authentication flow and token refresh
  - _Requirements: All requirements (integration testing)_

- [ ] 15. Add instrumented tests for WorkManager
  - Test SyncWorker periodic execution
  - Test network constraint enforcement
  - Test retry behavior on failures
  - Test database migration with sync fields
  - _Requirements: 2.4, 4.2, 4.4 (instrumented testing)_
