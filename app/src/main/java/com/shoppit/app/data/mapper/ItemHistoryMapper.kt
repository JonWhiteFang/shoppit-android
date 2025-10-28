package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.ItemHistoryEntity
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ItemHistory

/**
 * Convert ItemHistoryEntity to domain model ItemHistory.
 */
fun ItemHistoryEntity.toDomainModel(): ItemHistory {
    return ItemHistory(
        id = id,
        itemName = itemName,
        quantity = quantity,
        unit = unit,
        category = ItemCategory.valueOf(category),
        purchaseCount = purchaseCount,
        lastPurchasedAt = lastPurchasedAt,
        averagePrice = averagePrice
    )
}

/**
 * Convert domain model ItemHistory to ItemHistoryEntity.
 */
fun ItemHistory.toEntity(): ItemHistoryEntity {
    return ItemHistoryEntity(
        id = id,
        itemName = itemName,
        quantity = quantity,
        unit = unit,
        category = category.name,
        purchaseCount = purchaseCount,
        lastPurchasedAt = lastPurchasedAt,
        averagePrice = averagePrice
    )
}
