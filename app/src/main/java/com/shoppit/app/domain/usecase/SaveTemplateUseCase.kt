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
        // Validate template name
        if (name.isBlank()) {
            return Result.failure(AppError.ValidationError("Template name cannot be empty"))
        }
        
        // Validate template name length
        if (name.length > 50) {
            return Result.failure(
                AppError.ValidationError("Template name is too long. Maximum 50 characters.")
            )
        }
        
        // Validate description length
        if (description.length > 200) {
            return Result.failure(
                AppError.ValidationError("Template description is too long. Maximum 200 characters.")
            )
        }
        
        return try {
            shoppingListRepository.getShoppingList().first().fold(
                onSuccess = { items ->
                    // Check if list is empty
                    if (items.isEmpty()) {
                        return@fold Result.failure(
                            AppError.ValidationError(
                                "Cannot save empty shopping list as template. Add some items first."
                            )
                        )
                    }
                    
                    // Check if list is too large (performance consideration)
                    if (items.size > 100) {
                        return@fold Result.failure(
                            AppError.ValidationError(
                                "Shopping list is too large. Maximum 100 items per template."
                            )
                        )
                    }
                    
                    templateRepository.saveTemplate(name.trim(), description.trim(), items)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(AppError.UnknownError(e.message ?: "Failed to save template"))
        }
    }
}
