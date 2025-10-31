package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme

/**
 * Filter chips component for tag-based meal filtering.
 * Displays all available meal tags with meal counts and multi-select support.
 *
 * Requirements: 2.1-2.5, 9.1-9.2
 *
 * @param availableTags List of all available meal tags
 * @param selectedTags Set of currently selected tags
 * @param mealCountByTag Map of tag to meal count for that tag
 * @param onTagToggle Callback when a tag is toggled
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SuggestionFilters(
    availableTags: List<MealTag>,
    selectedTags: Set<MealTag>,
    mealCountByTag: Map<MealTag, Int>,
    onTagToggle: (MealTag) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        availableTags.forEach { tag ->
            val isSelected = selectedTags.contains(tag)
            val mealCount = mealCountByTag[tag] ?: 0
            val chipLabel = "${tag.displayName} ($mealCount)"
            
            // Build content description for accessibility
            val chipDescription = buildString {
                append("Filter by ${tag.displayName}. ")
                append("$mealCount meals available. ")
                if (isSelected) {
                    append("Currently selected.")
                } else {
                    append("Not selected.")
                }
            }

            FilterChip(
                selected = isSelected,
                onClick = { onTagToggle(tag) },
                label = {
                    Text(
                        text = chipLabel,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                modifier = Modifier
                    .semantics {
                        contentDescription = chipDescription
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                    }
                    .onKeyEvent { keyEvent ->
                        // Handle Enter/Space key for toggling
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.Enter, Key.Spacebar -> {
                                    onTagToggle(tag)
                                    true
                                }
                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionFiltersPreview() {
    ShoppitTheme {
        SuggestionFilters(
            availableTags = listOf(
                MealTag.BREAKFAST,
                MealTag.LUNCH,
                MealTag.DINNER,
                MealTag.VEGETARIAN,
                MealTag.QUICK,
                MealTag.HEALTHY
            ),
            selectedTags = setOf(MealTag.LUNCH, MealTag.HEALTHY),
            mealCountByTag = mapOf(
                MealTag.BREAKFAST to 5,
                MealTag.LUNCH to 8,
                MealTag.DINNER to 12,
                MealTag.VEGETARIAN to 6,
                MealTag.QUICK to 10,
                MealTag.HEALTHY to 7
            ),
            onTagToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionFiltersNoSelectionPreview() {
    ShoppitTheme {
        SuggestionFilters(
            availableTags = listOf(
                MealTag.BREAKFAST,
                MealTag.LUNCH,
                MealTag.DINNER,
                MealTag.SNACK
            ),
            selectedTags = emptySet(),
            mealCountByTag = mapOf(
                MealTag.BREAKFAST to 3,
                MealTag.LUNCH to 5,
                MealTag.DINNER to 8,
                MealTag.SNACK to 2
            ),
            onTagToggle = {}
        )
    }
}
