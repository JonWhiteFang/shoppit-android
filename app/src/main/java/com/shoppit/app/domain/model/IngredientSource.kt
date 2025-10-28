package com.shoppit.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class IngredientSource(
    val mealId: Long,
    val mealName: String,
    val quantity: String,
    val unit: String
)
