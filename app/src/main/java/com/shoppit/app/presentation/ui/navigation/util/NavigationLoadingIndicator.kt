package com.shoppit.app.presentation.ui.navigation.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

/**
 * Shows a loading indicator for navigation transitions that exceed 100ms.
 * Provides visual feedback during slow screen loads.
 *
 * Requirements:
 * - 7.3: Show loading indicators for operations exceeding 100ms
 *
 * @param isLoading Whether the screen is currently loading
 * @param delayMs Delay before showing the indicator (default 100ms)
 * @param modifier Optional modifier
 */
@Composable
fun NavigationLoadingIndicator(
    isLoading: Boolean,
    delayMs: Long = 100L,
    modifier: Modifier = Modifier
) {
    var showIndicator by remember { mutableStateOf(false) }
    
    LaunchedEffect(isLoading) {
        if (isLoading) {
            // Wait for the delay before showing indicator
            delay(delayMs)
            showIndicator = true
        } else {
            // Hide immediately when loading completes
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
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .semantics {
                    contentDescription = "Loading screen content"
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Wrapper composable that shows loading indicator during initial screen load.
 * Automatically manages the loading state based on content readiness.
 *
 * @param isContentReady Whether the screen content is ready to display
 * @param content The screen content to display when ready
 */
@Composable
fun NavigationLoadingWrapper(
    isContentReady: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isContentReady) {
            content()
        }
        
        NavigationLoadingIndicator(
            isLoading = !isContentReady,
            modifier = Modifier.fillMaxSize()
        )
    }
}
