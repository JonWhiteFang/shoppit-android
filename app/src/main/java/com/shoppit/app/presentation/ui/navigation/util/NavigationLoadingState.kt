package com.shoppit.app.presentation.ui.navigation.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Shows a loading indicator during navigation transitions.
 * Only displays if the transition takes longer than a threshold.
 * 
 * Requirements:
 * - 9.5: Show loading indicators during data-heavy transitions
 * 
 * @param isLoading Whether navigation is in progress
 * @param delayMs Delay before showing the indicator (default: 200ms)
 * @param modifier Optional modifier
 */
@Composable
fun NavigationLoadingIndicator(
    isLoading: Boolean,
    delayMs: Long = 200L,
    modifier: Modifier = Modifier
) {
    var showIndicator by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLoading) {
        if (isLoading) {
            // Delay before showing to avoid flashing for quick transitions
            delay(delayMs)
            showIndicator = true
        } else {
            showIndicator = false
        }
    }
    
    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Loading next screen"
                    },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Shows a skeleton screen during navigation for predictable layouts.
 * Provides visual feedback while data is loading.
 * 
 * Requirements:
 * - 9.5: Use Skeleton screens for predictable layouts
 * 
 * @param isLoading Whether data is loading
 * @param content The actual content to show when loaded
 * @param skeleton The skeleton placeholder to show while loading
 */
@Composable
fun NavigationSkeletonScreen(
    isLoading: Boolean,
    content: @Composable () -> Unit,
    skeleton: @Composable () -> Unit
) {
    if (isLoading) {
        skeleton()
    } else {
        content()
    }
}

/**
 * Progressive loading wrapper that shows content as it becomes available.
 * Useful for large lists that load incrementally.
 * 
 * Requirements:
 * - 9.5: Implement progressive loading for large lists
 * 
 * @param isInitialLoading Whether initial data is loading
 * @param isLoadingMore Whether more data is being loaded
 * @param content The content to display
 * @param loadingIndicator Optional custom loading indicator
 */
@Composable
fun ProgressiveLoadingWrapper(
    isInitialLoading: Boolean,
    isLoadingMore: Boolean,
    content: @Composable () -> Unit,
    loadingIndicator: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = "Loading content"
                }
            )
        }
    }
) {
    when {
        isInitialLoading -> loadingIndicator()
        else -> {
            content()
            // Show loading indicator at bottom when loading more
            if (isLoadingMore) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(32.dp)
                            .semantics {
                                contentDescription = "Loading more content"
                            }
                    )
                }
            }
        }
    }
}

/**
 * Manages loading state during navigation transitions.
 * Tracks when navigation starts and ends to show appropriate loading UI.
 */
class NavigationLoadingStateManager {
    private val _isLoading = mutableStateOf(false)
    val isLoading: Boolean get() = _isLoading.value
    
    /**
     * Marks navigation as started (loading).
     */
    fun startLoading() {
        _isLoading.value = true
    }
    
    /**
     * Marks navigation as completed (not loading).
     */
    fun stopLoading() {
        _isLoading.value = false
    }
    
    /**
     * Executes a navigation action with loading state management.
     * 
     * @param action The navigation action to perform
     */
    suspend fun withLoading(action: suspend () -> Unit) {
        try {
            startLoading()
            action()
        } finally {
            stopLoading()
        }
    }
}

/**
 * Composable that manages loading state for navigation.
 * 
 * @param currentRoute The current route
 * @param previousRoute The previous route
 * @param onLoadingStateChange Callback when loading state changes
 */
@Composable
fun NavigationLoadingStateEffect(
    currentRoute: String?,
    previousRoute: String?,
    onLoadingStateChange: (Boolean) -> Unit
) {
    var isTransitioning by remember { mutableStateOf(false) }
    
    LaunchedEffect(currentRoute) {
        if (currentRoute != previousRoute && currentRoute != null) {
            // Start loading
            isTransitioning = true
            onLoadingStateChange(true)
            
            // Wait a bit for the transition to complete
            delay(100) // Match target navigation time
            
            // Stop loading
            isTransitioning = false
            onLoadingStateChange(false)
        }
    }
}
