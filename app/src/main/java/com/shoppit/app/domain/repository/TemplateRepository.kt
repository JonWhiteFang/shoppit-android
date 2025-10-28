package com.shoppit.app.domain.repository

import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.model.ShoppingTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing shopping list templates.
 * Provides CRUD operations for saved shopping list templates.
 */
interface TemplateRepository {
    
    /**
     * Get all shopping templates ordered by last used date.
     * @return Flow of Result containing list of ShoppingTemplate
     */
    fun getAllTemplates(): Flow<Result<List<ShoppingTemplate>>>
    
    /**
     * Get a specific template by ID with its items.
     * @param id Template ID
     * @return Flow of Result containing ShoppingTemplate or null if not found
     */
    fun getTemplate(id: Long): Flow<Result<ShoppingTemplate?>>
    
    /**
     * Save current shopping list as a new template.
     * @param name Template name
     * @param description Template description
     * @param items List of shopping list items to save in template
     * @return Result containing the new template ID
     */
    suspend fun saveTemplate(
        name: String,
        description: String,
        items: List<ShoppingListItem>
    ): Result<Long>
    
    /**
     * Update an existing template.
     * @param template The template to update
     * @return Result indicating success or failure
     */
    suspend fun updateTemplate(template: ShoppingTemplate): Result<Unit>
    
    /**
     * Delete a template by ID.
     * @param id Template ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteTemplate(id: Long): Result<Unit>
    
    /**
     * Update the last used timestamp for a template.
     * Called when a template is loaded into the shopping list.
     * @param id Template ID
     * @return Result indicating success or failure
     */
    suspend fun updateLastUsed(id: Long): Result<Unit>
}
