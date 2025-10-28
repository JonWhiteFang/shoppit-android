package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.repository.ShoppingListRepository
import com.shoppit.app.domain.repository.StoreSectionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for moving a shopping list item to a different store section.
 * Learns the user's section preference for the item name for future auto-categorization.
 */
class MoveItemToSectionUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val storeSectionRepository: StoreSectionRepository
) {
    /**
     * Move an item to a different store section and learn the preference.
     * @param itemId The ID of the item to move
     * @param sectionName The name of the target section
     * @return Result indicating success or failure
     */
    suspend operator fun invoke(itemId: Long, sectionName: String): Result<Unit> {
        return try {
            shoppingListRepository.getShoppingListItem(itemId).first().fold(
                onSuccess = { item ->
                    // Move the item to the new section
                    shoppingListRepository.moveItemToSection(itemId, sectionName).also { result ->
                        // If successful, learn the section preference for this item name
                        if (result.isSuccess) {
                            storeSectionRepository.learnItemSection(item.name, sectionName)
                        }
                    }
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
