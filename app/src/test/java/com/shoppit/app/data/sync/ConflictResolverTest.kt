package com.shoppit.app.data.sync

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ConflictResolver.
 *
 * Tests the Last-Write-Wins conflict resolution strategy including:
 * - Local version wins when newer
 * - Remote version wins when newer
 * - Server version preferred when timestamps are equal
 * - Conflict detection logic
 * - Batch conflict resolution
 */
class ConflictResolverTest {
    
    private lateinit var conflictResolver: ConflictResolver
    
    @Before
    fun setup() {
        conflictResolver = ConflictResolver()
    }
    
    // ========== Single Conflict Resolution Tests ==========
    
    @Test
    fun `resolve returns UseLocal when local version is newer`() {
        // Given
        val localMeal = createMeal(id = 1, updatedAt = 2000L)
        val remoteMeal = createMeal(id = 1, updatedAt = 1000L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val resolution = conflictResolver.resolve(local, remote)
        
        // Then
        assertTrue(resolution is ConflictResolution.UseLocal)
        assertEquals(local, (resolution as ConflictResolution.UseLocal).entity)
    }
    
    @Test
    fun `resolve returns UseRemote when remote version is newer`() {
        // Given
        val localMeal = createMeal(id = 1, updatedAt = 1000L)
        val remoteMeal = createMeal(id = 1, updatedAt = 2000L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val resolution = conflictResolver.resolve(local, remote)
        
        // Then
        assertTrue(resolution is ConflictResolution.UseRemote)
        assertEquals(remote, (resolution as ConflictResolution.UseRemote).entity)
    }
    
    @Test
    fun `resolve returns UseRemote when timestamps are equal - server preference`() {
        // Given - identical timestamps
        val timestamp = 1000L
        val localMeal = createMeal(id = 1, updatedAt = timestamp)
        val remoteMeal = createMeal(id = 1, updatedAt = timestamp)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val resolution = conflictResolver.resolve(local, remote)
        
        // Then - server version should be preferred
        assertTrue(resolution is ConflictResolution.UseRemote)
        assertEquals(remote, (resolution as ConflictResolution.UseRemote).entity)
    }
    
    @Test
    fun `resolve handles large timestamp differences`() {
        // Given - very large timestamp difference
        val localMeal = createMeal(id = 1, updatedAt = Long.MAX_VALUE)
        val remoteMeal = createMeal(id = 1, updatedAt = 0L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val resolution = conflictResolver.resolve(local, remote)
        
        // Then
        assertTrue(resolution is ConflictResolution.UseLocal)
    }
    
    // ========== Conflict Detection Tests ==========
    
    @Test
    fun `hasConflict returns true when both versions modified after last sync`() {
        // Given
        val lastSyncTime = 1000L
        val localMeal = createMeal(id = 1, updatedAt = 1500L)
        val remoteMeal = createMeal(id = 1, updatedAt = 1600L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)
        
        // Then
        assertTrue(hasConflict)
    }
    
    @Test
    fun `hasConflict returns false when only local version modified`() {
        // Given
        val lastSyncTime = 1000L
        val localMeal = createMeal(id = 1, updatedAt = 1500L)
        val remoteMeal = createMeal(id = 1, updatedAt = 900L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)
        
        // Then
        assertFalse(hasConflict)
    }
    
    @Test
    fun `hasConflict returns false when only remote version modified`() {
        // Given
        val lastSyncTime = 1000L
        val localMeal = createMeal(id = 1, updatedAt = 900L)
        val remoteMeal = createMeal(id = 1, updatedAt = 1500L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)
        
        // Then
        assertFalse(hasConflict)
    }
    
    @Test
    fun `hasConflict returns false when neither version modified`() {
        // Given
        val lastSyncTime = 1000L
        val localMeal = createMeal(id = 1, updatedAt = 900L)
        val remoteMeal = createMeal(id = 1, updatedAt = 800L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, lastSyncTime)
        
        // Then
        assertFalse(hasConflict)
    }
    
    @Test
    fun `hasConflict returns false when lastSyncTime is null`() {
        // Given - first sync, no previous sync time
        val localMeal = createMeal(id = 1, updatedAt = 1500L)
        val remoteMeal = createMeal(id = 1, updatedAt = 1600L)
        
        val local = SyncableMeal.from(localMeal, serverId = "server-1")
        val remote = SyncableMeal.from(remoteMeal, serverId = "server-1")
        
        // When
        val hasConflict = conflictResolver.hasConflict(local, remote, null)
        
        // Then - no conflict on first sync
        assertFalse(hasConflict)
    }
    
    // ========== Batch Resolution Tests ==========
    
    @Test
    fun `resolveBatch resolves multiple conflicts correctly`() {
        // Given - 3 conflicts with different outcomes
        val conflicts = listOf(
            // Local wins
            SyncableMeal.from(createMeal(id = 1, updatedAt = 2000L), "server-1") to
                    SyncableMeal.from(createMeal(id = 1, updatedAt = 1000L), "server-1"),
            // Remote wins
            SyncableMeal.from(createMeal(id = 2, updatedAt = 1000L), "server-2") to
                    SyncableMeal.from(createMeal(id = 2, updatedAt = 2000L), "server-2"),
            // Equal timestamps - server wins
            SyncableMeal.from(createMeal(id = 3, updatedAt = 1500L), "server-3") to
                    SyncableMeal.from(createMeal(id = 3, updatedAt = 1500L), "server-3")
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
        val conflicts = emptyList<Pair<SyncableMeal, SyncableMeal>>()
        
        // When
        val resolutions = conflictResolver.resolveBatch(conflicts)
        
        // Then
        assertTrue(resolutions.isEmpty())
    }
    
    @Test
    fun `resolveBatch handles single conflict`() {
        // Given
        val conflicts = listOf(
            SyncableMeal.from(createMeal(id = 1, updatedAt = 2000L), "server-1") to
                    SyncableMeal.from(createMeal(id = 1, updatedAt = 1000L), "server-1")
        )
        
        // When
        val resolutions = conflictResolver.resolveBatch(conflicts)
        
        // Then
        assertEquals(1, resolutions.size)
        assertTrue(resolutions[1L] is ConflictResolution.UseLocal)
    }
    
    // ========== Helper Methods ==========
    
    private fun createMeal(
        id: Long,
        name: String = "Test Meal",
        updatedAt: Long
    ): Meal {
        return Meal(
            id = id,
            name = name,
            ingredients = listOf(
                Ingredient(name = "Test Ingredient", quantity = "1", unit = "cup")
            ),
            notes = "Test notes",
            tags = emptySet(),
            createdAt = updatedAt - 1000, // Created before updated
            updatedAt = updatedAt
        )
    }
}
