package com.shoppit.app.domain.model

enum class ItemCategory {
    PRODUCE,
    DAIRY,
    MEAT,
    PANTRY,
    OTHER;
    
    fun displayName(): String = when (this) {
        PRODUCE -> "Produce"
        DAIRY -> "Dairy"
        MEAT -> "Meat & Seafood"
        PANTRY -> "Pantry"
        OTHER -> "Other"
    }
}
