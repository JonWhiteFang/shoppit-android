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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.presentation.ui.common.EmptyState
import com.shoppit.app.presentation.ui.common.LoadingScreen

/**
 * Stateful composable for the shopping mode screen.
 * Provides a simplified interface optimized for in-store shopping.
 *
 * @param onExitShoppingMode Callback when user exits shopping mode
 * @param viewModel The ViewModel for managing shopping list state
 * @param modifier Optional modifier for the screen
 */
@Composable
fun ShoppingModeScreen(
    onExitShoppingMode: () -> Unit,
    viewModel: ShoppingListViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ShoppingModeContent(
        uiState = uiState,
        onItemCheckedChange = viewModel::toggleItemChecked,
        onExitShoppingMode = {
            viewModel.toggleShoppingMode(false)
            onExitShoppingMode()
        },
        modifier = modifier
    )
}

/**
 * Stateless composable for the shopping mode content.
 * Displays a simplified view with large text and only unchecked items.
 *
 * @param uiState The current UI state
 * @param onItemCheckedChange Callback when an item's checked status changes
 * @param onExitShoppingMode Callback when user exits shopping mode
 * @param modifier Optional modifier for the content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingModeContent(
    uiState: ShoppingListUiState,
    onItemCheckedChange: (Long, Boolean) -> Unit,
    onExitShoppingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Shopping Mode",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onExitShoppingMode) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Exit shopping mode"
                        )
                    }
                }
            )
        }
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
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
                
                else -> {
                    // Filter to show only unchecked items
                    val uncheckedItems = uiState.shoppingListData.itemsByCategory
                        .flatMap { (_, items) -> items }
                        .filter { !it.isChecked }
                    
                    if (uncheckedItems.isEmpty()) {
                        EmptyState(
                            message = "All items checked!\nGreat job shopping!",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            // Summary header with large text
                            ShoppingModeSummary(
                                totalItems = uiState.shoppingListData.totalItems,
                                checkedItems = uiState.shoppingListData.checkedItems,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                            
                            // Shopping list items with large text
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                            ) {
                                items(
                                    items = uncheckedItems,
                                    key = { it.id }
                                ) { item ->
                                    ShoppingModeItemCard(
                                        item = item,
                                        onCheckedChange = { checked ->
                                            onItemCheckedChange(item.id, checked)
                                        }
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
 * Displays a summary of the shopping progress with large text.
 */
@Composable
fun ShoppingModeSummary(
    totalItems: Int,
    checkedItems: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$checkedItems of $totalItems",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (totalItems > 0) (checkedItems * 100 / totalItems) else 0}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
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
 * Simplified item card for shopping mode with large text.
 * Shows only essential information: checkbox, name, and quantity.
 */
@Composable
fun ShoppingModeItemCard(
    item: ShoppingListItem,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Item name with 20% larger text
                Text(
                    text = item.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Quantity with larger text
                if (item.quantity.isNotBlank() || item.unit.isNotBlank()) {
                    Text(
                        text = "${item.quantity} ${item.unit}".trim(),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
