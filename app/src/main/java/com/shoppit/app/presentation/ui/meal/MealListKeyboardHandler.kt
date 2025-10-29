package com.shoppit.app.presentation.ui.meal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier

/**
 * Keyboard navigation handler for the meal list screen.
 *
 * Requirements:
 * - 4.1: Support keyboard navigation
 * - 4.2: Support Escape key to clear search
 *
 * Keyboard shortcuts:
 * - Escape: Clear search query
 * - Tab: Navigate through interactive elements (handled by system)
 *
 * @param searchQuery Current search query
 * @param onClearSearch Callback to clear the search query
 * @param modifier Optional modifier
 * @param content The content to wrap with keyboard handling
 */
@Composable
fun MealListKeyboardHandler(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = modifier.onKeyEvent { keyEvent ->
            handleKeyEvent(
                keyEvent = keyEvent,
                searchQuery = searchQuery,
                onClearSearch = onClearSearch,
                focusManager = focusManager
            )
        }
    ) {
        content()
    }
}

/**
 * Handles keyboard events for the meal list screen.
 *
 * @param keyEvent The keyboard event to handle
 * @param searchQuery Current search query
 * @param onClearSearch Callback to clear the search query
 * @param focusManager Focus manager for clearing focus
 * @return True if the event was handled, false otherwise
 */
private fun handleKeyEvent(
    keyEvent: KeyEvent,
    searchQuery: String,
    onClearSearch: () -> Unit,
    focusManager: FocusManager
): Boolean {
    // Only handle key down events
    if (keyEvent.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
        return false
    }
    
    return when (keyEvent.key) {
        Key.Escape -> {
            // Clear search if there's a query, otherwise clear focus
            if (searchQuery.isNotEmpty()) {
                onClearSearch()
                true
            } else {
                focusManager.clearFocus()
                true
            }
        }
        else -> false
    }
}
