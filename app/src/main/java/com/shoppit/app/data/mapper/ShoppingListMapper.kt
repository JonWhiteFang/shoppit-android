package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.ShoppingListItemEntity
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListItem

fun ShoppingListItemEntity.toDomainModel(): ShoppingListItem {
    return ShoppingListItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = ItemCategory.valueOf(category),
        isChecked = isChecked,
        isManual = isManual,
        mealIds = if (mealIds.isBlank()) emptyList() else mealIds.split(",").mapNotNull { it.toLongOrNull() },
        createdAt = createdAt
    )
}

fun ShoppingListItem.toEntity(): ShoppingListItemEntity {
    return ShoppingListItemEntity(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category.name,
        isChecked = isChecked,
        isManual = isManual,
        mealIds = mealIds.joinToString(","),
        createdAt = createdAt
    )
}
