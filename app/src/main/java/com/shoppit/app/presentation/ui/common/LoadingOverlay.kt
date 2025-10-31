package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * A semi-transparent overlay with a loading indicator that blocks user interaction.
 * 
 * Displays a circular progress indicator centered on the screen with a semi-transparent
 * background that prevents user interaction with underlying content.
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4
 * 
 * @param isLoading Whether to show the loading overlay
 * @param modifier Optional modifier for the overlay
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable(enabled = false) { } // Block clicks
                .semantics {
                    contentDescription = "Loading, please wait"
                },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = "Loading indicator"
                }
            )
        }
    }
}
