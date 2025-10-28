package com.shoppit.app.presentation.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.StoreSection
import com.shoppit.app.presentation.ui.common.LoadingScreen

/**
 * Screen for editing store sections.
 * Allows users to reorder sections, create custom sections, and customize colors.
 *
 * @param sections List of store sections
 * @param isLoading Whether sections are loading
 * @param onNavigateBack Callback when back button is clicked
 * @param onReorderSections Callback when sections are reordered
 * @param onCreateSection Callback when a new section is created
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSectionEditor(
    sections: List<StoreSection>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onReorderSections: (List<StoreSection>) -> Unit,
    onCreateSection: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var reorderedSections by remember(sections) { mutableStateOf(sections) }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Store Sections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom section"
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    LoadingScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Drag sections to reorder them to match your store layout",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = reorderedSections,
                                key = { it.id }
                            ) { section ->
                                StoreSectionCard(
                                    section = section,
                                    onMoveUp = {
                                        val index = reorderedSections.indexOf(section)
                                        if (index > 0) {
                                            val newList = reorderedSections.toMutableList()
                                            newList.removeAt(index)
                                            newList.add(index - 1, section)
                                            reorderedSections = newList.mapIndexed { i, s ->
                                                s.copy(displayOrder = i)
                                            }
                                            onReorderSections(reorderedSections)
                                        }
                                    },
                                    onMoveDown = {
                                        val index = reorderedSections.indexOf(section)
                                        if (index < reorderedSections.size - 1) {
                                            val newList = reorderedSections.toMutableList()
                                            newList.removeAt(index)
                                            newList.add(index + 1, section)
                                            reorderedSections = newList.mapIndexed { i, s ->
                                                s.copy(displayOrder = i)
                                            }
                                            onReorderSections(reorderedSections)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Create section dialog
    if (showCreateDialog) {
        CreateSectionDialog(
            onConfirm = { name, color ->
                onCreateSection(name, color)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

/**
 * Card for displaying a store section with drag handle.
 */
@Composable
fun StoreSectionCard(
    section: StoreSection,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle icon
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Color indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = try {
                            Color(android.graphics.Color.parseColor(section.color))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape
                    )
            )
            
            // Section name
            Text(
                text = section.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            // Move buttons
            Column {
                TextButton(onClick = onMoveUp) {
                    Text("↑")
                }
                TextButton(onClick = onMoveDown) {
                    Text("↓")
                }
            }
        }
    }
}

/**
 * Dialog for creating a new custom section.
 */
@Composable
fun CreateSectionDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sectionName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF6200EE") }
    
    val predefinedColors = listOf(
        "#FF6200EE", // Purple
        "#FF03DAC5", // Teal
        "#FF018786", // Dark Teal
        "#FFB00020", // Red
        "#FFFF6F00", // Orange
        "#FF2E7D32", // Green
        "#FF1976D2", // Blue
        "#FF7B1FA2"  // Deep Purple
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Section") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = sectionName,
                    onValueChange = { sectionName = it },
                    label = { Text("Section Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Choose a color:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Color picker grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(color)),
                                    shape = CircleShape
                                )
                                .then(
                                    if (color == selectedColor) {
                                        Modifier.padding(4.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(sectionName, selectedColor) },
                enabled = sectionName.isNotBlank()
            ) {
                Text("Create")
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
