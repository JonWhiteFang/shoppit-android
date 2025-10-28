package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Navigation bar for the meal planner showing week range and navigation controls.
 *
 * @param weekStart Start date of the week (Monday)
 * @param weekEnd End date of the week (Sunday)
 * @param onPreviousWeek Callback when previous week button is clicked
 * @param onNextWeek Callback when next week button is clicked
 * @param onToday Callback when today button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun WeekNavigationBar(
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous week"
                )
            }

            Text(
                text = formatWeekRange(weekStart, weekEnd),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
                IconButton(onClick = onToday) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Go to today"
                    )
                }

                IconButton(onClick = onNextWeek) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next week"
                    )
                }
            }
        }
    }
}

/**
 * Formats the week range for display.
 */
private fun formatWeekRange(start: LocalDate, end: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    val startFormatted = start.format(formatter)
    val endFormatted = end.format(formatter)

    return if (start.month == end.month) {
        "${start.format(DateTimeFormatter.ofPattern("MMM d"))} - ${end.dayOfMonth}, ${end.year}"
    } else {
        "$startFormatted - $endFormatted, ${end.year}"
    }
}

@Preview(showBackground = true)
@Composable
private fun WeekNavigationBarPreview() {
    ShoppitTheme {
        WeekNavigationBar(
            weekStart = LocalDate.of(2024, 1, 1),
            weekEnd = LocalDate.of(2024, 1, 7),
            onPreviousWeek = {},
            onNextWeek = {},
            onToday = {}
        )
    }
}
