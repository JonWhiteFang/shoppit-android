package com.shoppit.app.data.sync

import com.shoppit.app.domain.model.Meal

/**
 * Wrapper for Meal that implements SyncableEntity for conflict resolution.
 *
 * This class bridges the domain model (Meal) with sync requirements,
 * adding the serverId field needed for cloud synchronization.
 */
data class SyncableMeal(
    val meal: Meal,
    override val serverId: String?
) : SyncableEntity {
    
    override val id: Long
        get() = meal.id
    
    override val updatedAt: Long
        get() = meal.updatedAt
    
    /**
     * Creates a new Meal with updated serverId.
     */
    fun toMeal(): Meal = meal
    
    companion object {
        /**
         * Creates a SyncableMeal from a Meal and optional serverId.
         */
        fun from(meal: Meal, serverId: String? = null): SyncableMeal {
            return SyncableMeal(meal, serverId)
        }
    }
}
