package com.shoppit.app.data.mapper

import com.shoppit.app.data.local.entity.StoreSectionEntity
import com.shoppit.app.domain.model.StoreSection

/**
 * Convert StoreSectionEntity to domain model StoreSection.
 */
fun StoreSectionEntity.toDomainModel(): StoreSection {
    return StoreSection(
        id = id,
        name = name,
        displayOrder = displayOrder,
        isCollapsed = isCollapsed,
        color = color
    )
}

/**
 * Convert domain model StoreSection to StoreSectionEntity.
 */
fun StoreSection.toEntity(): StoreSectionEntity {
    return StoreSectionEntity(
        id = id,
        name = name,
        displayOrder = displayOrder,
        isCollapsed = isCollapsed,
        color = color
    )
}
