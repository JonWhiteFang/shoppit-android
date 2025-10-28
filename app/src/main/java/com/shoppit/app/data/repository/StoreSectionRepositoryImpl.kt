package com.shoppit.app.data.repository

import com.shoppit.app.data.error.PersistenceError
import com.shoppit.app.data.local.dao.StoreSectionDao
import com.shoppit.app.data.mapper.toDomainModel
import com.shoppit.app.data.mapper.toEntity
import com.shoppit.app.domain.model.StoreSection
import com.shoppit.app.domain.repository.StoreSectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of StoreSectionRepository that manages store section configurations using Room database.
 */
class StoreSectionRepositoryImpl @Inject constructor(
    private val storeSectionDao: StoreSectionDao
) : StoreSectionRepository {
    
    override fun getAllSections(): Flow<Result<List<StoreSection>>> {
        return storeSectionDao.getAllSections()
            .map { entities ->
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e ->
                emit(Result.failure(PersistenceError.QueryFailed("getAllSections", e)))
            }
    }
    
    override suspend fun updateSectionOrder(sections: List<StoreSection>): Result<Unit> {
        return try {
            // Update each section's display order
            sections.forEach { section ->
                storeSectionDao.updateDisplayOrder(section.id, section.displayOrder)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("updateSectionOrder", e))
        }
    }
    
    override suspend fun toggleSectionCollapsed(sectionId: Long, isCollapsed: Boolean): Result<Unit> {
        return try {
            storeSectionDao.updateCollapsedState(sectionId, isCollapsed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("toggleSectionCollapsed", e))
        }
    }
    
    override suspend fun createCustomSection(name: String, color: String): Result<Long> {
        return try {
            // Validate section name
            if (name.isBlank()) {
                return Result.failure(
                    PersistenceError.ValidationFailed(
                        listOf(
                            com.shoppit.app.data.error.ValidationError(
                                field = "name",
                                message = "Section name cannot be empty",
                                code = "EMPTY_NAME"
                            )
                        )
                    )
                )
            }
            
            // Validate color format (basic hex color validation)
            if (!color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                return Result.failure(
                    PersistenceError.ValidationFailed(
                        listOf(
                            com.shoppit.app.data.error.ValidationError(
                                field = "color",
                                message = "Color must be in hex format (#RRGGBB)",
                                code = "INVALID_COLOR"
                            )
                        )
                    )
                )
            }
            
            // Check if section with same name already exists
            val existingSection = storeSectionDao.getSectionByName(name)
            if (existingSection != null) {
                return Result.failure(
                    PersistenceError.ConstraintViolation(
                        constraint = "unique_section_name",
                        details = "A section with name '$name' already exists"
                    )
                )
            }
            
            // Get current section count to set display order
            val sectionCount = storeSectionDao.getSectionCount()
            
            // Create new section
            val newSection = StoreSection(
                name = name,
                displayOrder = sectionCount,
                isCollapsed = false,
                color = color
            )
            
            val sectionId = storeSectionDao.insertSection(newSection.toEntity())
            Result.success(sectionId)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("createCustomSection", e))
        }
    }
    
    override suspend fun deleteCustomSection(sectionId: Long): Result<Unit> {
        return try {
            // Note: In a production app, we should check if this is a default section
            // and prevent deletion. For now, we'll allow deletion of any section.
            storeSectionDao.deleteSection(sectionId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(PersistenceError.WriteFailed("deleteCustomSection", e))
        }
    }
    
    override suspend fun learnItemSection(itemName: String, sectionName: String): Result<Unit> {
        // TODO: Implement item-section learning using SharedPreferences or a separate table
        // For now, this is a placeholder that returns success
        // This feature will be implemented when a preferences system is added
        return Result.success(Unit)
    }
}
