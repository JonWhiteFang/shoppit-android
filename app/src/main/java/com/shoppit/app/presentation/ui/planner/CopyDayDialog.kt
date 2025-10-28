package com.shoppit.app.presentation.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dialog for copying meal plans from one day to another.
 * Allows user to select a target date and choose whether to replace existing meals.
 *
 * @param sourceDate The date to copy meals from
 * @param availableDates List of dates that can be selected as target
 * @param onCopyDay Callback when copy is confirmed with (targetDate, replaceExisting)
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun CopyDayDialog(
    sourceDate: LocalDate,
    availableDates: List<LocalDate>,
    onCopyDay: (LocalDate, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var replaceExisting by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Copy Day")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Copy meals from ${sourceDate.format(dateFormatter)} to:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date selection list
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    availableDates.forEach { date ->
                        DateSelectionItem(
                            date = date,
                            isSelected = date == selectedDate,
                            onClick = { selectedDate = date }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Replace existing checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { replaceExisting = !replaceExisting }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = replaceExisting,
                        onCheckedChange = { replaceExisting = it }
                    )
                    Text(
                        text = "Replace existing meals",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDate?.let { date ->
                        onCopyDay(date, replaceExisting)
                    }
                },
                enabled = selectedDate != null
            ) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Individual date selection item in the copy day dialog.
 */
@Composable
private fun DateSelectionItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(dateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CopyDayDialogPreview() {
    ShoppitTheme {
        CopyDayDialog(
            sourceDate = LocalDate.of(2024, 1, 15),
            availableDates = listOf(
                LocalDate.of(2024, 1, 16),
                LocalDate.of(2024, 1, 17),
                LocalDate.of(2024, 1, 18),
                LocalDate.of(2024, 1, 19),
                LocalDate.of(2024, 1, 20),
                LocalDate.of(2024, 1, 21)
            ),
            onCopyDay = { _, _ -> },
            onDismiss = {}
        )
    }
}
