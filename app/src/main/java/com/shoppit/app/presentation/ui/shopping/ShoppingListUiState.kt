package com.shoppit.app.presentation.ui.shopping

import androidx.compose.runtime.Stable
import com.shoppit.app.domain.model.IngredientSource
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListData
import com.shoppit.app.domain.model.ShoppingListItem

/**
 * Sealed class representing different confirmation actions.
 */
sealed class ConfirmationAction {
    data object ClearChecked : ConfirmationAction()
    data object UncheckAll : ConfirmationAction()
    data class DeleteManualItem(val itemId: Long) : ConfirmationAction()
}

/**
 * UI state for the shopping list screen.
 * Contains all state needed to render the shopping list UI.
 */
@Stable
data class ShoppingListUiState(
    val shoppingListData: ShoppingListData? = null,
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isClearingChecked: Boolean = false,
    val isUncheckingAll: Boolean = false,
    val error: String? = null,
    val showAddItemDialog: Boolean = false,
    val showItemDetail: ShoppingListItem? = null,
    val itemSources: List<IngredientSource> = emptyList(),
    val filterUncheckedOnly: Boolean = false,
    val searchQuery: String = "",
    val shareText: String? = null,
    val confirmationAction: ConfirmationAction? = null,
    val collapsedCategories: Set<ItemCategory> = emptySet(),
    
    // Shopping mode state (Task 5.1)
    val shoppingModePreferences: com.shoppit.app.domain.model.ShoppingModePreferences = 
        com.shoppit.app.domain.model.ShoppingModePreferences(),
    
    // Quick add state (Task 5.2)
    val frequentItems: List<com.shoppit.app.domain.model.ItemHistory> = emptyList(),
    val showQuickAddSheet: Boolean = false,
    val isLoadingHistory: Boolean = false,
    
    // Section management state (Task 5.5)
    val storeSections: List<com.shoppit.app.domain.model.StoreSection> = emptyList(),
    
    // Item enhancement state (Task 5.6)
    val showNoteDialog: ShoppingListItem? = null,
    
    // Template management state (Task 5.7)
    val templates: List<com.shoppit.app.domain.model.ShoppingTemplate> = emptyList(),
    val showSaveTemplateDialog: Boolean = false,
    val showLoadTemplateDialog: Boolean = false,
    val isLoadingTemplates: Boolean = false,
    val isSavingTemplate: Boolean = false,
    val isLoadingTemplate: Boolean = false,
    
    // Budget tracking state (Task 5.8)
    val budgetSummary: com.shoppit.app.domain.model.BudgetSummary? = null,
    val showPriceDialog: ShoppingListItem? = null,
    
    // Voice input state (Task 5.9)
    val showVoiceInputDialog: Boolean = false,
    val isProcessingVoice: Boolean = false,
    
    // Suggestions state (Task 5.10)
    val suggestedItems: List<String> = emptyList(),
    
    // Barcode scanning state (Task 7.1)
    val showBarcodeScanner: Boolean = false,
    val isProcessingBarcode: Boolean = false,
    
    // Undo functionality state (Task 7.2)
    val lastCheckedItem: ShoppingListItem? = null,
    val showUndoSnackbar: Boolean = false,
    
    // Export functionality state (Task 7.3)
    val showExportDialog: Boolean = false,
    val exportData: String? = null,
    val clipboardData: String? = null
)
