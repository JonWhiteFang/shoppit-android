package com.shoppit.app.presentation.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.usecase.ExportShoppingListUseCase

/**
 * Dialog for exporting shopping list in various formats.
 * 
 * Allows user to choose between text, CSV, and JSON formats,
 * and either share or copy to clipboard.
 */
@Composable
fun ExportDialog(
    onExport: (ExportShoppingListUseCase.ExportFormat, ExportAction) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf(ExportShoppingListUseCase.ExportFormat.TEXT) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Shopping List") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose export format:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Format options
                ExportFormatOption(
                    format = ExportShoppingListUseCase.ExportFormat.TEXT,
                    icon = Icons.Default.Description,
                    label = "Plain Text",
                    description = "Simple text format, easy to read",
                    isSelected = selectedFormat == ExportShoppingListUseCase.ExportFormat.TEXT,
                    onClick = { selectedFormat = ExportShoppingListUseCase.ExportFormat.TEXT }
                )
                
                ExportFormatOption(
                    format = ExportShoppingListUseCase.ExportFormat.CSV,
                    icon = Icons.Default.TableChart,
                    label = "CSV",
                    description = "Spreadsheet format for Excel/Sheets",
                    isSelected = selectedFormat == ExportShoppingListUseCase.ExportFormat.CSV,
                    onClick = { selectedFormat = ExportShoppingListUseCase.ExportFormat.CSV }
                )
                
                ExportFormatOption(
                    format = ExportShoppingListUseCase.ExportFormat.JSON,
                    icon = Icons.Default.Description,
                    label = "JSON",
                    description = "Structured data format",
                    isSelected = selectedFormat == ExportShoppingListUseCase.ExportFormat.JSON,
                    onClick = { selectedFormat = ExportShoppingListUseCase.ExportFormat.JSON }
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onExport(selectedFormat, ExportAction.COPY_TO_CLIPBOARD) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                
                Button(
                    onClick = { onExport(selectedFormat, ExportAction.SHARE) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Single export format option with radio button.
 */
@Composable
private fun ExportFormatOption(
    format: ExportShoppingListUseCase.ExportFormat,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

/**
 * Export action type.
 */
enum class ExportAction {
    SHARE,
    COPY_TO_CLIPBOARD
}
