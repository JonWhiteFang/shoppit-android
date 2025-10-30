package com.shoppit.app.domain.model

/**
 * Types of entities that can be synchronized.
 */
enum class EntityType {
    /** Meal entity */
    MEAL,
    
    /** Meal plan entity */
    MEAL_PLAN,
    
    /** Shopping list item entity */
    SHOPPING_LIST_ITEM;
    
    /**
     * Converts the enum to its string representation for database storage.
     */
    fun toStorageString(): String = when (this) {
        MEAL -> "meal"
        MEAL_PLAN -> "meal_plan"
        SHOPPING_LIST_ITEM -> "shopping_list_item"
    }
    
    companion object {
        /**
         * Converts a storage string back to an EntityType enum.
         */
        fun fromStorageString(value: String): EntityType = when (value) {
            "meal" -> MEAL
            "meal_plan" -> MEAL_PLAN
            "shopping_list_item" -> SHOPPING_LIST_ITEM
            else -> throw IllegalArgumentException("Unknown entity type: $value")
        }
    }
}
