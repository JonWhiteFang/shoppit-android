package com.shoppit.app.data.cache

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LruCacheManagerTest {
    
    private lateinit var cacheManager: LruCacheManager<String, String>
    
    @Before
    fun setup() {
        cacheManager = LruCacheManager(maxSize = 3, defaultTtl = 60000L)
    }
    
    @Test
    fun `put and get returns cached value`() {
        // Given
        val key = "key1"
        val value = "value1"
        
        // When
        cacheManager.put(key, value)
        val result = cacheManager.get(key)
        
        // Then
        assertEquals(value, result)
    }
    
    @Test
    fun `get returns null for non-existent key`() {
        // When
        val result = cacheManager.get("nonexistent")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `evicts least recently used entry when cache is full`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")
        
        // When - add fourth item, should evict key1
        cacheManager.put("key4", "value4")
        
        // Then
        assertNull(cacheManager.get("key1"))
        assertEquals("value2", cacheManager.get("key2"))
        assertEquals("value3", cacheManager.get("key3"))
        assertEquals("value4", cacheManager.get("key4"))
    }
    
    @Test
    fun `accessing entry updates LRU order`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")
        
        // When - access key1 to make it most recently used
        cacheManager.get("key1")
        
        // Then - add fourth item, should evict key2 (not key1)
        cacheManager.put("key4", "value4")
        
        assertEquals("value1", cacheManager.get("key1"))
        assertNull(cacheManager.get("key2"))
        assertEquals("value3", cacheManager.get("key3"))
        assertEquals("value4", cacheManager.get("key4"))
    }
    
    @Test
    fun `invalidate removes specific entry`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        
        // When
        cacheManager.invalidate("key1")
        
        // Then
        assertNull(cacheManager.get("key1"))
        assertEquals("value2", cacheManager.get("key2"))
    }
    
    @Test
    fun `invalidateAll removes all entries`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        cacheManager.put("key3", "value3")
        
        // When
        cacheManager.invalidateAll()
        
        // Then
        assertNull(cacheManager.get("key1"))
        assertNull(cacheManager.get("key2"))
        assertNull(cacheManager.get("key3"))
        assertEquals(0, cacheManager.size())
    }
    
    @Test
    fun `size returns correct number of entries`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        
        // Then
        assertEquals(2, cacheManager.size())
    }
    
    @Test
    fun `clear removes all entries`() {
        // Given
        cacheManager.put("key1", "value1")
        cacheManager.put("key2", "value2")
        
        // When
        cacheManager.clear()
        
        // Then
        assertEquals(0, cacheManager.size())
        assertNull(cacheManager.get("key1"))
        assertNull(cacheManager.get("key2"))
    }
    
    @Test
    fun `put updates existing entry`() {
        // Given
        cacheManager.put("key1", "value1")
        
        // When
        cacheManager.put("key1", "value2")
        
        // Then
        assertEquals("value2", cacheManager.get("key1"))
        assertEquals(1, cacheManager.size())
    }
    
    @Test
    fun `eviction callback is called when entry is evicted`() {
        // Given
        var evictedKey: String? = null
        var evictedValue: String? = null
        val cacheWithCallback = LruCacheManager<String, String>(
            maxSize = 2,
            defaultTtl = 60000L,
            onEviction = { key, value ->
                evictedKey = key
                evictedValue = value
            }
        )
        
        cacheWithCallback.put("key1", "value1")
        cacheWithCallback.put("key2", "value2")
        
        // When - add third item, should evict key1
        cacheWithCallback.put("key3", "value3")
        
        // Then
        assertEquals("key1", evictedKey)
        assertEquals("value1", evictedValue)
    }
    
    @Test
    fun `handles null values correctly`() {
        // Given
        val cacheWithNullable = LruCacheManager<String, String?>(maxSize = 3, defaultTtl = 60000L)
        
        // When
        cacheWithNullable.put("key1", null)
        
        // Then
        assertNull(cacheWithNullable.get("key1"))
        assertEquals(1, cacheWithNullable.size())
    }
}
