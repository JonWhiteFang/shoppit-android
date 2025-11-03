package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.ShoppingListDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.error.ErrorLogger
import com.shoppit.app.domain.model.BudgetSummary
import com.shoppit.app.domain.model.EntityType
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.model.SyncOperation
import com.shoppit.app.domain.repository.ShoppingListRepository
import com.shoppit.app.domain.repository.SyncEngine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ShoppingListRepositoryImpl @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val syncEngine: SyncEngine,
    private val errorLogger: ErrorLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ShoppingListRepository {
    
    override fun getShoppingList(): Flow<Result<List<ShoppingListItem>>> {
        return shoppingListDao.getAllItems()
            .map { entities -> 
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> 
                errorLogger.logError(e, "ShoppingListRepositoryImpl.getShoppingList")
                emit(Result.failure(PersistenceError.QueryFailed("getAllItems", e)))
            }
    }
    
    override fun getShoppingListItem(id: Long): Flow<Result<ShoppingListItem>> {
        return shoppingListDao.getItemById(id)
            .map { entity ->
                entity?.let { Result.success(it.toDomainModel()) }
                    ?: Result.failure(PersistenceError.QueryFailed(
                        "getItemById",
                        IllegalStateException("Shopping list item not found")
                    ))
            }
            .catch { e ->
                errorLogger.logError(e, "ShoppingListRepositoryImpl.getShoppingListItem", mapOf("itemId" to id))
                emit(Result.failure(PersistenceError.QueryFailed("getItemById", e)))
            }
    }
    
    override suspend fun addShoppingListItem(item: ShoppingListItem): Result<Long> {
        return try {
            val id = shoppingListDao.insertItem(item.toEntity())
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, id, SyncOperation.CREATE)
            
            Result.success(id)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.addShoppingListItem", mapOf("itemName" to item.name))
            Result.failure(PersistenceError.WriteFailed("insertItem", e))
        }
    }
    
    override suspend fun addShoppingListItems(items: List<ShoppingListItem>): Result<List<Long>> {
        return try {
            val ids = shoppingListDao.insertItems(items.map { it.toEntity() })
            
            // Queue changes for sync
            ids.forEach { id ->
                syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, id, SyncOperation.CREATE)
            }
            
            Result.success(ids)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.addShoppingListItems", mapOf("itemCount" to items.size))
            Result.failure(PersistenceError.WriteFailed("insertItems", e))
        }
    }
    
    override suspend fun updateShoppingListItem(item: ShoppingListItem): Result<Unit> {
        return try {
            shoppingListDao.updateItem(item.toEntity())
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, item.id, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.updateShoppingListItem", mapOf("itemId" to item.id, "itemName" to item.name))
            Result.failure(PersistenceError.WriteFailed("updateItem", e))
        }
    }
    
    override suspend fun deleteShoppingListItem(id: Long): Result<Unit> {
        return try {
            shoppingListDao.deleteItemById(id)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, id, SyncOperation.DELETE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.deleteShoppingListItem", mapOf("itemId" to id))
            Result.failure(PersistenceError.WriteFailed("deleteItemById", e))
        }
    }
    
    override suspend fun deleteCheckedItems(): Result<Unit> {
        return try {
            shoppingListDao.deleteCheckedItems()
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.deleteCheckedItems")
            Result.failure(PersistenceError.WriteFailed("deleteCheckedItems", e))
        }
    }
    
    override suspend fun deleteAutoGeneratedItems(): Result<Unit> {
        return try {
            shoppingListDao.deleteAutoGeneratedItems()
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.deleteAutoGeneratedItems")
            Result.failure(PersistenceError.WriteFailed("deleteAutoGeneratedItems", e))
        }
    }
    
    override suspend fun uncheckAllItems(): Result<Unit> {
        return try {
            shoppingListDao.uncheckAllItems()
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.uncheckAllItems")
            Result.failure(PersistenceError.WriteFailed("uncheckAllItems", e))
        }
    }
    
    // Management features
    
    override suspend fun updateItemNotes(itemId: Long, notes: String): Result<Unit> {
        return try {
            shoppingListDao.updateItemNotes(itemId, notes)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, itemId, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.updateItemNotes", mapOf("itemId" to itemId))
            Result.failure(PersistenceError.WriteFailed("updateItemNotes", e))
        }
    }
    
    override suspend fun toggleItemPriority(itemId: Long, isPriority: Boolean): Result<Unit> {
        return try {
            shoppingListDao.updateItemPriority(itemId, isPriority)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, itemId, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.toggleItemPriority", mapOf("itemId" to itemId, "isPriority" to isPriority))
            Result.failure(PersistenceError.WriteFailed("toggleItemPriority", e))
        }
    }
    
    override suspend fun updateItemOrder(itemId: Long, newOrder: Int): Result<Unit> {
        return try {
            shoppingListDao.updateItemOrder(itemId, newOrder)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, itemId, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.updateItemOrder", mapOf("itemId" to itemId, "newOrder" to newOrder))
            Result.failure(PersistenceError.WriteFailed("updateItemOrder", e))
        }
    }
    
    override suspend fun updateItemPrice(itemId: Long, price: Double?): Result<Unit> {
        return try {
            shoppingListDao.updateItemPrice(itemId, price)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, itemId, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.updateItemPrice", mapOf("itemId" to itemId as Any, "price" to (price as Any? ?: "null")))
            Result.failure(PersistenceError.WriteFailed("updateItemPrice", e))
        }
    }
    
    override suspend fun moveItemToSection(itemId: Long, section: String): Result<Unit> {
        return try {
            shoppingListDao.updateItemSection(itemId, section)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, itemId, SyncOperation.UPDATE)
            
            Result.success(Unit)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.moveItemToSection", mapOf("itemId" to itemId, "section" to section))
            Result.failure(PersistenceError.WriteFailed("moveItemToSection", e))
        }
    }
    
    override suspend fun duplicateItem(itemId: Long): Result<Long> {
        return try {
            // Get the original item
            val originalItem = shoppingListDao.getItemById(itemId).first()
                ?: return Result.failure(
                    PersistenceError.QueryFailed(
                        "duplicateItem",
                        IllegalStateException("Item not found")
                    )
                )
            
            // Create a duplicate with modified name and reset properties
            val duplicateItem = originalItem.copy(
                id = 0, // Auto-generate new ID
                name = "${originalItem.name} (copy)",
                isManual = true,
                isChecked = false,
                createdAt = System.currentTimeMillis(),
                lastModifiedAt = System.currentTimeMillis()
            )
            
            val newId = shoppingListDao.insertItem(duplicateItem)
            
            // Queue change for sync
            syncEngine.queueChange(EntityType.SHOPPING_LIST_ITEM, newId, SyncOperation.CREATE)
            
            Result.success(newId)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.duplicateItem", mapOf("itemId" to itemId))
            Result.failure(PersistenceError.WriteFailed("duplicateItem", e))
        }
    }
    
    override fun getItemsBySection(section: String): Flow<Result<List<ShoppingListItem>>> {
        return shoppingListDao.getItemsBySection(section)
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                errorLogger.logError(e, "ShoppingListRepositoryImpl.getItemsBySection", mapOf("section" to section))
                emit(Result.failure(PersistenceError.QueryFailed("getItemsBySection", e)))
            }
    }
    
    override fun getPriorityItems(): Flow<Result<List<ShoppingListItem>>> {
        return shoppingListDao.getPriorityItems()
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                errorLogger.logError(e, "ShoppingListRepositoryImpl.getPriorityItems")
                emit(Result.failure(PersistenceError.QueryFailed("getPriorityItems", e)))
            }
    }
    
    override suspend fun getBudgetSummary(): Result<BudgetSummary> {
        return try {
            val totalEstimated = shoppingListDao.getTotalEstimatedPrice() ?: 0.0
            val checkedTotal = shoppingListDao.getCheckedItemsPrice() ?: 0.0
            val itemsWithPrices = shoppingListDao.getItemsWithPriceCount()
            val totalItems = shoppingListDao.getItemCount()
            val remainingBudget = totalEstimated - checkedTotal
            
            val summary = BudgetSummary(
                totalEstimated = totalEstimated,
                checkedTotal = checkedTotal,
                remainingBudget = remainingBudget,
                itemsWithPrices = itemsWithPrices,
                totalItems = totalItems
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            errorLogger.logError(e, "ShoppingListRepositoryImpl.getBudgetSummary")
            Result.failure(PersistenceError.QueryFailed("getBudgetSummary", e))
        }
    }
}
