package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Header component for a day column in the meal planner.
 * Shows the day name, date, and completion indicator.
 *
 * @param date The date for this day
 * @param isToday Whether this is today's date
 * @param planCount Number of meals planned for this day
 * @param onCopyDay Callback when copy day action is triggered
 * @param onClearDay Callback when clear day action is triggered
 * @param modifier Optional modifier
 */
@Composable
fun DayHeader(
    date: LocalDate,
    isToday: Boolean,
    planCount: Int,
    onCopyDay: (LocalDate) -> Unit,
    onClearDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isToday) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (isToday) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .background(backgroundColor)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEE")),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )

        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )

        if (planCount > 0) {
            Text(
                text = "$planCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DayHeaderPreview() {
    ShoppitTheme {
        DayHeader(
            date = LocalDate.of(2024, 1, 15),
            isToday = false,
            planCount = 2,
            onCopyDay = {},
            onClearDay = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayHeaderTodayPreview() {
    ShoppitTheme {
        DayHeader(
            date = LocalDate.now(),
            isToday = true,
            planCount = 3,
            onCopyDay = {},
            onClearDay = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayHeaderEmptyPreview() {
    ShoppitTheme {
        DayHeader(
            date = LocalDate.of(2024, 1, 16),
            isToday = false,
            planCount = 0,
            onCopyDay = {},
            onClearDay = {}
        )
    }
}
