package com.shoppit.app.data.sync

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for ConflictResolver.
 *
 * Tests cover:
 * - Last-Write-Wins resolution logic
 * - Timestamp comparison (local newer, remote newer, equal)
 * - Conflict detection
 * - Batch conflict resolution
 * - Conflict logging
 */
class ConflictResolverTest {

    private lateinit var conflictResolver: ConflictResolver

    @Before
    fun setup() {
        conflictResolver = ConflictResolver()
    }

    // ========== Last-Write-Wins Resolution Tests ==========

    @Test
    fun `resolve returns UseLocal when local timestamp is newer`() {
        // Given
        val localTimestamp = System.currentTimeMillis()
        val remoteTimestamp = localTimestamp - 1000 // 1 second older

        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = localTimestamp
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = remoteTimestamp
        )

        // When
        val resolution = conflictResolver.resolve(local, remote)

        // Then
        assertTrue(resolution is ConflictResolution.UseLocal)
        assertEquals(local, (resolution as ConflictResolution.UseLocal).entity)
    }

    @Test
    fun `resolve returns UseRemote when remote timestamp is newer`() {
        // Given
        val remoteTimestamp = System.currentTimeMillis()
        val localTimestamp = remoteTimestamp - 1000 // 1 second older

        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = localTimestamp
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = remoteTimestamp
        )

        // When
        val resolution = conflictResolver.resolve(local, remote)

        // Then
        assertTrue(resolution is ConflictResolution.UseRemote)
        assertEquals(remote, (resolution as ConflictResolution.UseRemote).entity)
    }

    @Test
    fun `resolve returns UseRemote when timestamps are equal`() {
        // Given
        val timestamp = System.currentTimeMillis()

        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = timestamp
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = timestamp
        )

        // When
        val resolution = conflictResolver.resolve(local, remote)

        // Then
        assertTrue(resolution is ConflictResolution.UseRemote)
        assertEquals(remote, (resolution as ConflictResolution.UseRemote).entity)
    }

    @Test
    fun `resolve handles large timestamp differences`() {
        // Given
        val localTimestamp = System.currentTimeMillis()
        val remoteTimestamp = localTimestamp - 86400000 // 1 day older

        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = localTimestamp
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = remoteTimestamp
        )

        // When
        val resolution = conflictResolver.resolve(local, remote)

        // Then
        assertTrue(resolution is ConflictResolution.UseLocal)
    }

    @Test
    fun `resolve handles entities with null serverId`() {
        // Given
        val localTimestamp = System.currentTimeMillis()
        val remoteTimestamp = localTimestamp - 1000

        val local = TestSyncableEntity(
            id = 1,
            serverId = null,
            updatedAt = localTimestamp
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = remoteTimestamp
        )

        // When
        val resolution = conflictResolver.resolve(local, remote)

        // Then
        assertTrue(resolution is ConflictResolution.UseLocal)
    }

    // ========== Batch Resolution Tests ==========

    @Test
    fun `resolveBatch resolves multiple conflicts correctly`() {
        // Given
        val now = System.currentTimeMillis()
        
        val conflicts = listOf(
            // Conflict 1: Local wins
            TestSyncableEntity(1, "s1", now) to TestSyncableEntity(1, "s1", now - 1000),
            // Conflict 2: Remote wins
            TestSyncableEntity(2, "s2", now - 1000) to TestSyncableEntity(2, "s2", now),
            // Conflict 3: Equal timestamps, remote wins
            TestSyncableEntity(3, "s3", now) to TestSyncableEntity(3, "s3", now)
        )

        // When
        val resolutions = conflictResolver.resolveBatch(conflicts)

        // Then
        assertEquals(3, resolutions.size)
        assertTrue(resolutions[1L] is ConflictResolution.UseLocal)
        assertTrue(resolutions[2L] is ConflictResolution.UseRemote)
        assertTrue(resolutions[3L] is ConflictResolution.UseRemote)
    }

    @Test
    fun `resolveBatch handles empty list`() {
        // Given
        val conflicts = emptyList<Pair<TestSyncableEntity, TestSyncableEntity>>()

        // When
        val resolutions = conflictResolver.resolveBatch(conflicts)

        // Then
        assertTrue(resolutions.isEmpty())
    }

    @Test
    fun `resolveBatch handles single conflict`() {
        // Given
        val now = System.currentTimeMillis()
        val conflicts = listOf(
            TestSyncableEntity(1, "s1", now) to TestSyncableEntity(1, "s1", now - 1000)
        )

        // When
        val resolutions = conflictResolver.resolveBatch(conflicts)

        // Then
        assertEquals(1, resolutions.size)
        assertTrue(resolutions[1L] is ConflictResolution.UseLocal)
    }

    // ========== Conflict Detection Tests ==========

    @Test
    fun `hasConflict returns true when both versions modified after last sync`() {
        // Given
        val lastSyncTime = System.currentTimeMillis() - 10000 // 10 seconds ago
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime + 5000 // Modified 5 seconds ago
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime + 3000 // Modified 7 seconds ago
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)

        // Then
        assertTrue(hasConflict)
    }

    @Test
    fun `hasConflict returns false when only local modified`() {
        // Given
        val lastSyncTime = System.currentTimeMillis() - 10000
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime + 5000 // Modified after sync
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime - 1000 // Modified before sync
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)

        // Then
        assertFalse(hasConflict)
    }

    @Test
    fun `hasConflict returns false when only remote modified`() {
        // Given
        val lastSyncTime = System.currentTimeMillis() - 10000
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime - 1000 // Modified before sync
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime + 5000 // Modified after sync
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)

        // Then
        assertFalse(hasConflict)
    }

    @Test
    fun `hasConflict returns false when neither modified after sync`() {
        // Given
        val lastSyncTime = System.currentTimeMillis() - 10000
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime - 5000
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime - 3000
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)

        // Then
        assertFalse(hasConflict)
    }

    @Test
    fun `hasConflict returns false when lastSyncTime is null`() {
        // Given
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = System.currentTimeMillis()
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = System.currentTimeMillis()
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, null)

        // Then
        assertFalse(hasConflict)
    }

    @Test
    fun `hasConflict handles edge case where timestamps equal lastSyncTime`() {
        // Given
        val lastSyncTime = System.currentTimeMillis()
        val local = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime // Exactly at sync time
        )
        val remote = TestSyncableEntity(
            id = 1,
            serverId = "server_1",
            updatedAt = lastSyncTime // Exactly at sync time
        )

        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)

        // Then
        assertFalse(hasConflict) // Not modified AFTER sync
    }

    // ========== Test Helper Class ==========

    /**
     * Test implementation of SyncableEntity for testing purposes.
     */
    private data class TestSyncableEntity(
        override val id: Long,
        override val serverId: String?,
        override val updatedAt: Long
    ) : SyncableEntity
}
