package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.ShoppingTemplateEntity
import com.shoppit.app.data.local.entity.TemplateItemEntity
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingTemplate
import com.shoppit.app.domain.model.TemplateItem

/**
 * Convert ShoppingTemplateEntity to domain model ShoppingTemplate.
 * Note: This does not include items - use with getTemplateItems to build complete template.
 */
fun ShoppingTemplateEntity.toDomainModel(items: List<TemplateItem> = emptyList()): ShoppingTemplate {
    return ShoppingTemplate(
        id = id,
        name = name,
        description = description,
        items = items,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}

/**
 * Convert domain model ShoppingTemplate to ShoppingTemplateEntity.
 * Note: This does not include items - use toTemplateItemEntities for items.
 */
fun ShoppingTemplate.toEntity(): ShoppingTemplateEntity {
    return ShoppingTemplateEntity(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt
    )
}

/**
 * Convert TemplateItemEntity to domain model TemplateItem.
 */
fun TemplateItemEntity.toDomainModel(): TemplateItem {
    return TemplateItem(
        name = name,
        quantity = quantity,
        unit = unit,
        category = ItemCategory.valueOf(category),
        notes = notes
    )
}

/**
 * Convert domain model TemplateItem to TemplateItemEntity.
 * @param templateId The ID of the template this item belongs to
 */
fun TemplateItem.toEntity(templateId: Long): TemplateItemEntity {
    return TemplateItemEntity(
        templateId = templateId,
        name = name,
        quantity = quantity,
        unit = unit,
        category = category.name,
        notes = notes
    )
}

/**
 * Convert list of TemplateItems to list of TemplateItemEntities.
 * @param templateId The ID of the template these items belong to
 */
fun List<TemplateItem>.toTemplateItemEntities(templateId: Long): List<TemplateItemEntity> {
    return map { it.toEntity(templateId) }
}
