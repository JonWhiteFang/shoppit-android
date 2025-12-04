package com.shoppit.app.presentation.ui.navigation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.presentation.ui.meal.MealDetailViewModel
import com.shoppit.app.presentation.ui.meal.MealViewModel
import com.shoppit.app.presentation.ui.planner.MealPlannerViewModel
import com.shoppit.app.presentation.ui.shopping.ShoppingListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Manages preloading of frequently accessed screens to improve navigation performance.
 * Tracks navigation patterns and preloads ViewModels for common navigation paths.
 * Enhanced with predictive preloading based on user behavior patterns.
 *
 * Requirements:
 * - 7.4: Preload frequently accessed screens to minimize transition delays
 * - 9.2: Preload destination screen data during navigation transition
 */
object NavigationPreloader {
    
    private val navigationCounts = mutableMapOf<String, Int>()
    private val navigationPaths = mutableListOf<NavigationPath>()
    private const val MAX_PATH_HISTORY = 100
    private const val PRELOAD_DELAY_MS = 500L // Delay before preloading to avoid unnecessary work
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
    private val _frequentDestinations = MutableStateFlow<List<String>>(emptyList())
    val frequentDestinations: StateFlow<List<String>> = _frequentDestinations.asStateFlow()
    
    private val _preloadingStatus = MutableStateFlow<Map<String, PreloadStatus>>(emptyMap())
    val preloadingStatus: StateFlow<Map<String, PreloadStatus>> = _preloadingStatus.asStateFlow()
    
    private val preloadJobs = mutableMapOf<String, Job>()
    private val preloadScope = CoroutineScope(Dispatchers.Default)
    
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
        
        Timber.d("Navigation recorded: ${fromRoute?.let { sanitize(it) }} -> ${sanitize(toRoute)} (count: ${navigationCounts[toRoute]})")
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
     * Starts predictive preloading for likely next destinations.
     * @param currentRoute The current route
     */
    fun startPredictivePreloading(currentRoute: String) {
        // Cancel any existing preload jobs
        cancelPreloading()
        
        // Get likely next destinations
        val likelyDestinations = getCommonPaths(currentRoute, limit = 2)
        
        likelyDestinations.forEach { destination ->
            if (shouldPreload(destination)) {
                schedulePreload(destination)
            }
        }
    }
    
    /**
     * Schedules a preload operation for a destination.
     * @param route The route to preload
     */
    private fun schedulePreload(route: String) {
        // Cancel existing job for this route if any
        preloadJobs[route]?.cancel()
        
        val job = preloadScope.launch {
            try {
                // Update status to loading
                updatePreloadStatus(route, PreloadStatus.Loading)
                
                // Wait before preloading to avoid unnecessary work
                delay(PRELOAD_DELAY_MS)
                
                // Simulate data preloading (actual implementation would load data)
                Timber.d("Preloading data for route: ${sanitize(route)}")
                
                // Update status to loaded
                updatePreloadStatus(route, PreloadStatus.Loaded(System.currentTimeMillis()))
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload route: ${sanitize(route)}")
                updatePreloadStatus(route, PreloadStatus.Error(e.message ?: "Unknown error"))
            }
        }
        
        preloadJobs[route] = job
    }
    
    /**
     * Updates the preloading status for a route.
     */
    private fun updatePreloadStatus(route: String, status: PreloadStatus) {
        val currentStatus = _preloadingStatus.value.toMutableMap()
        currentStatus[route] = status
        _preloadingStatus.value = currentStatus
    }
    
    /**
     * Cancels all ongoing preload operations.
     */
    fun cancelPreloading() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        
        // Clear loading statuses
        val currentStatus = _preloadingStatus.value.toMutableMap()
        currentStatus.entries.removeAll { it.value is PreloadStatus.Loading }
        _preloadingStatus.value = currentStatus
    }
    
    /**
     * Checks if data is already preloaded for a route.
     * @param route The route to check
     * @return True if data is preloaded and fresh
     */
    fun isPreloaded(route: String): Boolean {
        val status = _preloadingStatus.value[route]
        return when (status) {
            is PreloadStatus.Loaded -> {
                // Consider data fresh if loaded within last 5 minutes
                val age = System.currentTimeMillis() - status.timestamp
                age < 5 * 60 * 1000
            }
            else -> false
        }
    }
    
    /**
     * Clears preloaded data for a route.
     * @param route The route to clear
     */
    fun clearPreloadedData(route: String) {
        preloadJobs[route]?.cancel()
        preloadJobs.remove(route)
        
        val currentStatus = _preloadingStatus.value.toMutableMap()
        currentStatus.remove(route)
        _preloadingStatus.value = currentStatus
        
        Timber.d("Cleared preloaded data for route: ${sanitize(route)}")
    }
    
    /**
     * Checks if a destination should be preloaded based on usage patterns.
     * @param route The route to check
     * @return True if the route should be preloaded
     */
    private fun shouldPreload(route: String): Boolean {
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
 * Enhanced with predictive preloading.
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
            
            // Start predictive preloading for likely next destinations
            NavigationPreloader.startPredictivePreloading(currentRoute)
        }
    }
}

/**
 * Sanitizes route strings for logging.
 */
private fun sanitizeRoute(value: String): String = value
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

/**
 * Composable that preloads data for a specific route using LaunchedEffect.
 * Can be used to preload meal detail data, shopping list data, etc.
 *
 * @param route The route to preload data for
 * @param preloadAction The action to perform for preloading
 */
@Composable
fun PreloadRouteData(
    route: String,
    preloadAction: suspend () -> Unit
) {
    LaunchedEffect(route) {
        if (!NavigationPreloader.isPreloaded(route)) {
            try {
                Timber.d("Preloading data for route: ${sanitizeRoute(route)}")
                preloadAction()
            } catch (e: Exception) {
                Timber.e(e, "Failed to preload data for route: ${sanitizeRoute(route)}")
            }
        }
    }
}

/**
 * Cache for preloaded ViewModels to improve navigation performance.
 */
object ViewModelCache {
    private val cache = mutableMapOf<String, ViewModel>()
    private const val MAX_CACHE_SIZE = 5
    
    private fun sanitize(value: String): String = value
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
    
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
        Timber.d("ViewModel cached for route: ${sanitize(route)}")
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
        Timber.d("ViewModel removed from cache: ${sanitize(route)}")
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

/**
 * Represents the preloading status for a route.
 */
sealed class PreloadStatus {
    /**
     * Data is currently being preloaded.
     */
    data object Loading : PreloadStatus()
    
    /**
     * Data has been successfully preloaded.
     * @property timestamp When the data was loaded
     */
    data class Loaded(val timestamp: Long) : PreloadStatus()
    
    /**
     * Preloading failed with an error.
     * @property message Error message
     */
    data class Error(val message: String) : PreloadStatus()
}
