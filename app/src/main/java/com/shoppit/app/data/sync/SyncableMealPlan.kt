package com.shoppit.app.data.sync

import com.shoppit.app.domain.model.MealPlan

/**
 * Wrapper for MealPlan that implements SyncableEntity for conflict resolution.
 *
 * This class bridges the domain model (MealPlan) with sync requirements,
 * adding the serverId and updatedAt fields needed for cloud synchronization.
 */
data class SyncableMealPlan(
    val mealPlan: MealPlan,
    override val serverId: String?,
    override val updatedAt: Long
) : SyncableEntity {
    
    override val id: Long
        get() = mealPlan.id
    
    /**
     * Creates a new MealPlan from this syncable version.
     */
    fun toMealPlan(): MealPlan = mealPlan
    
    companion object {
        /**
         * Creates a SyncableMealPlan from a MealPlan and sync metadata.
         */
        fun from(
            mealPlan: MealPlan,
            serverId: String? = null,
            updatedAt: Long = System.currentTimeMillis()
        ): SyncableMealPlan {
            return SyncableMealPlan(mealPlan, serverId, updatedAt)
        }
    }
}
