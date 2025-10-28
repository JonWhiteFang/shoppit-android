package com.shoppit.app.presentation.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import com.shoppit.app.domain.model.ShoppingTemplate
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.LoadingScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for managing shopping list templates.
 * Allows users to view, create, load, and delete templates.
 *
 * @param templates List of shopping templates
 * @param isLoading Whether templates are loading
 * @param onNavigateBack Callback when back button is clicked
 * @param onCreateTemplate Callback when create template is clicked
 * @param onLoadTemplate Callback when a template is loaded
 * @param onDeleteTemplate Callback when a template is deleted
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateManagerScreen(
    templates: List<ShoppingTemplate>,
    isLoading: Boolean,
    onNavigateBack: () -> Unit,
    onCreateTemplate: (String, String) -> Unit,
    onLoadTemplate: (Long) -> Unit,
    onDeleteTemplate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<ShoppingTemplate?>(null) }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Shopping Templates") },
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
                    contentDescription = "Create template"
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
                
                templates.isEmpty() -> {
                    EmptyState(
                        message = "No templates yet.\nCreate a template from your current shopping list!",
                        actionLabel = "Create Template",
                        onActionClick = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                    ) {
                        items(
                            items = templates,
                            key = { it.id }
                        ) { template ->
                            TemplateCard(
                                template = template,
                                onClick = { onLoadTemplate(template.id) },
                                onDelete = { templateToDelete = template }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Create template dialog
    if (showCreateDialog) {
        CreateTemplateDialog(
            onConfirm = { name, description ->
                onCreateTemplate(name, description)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    
    // Delete confirmation dialog
    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Delete Template") },
            text = { Text("Are you sure you want to delete \"${template.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTemplate(template.id)
                        templateToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card for displaying a shopping template.
 * Shows template name, description, item count, and metadata.
 */
@Composable
fun TemplateCard(
    template: ShoppingTemplate,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val createdDate = remember(template.createdAt) {
        dateFormat.format(Date(template.createdAt))
    }
    val lastUsedDate = remember(template.lastUsedAt) {
        template.lastUsedAt?.let { dateFormat.format(Date(it)) }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Template name
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Description
                if (template.description.isNotBlank()) {
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Item count
                Text(
                    text = "${template.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Created date
                Text(
                    text = "Created: $createdDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Last used date
                lastUsedDate?.let {
                    Text(
                        text = "Last used: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete template",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Dialog for creating a new template.
 */
@Composable
fun CreateTemplateDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var templateName by remember { mutableStateOf("") }
    var templateDescription by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Template") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = templateName,
                    onValueChange = { templateName = it },
                    label = { Text("Template Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = templateDescription,
                    onValueChange = { templateDescription = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "This will save your current shopping list as a template.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(templateName, templateDescription) },
                enabled = templateName.isNotBlank()
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
