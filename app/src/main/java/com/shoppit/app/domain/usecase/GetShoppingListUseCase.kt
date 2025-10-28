package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingListData
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for retrieving the shopping list with real-time updates.
 * Groups items by category and calculates summary statistics.
 */
class GetShoppingListUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    /**
     * Retrieves the shopping list as a reactive Flow.
     * Emits updates whenever the shopping list changes.
     * 
     * @return Flow emitting Result with ShoppingListData or error
     */
    operator fun invoke(): Flow<Result<ShoppingListData>> {
        return repository.getShoppingList()
            .map { result ->
                result.map { items ->
                    val itemsByCategory = items.groupBy { it.category }
                    ShoppingListData(
                        itemsByCategory = itemsByCategory,
                        totalItems = items.size,
                        checkedItems = items.count { it.isChecked }
                    )
                }
            }
    }
}
