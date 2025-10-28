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
    private val getItemSourcesUseCase: GetItemSourcesUseCase,
    // New use cases for management features
    private val toggleShoppingModeUseCase: com.shoppit.app.domain.usecase.ToggleShoppingModeUseCase,
    private val getItemHistoryUseCase: com.shoppit.app.domain.usecase.GetItemHistoryUseCase,
    private val quickAddItemUseCase: com.shoppit.app.domain.usecase.QuickAddItemUseCase,
    private val adjustQuantityUseCase: com.shoppit.app.domain.usecase.AdjustQuantityUseCase,
    private val reorderItemsUseCase: com.shoppit.app.domain.usecase.ReorderItemsUseCase,
    private val moveItemToSectionUseCase: com.shoppit.app.domain.usecase.MoveItemToSectionUseCase,
    private val addItemNoteUseCase: com.shoppit.app.domain.usecase.AddItemNoteUseCase,
    private val togglePriorityUseCase: com.shoppit.app.domain.usecase.TogglePriorityUseCase,
    private val duplicateItemUseCase: com.shoppit.app.domain.usecase.DuplicateItemUseCase,
    private val saveTemplateUseCase: com.shoppit.app.domain.usecase.SaveTemplateUseCase,
    private val loadTemplateUseCase: com.shoppit.app.domain.usecase.LoadTemplateUseCase,
    private val updatePriceEstimateUseCase: com.shoppit.app.domain.usecase.UpdatePriceEstimateUseCase,
    private val getBudgetSummaryUseCase: com.shoppit.app.domain.usecase.GetBudgetSummaryUseCase,
    private val processVoiceInputUseCase: com.shoppit.app.domain.usecase.ProcessVoiceInputUseCase,
    private val getSuggestedItemsUseCase: com.shoppit.app.domain.usecase.GetSuggestedItemsUseCase,
    private val scanBarcodeUseCase: com.shoppit.app.domain.usecase.ScanBarcodeUseCase,
    private val exportShoppingListUseCase: com.shoppit.app.domain.usecase.ExportShoppingListUseCase,
    private val templateRepository: com.shoppit.app.domain.repository.TemplateRepository,
    private val storeSectionRepository: com.shoppit.app.domain.repository.StoreSectionRepository,
    private val updateItemHistoryUseCase: com.shoppit.app.domain.usecase.UpdateItemHistoryUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()
    
    init {
        loadShoppingList()
        loadShoppingModeState()
        loadFrequentItems()
        loadTemplates()
        loadStoreSections()
        loadBudgetSummary()
        loadSuggestedItems()
        observeCheckedItemsForHistory()
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
     * Observes checked items and updates purchase history.
     * Tracks purchase counts and average prices for items.
     * 
     * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
     */
    private fun observeCheckedItemsForHistory() {
        viewModelScope.launch {
            getShoppingListUseCase()
                .collect { result ->
                    result.fold(
                        onSuccess = { data ->
                            // Track newly checked items
                            data.itemsByCategory.values.flatten()
                                .filter { it.isChecked }
                                .forEach { item ->
                                    // Update history for checked items
                                    updateItemHistoryUseCase(item)
                                }
                        },
                        onFailure = { /* Ignore errors in history tracking */ }
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
     * Tracks the item for undo functionality when checking.
     * Updates purchase history when item is checked.
     */
    fun toggleItemChecked(itemId: Long, isChecked: Boolean) {
        viewModelScope.launch {
            // If checking an item, store it for undo and update history
            if (isChecked) {
                val item = uiState.value.shoppingListData?.itemsByCategory
                    ?.flatMap { it.value }
                    ?.find { it.id == itemId }
                
                if (item != null) {
                    _uiState.update { 
                        it.copy(
                            lastCheckedItem = item,
                            showUndoSnackbar = true
                        )
                    }
                    
                    // Update history when item is checked
                    updateItemHistoryUseCase(item).fold(
                        onSuccess = { /* History updated successfully */ },
                        onFailure = { /* Ignore history update errors */ }
                    )
                }
            }
            
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
    
    // ========== Shopping Mode Management (Task 5.1) ==========
    
    /**
     * Loads shopping mode state and observes changes.
     */
    private fun loadShoppingModeState() {
        viewModelScope.launch {
            toggleShoppingModeUseCase.getShoppingModeState()
                .catch { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to load shopping mode state")
                    }
                }
                .collect { preferences ->
                    _uiState.update { it.copy(shoppingModePreferences = preferences) }
                }
        }
    }
    
    /**
     * Toggles shopping mode on/off.
     */
    fun toggleShoppingMode(enabled: Boolean) {
        viewModelScope.launch {
            toggleShoppingModeUseCase(enabled).fold(
                onSuccess = { /* State updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to toggle shopping mode")
                    }
                }
            )
        }
    }
    
    // ========== Quick Add Functionality (Task 5.2) ==========
    
    /**
     * Loads frequent items for quick add on initialization.
     */
    private fun loadFrequentItems() {
        viewModelScope.launch {
            getItemHistoryUseCase.getFrequentItems(20)
                .catch { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to load frequent items")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { items ->
                            _uiState.update { it.copy(frequentItems = items) }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(error = error.message ?: "Failed to load frequent items")
                            }
                        }
                    )
                }
        }
    }
    
    /**
     * Quickly adds an item from history to the shopping list.
     */
    fun quickAddItem(historyItem: com.shoppit.app.domain.model.ItemHistory) {
        viewModelScope.launch {
            quickAddItemUseCase(historyItem).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to add item")
                    }
                }
            )
        }
    }
    
    /**
     * Shows the quick add sheet.
     */
    fun showQuickAddSheet() {
        _uiState.update { it.copy(showQuickAddSheet = true) }
    }
    
    /**
     * Dismisses the quick add sheet.
     */
    fun dismissQuickAddSheet() {
        _uiState.update { it.copy(showQuickAddSheet = false) }
    }
    
    // ========== Quantity Adjustment (Task 5.3) ==========
    
    /**
     * Increments the quantity of an item.
     */
    fun incrementQuantity(itemId: Long) {
        viewModelScope.launch {
            adjustQuantityUseCase(itemId, increment = true).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to adjust quantity")
                    }
                }
            )
        }
    }
    
    /**
     * Decrements the quantity of an item.
     */
    fun decrementQuantity(itemId: Long) {
        viewModelScope.launch {
            adjustQuantityUseCase(itemId, increment = false).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to adjust quantity")
                    }
                }
            )
        }
    }
    
    // ========== Item Reordering (Task 5.4) ==========
    
    /**
     * Reorders an item to a new position.
     */
    fun reorderItem(itemId: Long, newPosition: Int) {
        viewModelScope.launch {
            reorderItemsUseCase(itemId, newPosition).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to reorder item")
                    }
                }
            )
        }
    }
    
    // ========== Section Management (Task 5.5) ==========
    
    /**
     * Loads store sections on initialization.
     */
    private fun loadStoreSections() {
        viewModelScope.launch {
            storeSectionRepository.getAllSections()
                .catch { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to load store sections")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { sections ->
                            _uiState.update { it.copy(storeSections = sections) }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(error = error.message ?: "Failed to load store sections")
                            }
                        }
                    )
                }
        }
    }
    
    /**
     * Moves an item to a different section.
     */
    fun moveItemToSection(itemId: Long, sectionName: String) {
        viewModelScope.launch {
            moveItemToSectionUseCase(itemId, sectionName).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to move item")
                    }
                }
            )
        }
    }
    
    /**
     * Toggles the collapsed state of a section.
     */
    fun toggleSectionCollapsed(sectionId: Long, isCollapsed: Boolean) {
        viewModelScope.launch {
            storeSectionRepository.toggleSectionCollapsed(sectionId, isCollapsed).fold(
                onSuccess = { /* Sections update automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to toggle section")
                    }
                }
            )
        }
    }
    
    // ========== Item Enhancement Actions (Task 5.6) ==========
    
    /**
     * Shows the note dialog for an item.
     */
    fun showNoteDialog(item: ShoppingListItem) {
        _uiState.update { it.copy(showNoteDialog = item) }
    }
    
    /**
     * Dismisses the note dialog.
     */
    fun dismissNoteDialog() {
        _uiState.update { it.copy(showNoteDialog = null) }
    }
    
    /**
     * Adds or updates a note for an item.
     */
    fun addItemNote(itemId: Long, notes: String) {
        viewModelScope.launch {
            addItemNoteUseCase(itemId, notes).fold(
                onSuccess = { 
                    _uiState.update { it.copy(showNoteDialog = null) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to add note")
                    }
                }
            )
        }
    }
    
    /**
     * Toggles the priority status of an item.
     */
    fun toggleItemPriority(itemId: Long, isPriority: Boolean) {
        viewModelScope.launch {
            togglePriorityUseCase(itemId, isPriority).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to toggle priority")
                    }
                }
            )
        }
    }
    
    /**
     * Duplicates an item.
     */
    fun duplicateItem(itemId: Long) {
        viewModelScope.launch {
            duplicateItemUseCase(itemId).fold(
                onSuccess = { /* List updates automatically via Flow */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to duplicate item")
                    }
                }
            )
        }
    }
    
    // ========== Template Management (Task 5.7) ==========
    
    /**
     * Loads templates on initialization.
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            templateRepository.getAllTemplates()
                .catch { e ->
                    _uiState.update { 
                        it.copy(error = e.message ?: "Failed to load templates")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { templates ->
                            _uiState.update { it.copy(templates = templates) }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(error = error.message ?: "Failed to load templates")
                            }
                        }
                    )
                }
        }
    }
    
    /**
     * Shows the save template dialog.
     */
    fun showSaveTemplateDialog() {
        _uiState.update { it.copy(showSaveTemplateDialog = true) }
    }
    
    /**
     * Dismisses the save template dialog.
     */
    fun dismissSaveTemplateDialog() {
        _uiState.update { it.copy(showSaveTemplateDialog = false) }
    }
    
    /**
     * Saves the current shopping list as a template.
     */
    fun saveTemplate(name: String, description: String) {
        viewModelScope.launch {
            saveTemplateUseCase(name, description).fold(
                onSuccess = { 
                    _uiState.update { it.copy(showSaveTemplateDialog = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to save template")
                    }
                }
            )
        }
    }
    
    /**
     * Shows the load template dialog.
     */
    fun showLoadTemplateDialog() {
        _uiState.update { it.copy(showLoadTemplateDialog = true) }
    }
    
    /**
     * Dismisses the load template dialog.
     */
    fun dismissLoadTemplateDialog() {
        _uiState.update { it.copy(showLoadTemplateDialog = false) }
    }
    
    /**
     * Loads a template into the shopping list.
     */
    fun loadTemplate(templateId: Long) {
        viewModelScope.launch {
            loadTemplateUseCase(templateId).fold(
                onSuccess = { 
                    _uiState.update { it.copy(showLoadTemplateDialog = false) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to load template")
                    }
                }
            )
        }
    }
    
    // ========== Budget Tracking (Task 5.8) ==========
    
    /**
     * Loads budget summary on initialization.
     */
    private fun loadBudgetSummary() {
        viewModelScope.launch {
            // Reload budget summary whenever shopping list changes
            getShoppingListUseCase()
                .collect { result ->
                    result.fold(
                        onSuccess = {
                            // Fetch budget summary
                            getBudgetSummaryUseCase().fold(
                                onSuccess = { summary ->
                                    _uiState.update { it.copy(budgetSummary = summary) }
                                },
                                onFailure = { error ->
                                    _uiState.update { 
                                        it.copy(error = error.message ?: "Failed to load budget summary")
                                    }
                                }
                            )
                        },
                        onFailure = { /* Already handled in loadShoppingList */ }
                    )
                }
        }
    }
    
    /**
     * Shows the price dialog for an item.
     */
    fun showPriceDialog(item: ShoppingListItem) {
        _uiState.update { it.copy(showPriceDialog = item) }
    }
    
    /**
     * Dismisses the price dialog.
     */
    fun dismissPriceDialog() {
        _uiState.update { it.copy(showPriceDialog = null) }
    }
    
    /**
     * Updates the price estimate for an item.
     */
    fun updateItemPrice(itemId: Long, price: Double?) {
        viewModelScope.launch {
            updatePriceEstimateUseCase(itemId, price).fold(
                onSuccess = { 
                    _uiState.update { it.copy(showPriceDialog = null) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to update price")
                    }
                }
            )
        }
    }
    
    // ========== Voice Input (Task 5.9) ==========
    
    /**
     * Shows the voice input dialog.
     */
    fun showVoiceInputDialog() {
        _uiState.update { it.copy(showVoiceInputDialog = true) }
    }
    
    /**
     * Dismisses the voice input dialog.
     */
    fun dismissVoiceInputDialog() {
        _uiState.update { it.copy(showVoiceInputDialog = false, isProcessingVoice = false) }
    }
    
    /**
     * Processes voice input and adds the item to the shopping list.
     */
    fun processVoiceInput(voiceText: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingVoice = true) }
            
            processVoiceInputUseCase(voiceText).fold(
                onSuccess = { 
                    _uiState.update { 
                        it.copy(
                            showVoiceInputDialog = false,
                            isProcessingVoice = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to process voice input",
                            isProcessingVoice = false
                        )
                    }
                }
            )
        }
    }
    
    // ========== Suggestions (Task 5.10) ==========
    
    /**
     * Loads suggested items on initialization.
     */
    private fun loadSuggestedItems() {
        viewModelScope.launch {
            getSuggestedItemsUseCase().fold(
                onSuccess = { suggestions ->
                    _uiState.update { it.copy(suggestedItems = suggestions) }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to load suggestions")
                    }
                }
            )
        }
    }
    
    /**
     * Adds a suggested item to the shopping list.
     */
    fun addSuggestedItem(itemName: String) {
        viewModelScope.launch {
            addManualItemUseCase(itemName, "1", "").fold(
                onSuccess = { 
                    // Remove from suggestions
                    _uiState.update { currentState ->
                        currentState.copy(
                            suggestedItems = currentState.suggestedItems.filter { it != itemName }
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to add suggested item")
                    }
                }
            )
        }
    }
    
    /**
     * Dismisses a suggested item.
     */
    fun dismissSuggestedItem(itemName: String) {
        _uiState.update { currentState ->
            currentState.copy(
                suggestedItems = currentState.suggestedItems.filter { it != itemName }
            )
        }
    }
    
    // ========== Barcode Scanning (Task 7.1) ==========
    
    /**
     * Shows the barcode scanner.
     */
    fun showBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScanner = true) }
    }
    
    /**
     * Dismisses the barcode scanner.
     */
    fun dismissBarcodeScanner() {
        _uiState.update { it.copy(showBarcodeScanner = false, isProcessingBarcode = false) }
    }
    
    /**
     * Processes a scanned barcode and adds the item to the shopping list.
     */
    fun processBarcode(barcode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingBarcode = true) }
            
            scanBarcodeUseCase(com.shoppit.app.domain.usecase.ScanBarcodeUseCase.Params(barcode)).fold(
                onSuccess = { 
                    _uiState.update { 
                        it.copy(
                            showBarcodeScanner = false,
                            isProcessingBarcode = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to process barcode",
                            isProcessingBarcode = false
                        )
                    }
                }
            )
        }
    }
    
    // ========== Undo Functionality (Task 7.2) ==========
    
    /**
     * Undoes the last check action.
     */
    fun undoLastCheck() {
        viewModelScope.launch {
            val lastItem = uiState.value.lastCheckedItem
            if (lastItem != null) {
                toggleItemCheckedUseCase(lastItem.id, false).fold(
                    onSuccess = { 
                        _uiState.update { 
                            it.copy(
                                lastCheckedItem = null,
                                showUndoSnackbar = false
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(
                                error = error.message ?: "Failed to undo",
                                lastCheckedItem = null,
                                showUndoSnackbar = false
                            )
                        }
                    }
                )
            }
        }
    }
    
    /**
     * Dismisses the undo snackbar.
     */
    fun dismissUndoSnackbar() {
        _uiState.update { 
            it.copy(
                lastCheckedItem = null,
                showUndoSnackbar = false
            )
        }
    }
    
    // ========== Export Functionality (Task 7.3) ==========
    
    /**
     * Shows the export dialog.
     */
    fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }
    
    /**
     * Dismisses the export dialog.
     */
    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }
    
    /**
     * Exports the shopping list in the specified format.
     */
    fun exportShoppingList(
        format: com.shoppit.app.domain.usecase.ExportShoppingListUseCase.ExportFormat,
        action: com.shoppit.app.presentation.ui.shopping.ExportAction
    ) {
        viewModelScope.launch {
            exportShoppingListUseCase(
                com.shoppit.app.domain.usecase.ExportShoppingListUseCase.Params(format)
            ).fold(
                onSuccess = { exportedData ->
                    when (action) {
                        com.shoppit.app.presentation.ui.shopping.ExportAction.SHARE -> {
                            _uiState.update { 
                                it.copy(
                                    exportData = exportedData,
                                    showExportDialog = false
                                )
                            }
                        }
                        com.shoppit.app.presentation.ui.shopping.ExportAction.COPY_TO_CLIPBOARD -> {
                            _uiState.update { 
                                it.copy(
                                    clipboardData = exportedData,
                                    showExportDialog = false
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            error = error.message ?: "Failed to export shopping list",
                            showExportDialog = false
                        )
                    }
                }
            )
        }
    }
    
    /**
     * Clears the export data after sharing is complete.
     */
    fun clearExportData() {
        _uiState.update { it.copy(exportData = null) }
    }
    
    /**
     * Clears the clipboard data after copying is complete.
     */
    fun clearClipboardData() {
        _uiState.update { it.copy(clipboardData = null) }
    }
}
