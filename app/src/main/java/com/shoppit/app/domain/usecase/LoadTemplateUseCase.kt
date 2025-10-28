package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import com.shoppit.app.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for loading a template into the current shopping list.
 * Template items are added to the existing list without removing current items.
 */
class LoadTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    /**
     * Load a template and add its items to the current shopping list.
     * Existing items in the shopping list are preserved (merge logic).
     * @param templateId The ID of the template to load
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(templateId: Long): Result<Unit> {
        return try {
            templateRepository.getTemplate(templateId).first().fold(
                onSuccess = { template ->
                    if (template == null) {
                        return Result.failure(AppError.NotFoundError("Template not found"))
                    }
                    
                    // Convert template items to shopping list items
                    val items = template.items.map { templateItem ->
                        ShoppingListItem(
                            name = templateItem.name,
                            quantity = templateItem.quantity,
                            unit = templateItem.unit,
                            category = templateItem.category,
                            notes = templateItem.notes,
                            isManual = true,
                            storeSection = templateItem.category.name
                        )
                    }
                    
                    // Add items to shopping list
                    shoppingListRepository.addShoppingListItems(items).also { result ->
                        // Update last used timestamp if successful
                        if (result.isSuccess) {
                            templateRepository.updateLastUsed(templateId)
                        }
                    }.map { Unit }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError("Failed to load template: ${e.message}"))
        }
    }
}
