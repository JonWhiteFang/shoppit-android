package com.shoppit.app.presentation.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppit.app.domain.usecase.AddManualItemUseCase
import com.shoppit.app.domain.usecase.ClearCheckedItemsUseCase
import com.shoppit.app.domain.usecase.DeleteManualItemUseCase
import com.shoppit.app.domain.usecase.GenerateShoppingListUseCase
import com.shoppit.app.domain.usecase.GetItemSourcesUseCase
import com.shoppit.app.domain.usecase.GetShoppingListUseCase
import com.shoppit.app.domain.usecase.ToggleItemCheckedUseCase
import com.shoppit.app.domain.usecase.UncheckAllItemsUseCase
import com.shoppit.app.domain.usecase.UpdateManualItemUseCase
import com.shoppit.app.domain.model.ShoppingListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the shopping list screen.
 * Manages shopping list state and handles user actions.
 */
@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val getShoppingListUseCase: GetShoppingListUseCase,
    private val generateShoppingListUseCase: GenerateShoppingListUseCase,
    private val toggleItemCheckedUseCase: ToggleItemCheckedUseCase,
    private val addManualItemUseCase: AddManualItemUseCase,
    private val updateManualItemUseCase: UpdateManualItemUseCase,
    private val deleteManualItemUseCase: DeleteManualItemUseCase,
    private val clearCheckedItemsUseCase: ClearCheckedItemsUseCase,
    private val uncheckAllItemsUseCase: UncheckAllItemsUseCase,
    private val getItemSourcesUseCase: GetItemSourcesUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()
    
    init {
        loadShoppingList()
    }
    
    /**
     * Loads the shopping list with real-time updates.
     */
    private fun loadShoppingList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getShoppingListUseCase()
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load shopping list"
                        )
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { data ->
                            _uiState.update { 
                                it.copy(
                                    shoppingListData = data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load shopping list"
                                )
                            }
                        }
                    )
                }
        }
    }
    
    /**
     * Generates a shopping list from the current week's meal plans.
     */
    fun generateShoppingList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            
            generateShoppingListUseCase().fold(
                onSuccess = {
                    _uiState.update { it.copy(isGenerating = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isGenerating = false,
                            error = error.message ?: "Failed to generate shopping list"
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Toggles the checked status of a shopping list item.
     */
    fun toggleItemChecked(itemId: Long, isChecked: Boolean) {
        viewModelScope.launch {
            toggleItemCheckedUseCase(itemId, isChecked).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to update item")
                    }
                }
            )
        }
    }
    
    /**
     * Shows the add item dialog.
     */
    fun showAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = true) }
    }
    
    /**
     * Dismisses the add item dialog.
     */
    fun dismissAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = false) }
    }
    
    /**
     * Adds a manual shopping list item.
     */
    fun addManualItem(name: String, quantity: String, unit: String) {
        viewModelScope.launch {
            addManualItemUseCase(name, quantity, unit).fold(
                onSuccess = {
                    _uiState.update { it.copy(showAddItemDialog = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to add item")
                    }
                }
            )
        }
    }
    
    /**
     * Shows the item detail dialog and loads source meals if applicable.
     */
    fun showItemDetail(item: ShoppingListItem) {
        viewModelScope.launch {
            if (item.isManual) {
                _uiState.update { 
                    it.copy(
                        showItemDetail = item,
                        itemSources = emptyList()
                    )
                }
            } else {
                getItemSourcesUseCase(item.name, item.mealIds).fold(
                    onSuccess = { sources ->
                        _uiState.update { 
                            it.copy(
                                showItemDetail = item,
                                itemSources = sources
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(error = error.message ?: "Failed to load item sources")
                        }
                    }
                )
            }
        }
    }
    
    /**
     * Dismisses the item detail dialog.
     */
    fun dismissItemDetail() {
        _uiState.update { 
            it.copy(
                showItemDetail = null,
                itemSources = emptyList()
            )
        }
    }
    
    /**
     * Shows confirmation dialog for deleting a manual item.
     */
    fun showDeleteManualItemConfirmation(itemId: Long) {
        _uiState.update { it.copy(confirmationAction = ConfirmationAction.DeleteManualItem(itemId)) }
    }
    
    /**
     * Deletes a manual shopping list item after confirmation.
     */
    fun deleteManualItem(itemId: Long) {
        viewModelScope.launch {
            deleteManualItemUseCase(itemId).fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            showItemDetail = null,
                            confirmationAction = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to delete item",
                            confirmationAction = null
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Shows confirmation dialog for clearing checked items.
     */
    fun showClearCheckedConfirmation() {
        _uiState.update { it.copy(confirmationAction = ConfirmationAction.ClearChecked) }
    }
    
    /**
     * Clears all checked items from the shopping list after confirmation.
     */
    fun clearCheckedItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingChecked = true, confirmationAction = null) }
            
            clearCheckedItemsUseCase().fold(
                onSuccess = { 
                    _uiState.update { it.copy(isClearingChecked = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to clear checked items",
                            isClearingChecked = false
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Shows confirmation dialog for unchecking all items.
     */
    fun showUncheckAllConfirmation() {
        _uiState.update { it.copy(confirmationAction = ConfirmationAction.UncheckAll) }
    }
    
    /**
     * Unchecks all items in the shopping list after confirmation.
     */
    fun uncheckAllItems() {
        viewModelScope.launch {
            _uiState.update { it.copy(isUncheckingAll = true, confirmationAction = null) }
            
            uncheckAllItemsUseCase().fold(
                onSuccess = { 
                    _uiState.update { it.copy(isUncheckingAll = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to uncheck items",
                            isUncheckingAll = false
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Toggles the filter to show only unchecked items.
     */
    fun toggleFilter() {
        _uiState.update { it.copy(filterUncheckedOnly = !it.filterUncheckedOnly) }
    }
    
    /**
     * Toggles the collapsed state of a category.
     */
    fun toggleCategoryCollapsed(category: com.shoppit.app.domain.model.ItemCategory) {
        _uiState.update { currentState ->
            val collapsedCategories = currentState.collapsedCategories.toMutableSet()
            if (category in collapsedCategories) {
                collapsedCategories.remove(category)
            } else {
                collapsedCategories.add(category)
            }
            currentState.copy(collapsedCategories = collapsedCategories)
        }
    }
    
    /**
     * Updates the search query.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * Dismisses the confirmation dialog.
     */
    fun dismissConfirmation() {
        _uiState.update { it.copy(confirmationAction = null) }
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Generates formatted text for sharing the shopping list.
     * Only includes unchecked items, grouped by category.
     */
    fun shareShoppingList() {
        val data = uiState.value.shoppingListData ?: return
        
        val text = buildString {
            appendLine("Shopping List")
            appendLine()
            
            data.itemsByCategory.forEach { (category, items) ->
                val uncheckedItems = items.filter { !it.isChecked }
                if (uncheckedItems.isNotEmpty()) {
                    appendLine(category.displayName())
                    uncheckedItems.forEach { item ->
                        appendLine("  - ${item.name} (${item.quantity} ${item.unit})")
                    }
                    appendLine()
                }
            }
        }
        
        _uiState.update { it.copy(shareText = text) }
    }
    
    /**
     * Clears the share text after sharing is complete.
     */
    fun clearShareText() {
        _uiState.update { it.copy(shareText = null) }
    }
}
