package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.repository.ShoppingListRepository
import com.shoppit.app.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for saving the current shopping list as a template.
 * Templates allow users to quickly recreate recurring shopping lists.
 */
class SaveTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    /**
     * Save the current shopping list as a new template.
     * @param name Template name (required, cannot be blank)
     * @param description Optional template description
     * @return Result containing the new template ID
     */
    suspend operator fun invoke(name: String, description: String = ""): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(AppError.ValidationError("Template name cannot be empty"))
        }
        
        return try {
            shoppingListRepository.getShoppingList().first().fold(
                onSuccess = { items ->
                    if (items.isEmpty()) {
                        Result.failure(AppError.ValidationError("Cannot save empty shopping list as template"))
                    } else {
                        templateRepository.saveTemplate(name, description, items)
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError("Failed to save template: ${e.message}"))
        }
    }
}
