package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Header component displaying meal count and clear filters button.
 *
 * Requirements:
 * - 4.3: Display count of filtered results vs total
 * - 4.4: Show "Clear filters" button when filters are active
 *
 * @param totalCount Total number of meals in the library
 * @param filteredCount Number of meals after applying filters
 * @param isFiltered Whether any filters are currently active
 * @param onClearFilters Callback when clear filters button is clicked
 * @param modifier Optional modifier for the header
 */
@Composable
fun ResultsHeader(
    totalCount: Int,
    filteredCount: Int,
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val countText = if (isFiltered) {
            "$filteredCount of $totalCount meals"
        } else {
            "$totalCount meal${if (totalCount != 1) "s" else ""}"
        }
        
        Text(
            text = countText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = countText
            }
        )
        
        if (isFiltered) {
            TextButton(
                onClick = onClearFilters,
                modifier = Modifier.semantics {
                    contentDescription = "Clear all filters"
                }
            ) {
                Text("Clear filters")
            }
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun ResultsHeaderNoFiltersPreview() {
    ShoppitTheme {
        ResultsHeader(
            totalCount = 12,
            filteredCount = 12,
            isFiltered = false,
            onClearFilters = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultsHeaderWithFiltersPreview() {
    ShoppitTheme {
        ResultsHeader(
            totalCount = 12,
            filteredCount = 5,
            isFiltered = true,
            onClearFilters = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultsHeaderSingleMealPreview() {
    ShoppitTheme {
        ResultsHeader(
            totalCount = 1,
            filteredCount = 1,
            isFiltered = false,
            onClearFilters = {}
        )
    }
}
