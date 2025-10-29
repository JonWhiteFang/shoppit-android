package com.shoppit.app.presentation.ui.navigation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages preloading of frequently accessed screens to improve navigation performance.
 * Tracks navigation patterns and preloads ViewModels for common navigation paths.
 *
 * Requirements:
 * - 7.4: Preload frequently accessed screens to minimize transition delays
 */
object NavigationPreloader {
    
    private val navigationCounts = mutableMapOf<String, Int>()
    private val navigationPaths = mutableListOf<NavigationPath>()
    private const val MAX_PATH_HISTORY = 100
    
    private val _frequentDestinations = MutableStateFlow<List<String>>(emptyList())
    val frequentDestinations: StateFlow<List<String>> = _frequentDestinations.asStateFlow()
    
    /**
     * Records a navigation event to track usage patterns.
     * @param fromRoute The route navigated from
     * @param toRoute The route navigated to
     */
    fun recordNavigation(fromRoute: String?, toRoute: String) {
        // Update navigation count
        navigationCounts[toRoute] = (navigationCounts[toRoute] ?: 0) + 1
        
        // Record navigation path
        if (fromRoute != null) {
            val path = NavigationPath(fromRoute, toRoute, System.currentTimeMillis())
            navigationPaths.add(path)
            
            // Keep history size manageable
            if (navigationPaths.size > MAX_PATH_HISTORY) {
                navigationPaths.removeAt(0)
            }
        }
        
        updateFrequentDestinations()
        
        Timber.d("Navigation recorded: $fromRoute -> $toRoute (count: ${navigationCounts[toRoute]})")
    }
    
    /**
     * Updates the list of frequently accessed destinations.
     */
    private fun updateFrequentDestinations() {
        val sorted = navigationCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        _frequentDestinations.value = sorted
    }
    
    /**
     * Gets the most likely next destination from the current route.
     * @param currentRoute The current route
     * @return The most likely next destination, or null if no pattern found
     */
    fun predictNextDestination(currentRoute: String): String? {
        // Find all paths from current route
        val pathsFromCurrent = navigationPaths.filter { it.fromRoute == currentRoute }
        
        if (pathsFromCurrent.isEmpty()) return null
        
        // Count destinations
        val destinationCounts = pathsFromCurrent
            .groupBy { it.toRoute }
            .mapValues { it.value.size }
        
        // Return most common destination
        return destinationCounts.maxByOrNull { it.value }?.key
    }
    
    /**
     * Gets common navigation paths from a given route.
     * @param fromRoute The starting route
     * @param limit Maximum number of paths to return
     * @return List of common destination routes
     */
    fun getCommonPaths(fromRoute: String, limit: Int = 3): List<String> {
        val pathsFromRoute = navigationPaths.filter { it.fromRoute == fromRoute }
        
        return pathsFromRoute
            .groupBy { it.toRoute }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
    
    /**
     * Checks if a destination should be preloaded based on usage patterns.
     * @param route The route to check
     * @return True if the route should be preloaded
     */
    fun shouldPreload(route: String): Boolean {
        val count = navigationCounts[route] ?: 0
        return count >= 3 // Preload if accessed 3 or more times
    }
    
    /**
     * Gets navigation statistics for debugging.
     */
    fun getStatistics(): String {
        return buildString {
            appendLine("Navigation Preloader Statistics")
            appendLine("================================")
            appendLine("Total unique destinations: ${navigationCounts.size}")
            appendLine("Total navigation events: ${navigationPaths.size}")
            appendLine("\nFrequent Destinations:")
            _frequentDestinations.value.forEachIndexed { index, route ->
                val count = navigationCounts[route] ?: 0
                appendLine("  ${index + 1}. $route (${count} visits)")
            }
        }
    }
    
    /**
     * Clears all recorded navigation data.
     */
    fun clearData() {
        navigationCounts.clear()
        navigationPaths.clear()
        _frequentDestinations.value = emptyList()
        Timber.d("Navigation preloader data cleared")
    }
}

/**
 * Represents a navigation path from one route to another.
 */
private data class NavigationPath(
    val fromRoute: String,
    val toRoute: String,
    val timestamp: Long
)

/**
 * Composable that records navigation events for preloading analysis.
 * Should be called in the NavHost to track navigation patterns.
 *
 * @param currentRoute The current route
 * @param previousRoute The previous route
 */
@Composable
fun RecordNavigationForPreloading(
    currentRoute: String?,
    previousRoute: String?
) {
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            NavigationPreloader.recordNavigation(previousRoute, currentRoute)
        }
    }
}

/**
 * Cache for preloaded ViewModels to improve navigation performance.
 */
object ViewModelCache {
    private val cache = mutableMapOf<String, ViewModel>()
    private const val MAX_CACHE_SIZE = 5
    
    /**
     * Caches a ViewModel for a specific route.
     * @param route The route associated with the ViewModel
     * @param viewModel The ViewModel to cache
     */
    fun cacheViewModel(route: String, viewModel: ViewModel) {
        // Implement LRU-like behavior
        if (cache.size >= MAX_CACHE_SIZE && !cache.containsKey(route)) {
            // Remove least recently used (first entry)
            cache.remove(cache.keys.first())
        }
        
        cache[route] = viewModel
        Timber.d("ViewModel cached for route: $route")
    }
    
    /**
     * Retrieves a cached ViewModel for a route.
     * @param route The route to get the ViewModel for
     * @return The cached ViewModel, or null if not found
     */
    fun getCachedViewModel(route: String): ViewModel? {
        return cache[route]
    }
    
    /**
     * Removes a ViewModel from the cache.
     * @param route The route to remove
     */
    fun removeViewModel(route: String) {
        cache.remove(route)
        Timber.d("ViewModel removed from cache: $route")
    }
    
    /**
     * Clears all cached ViewModels.
     */
    fun clearCache() {
        cache.clear()
        Timber.d("ViewModel cache cleared")
    }
    
    /**
     * Gets the current cache size.
     */
    fun getCacheSize(): Int = cache.size
}
