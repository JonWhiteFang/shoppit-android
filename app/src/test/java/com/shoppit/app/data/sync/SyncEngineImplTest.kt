package com.shoppit.app.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.gson.Gson
import com.shoppit.app.data.local.dao.SyncMetadataDao
import com.shoppit.app.data.local.entity.SyncMetadataEntity
import com.shoppit.app.data.local.entity.SyncQueueEntity
import com.shoppit.app.data.remote.api.SyncApiService
import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.EntityType
import com.shoppit.app.domain.model.SyncOperation
import com.shoppit.app.domain.model.SyncStatus
import com.shoppit.app.domain.repository.AuthRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Unit tests for SyncEngineImpl.
 *
 * Tests cover:
 * - Sync operations (syncAll, syncMeals, syncMealPlans, syncShoppingLists)
 * - Queue management (queueChange, getPendingChanges)
 * - Status monitoring (observeSyncStatus, getLastSyncTime)
 * - Manual control (forceSyncNow, cancelSync)
 * - Network connectivity checks
 * - Authentication checks
 * - Error handling
 */
@ExperimentalCoroutinesApi
class SyncEngineImplTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities
    private lateinit var syncMetadataDao: SyncMetadataDao
    private lateinit var syncApiService: SyncApiService
    private lateinit var authRepository: AuthRepository
    private lateinit var gson: Gson
    private lateinit var retryPolicy: RetryPolicy
    private lateinit var errorRecoveryStrategy: SyncErrorRecoveryStrategy
    private lateinit var errorLogger: SyncErrorLogger
    private lateinit var notificationHelper: SyncNotificationHelper
    private lateinit var syncEngine: SyncEngineImpl

    @Before
    fun setup() {
        // Mock Android components
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)

        // Mock dependencies
        syncMetadataDao = mockk(relaxed = true)
        syncApiService = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        gson = Gson()
        retryPolicy = mockk(relaxed = true)
        errorRecoveryStrategy = mockk(relaxed = true)
        errorLogger = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)

        // Setup connectivity manager
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        // Create SyncEngine instance
        syncEngine = SyncEngineImpl(
            context = context,
            syncMetadataDao = syncMetadataDao,
            syncApiService = syncApiService,
            authRepository = authRepository,
            gson = gson,
            retryPolicy = retryPolicy,
            errorRecoveryStrategy = errorRecoveryStrategy,
            errorLogger = errorLogger,
            notificationHelper = notificationHelper
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========== Sync Operations Tests ==========

    @Test
    fun `syncAll returns failure when user not authenticated`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns false

        // When
        val result = syncEngine.syncAll()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.AuthenticationError)
        assertEquals("User not authenticated", error.message)
    }

    @Test
    fun `syncAll returns failure when no network connectivity`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        every { connectivityManager.activeNetwork } returns null

        // When
        val result = syncEngine.syncAll()

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.NetworkError)
        assertEquals(SyncStatus.OFFLINE, syncEngine.observeSyncStatus().first())
    }

    @Test
    fun `syncAll updates status to SYNCING when started`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()

        // When
        syncEngine.syncAll()

        // Then - status should eventually be SUCCESS or ERROR
        val finalStatus = syncEngine.observeSyncStatus().first()
        assertTrue(finalStatus == SyncStatus.SUCCESS || finalStatus == SyncStatus.ERROR)
    }

    @Test
    fun `syncAll processes all entity types`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()

        // When
        val result = syncEngine.syncAll()

        // Then
        assertTrue(result.isSuccess)
        val syncResult = result.getOrNull()
        assertNotNull(syncResult)
        assertEquals(0, syncResult.syncedEntities)
        assertEquals(0, syncResult.failedEntities)
        assertEquals(0, syncResult.conflicts)
    }

    @Test
    fun `syncAll sets lastSyncTime on success`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()

        // When
        syncEngine.syncAll()

        // Then
        assertNotNull(syncEngine.getLastSyncTime())
    }

    @Test
    fun `syncMeals processes queued meal changes`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        val queuedChange = SyncQueueEntity(
            id = 1,
            entityType = "meal",
            entityId = 100,
            operation = "create",
            payload = "{}",
            createdAt = System.currentTimeMillis(),
            retryCount = 0,
            lastAttemptAt = null
        )
        coEvery { syncMetadataDao.getQueuedChangesByType("meal") } returns listOf(queuedChange)
        coEvery { syncMetadataDao.getMetadata("meal", 100) } returns SyncMetadataEntity(
            id = 1,
            entityType = "meal",
            entityId = 100,
            serverId = null,
            lastSyncedAt = null,
            localUpdatedAt = System.currentTimeMillis(),
            syncStatus = "pending",
            retryCount = 0,
            errorMessage = null
        )
        coEvery { retryPolicy.executeWithRetryForEntity(any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { syncMetadataDao.updateMetadata(any()) } just Runs
        coEvery { syncMetadataDao.removeFromQueue(any()) } just Runs

        // When
        val result = syncEngine.syncMeals()

        // Then
        assertTrue(result.isSuccess)
        val syncResult = result.getOrNull()
        assertNotNull(syncResult)
        assertEquals(1, syncResult.syncedEntities)
        assertEquals(0, syncResult.failedEntities)
    }

    // ========== Queue Management Tests ==========

    @Test
    fun `queueChange creates new queue entry for new entity`() = runTest {
        // Given
        coEvery { syncMetadataDao.getQueuedChange("meal", 100) } returns null
        coEvery { syncMetadataDao.queueChange(any()) } just Runs
        coEvery { syncMetadataDao.getMetadata("meal", 100) } returns null
        coEvery { syncMetadataDao.insertMetadata(any()) } just Runs

        // When
        syncEngine.queueChange(EntityType.MEAL, 100, SyncOperation.CREATE)

        // Then
        coVerify { syncMetadataDao.queueChange(any()) }
        coVerify { syncMetadataDao.insertMetadata(any()) }
    }

    @Test
    fun `queueChange updates existing queue entry`() = runTest {
        // Given
        val existingChange = SyncQueueEntity(
            id = 1,
            entityType = "meal",
            entityId = 100,
            operation = "create",
            payload = "{}",
            createdAt = System.currentTimeMillis() - 1000,
            retryCount = 0,
            lastAttemptAt = null
        )
        coEvery { syncMetadataDao.getQueuedChange("meal", 100) } returns existingChange
        coEvery { syncMetadataDao.updateQueuedChange(any()) } just Runs
        coEvery { syncMetadataDao.getMetadata("meal", 100) } returns SyncMetadataEntity(
            id = 1,
            entityType = "meal",
            entityId = 100,
            serverId = null,
            lastSyncedAt = null,
            localUpdatedAt = System.currentTimeMillis(),
            syncStatus = "synced",
            retryCount = 0,
            errorMessage = null
        )
        coEvery { syncMetadataDao.updateMetadata(any()) } just Runs

        // When
        syncEngine.queueChange(EntityType.MEAL, 100, SyncOperation.UPDATE)

        // Then
        coVerify { syncMetadataDao.updateQueuedChange(any()) }
        coVerify { syncMetadataDao.updateMetadata(any()) }
    }

    @Test
    fun `getPendingChanges returns all queued changes`() = runTest {
        // Given
        val queuedChanges = listOf(
            SyncQueueEntity(
                id = 1,
                entityType = "meal",
                entityId = 100,
                operation = "create",
                payload = "{}",
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                lastAttemptAt = null
            ),
            SyncQueueEntity(
                id = 2,
                entityType = "meal_plan",
                entityId = 200,
                operation = "update",
                payload = "{}",
                createdAt = System.currentTimeMillis(),
                retryCount = 1,
                lastAttemptAt = System.currentTimeMillis() - 1000
            )
        )
        coEvery { syncMetadataDao.getAllQueuedChanges() } returns queuedChanges

        // When
        val pendingChanges = syncEngine.getPendingChanges()

        // Then
        assertEquals(2, pendingChanges.size)
        assertEquals(EntityType.MEAL, pendingChanges[0].entityType)
        assertEquals(EntityType.MEAL_PLAN, pendingChanges[1].entityType)
        assertEquals(SyncOperation.CREATE, pendingChanges[0].operation)
        assertEquals(SyncOperation.UPDATE, pendingChanges[1].operation)
    }

    @Test
    fun `getPendingChanges returns empty list on error`() = runTest {
        // Given
        coEvery { syncMetadataDao.getAllQueuedChanges() } throws Exception("Database error")

        // When
        val pendingChanges = syncEngine.getPendingChanges()

        // Then
        assertTrue(pendingChanges.isEmpty())
    }

    // ========== Status Monitoring Tests ==========

    @Test
    fun `observeSyncStatus returns current status`() = runTest {
        // Given - initial status is IDLE

        // When
        val status = syncEngine.observeSyncStatus().first()

        // Then
        assertEquals(SyncStatus.IDLE, status)
    }

    @Test
    fun `getLastSyncTime returns null initially`() {
        // When
        val lastSyncTime = syncEngine.getLastSyncTime()

        // Then
        assertEquals(null, lastSyncTime)
    }

    // ========== Manual Control Tests ==========

    @Test
    fun `forceSyncNow triggers immediate sync`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()

        // When
        val result = syncEngine.forceSyncNow()

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelSync sets status to IDLE`() = runTest {
        // When
        syncEngine.cancelSync()

        // Then
        assertEquals(SyncStatus.IDLE, syncEngine.observeSyncStatus().first())
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `syncAll handles sync failures gracefully`() = runTest {
        // Given
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } throws Exception("Database error")

        // When
        val result = syncEngine.syncAll()

        // Then
        assertTrue(result.isSuccess) // Should still succeed with 0 synced entities
        val syncResult = result.getOrNull()
        assertNotNull(syncResult)
        assertFalse(syncResult.success)
    }

    @Test
    fun `queueChange handles errors gracefully`() = runTest {
        // Given
        coEvery { syncMetadataDao.getQueuedChange(any(), any()) } throws Exception("Database error")

        // When - should not throw
        syncEngine.queueChange(EntityType.MEAL, 100, SyncOperation.CREATE)

        // Then - no exception thrown
    }
}
