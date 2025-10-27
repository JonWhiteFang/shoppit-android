package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
 * Reusable empty state component that displays a message when there's no data
 * with an optional action button.
 *
 * @param message The message to display
 * @param actionLabel Optional label for the action button
 * @param onActionClick Optional callback for action button click
 * @param modifier Optional modifier for the empty state
 */
@Composable
fun EmptyState(
    message: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
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
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (actionLabel != null && onActionClick != null) {
                Button(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    ShoppitTheme {
        EmptyState(
            message = "No meals found. Add your first meal to get started!",
            actionLabel = "Add Meal",
            onActionClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateWithoutActionPreview() {
    ShoppitTheme {
        EmptyState(
            message = "No items in your shopping list."
        )
    }
}
