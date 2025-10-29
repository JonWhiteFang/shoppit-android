package com.shoppit.app.presentation.ui.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Horizontal scrolling row of filter chips for meal tag selection.
 *
 * Requirements:
 * - 3.1: Display all available meal tags
 * - 3.2: Support multiple tag selection
 * - 3.4: Show visual indicator for selected tags
 * - 4.1: Include accessibility support
 * - 4.5: Provide semantic information for screen readers
 *
 * @param selectedTags Set of currently selected tags
 * @param onTagToggle Callback when a tag is selected or deselected
 * @param modifier Optional modifier for the chip row
 */
@Composable
fun FilterChipRow(
    selectedTags: Set<MealTag>,
    onTagToggle: (MealTag) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(MealTag.entries.toTypedArray()) { tag ->
            val isSelected = selectedTags.contains(tag)
            
            FilterChip(
                selected = isSelected,
                onClick = { onTagToggle(tag) },
                label = { Text(tag.displayName) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                modifier = Modifier.semantics {
                    contentDescription = "${tag.displayName} filter"
                    stateDescription = if (isSelected) "Selected" else "Not selected"
                    role = Role.Checkbox
                }
            )
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun FilterChipRowEmptyPreview() {
    ShoppitTheme {
        FilterChipRow(
            selectedTags = emptySet(),
            onTagToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterChipRowWithSelectionPreview() {
    ShoppitTheme {
        FilterChipRow(
            selectedTags = setOf(MealTag.VEGETARIAN, MealTag.QUICK),
            onTagToggle = {}
        )
    }
}
