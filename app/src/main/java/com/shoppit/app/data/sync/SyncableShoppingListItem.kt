package com.shoppit.app.data.sync

import com.shoppit.app.domain.model.ShoppingListItem

/**
 * Wrapper for ShoppingListItem that implements SyncableEntity for conflict resolution.
 *
 * This class bridges the domain model (ShoppingListItem) with sync requirements,
 * adding the serverId field needed for cloud synchronization.
 */
data class SyncableShoppingListItem(
    val item: ShoppingListItem,
    override val serverId: String?
) : SyncableEntity {
    
    override val id: Long
        get() = item.id
    
    override val updatedAt: Long
        get() = item.lastModifiedAt
    
    /**
     * Creates a new ShoppingListItem from this syncable version.
     */
    fun toShoppingListItem(): ShoppingListItem = item
    
    companion object {
        /**
         * Creates a SyncableShoppingListItem from a ShoppingListItem and optional serverId.
         */
        fun from(item: ShoppingListItem, serverId: String? = null): SyncableShoppingListItem {
            return SyncableShoppingListItem(item, serverId)
        }
    }
}
