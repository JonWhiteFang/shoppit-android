package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.domain.model.MealSuggestion
import com.shoppit.app.domain.model.MealTag
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Card component displaying a meal suggestion with score and metadata.
 * Shows meal name, tags, ingredient count, last planned date, and high score indicator.
 *
 * Requirements: 4.4, 6.4, 10.1-10.5
 *
 * @param suggestion The meal suggestion to display
 * @param onClick Callback when the card is clicked to select the meal
 * @param onViewDetails Callback when "View Details" button is clicked
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionCard(
    suggestion: MealSuggestion,
    onClick: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHighScore = suggestion.score > 150
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    
    // Build content description for accessibility
    val cardDescription = buildString {
        append("Meal suggestion: ${suggestion.meal.name}. ")
        append("${suggestion.meal.ingredients.size} ingredients. ")
        if (isHighScore) {
            append("Highly recommended. ")
        }
        if (suggestion.lastPlannedDate != null) {
            append("Last planned on ${suggestion.lastPlannedDate.format(dateFormatter)}. ")
        }
        if (suggestion.meal.tags.isNotEmpty()) {
            append("Tags: ${suggestion.meal.tags.joinToString { it.displayName }}. ")
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = cardDescription },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighScore) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row with meal name and high score indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = suggestion.meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighScore) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )
                
                if (isHighScore) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Highly recommended",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .semantics { contentDescription = "Highly recommended meal" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Meal type tags
            if (suggestion.meal.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestion.meal.tags.forEach { tag ->
                        AssistChip(
                            onClick = { /* Tags are read-only in suggestion card */ },
                            label = {
                                Text(
                                    text = tag.displayName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = "Tag: ${tag.displayName}"
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Ingredient count and last planned date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${suggestion.meal.ingredients.size} ingredients",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "${suggestion.meal.ingredients.size} ingredients"
                    }
                )

                if (suggestion.lastPlannedDate != null) {
                    Text(
                        text = "Last: ${suggestion.lastPlannedDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "Last planned on ${suggestion.lastPlannedDate.format(dateFormatter)}"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Details button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.semantics {
                        contentDescription = "View details for ${suggestion.meal.name}"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Details")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionCardPreview() {
    ShoppitTheme {
        SuggestionCard(
            suggestion = MealSuggestion(
                meal = Meal(
                    id = 1,
                    name = "Spaghetti Carbonara",
                    ingredients = listOf(
                        Ingredient("Pasta", "400", "g"),
                        Ingredient("Eggs", "4", "pcs"),
                        Ingredient("Bacon", "200", "g")
                    ),
                    tags = setOf(MealTag.DINNER, MealTag.QUICK)
                ),
                score = 120.0,
                reasons = listOf("Matches meal type", "Not recently planned"),
                lastPlannedDate = LocalDate.now().minusDays(14),
                planCount = 2
            ),
            onClick = {},
            onViewDetails = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionCardHighScorePreview() {
    ShoppitTheme {
        SuggestionCard(
            suggestion = MealSuggestion(
                meal = Meal(
                    id = 2,
                    name = "Caesar Salad",
                    ingredients = listOf(
                        Ingredient("Lettuce", "1", "head"),
                        Ingredient("Croutons", "1", "cup"),
                        Ingredient("Parmesan", "50", "g")
                    ),
                    tags = setOf(MealTag.LUNCH, MealTag.HEALTHY, MealTag.VEGETARIAN)
                ),
                score = 180.0,
                reasons = listOf("Perfect match", "Never planned"),
                lastPlannedDate = null,
                planCount = 0
            ),
            onClick = {},
            onViewDetails = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionCardNoTagsPreview() {
    ShoppitTheme {
        SuggestionCard(
            suggestion = MealSuggestion(
                meal = Meal(
                    id = 3,
                    name = "Grilled Chicken",
                    ingredients = listOf(
                        Ingredient("Chicken breast", "2", "pcs")
                    ),
                    tags = emptySet()
                ),
                score = 95.0,
                reasons = listOf("Available option"),
                lastPlannedDate = null,
                planCount = 0
            ),
            onClick = {},
            onViewDetails = {}
        )
    }
}
