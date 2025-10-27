package com.shoppit.app.di

import com.shoppit.app.data.cache.CacheConfig
import com.shoppit.app.data.cache.CacheManager
import com.shoppit.app.data.cache.LruCacheManager
import com.shoppit.app.domain.model.Meal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier annotation for meal list cache.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MealListCache

/**
 * Qualifier annotation for meal detail cache.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MealDetailCache

/**
 * Hilt module for providing cache dependencies.
 * 
 * Provides cache managers for different data types with appropriate
 * configurations for size and TTL.
 */
@Module
@InstallIn(SingletonComponent::class)
object CacheModule {
    
    /**
     * Provides a cache manager for the complete list of meals.
     * 
     * @return CacheManager configured for meal list caching
     */
    @Provides
    @Singleton
    @MealListCache
    fun provideMealListCache(): CacheManager<String, List<Meal>> {
        return LruCacheManager(
            maxSize = CacheConfig.MEAL_LIST_CACHE_SIZE,
            defaultTtl = CacheConfig.MEAL_LIST_TTL_MS,
            onEviction = { key, value ->
                Timber.v("Meal list cache evicted: key=$key, size=${value.size}")
            }
        )
    }
    
    /**
     * Provides a cache manager for individual meal details.
     * 
     * @return CacheManager configured for meal detail caching
     */
    @Provides
    @Singleton
    @MealDetailCache
    fun provideMealDetailCache(): CacheManager<Long, Meal> {
        return LruCacheManager(
            maxSize = CacheConfig.MEAL_LIST_CACHE_SIZE,
            defaultTtl = CacheConfig.MEAL_DETAIL_TTL_MS,
            onEviction = { key, value ->
                Timber.v("Meal detail cache evicted: id=$key, name=${value.name}")
            }
        )
    }
}
