package com.shoppit.app.presentation.ui.meal

/**
 * Represents the pagination state for list rendering.
 * 
 * Requirements: 5.5
 * 
 * @property currentPage Current page number (0-indexed)
 * @property pageSize Number of items per page
 * @property totalItems Total number of items available
 * @property isLoadingMore Whether more items are being loaded
 * @property hasMorePages Whether there are more pages to load
 */
data class PaginationState(
    val currentPage: Int = 0,
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    val totalItems: Int = 0,
    val isLoadingMore: Boolean = false
) {
    val hasMorePages: Boolean
        get() = (currentPage + 1) * pageSize < totalItems
    
    val loadedItemsCount: Int
        get() = minOf((currentPage + 1) * pageSize, totalItems)
    
    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val PREFETCH_THRESHOLD = 5 // Load more when within 5 items of end
    }
}
