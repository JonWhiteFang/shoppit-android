package com.shoppit.app.data.remote.interceptor

import com.shoppit.app.data.auth.TokenStorage
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that adds authentication tokens to API requests.
 * Automatically includes the access token in the Authorization header for all requests.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Get the access token
        val accessToken = tokenStorage.getAccessToken()
        
        // If no token is available, proceed without authentication
        if (accessToken.isNullOrBlank()) {
            Timber.d("No access token available, proceeding without authentication")
            return chain.proceed(originalRequest)
        }
        
        // Add the Authorization header with the Bearer token
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()
        
        Timber.d("Added Authorization header to request: ${originalRequest.url}")
        
        return chain.proceed(authenticatedRequest)
    }
}
