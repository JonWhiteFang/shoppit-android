package com.shoppit.app.di

import com.shoppit.app.domain.validator.IngredientValidator
import com.shoppit.app.domain.validator.MealValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing meal-related dependencies.
 * 
 * This module provides:
 * - MealValidator: Validates meal data before persistence
 * 
 * Requirements:
 * - 1.3: Persist meal entity to database after validation
 * - 2.1: Retrieve all meals from database
 * - 7.1: Perform all CRUD operations using Room database
 */
@Module
@InstallIn(SingletonComponent::class)
object MealModule {
    
    /**
     * Provides a singleton instance of IngredientValidator.
     * 
     * @return IngredientValidator instance for validating ingredient data
     */
    @Provides
    @Singleton
    fun provideIngredientValidator(): IngredientValidator {
        return IngredientValidator()
    }
    
    /**
     * Provides a singleton instance of MealValidator.
     * 
     * @param ingredientValidator Validator for individual ingredients
     * @return MealValidator instance for validating meal data
     */
    @Provides
    @Singleton
    fun provideMealValidator(ingredientValidator: IngredientValidator): MealValidator {
        return MealValidator(ingredientValidator)
    }
}
