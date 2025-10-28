package com.shoppit.app.presentation.ui.shopping

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.ErrorScreen
import com.shoppit.app.presentation.ui.common.LoadingScreen

/**
 * Stateful composable for the shopping list screen.
 * Injects the ViewModel and manages state collection.
 *
 * @param onMealDetailClick Callback when a meal is clicked from item sources
 * @param viewModel The ViewModel for managing shopping list state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun ShoppingListScreen(
    onMealDetailClick: (Long) -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    
    ShoppingListContent(
        uiState = uiState,
        onItemCheckedChange = viewModel::toggleItemChecked,
        onItemClick = viewModel::showItemDetail,
        onAddItemClick = viewModel::showAddItemDialog,
        onGenerateList = viewModel::generateShoppingList,
        onClearChecked = viewModel::clearCheckedItems,
        onUncheckAll = viewModel::uncheckAllItems,
        onToggleFilter = viewModel::toggleFilter,
        onSearchQueryChange = viewModel::updateSearchQuery,
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
                { viewModel.deleteManualItem(item.id) }
            } else null,
            onMealClick = onMealDetailClick
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
    onGenerateList: () -> Unit,
    onClearChecked: () -> Unit,
    onUncheckAll: () -> Unit,
    onToggleFilter: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
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
                    IconButton(onClick = onGenerateList) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator()
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
                    IconButton(onClick = { /* TODO: Implement share */ }) {
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
                            val filteredItems = if (uiState.filterUncheckedOnly) {
                                items.filter { !it.isChecked }
                            } else {
                                items
                            }
                            
                            if (filteredItems.isNotEmpty()) {
                                // Category header
                                item(key = "header_${category.name}") {
                                    Text(
                                        text = category.displayName(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                
                                // Category items
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
