package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.StoreSection
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing store section configurations.
 * Provides operations for customizing section order, collapse state, and appearance.
 */
interface StoreSectionRepository {
    
    /**
     * Get all store sections ordered by display order.
     * @return Flow of Result containing list of StoreSection
     */
    fun getAllSections(): Flow<Result<List<StoreSection>>>
    
    /**
     * Update the display order of multiple sections.
     * Used when user reorders sections to match their store layout.
     * @param sections List of sections with updated display orders
     * @return Result indicating success or failure
     */
    suspend fun updateSectionOrder(sections: List<StoreSection>): Result<Unit>
    
    /**
     * Toggle the collapsed state of a section.
     * @param sectionId Section ID to toggle
     * @param isCollapsed New collapsed state
     * @return Result indicating success or failure
     */
    suspend fun toggleSectionCollapsed(sectionId: Long, isCollapsed: Boolean): Result<Unit>
    
    /**
     * Create a new custom section.
     * @param name Section name
     * @param color Section color in hex format
     * @return Result containing the new section ID
     */
    suspend fun createCustomSection(name: String, color: String): Result<Long>
    
    /**
     * Delete a custom section.
     * Note: Default sections cannot be deleted.
     * @param sectionId Section ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCustomSection(sectionId: Long): Result<Unit>
    
    /**
     * Learn and store the preferred section for an item name.
     * Used to auto-categorize items based on user's manual section assignments.
     * @param itemName Name of the item
     * @param sectionName Preferred section name
     * @return Result indicating success or failure
     */
    suspend fun learnItemSection(itemName: String, sectionName: String): Result<Unit>
}
