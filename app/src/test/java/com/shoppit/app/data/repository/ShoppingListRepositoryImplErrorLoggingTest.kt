package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.ShoppingListDao
import com.shoppit.app.data.local.entity.ShoppingListItemEntity
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.SyncEngine
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for error logging in ShoppingListRepositoryImpl.
 * Tests verify that ErrorLogger is called when exceptions occur.
 */
@ExperimentalCoroutinesApi
class ShoppingListRepositoryImplErrorLoggingTest {

    private lateinit var shoppingListDao: ShoppingListDao
    private lateinit var syncEngine: SyncEngine
    private lateinit var errorLogger: ErrorLogger
    private lateinit var repository: ShoppingListRepositoryImpl

    @Before
    fun setup() {
        shoppingListDao = mockk()
        syncEngine = mockk(relaxed = true)
        errorLogger = mockk(relaxed = true)
        
        repository = ShoppingListRepositoryImpl(
            shoppingListDao,
            syncEngine,
            errorLogger
        )
    }

    @Test
    fun `getShoppingList logs error when query fails`() = runTest {
        // Given
        val exception = RuntimeException("Query failed")
        every { shoppingListDao.getAllItems() } returns flowOf<List<ShoppingListItemEntity>>().apply {
            throw exception
        }

        // When
        try {
            repository.getShoppingList().first()
        } catch (e: Exception) {
            // Expected
        }

        // Then
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.getShoppingList") }
    }

    @Test
    fun `getShoppingListItem logs error with item ID`() = runTest {
        // Given
        val itemId = 123L
        val exception = RuntimeException("Query failed")
        every { shoppingListDao.getItemById(itemId) } returns flowOf<ShoppingListItemEntity?>().apply {
            throw exception
        }

        // When
        try {
            repository.getShoppingListItem(itemId).first()
        } catch (e: Exception) {
            // Expected
        }

        // Then
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.getShoppingListItem", 
                mapOf("itemId" to itemId)
            ) 
        }
    }

    @Test
    fun `addShoppingListItem logs error with item name`() = runTest {
        // Given
        val item = ShoppingListItem(
            id = 0,
            name = "Test Item",
            quantity = "1",
            unit = "pcs",
            category = ItemCategory.OTHER,
            isChecked = false,
            isManual = true
        )
        coEvery { shoppingListDao.insertItem(any()) } throws RuntimeException("Insert failed")

        // When
        val result = repository.addShoppingListItem(item)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.addShoppingListItem", 
                mapOf("itemName" to item.name)
            ) 
        }
    }

    @Test
    fun `addShoppingListItems logs error with item count`() = runTest {
        // Given
        val items = listOf(
            ShoppingListItem(id = 0, name = "Item 1", quantity = "1", unit = "pcs", category = ItemCategory.OTHER, isChecked = false, isManual = true),
            ShoppingListItem(id = 0, name = "Item 2", quantity = "2", unit = "kg", category = ItemCategory.PRODUCE, isChecked = false, isManual = true)
        )
        coEvery { shoppingListDao.insertItems(any()) } throws RuntimeException("Batch insert failed")

        // When
        val result = repository.addShoppingListItems(items)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.addShoppingListItems", 
                mapOf("itemCount" to items.size)
            ) 
        }
    }

    @Test
    fun `updateShoppingListItem logs error with item context`() = runTest {
        // Given
        val item = ShoppingListItem(
            id = 1,
            name = "Updated Item",
            quantity = "3",
            unit = "pcs",
            category = ItemCategory.OTHER,
            isChecked = false,
            isManual = true
        )
        coEvery { shoppingListDao.updateItem(any()) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateShoppingListItem(item)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.updateShoppingListItem", 
                mapOf("itemId" to item.id, "itemName" to item.name)
            ) 
        }
    }

    @Test
    fun `deleteShoppingListItem logs error with item ID`() = runTest {
        // Given
        val itemId = 456L
        coEvery { shoppingListDao.deleteItemById(itemId) } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteShoppingListItem(itemId)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.deleteShoppingListItem", 
                mapOf("itemId" to itemId)
            ) 
        }
    }

    @Test
    fun `deleteCheckedItems logs error`() = runTest {
        // Given
        coEvery { shoppingListDao.deleteCheckedItems() } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteCheckedItems()

        // Then
        assertTrue(result.isFailure)
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.deleteCheckedItems") }
    }

    @Test
    fun `deleteAutoGeneratedItems logs error`() = runTest {
        // Given
        coEvery { shoppingListDao.deleteAutoGeneratedItems() } throws RuntimeException("Delete failed")

        // When
        val result = repository.deleteAutoGeneratedItems()

        // Then
        assertTrue(result.isFailure)
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.deleteAutoGeneratedItems") }
    }

    @Test
    fun `uncheckAllItems logs error`() = runTest {
        // Given
        coEvery { shoppingListDao.uncheckAllItems() } throws RuntimeException("Update failed")

        // When
        val result = repository.uncheckAllItems()

        // Then
        assertTrue(result.isFailure)
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.uncheckAllItems") }
    }

    @Test
    fun `updateItemNotes logs error with item ID`() = runTest {
        // Given
        val itemId = 789L
        val notes = "Test notes"
        coEvery { shoppingListDao.updateItemNotes(itemId, notes) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateItemNotes(itemId, notes)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.updateItemNotes", 
                mapOf("itemId" to itemId)
            ) 
        }
    }

    @Test
    fun `toggleItemPriority logs error with context`() = runTest {
        // Given
        val itemId = 101L
        val isPriority = true
        coEvery { shoppingListDao.updateItemPriority(itemId, isPriority) } throws RuntimeException("Update failed")

        // When
        val result = repository.toggleItemPriority(itemId, isPriority)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.toggleItemPriority", 
                mapOf("itemId" to itemId, "isPriority" to isPriority)
            ) 
        }
    }

    @Test
    fun `updateItemOrder logs error with context`() = runTest {
        // Given
        val itemId = 202L
        val newOrder = 5
        coEvery { shoppingListDao.updateItemOrder(itemId, newOrder) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateItemOrder(itemId, newOrder)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.updateItemOrder", 
                mapOf("itemId" to itemId, "newOrder" to newOrder)
            ) 
        }
    }

    @Test
    fun `updateItemPrice logs error with context`() = runTest {
        // Given
        val itemId = 303L
        val price = 9.99
        coEvery { shoppingListDao.updateItemPrice(itemId, price) } throws RuntimeException("Update failed")

        // When
        val result = repository.updateItemPrice(itemId, price)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.updateItemPrice", 
                mapOf("itemId" to itemId, "price" to price)
            ) 
        }
    }

    @Test
    fun `moveItemToSection logs error with context`() = runTest {
        // Given
        val itemId = 404L
        val section = "Produce"
        coEvery { shoppingListDao.updateItemSection(itemId, section) } throws RuntimeException("Update failed")

        // When
        val result = repository.moveItemToSection(itemId, section)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.moveItemToSection", 
                mapOf("itemId" to itemId, "section" to section)
            ) 
        }
    }

    @Test
    fun `duplicateItem logs error with item ID`() = runTest {
        // Given
        val itemId = 505L
        coEvery { shoppingListDao.getItemById(itemId) } returns flowOf(null)

        // When
        val result = repository.duplicateItem(itemId)

        // Then
        assertTrue(result.isFailure)
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.duplicateItem", 
                mapOf("itemId" to itemId)
            ) 
        }
    }

    @Test
    fun `getItemsBySection logs error with section`() = runTest {
        // Given
        val section = "Dairy"
        val exception = RuntimeException("Query failed")
        every { shoppingListDao.getItemsBySection(section) } returns flowOf<List<ShoppingListItemEntity>>().apply {
            throw exception
        }

        // When
        try {
            repository.getItemsBySection(section).first()
        } catch (e: Exception) {
            // Expected
        }

        // Then
        verify { 
            errorLogger.logError(
                any(), 
                "ShoppingListRepositoryImpl.getItemsBySection", 
                mapOf("section" to section)
            ) 
        }
    }

    @Test
    fun `getPriorityItems logs error`() = runTest {
        // Given
        val exception = RuntimeException("Query failed")
        every { shoppingListDao.getPriorityItems() } returns flowOf<List<ShoppingListItemEntity>>().apply {
            throw exception
        }

        // When
        try {
            repository.getPriorityItems().first()
        } catch (e: Exception) {
            // Expected
        }

        // Then
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.getPriorityItems") }
    }

    @Test
    fun `getBudgetSummary logs error`() = runTest {
        // Given
        coEvery { shoppingListDao.getTotalEstimatedPrice() } throws RuntimeException("Query failed")

        // When
        val result = repository.getBudgetSummary()

        // Then
        assertTrue(result.isFailure)
        verify { errorLogger.logError(any(), "ShoppingListRepositoryImpl.getBudgetSummary") }
    }

    @Test
    fun `error includes cause parameter in PersistenceError`() = runTest {
        // Given
        val item = ShoppingListItem(
            id = 0,
            name = "Test Item",
            quantity = "1",
            unit = "pcs",
            category = ItemCategory.OTHER,
            isChecked = false,
            isManual = true
        )
        val cause = RuntimeException("Root cause")
        coEvery { shoppingListDao.insertItem(any()) } throws cause

        // When
        val result = repository.addShoppingListItem(item)

        // Then
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as PersistenceError.WriteFailed
        assertTrue(error.cause == cause)
    }
}
