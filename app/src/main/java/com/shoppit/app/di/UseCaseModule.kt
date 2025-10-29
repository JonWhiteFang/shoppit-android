package com.shoppit.app.di

import com.shoppit.app.domain.usecase.FilterMealsByTagsUseCase
import com.shoppit.app.domain.usecase.SearchMealsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Hilt module for providing use case instances.
 * Use cases are scoped to ViewModelComponent for ViewModel injection.
 *
 * Requirements:
 * - 5.1: Provide SearchMealsUseCase for dependency injection
 * - 5.2: Provide FilterMealsByTagsUseCase for dependency injection
 * - 5.3: Ensure use cases are injectable into ViewModels
 * - 5.4: Follow Hilt best practices for use case provision
 * - 5.5: Support constructor injection for use cases
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    /**
     * Provides SearchMealsUseCase instance.
     * Use case has no dependencies, so it's created directly.
     *
     * @return SearchMealsUseCase instance
     */
    @Provides
    fun provideSearchMealsUseCase(): SearchMealsUseCase {
        return SearchMealsUseCase()
    }
    
    /**
     * Provides FilterMealsByTagsUseCase instance.
     * Use case has no dependencies, so it's created directly.
     *
     * @return FilterMealsByTagsUseCase instance
     */
    @Provides
    fun provideFilterMealsByTagsUseCase(): FilterMealsByTagsUseCase {
        return FilterMealsByTagsUseCase()
    }
}
