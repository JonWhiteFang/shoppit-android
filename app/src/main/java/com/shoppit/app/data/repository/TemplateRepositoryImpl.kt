package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.TemplateDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.data.mapper.toTemplateItemEntities
import com.shoppit.app.di.IoDispatcher
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.model.ShoppingTemplate
import com.shoppit.app.domain.model.TemplateItem
import com.shoppit.app.domain.repository.TemplateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of TemplateRepository that manages shopping list templates using Room database.
 * Handles template CRUD operations with transaction support for template and items.
 */
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TemplateRepository {
    
    override fun getAllTemplates(): Flow<Result<List<ShoppingTemplate>>> {
        return templateDao.getAllTemplates()
            .map { templateEntities ->
                // For list view, we don't need to load all items for each template
                // Items will be loaded when a specific template is requested
                Result.success(templateEntities.map { it.toDomainModel() })
            }
            .catch { e ->
                emit(Result.failure(PersistenceError.QueryFailed("getAllTemplates", e)))
            }
    }
    
    override fun getTemplate(id: Long): Flow<Result<ShoppingTemplate?>> {
        return combine(
            templateDao.getTemplateById(id),
            templateDao.getTemplateItems(id)
        ) { templateEntity, itemEntities ->
            if (templateEntity == null) {
                Result.success(null)
            } else {
                val items = itemEntities.map { it.toDomainModel() }
                Result.success(templateEntity.toDomainModel(items))
            }
        }.catch { e ->
            emit(Result.failure(PersistenceError.QueryFailed("getTemplate", e)))
        }
    }
    
    override suspend fun saveTemplate(
        name: String,
        description: String,
        items: List<ShoppingListItem>
    ): Result<Long> {
        return try {
            // Validate template name
            if (name.isBlank()) {
                return Result.failure(
                    PersistenceError.ValidationFailed(
                        listOf(
                            com.shoppit.app.data.error.ValidationError(
                                field = "name",
                                message = "Template name cannot be empty",
                                code = "EMPTY_NAME"
                            )
                        )
                    )
                )
            }
            
            // Convert shopping list items to template items
            val templateItems = items.map { item ->
                TemplateItem(
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    category = item.category,
                    notes = item.notes
                )
            }
            
            // Create template entity
            val templateEntity = ShoppingTemplate(
                name = name,
                description = description,
                items = templateItems,
                createdAt = System.currentTimeMillis()
            ).toEntity()
            
            // Use transaction to insert template and items together
            val templateId = templateDao.insertTemplateWithItems(
                templateEntity,
                templateItems.toTemplateItemEntities(0) // ID will be set by DAO
            )
            
            Result.success(templateId)
        } catch (e: Exception) {
            Result.failure(PersistenceError.TransactionFailed("saveTemplate", e))
        }
    }
    
    override suspend fun updateTemplate(template: ShoppingTemplate): Result<Unit> {
        return try {
            // Validate template
            if (template.name.isBlank()) {
                return Result.failure(
                    PersistenceError.ValidationFailed(
                        listOf(
                            com.shoppit.app.data.error.ValidationError(
                                field = "name",
                                message = "Template name cannot be empty",
                                code = "EMPTY_NAME"
                            )
                        )
                    )
                )
            }
            
            // Update template metadata
            templateDao.updateTemplate(template.toEntity())
            
            // Delete existing items and insert new ones (simpler than updating)
            templateDao.deleteTemplateItems(template.id)
            templateDao.insertTemplateItems(
                template.items.toTemplateItemEntities(template.id)
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.TransactionFailed("updateTemplate", e))
        }
    }
    
    override suspend fun deleteTemplate(id: Long): Result<Unit> {
        return try {
            // Use transaction to delete template and items together
            templateDao.deleteTemplateWithItems(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.TransactionFailed("deleteTemplate", e))
        }
    }
    
    override suspend fun updateLastUsed(id: Long): Result<Unit> {
        return try {
            val timestamp = System.currentTimeMillis()
            templateDao.updateLastUsed(id, timestamp)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("updateLastUsed", e))
        }
    }
}
