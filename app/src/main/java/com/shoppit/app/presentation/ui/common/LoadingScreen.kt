package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Reusable loading screen component that displays a circular progress indicator
 * with an optional message.
 *
 * @param modifier Optional modifier for the loading screen
 * @param message Optional message to display below the progress indicator
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String = "Loading..."
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingScreenPreview() {
    ShoppitTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingScreenWithCustomMessagePreview() {
    ShoppitTheme {
        LoadingScreen(message = "Loading meals...")
    }
}
