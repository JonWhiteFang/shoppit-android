package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ItemHistory
import com.shoppit.app.domain.repository.ItemHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving shopping item purchase history.
 * Provides access to recent and frequently purchased items.
 */
class GetItemHistoryUseCase @Inject constructor(
    private val repository: ItemHistoryRepository
) {
    /**
     * Get recent purchase history ordered by last purchased date.
     * @param limit Maximum number of items to return (default 100)
     * @return Flow of Result containing list of ItemHistory
     */
    operator fun invoke(limit: Int = 100): Flow<Result<List<ItemHistory>>> {
        // Validate limit parameter
        val validatedLimit = when {
            limit < 1 -> 1
            limit > 100 -> 100 // Cap at 100 for performance
            else -> limit
        }
        
        return repository.getRecentHistory(validatedLimit)
    }
    
    /**
     * Get frequently purchased items ordered by purchase count.
     * Used for quick add suggestions.
     * @param limit Maximum number of items to return (default 20)
     * @return Flow of Result containing list of ItemHistory
     */
    fun getFrequentItems(limit: Int = 20): Flow<Result<List<ItemHistory>>> {
        // Validate limit parameter
        val validatedLimit = when {
            limit < 1 -> 1
            limit > 50 -> 50 // Cap at 50 for quick add performance
            else -> limit
        }
        
        return repository.getFrequentItems(validatedLimit)
    }
}
