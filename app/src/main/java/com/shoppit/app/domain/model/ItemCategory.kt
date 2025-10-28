package com.shoppit.app.domain.model

enum class ItemCategory {
    PRODUCE,
    DAIRY,
    MEAT,
    BAKERY,
    FROZEN,
    PANTRY,
    OTHER;
    
    fun displayName(): String = when (this) {
        PRODUCE -> "Produce"
        DAIRY -> "Dairy"
        MEAT -> "Meat & Seafood"
        BAKERY -> "Bakery"
        FROZEN -> "Frozen"
        PANTRY -> "Pantry"
        OTHER -> "Other"
    }
}
