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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.LoadingScreen
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Stateful composable for the shopping list screen.
 * Injects the ViewModel and manages state collection.
 *
 * @param onMealDetailClick Callback when a meal is clicked from item sources
 * @param onNavigateToHistory Callback to navigate to item history screen
 * @param onNavigateToTemplates Callback to navigate to template manager screen
 * @param onNavigateToSectionEditor Callback to navigate to store section editor
 * @param onNavigateToShoppingMode Callback to navigate to shopping mode screen
 * @param viewModel The ViewModel for managing shopping list state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun ShoppingListScreen(
    onMealDetailClick: (Long) -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSectionEditor: () -> Unit = {},
    onNavigateToShoppingMode: () -> Unit = {},
    viewModel: ShoppingListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    // Show undo snackbar when item is checked
    LaunchedEffect(uiState.showUndoSnackbar) {
        if (uiState.showUndoSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Item checked",
                actionLabel = "Undo",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            
            when (result) {
                androidx.compose.material3.SnackbarResult.ActionPerformed -> {
                    viewModel.undoLastCheck()
                }
                androidx.compose.material3.SnackbarResult.Dismissed -> {
                    viewModel.dismissUndoSnackbar()
                }
            }
        }
    }
    
    // Handle share intent
    LaunchedEffect(uiState.shareText) {
        uiState.shareText?.let { text ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Shopping List")
            context.startActivity(shareIntent)
            viewModel.clearShareText()
        }
    }
    
    // Handle export data sharing
    LaunchedEffect(uiState.exportData) {
        uiState.exportData?.let { data ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, data)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Shopping List")
            context.startActivity(shareIntent)
            viewModel.clearExportData()
        }
    }
    
    // Handle clipboard copy
    LaunchedEffect(uiState.clipboardData) {
        uiState.clipboardData?.let { data ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Shopping List", data)
            clipboard.setPrimaryClip(clip)
            
            snackbarHostState.showSnackbar("Copied to clipboard")
            viewModel.clearClipboardData()
        }
    }
    
    ShoppingListContent(
        uiState = uiState,
        onItemCheckedChange = viewModel::toggleItemChecked,
        onItemClick = viewModel::showItemDetail,
        onAddItemClick = viewModel::showAddItemDialog,
        onScanBarcodeClick = viewModel::showBarcodeScanner,
        onExportClick = viewModel::showExportDialog,
        onGenerateList = viewModel::generateShoppingList,
        onClearChecked = viewModel::showClearCheckedConfirmation,
        onUncheckAll = viewModel::showUncheckAllConfirmation,
        onToggleFilter = viewModel::toggleFilter,
        onSearchQueryChange = viewModel::updateSearchQuery,
        onShareList = viewModel::shareShoppingList,
        onToggleCategoryCollapsed = viewModel::toggleCategoryCollapsed,
        onMealDetailClick = onMealDetailClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
    
    // Show add item dialog
    if (uiState.showAddItemDialog) {
        AddItemDialog(
            onConfirm = viewModel::addManualItem,
            onDismiss = viewModel::dismissAddItemDialog
        )
    }
    
    // Show item detail dialog
    uiState.showItemDetail?.let { item ->
        ItemDetailDialog(
            item = item,
            sources = uiState.itemSources,
            onDismiss = viewModel::dismissItemDetail,
            onDelete = if (item.isManual) {
                { viewModel.showDeleteManualItemConfirmation(item.id) }
            } else null,
            onMealClick = onMealDetailClick
        )
    }
    
    // Show confirmation dialogs
    when (val action = uiState.confirmationAction) {
        is ConfirmationAction.ClearChecked -> {
            val checkedCount = uiState.shoppingListData?.checkedItems ?: 0
            ConfirmationDialog(
                title = "Clear Checked Items",
                message = "Are you sure you want to remove $checkedCount checked item${if (checkedCount != 1) "s" else ""}?",
                confirmText = "Clear",
                onConfirm = viewModel::clearCheckedItems,
                onDismiss = viewModel::dismissConfirmation
            )
        }
        is ConfirmationAction.UncheckAll -> {
            val totalCount = uiState.shoppingListData?.totalItems ?: 0
            ConfirmationDialog(
                title = "Uncheck All Items",
                message = "Are you sure you want to uncheck all $totalCount item${if (totalCount != 1) "s" else ""}?",
                confirmText = "Uncheck All",
                onConfirm = viewModel::uncheckAllItems,
                onDismiss = viewModel::dismissConfirmation
            )
        }
        is ConfirmationAction.DeleteManualItem -> {
            ConfirmationDialog(
                title = "Delete Item",
                message = "Are you sure you want to delete this item?",
                confirmText = "Delete",
                onConfirm = { viewModel.deleteManualItem(action.itemId) },
                onDismiss = viewModel::dismissConfirmation
            )
        }
        null -> { /* No confirmation needed */ }
    }
    
    // Show barcode scanner
    if (uiState.showBarcodeScanner) {
        BarcodeScanner(
            onBarcodeDetected = viewModel::processBarcode,
            onDismiss = viewModel::dismissBarcodeScanner
        )
    }
    
    // Show export dialog
    if (uiState.showExportDialog) {
        ExportDialog(
            onExport = viewModel::exportShoppingList,
            onDismiss = viewModel::dismissExportDialog
        )
    }
}

/**
 * Stateless composable for the shopping list content.
 * Handles different UI states and renders appropriate content.
 *
 * @param uiState The current UI state
 * @param onItemCheckedChange Callback when an item's checked status changes
 * @param onItemClick Callback when an item is clicked
 * @param onAddItemClick Callback when the add item button is clicked
 * @param onScanBarcodeClick Callback when the scan barcode button is clicked
 * @param onExportClick Callback when the export button is clicked
 * @param onGenerateList Callback when the generate list button is clicked
 * @param onClearChecked Callback when clear checked items is requested
 * @param onUncheckAll Callback when uncheck all is requested
 * @param onToggleFilter Callback when filter toggle is clicked
 * @param onSearchQueryChange Callback when search query changes
 * @param onMealDetailClick Callback when a meal is clicked from item sources
 * @param snackbarHostState State for showing snackbar messages
 * @param modifier Optional modifier for the content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListContent(
    uiState: ShoppingListUiState,
    onItemCheckedChange: (Long, Boolean) -> Unit,
    onItemClick: (com.shoppit.app.domain.model.ShoppingListItem) -> Unit,
    onAddItemClick: () -> Unit,
    onScanBarcodeClick: () -> Unit,
    onExportClick: () -> Unit,
    onGenerateList: () -> Unit,
    onClearChecked: () -> Unit,
    onUncheckAll: () -> Unit,
    onToggleFilter: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onShareList: () -> Unit,
    onToggleCategoryCollapsed: (ItemCategory) -> Unit,
    onMealDetailClick: (Long) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Shopping List") },
                actions = {
                    IconButton(onClick = onScanBarcodeClick) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Scan barcode"
                        )
                    }
                    IconButton(onClick = onExportClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export shopping list"
                        )
                    }
                    IconButton(onClick = onGenerateList, enabled = !uiState.isGenerating) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(12.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Generate shopping list"
                            )
                        }
                    }
                    IconButton(onClick = onToggleFilter) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter items",
                            tint = if (uiState.filterUncheckedOnly) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(onClick = onShareList) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share shopping list"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItemClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add manual item"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    LoadingScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                
                uiState.shoppingListData == null -> {
                    EmptyState(
                        message = "No shopping list yet",
                        actionLabel = "Generate from meal plans",
                        onActionClick = onGenerateList,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                
                uiState.shoppingListData.totalItems == 0 -> {
                    EmptyState(
                        message = "Your shopping list is empty",
                        actionLabel = "Add items manually or generate from meal plans",
                        onActionClick = onAddItemClick,
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
                    ) {
                        // Summary header
                        ShoppingListSummary(
                            totalItems = uiState.shoppingListData.totalItems,
                            checkedItems = uiState.shoppingListData.checkedItems,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        
                        // Shopping list items grouped by category
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.shoppingListData.itemsByCategory.forEach { (category, items) ->
                                // Filter items based on filter state
                                val filteredItems = if (uiState.filterUncheckedOnly) {
                                    items.filter { !it.isChecked }
                                } else {
                                    items
                                }
                                
                                if (filteredItems.isNotEmpty()) {
                                    val isCollapsed = category in uiState.collapsedCategories
                                    val uncheckedCount = filteredItems.count { !it.isChecked }
                                    
                                    // Category header
                                    item(key = "header_${category.name}") {
                                        CategoryHeader(
                                            category = category,
                                            uncheckedCount = uncheckedCount,
                                            isCollapsed = isCollapsed,
                                            onClick = { onToggleCategoryCollapsed(category) }
                                        )
                                    }
                                    
                                    // Category items (only show if not collapsed)
                                    if (!isCollapsed) {
                                        items(
                                            items = filteredItems,
                                            key = { it.id }
                                        ) { item ->
                                            ShoppingListItemCard(
                                                item = item,
                                                onCheckedChange = { checked ->
                                                    onItemCheckedChange(item.id, checked)
                                                },
                                                onClick = { onItemClick(item) },
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Show loading overlay for bulk operations
            if (uiState.isClearingChecked || uiState.isUncheckingAll) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Displays a summary of the shopping list with progress.
 */
@Composable
fun ShoppingListSummary(
    totalItems: Int,
    checkedItems: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$checkedItems of $totalItems items",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${if (totalItems > 0) (checkedItems * 100 / totalItems) else 0}%",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        LinearProgressIndicator(
            progress = if (totalItems > 0) checkedItems.toFloat() / totalItems else 0f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Collapsible category header with item count.
 */
@Composable
fun CategoryHeader(
    category: ItemCategory,
    uncheckedCount: Int,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.displayName(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (uncheckedCount > 0) {
                Text(
                    text = "($uncheckedCount)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = if (isCollapsed) "Expand category" else "Collapse category",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
