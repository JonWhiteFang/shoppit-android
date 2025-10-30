package com.shoppit.app.data.remote.api

import com.shoppit.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit API service for data synchronization with the cloud backend.
 * Handles all HTTP communication for syncing meals, meal plans, and shopping lists.
 */
interface SyncApiService {
    
    // ========== Meal Sync Endpoints ==========
    
    /**
     * Sync meals with the backend.
     * Uploads local changes and receives server updates.
     */
    @POST("api/v1/meals/sync")
    suspend fun syncMeals(@Body request: MealSyncRequest): Response<MealSyncResponse>
    
    /**
     * Get meal changes from the backend since a specific timestamp.
     * Used for pulling updates without uploading local changes.
     */
    @GET("api/v1/meals/changes")
    suspend fun getMealChanges(@Query("since") timestamp: Long): Response<MealChangesResponse>
    
    // ========== Meal Plan Sync Endpoints ==========
    
    /**
     * Sync meal plans with the backend.
     * Uploads local changes and receives server updates.
     */
    @POST("api/v1/meal-plans/sync")
    suspend fun syncMealPlans(@Body request: MealPlanSyncRequest): Response<MealPlanSyncResponse>
    
    /**
     * Get meal plan changes from the backend since a specific timestamp.
     * Used for pulling updates without uploading local changes.
     */
    @GET("api/v1/meal-plans/changes")
    suspend fun getMealPlanChanges(@Query("since") timestamp: Long): Response<MealPlanChangesResponse>
    
    // ========== Shopping List Sync Endpoints ==========
    
    /**
     * Sync shopping list items with the backend.
     * Uploads local changes and receives server updates.
     */
    @POST("api/v1/shopping-lists/sync")
    suspend fun syncShoppingLists(@Body request: ShoppingListSyncRequest): Response<ShoppingListSyncResponse>
    
    /**
     * Get shopping list changes from the backend since a specific timestamp.
     * Used for pulling updates without uploading local changes.
     */
    @GET("api/v1/shopping-lists/changes")
    suspend fun getShoppingListChanges(@Query("since") timestamp: Long): Response<ShoppingListChangesResponse>
    
    // ========== Batch Operations ==========
    
    /**
     * Batch sync all entities (meals, meal plans, shopping lists) in a single request.
     * More efficient for syncing multiple entity types at once.
     */
    @POST("api/v1/sync/batch")
    suspend fun batchSync(@Body request: BatchSyncRequest): Response<BatchSyncResponse>
}
