package com.shoppit.app.domain.model

data class IngredientSource(
    val mealId: Long,
    val mealName: String,
    val quantity: String,
    val unit: String
)
