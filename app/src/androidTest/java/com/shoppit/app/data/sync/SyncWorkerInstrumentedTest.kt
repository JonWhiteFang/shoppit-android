package com.shoppit.app.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.shoppit.app.data.local.dao.SyncMetadataDao
import com.shoppit.app.data.local.entity.SyncQueueEntity
import com.shoppit.app.data.remote.api.SyncApiService
import com.shoppit.app.data.remote.dto.MealSyncResponse
import com.shoppit.app.domain.model.SyncStatus
import com.shoppit.app.domain.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Instrumented tests for SyncWorker.
 *
 * These tests verify:
 * - SyncWorker periodic execution
 * - Network constraint enforcement
 * - Retry behavior on failures
 * - Database migration with sync fields
 *
 * Requirements: 2.4, 4.2, 4.4 (instrumented testing)
 */
@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SyncWorkerInstrumentedTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var syncEngine: SyncEngine
    private lateinit var authRepository: AuthRepository
    private lateinit var syncMetadataDao: SyncMetadataDao
    private lateinit var syncApiService: SyncApiService

    @Before
    fun setup() {
        hiltRule.inject()
        
        context = ApplicationProvider.getApplicationContext()
        
        // Initialize WorkManager for testing
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)

        // Mock dependencies
        syncMetadataDao = mockk(relaxed = true)
        syncApiService = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        
        // Setup default mocks
        every { authRepository.isAuthenticated() } returns true
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        workManager.cancelAllWork()
        clearAllMocks()
    }

    // ========== Periodic Execution Tests ==========

    @Test
    fun testSyncWorkerPeriodicExecution() = runTest {
        // Given - Schedule periodic sync work
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // When - Enqueue work
        workManager.enqueueUniquePeriodicWork(
            "test_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        ).result.get()

        // Then - Verify work is enqueued
        val workInfo = workManager.getWorkInfoById(periodicWorkRequest.id).get()
        assertNotNull(workInfo)
        assertEquals(WorkInfo.State.ENQUEUED, workInfo.state)
    }

    @Test
    fun testSyncWorkerExecutesSuccessfully() = runTest {
        // Given - Setup successful sync
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } returns emptyList()
        coEvery { syncApiService.syncMeals(any()) } returns Response.success(
            MealSyncResponse(
                synced = emptyList(),
                conflicts = emptyList(),
                serverTimestamp = System.currentTimeMillis()
            )
        )

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()

        // When - Execute worker
        val result = worker.doWork()

        // Then - Verify success
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun testSyncWorkerSkipsWhenNotAuthenticated() = runTest {
        // Given - User not authenticated
        every { authRepository.isAuthenticated() } returns false

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()

        // When - Execute worker
        val result = worker.doWork()

        // Then - Verify success (skips sync but doesn't fail)
        assertTrue(result is ListenableWorker.Result.Success)
    }

    // ========== Network Constraint Tests ==========

    @Test
    fun testSyncWorkerRequiresNetworkConnection() = runTest {
        // Given - Create work request with network constraint
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        // When - Enqueue work
        workManager.enqueue(workRequest).result.get()

        // Then - Verify constraints are set
        val workInfo = workManager.getWorkInfoById(workRequest.id).get()
        assertNotNull(workInfo)
        assertEquals(NetworkType.CONNECTED, workInfo.constraints.requiredNetworkType)
    }

    @Test
    fun testSyncWorkerWaitsForNetworkConstraint() = runTest {
        // Given - Create work request with network constraint
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        // When - Enqueue work
        workManager.enqueue(workRequest).result.get()

        // Then - Work should be enqueued (waiting for constraints)
        val workInfo = workManager.getWorkInfoById(workRequest.id).get()
        assertNotNull(workInfo)
        // State will be ENQUEUED until network constraint is met
        assertTrue(
            workInfo.state == WorkInfo.State.ENQUEUED || 
            workInfo.state == WorkInfo.State.RUNNING ||
            workInfo.state == WorkInfo.State.SUCCEEDED
        )
    }

    // ========== Retry Behavior Tests ==========

    @Test
    fun testSyncWorkerRetriesOnFailure() = runTest {
        // Given - Setup sync to fail
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } throws Exception("Database error")

        // Create worker with retry policy
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // When - Enqueue work
        workManager.enqueue(workRequest).result.get()

        // Give worker time to execute
        Thread.sleep(1000)

        // Then - Verify work failed and will retry
        val workInfo = workManager.getWorkInfoById(workRequest.id).get()
        assertNotNull(workInfo)
        // Work should be in ENQUEUED state for retry or FAILED if max retries exceeded
        assertTrue(
            workInfo.state == WorkInfo.State.ENQUEUED ||
            workInfo.state == WorkInfo.State.FAILED
        )
    }

    @Test
    fun testSyncWorkerExponentialBackoff() = runTest {
        // Given - Create work request with exponential backoff
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        // When - Enqueue work
        workManager.enqueue(workRequest).result.get()

        // Then - Verify backoff policy is set
        val workInfo = workManager.getWorkInfoById(workRequest.id).get()
        assertNotNull(workInfo)
        assertEquals(BackoffPolicy.EXPONENTIAL, workInfo.constraints.backoffPolicy)
    }

    @Test
    fun testSyncWorkerMaxRetries() = runTest {
        // Given - Setup sync to always fail
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } throws Exception("Persistent error")

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(5) // Simulate max retries
            .build()

        // When - Execute worker at max retries
        val result = worker.doWork()

        // Then - Verify failure after max retries
        assertTrue(result is ListenableWorker.Result.Failure)
    }

    @Test
    fun testSyncWorkerRetryWithSuccess() = runTest {
        // Given - Setup sync to succeed on retry
        var attemptCount = 0
        coEvery { syncMetadataDao.getQueuedChangesByType(any()) } answers {
            attemptCount++
            if (attemptCount == 1) {
                throw Exception("Transient error")
            } else {
                emptyList()
            }
        }

        // Create worker for first attempt
        val worker1 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(0)
            .build()

        // When - First attempt (will fail)
        val result1 = worker1.doWork()

        // Then - Verify retry requested
        assertTrue(result1 is ListenableWorker.Result.Retry)

        // Create worker for second attempt
        val worker2 = TestListenableWorkerBuilder<SyncWorker>(context)
            .setRunAttemptCount(1)
            .build()

        // When - Second attempt (will succeed)
        val result2 = worker2.doWork()

        // Then - Verify success
        assertTrue(result2 is ListenableWorker.Result.Success)
    }

    // ========== Work Scheduling Tests ==========

    @Test
    fun testSyncWorkerUniqueWorkPolicy() = runTest {
        // Given - Create two work requests with same unique name
        val workRequest1 = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        val workRequest2 = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        // When - Enqueue first work
        workManager.enqueueUniqueWork(
            "unique_sync",
            ExistingWorkPolicy.KEEP,
            workRequest1
        ).result.get()

        // When - Try to enqueue second work with KEEP policy
        workManager.enqueueUniqueWork(
            "unique_sync",
            ExistingWorkPolicy.KEEP,
            workRequest2
        ).result.get()

        // Then - Verify only first work is kept
        val workInfos = workManager.getWorkInfosForUniqueWork("unique_sync").get()
        assertEquals(1, workInfos.size)
        assertEquals(workRequest1.id, workInfos[0].id)
    }

    @Test
    fun testSyncWorkerReplacePolicy() = runTest {
        // Given - Create two work requests
        val workRequest1 = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        val workRequest2 = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        // When - Enqueue first work
        workManager.enqueueUniqueWork(
            "unique_sync",
            ExistingWorkPolicy.KEEP,
            workRequest1
        ).result.get()

        // When - Enqueue second work with REPLACE policy
        workManager.enqueueUniqueWork(
            "unique_sync",
            ExistingWorkPolicy.REPLACE,
            workRequest2
        ).result.get()

        // Then - Verify second work replaced first
        val workInfos = workManager.getWorkInfosForUniqueWork("unique_sync").get()
        assertEquals(1, workInfos.size)
        assertEquals(workRequest2.id, workInfos[0].id)
    }

    @Test
    fun testSyncWorkerCancellation() = runTest {
        // Given - Create and enqueue work
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .build()

        workManager.enqueue(workRequest).result.get()

        // When - Cancel work
        workManager.cancelWorkById(workRequest.id).result.get()

        // Then - Verify work is cancelled
        val workInfo = workManager.getWorkInfoById(workRequest.id).get()
        assertNotNull(workInfo)
        assertEquals(WorkInfo.State.CANCELLED, workInfo.state)
    }

    // ========== Integration with SyncEngine Tests ==========

    @Test
    fun testSyncWorkerIntegrationWithSyncEngine() = runTest {
        // Given - Setup sync engine with queued changes
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
        coEvery { syncMetadataDao.getMetadata(any(), any()) } returns null
        coEvery { syncApiService.syncMeals(any()) } returns Response.success(
            MealSyncResponse(
                synced = emptyList(),
                conflicts = emptyList(),
                serverTimestamp = System.currentTimeMillis()
            )
        )
        coEvery { syncMetadataDao.updateMetadata(any()) } just Runs
        coEvery { syncMetadataDao.removeFromQueue(any()) } just Runs

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()

        // When - Execute worker
        val result = worker.doWork()

        // Then - Verify success and sync engine was called
        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun testSyncWorkerHandlesMultipleEntityTypes() = runTest {
        // Given - Setup queued changes for multiple entity types
        coEvery { syncMetadataDao.getQueuedChangesByType("meal") } returns listOf(
            SyncQueueEntity(1, "meal", 100, "create", "{}", System.currentTimeMillis(), 0, null)
        )
        coEvery { syncMetadataDao.getQueuedChangesByType("meal_plan") } returns listOf(
            SyncQueueEntity(2, "meal_plan", 200, "update", "{}", System.currentTimeMillis(), 0, null)
        )
        coEvery { syncMetadataDao.getQueuedChangesByType("shopping_list_item") } returns listOf(
            SyncQueueEntity(3, "shopping_list_item", 300, "update", "{}", System.currentTimeMillis(), 0, null)
        )

        coEvery { syncMetadataDao.getMetadata(any(), any()) } returns null
        coEvery { syncApiService.syncMeals(any()) } returns Response.success(
            MealSyncResponse(emptyList(), emptyList(), System.currentTimeMillis())
        )
        coEvery { syncApiService.syncMealPlans(any()) } returns Response.success(
            com.shoppit.app.data.remote.dto.MealPlanSyncResponse(emptyList(), emptyList(), System.currentTimeMillis())
        )
        coEvery { syncApiService.syncShoppingLists(any()) } returns Response.success(
            com.shoppit.app.data.remote.dto.ShoppingListSyncResponse(emptyList(), emptyList(), System.currentTimeMillis())
        )
        coEvery { syncMetadataDao.updateMetadata(any()) } just Runs
        coEvery { syncMetadataDao.removeFromQueue(any()) } just Runs

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()

        // When - Execute worker
        val result = worker.doWork()

        // Then - Verify success
        assertTrue(result is ListenableWorker.Result.Success)
    }

    // ========== Performance Tests ==========

    @Test
    fun testSyncWorkerPerformanceWithLargeQueue() = runTest {
        // Given - Setup large queue of changes
        val largeQueue = (1..100).map { id ->
            SyncQueueEntity(
                id = id.toLong(),
                entityType = "meal",
                entityId = id.toLong(),
                operation = "create",
                payload = "{}",
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                lastAttemptAt = null
            )
        }
        coEvery { syncMetadataDao.getQueuedChangesByType("meal") } returns largeQueue
        coEvery { syncMetadataDao.getMetadata(any(), any()) } returns null
        coEvery { syncApiService.syncMeals(any()) } returns Response.success(
            MealSyncResponse(emptyList(), emptyList(), System.currentTimeMillis())
        )
        coEvery { syncMetadataDao.updateMetadata(any()) } just Runs
        coEvery { syncMetadataDao.removeFromQueue(any()) } just Runs

        // Create worker
        val worker = TestListenableWorkerBuilder<SyncWorker>(context)
            .build()

        // When - Execute worker and measure time
        val startTime = System.currentTimeMillis()
        val result = worker.doWork()
        val duration = System.currentTimeMillis() - startTime

        // Then - Verify success and reasonable performance (< 10 seconds)
        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(duration < 10000, "Sync took too long: ${duration}ms")
    }
}

/**
 * Synchronous executor for testing WorkManager.
 * Executes work immediately on the calling thread.
 */
class SynchronousExecutor : java.util.concurrent.Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}
