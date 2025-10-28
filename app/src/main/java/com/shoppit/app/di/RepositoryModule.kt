package com.shoppit.app.di

import com.shoppit.app.data.repository.ItemHistoryRepositoryImpl
import com.shoppit.app.data.repository.MealPlanRepositoryImpl
import com.shoppit.app.data.repository.MealRepositoryImpl
import com.shoppit.app.data.repository.ShoppingListRepositoryImpl
import com.shoppit.app.data.repository.StoreSectionRepositoryImpl
import com.shoppit.app.data.repository.TemplateRepositoryImpl
import com.shoppit.app.domain.repository.ItemHistoryRepository
import com.shoppit.app.domain.repository.MealPlanRepository
import com.shoppit.app.domain.repository.MealRepository
import com.shoppit.app.domain.repository.ShoppingListRepository
import com.shoppit.app.domain.repository.StoreSectionRepository
import com.shoppit.app.domain.repository.TemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMealRepository(
        impl: MealRepositoryImpl
    ): MealRepository
    
    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(
        impl: MealPlanRepositoryImpl
    ): MealPlanRepository
    
    @Binds
    @Singleton
    abstract fun bindShoppingListRepository(
        impl: ShoppingListRepositoryImpl
    ): ShoppingListRepository
    
    @Binds
    @Singleton
    abstract fun bindItemHistoryRepository(
        impl: ItemHistoryRepositoryImpl
    ): ItemHistoryRepository
    
    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        impl: TemplateRepositoryImpl
    ): TemplateRepository
    
    @Binds
    @Singleton
    abstract fun bindStoreSectionRepository(
        impl: StoreSectionRepositoryImpl
    ): StoreSectionRepository
}