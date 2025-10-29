package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Search bar composable for filtering meals by name or ingredients.
 *
 * Requirements:
 * - 1.1: Display search input field
 * - 1.2: Support real-time search as user types
 * - 4.2: Provide clear button to reset search
 * - 4.3: Include accessibility support
 *
 * @param query Current search query text
 * @param onQueryChange Callback when search query changes
 * @param modifier Optional modifier for the search bar
 */
@Composable
fun MealSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Search meals or ingredients"
            },
        placeholder = { Text("Search meals or ingredients...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.semantics {
                        contentDescription = "Clear search"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search button"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun MealSearchBarEmptyPreview() {
    ShoppitTheme {
        MealSearchBar(
            query = "",
            onQueryChange = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSearchBarWithTextPreview() {
    ShoppitTheme {
        MealSearchBar(
            query = "pasta",
            onQueryChange = {}
        )
    }
}
