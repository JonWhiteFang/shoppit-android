package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.domain.model.MealType
import com.shoppit.app.domain.model.SuggestionContext
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate

/**
 * Bottom sheet displaying meal suggestions with search and filter capabilities.
 * Integrates with the meal planning flow to provide intelligent meal recommendations.
 *
 * Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 7.1-7.5, 8.1-8.5, 9.1-9.5, 10.1-10.5
 *
 * @param uiState Current suggestion UI state
 * @param searchQuery Current search query
 * @param selectedTags Currently selected filter tags
 * @param availableTags All available meal tags
 * @param mealCountByTag Map of tag to meal count
 * @param onSearchQueryChange Callback when search query changes
 * @param onTagToggle Callback when a filter tag is toggled
 * @param onMealSelected Callback when a meal is selected
 * @param onViewDetails Callback when "View Details" is clicked for a meal
 * @param onBrowseAll Callback when "Browse All Meals" is clicked
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MealSuggestionsBottomSheet(
    uiState: SuggestionUiState,
    searchQuery: String,
    selectedTags: Set<MealTag>,
    availableTags: List<MealTag>,
    mealCountByTag: Map<MealTag, Int>,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (MealTag) -> Unit,
    onMealSelected: (Meal) -> Unit,
    onViewDetails: (Meal) -> Unit,
    onBrowseAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var searchActive by remember { mutableStateOf(false) }

    // Announce state changes for screen readers
    val stateAnnouncement = when (uiState) {
        is SuggestionUiState.Loading -> "Loading meal suggestions"
        is SuggestionUiState.Success -> "${uiState.suggestions.size} meal suggestions available"
        is SuggestionUiState.Error -> "Error loading suggestions: ${uiState.message}"
        is SuggestionUiState.Empty -> when (uiState.reason) {
            EmptyReason.NO_MEALS -> "No meals in library"
            EmptyReason.NO_MATCHES -> "No meals match current filters"
            EmptyReason.ALL_PLANNED -> "All meals already planned"
        }
        SuggestionUiState.Hidden -> ""
    }

    LaunchedEffect(stateAnnouncement) {
        if (stateAnnouncement.isNotBlank()) {
            // Announcement will be made via live region
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = stateAnnouncement
            }
            .onKeyEvent { keyEvent ->
                // Handle keyboard navigation
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.Tab -> {
                            // Let default tab handling work
                            false
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            // Search bar
            SearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = {
                    keyboardController?.hide()
                    searchActive = false
                },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Search meals...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onSearchQueryChange("")
                                keyboardController?.hide()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics {
                        contentDescription = "Search for meals by name"
                    }
            ) {
                // Empty search suggestions - we handle filtering in the use case
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter chips
            if (availableTags.isNotEmpty()) {
                SuggestionFilters(
                    availableTags = availableTags,
                    selectedTags = selectedTags,
                    mealCountByTag = mealCountByTag,
                    onTagToggle = onTagToggle
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content based on state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (uiState) {
                    is SuggestionUiState.Loading -> {
                        LoadingContent()
                    }

                    is SuggestionUiState.Success -> {
                        SuggestionList(
                            suggestions = uiState.suggestions,
                            onMealSelected = onMealSelected,
                            onViewDetails = onViewDetails
                        )
                    }

                    is SuggestionUiState.Error -> {
                        ErrorContent(
                            message = uiState.message,
                            onRetry = { /* Retry handled by parent */ }
                        )
                    }

                    is SuggestionUiState.Empty -> {
                        EmptyContent(
                            reason = uiState.reason,
                            onBrowseAll = onBrowseAll
                        )
                    }

                    SuggestionUiState.Hidden -> {
                        // Should not reach here
                    }
                }
            }

            // Browse All button at bottom
            OutlinedButton(
                onClick = onBrowseAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .semantics {
                        contentDescription = "Browse all meals in library"
                    }
            ) {
                Text("Browse All Meals")
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.semantics {
                    contentDescription = "Loading meal suggestions"
                }
            )
            Text(
                text = "Finding suggestions...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuggestionList(
    suggestions: List<MealSuggestion>,
    onMealSelected: (Meal) -> Unit,
    onViewDetails: (Meal) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics {
            contentDescription = "${suggestions.size} meal suggestions"
        }
    ) {
        items(
            items = suggestions,
            key = { it.meal.id }
        ) { suggestion ->
            SuggestionCard(
                suggestion = suggestion,
                onClick = { onMealSelected(suggestion.meal) },
                onViewDetails = { onViewDetails(suggestion.meal) }
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    ErrorScreen(
        message = message,
        onRetry = onRetry
    )
}

@Composable
private fun EmptyContent(
    reason: EmptyReason,
    onBrowseAll: () -> Unit
) {
    val (message, actionLabel) = when (reason) {
        EmptyReason.NO_MEALS -> Pair(
            "Your meal library is empty.\nStart by adding your favorite meals to get personalized suggestions!",
            "Add Your First Meal"
        )
        EmptyReason.NO_MATCHES -> Pair(
            "No meals match your current search or filters.\nTry adjusting your filters or clearing the search to see more options.",
            null
        )
        EmptyReason.ALL_PLANNED -> Pair(
            "Great planning! All your meals are already scheduled for this week.\nYou can still browse your meal library if you'd like to make changes.",
            null
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics {
                    contentDescription = message
                }
            )

            if (actionLabel != null && reason == EmptyReason.NO_MEALS) {
                Button(
                    onClick = onBrowseAll,
                    modifier = Modifier.semantics {
                        contentDescription = "Add your first meal"
                    }
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSuggestionsBottomSheetSuccessPreview() {
    ShoppitTheme {
        MealSuggestionsBottomSheet(
            uiState = SuggestionUiState.Success(
                suggestions = listOf(
                    MealSuggestion(
                        meal = Meal(
                            id = 1,
                            name = "Spaghetti Carbonara",
                            ingredients = listOf(
                                Ingredient("Pasta", "400", "g"),
                                Ingredient("Eggs", "4", "pcs")
                            ),
                            tags = setOf(MealTag.DINNER, MealTag.QUICK)
                        ),
                        score = 180.0,
                        reasons = listOf("Perfect match"),
                        lastPlannedDate = null,
                        planCount = 0
                    ),
                    MealSuggestion(
                        meal = Meal(
                            id = 2,
                            name = "Caesar Salad",
                            ingredients = listOf(
                                Ingredient("Lettuce", "1", "head")
                            ),
                            tags = setOf(MealTag.LUNCH, MealTag.HEALTHY)
                        ),
                        score = 120.0,
                        reasons = listOf("Good match"),
                        lastPlannedDate = LocalDate.now().minusDays(7),
                        planCount = 1
                    )
                ),
                context = SuggestionContext(
                    targetDate = LocalDate.now(),
                    targetMealType = MealType.LUNCH
                )
            ),
            searchQuery = "",
            selectedTags = emptySet(),
            availableTags = listOf(MealTag.BREAKFAST, MealTag.LUNCH, MealTag.DINNER),
            mealCountByTag = mapOf(
                MealTag.BREAKFAST to 5,
                MealTag.LUNCH to 8,
                MealTag.DINNER to 10
            ),
            onSearchQueryChange = {},
            onTagToggle = {},
            onMealSelected = {},
            onViewDetails = {},
            onBrowseAll = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSuggestionsBottomSheetLoadingPreview() {
    ShoppitTheme {
        MealSuggestionsBottomSheet(
            uiState = SuggestionUiState.Loading,
            searchQuery = "",
            selectedTags = emptySet(),
            availableTags = emptyList(),
            mealCountByTag = emptyMap(),
            onSearchQueryChange = {},
            onTagToggle = {},
            onMealSelected = {},
            onViewDetails = {},
            onBrowseAll = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealSuggestionsBottomSheetEmptyPreview() {
    ShoppitTheme {
        MealSuggestionsBottomSheet(
            uiState = SuggestionUiState.Empty(EmptyReason.NO_MEALS),
            searchQuery = "",
            selectedTags = emptySet(),
            availableTags = emptyList(),
            mealCountByTag = emptyMap(),
            onSearchQueryChange = {},
            onTagToggle = {},
            onMealSelected = {},
            onViewDetails = {},
            onBrowseAll = {},
            onDismiss = {}
        )
    }
}
