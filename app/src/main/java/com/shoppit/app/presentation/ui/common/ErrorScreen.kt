package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Reusable error screen component that displays an error message
 * with an optional retry button.
 *
 * @param message The error message to display
 * @param onRetry Optional callback for retry action
 * @param modifier Optional modifier for the error screen
 */
@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            onRetry?.let {
                Button(onClick = it) {
                    Text("Retry")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenPreview() {
    ShoppitTheme {
        ErrorScreen(
            message = "Failed to load data. Please try again.",
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorScreenWithoutRetryPreview() {
    ShoppitTheme {
        ErrorScreen(
            message = "An unexpected error occurred."
        )
    }
}
