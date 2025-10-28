# Implementation Plan

- [x] 1. Extend domain models with management features
  - Add new fields to ShoppingListItem (notes, isPriority, customOrder, estimatedPrice, storeSection, lastModifiedAt)
  - Create ItemHistory data class for purchase tracking
  - Create ShoppingTemplate and TemplateItem data classes
  - Create StoreSection data class for custom organization
  - Define ShoppingModePreferences and BudgetSummary data classes
  - _Requirements: 1.1, 2.1, 3.1, 5.1, 7.1, 8.1, 10.1, 11.1, 12.1_

- [ ] 2. Implement database schema extensions
  - [x] 2.1 Create database migration from version 3 to 4
    - Add new columns to shopping_list_items table
    - Create item_history table with indices
    - Create shopping_templates and template_items tables
    - Create store_sections table
    - Add indices for new columns (is_priority, store_section)
    - _Requirements: 1.5, 3.4, 5.5, 10.5, 11.4_
  
  - [x] 2.2 Create new DAOs
    - Implement ItemHistoryDao with CRUD operations
    - Implement TemplateDao with template and item operations
    - Implement StoreSectionDao for section management
    - Extend ShoppingListDao with new query methods
    - _Requirements: 1.1, 10.1, 11.1, 5.1_
  
  - [x] 2.3 Update database class
    - Register new entities in AppDatabase
    - Add new DAOs to database
    - Update database version to 4
    - Register migration in database builder
    - _Requirements: 1.5, 10.5, 11.4_

- [x] 3. Implement new repositories
  - [x] 3.1 Create ItemHistoryRepository
    - Define interface with Flow-based queries
    - Implement ItemHistoryRepositoryImpl with error handling
    - Add methods for recent history and frequent items
    - Implement purchase count increment logic
    - Create mapper functions for entity/domain conversions
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 3.2 Create TemplateRepository
    - Define interface for template CRUD operations
    - Implement TemplateRepositoryImpl with transaction support
    - Add methods for saving and loading templates
    - Implement last used timestamp tracking
    - Create mapper functions for template entities
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 3.3 Create StoreSectionRepository
    - Define interface for section management
    - Implement StoreSectionRepositoryImpl
    - Add methods for section ordering and collapse state
    - Implement custom section creation
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 3.4 Extend ShoppingListRepository
    - Add methods for notes, priority, order, price updates
    - Add method for moving items between sections
    - Add method for duplicating items
    - Add method for budget summary calculation
    - Update existing implementation with new methods
    - _Requirements: 2.1, 2.4, 3.1, 3.3, 6.1, 6.3, 7.1, 7.5, 8.1, 8.4, 9.1, 9.4, 12.1, 12.4_

- [x] 4. Implement management use cases
  - [x] 4.1 Create quick add and history use cases
    - Implement QuickAddItemUseCase with history lookup
    - Implement GetItemHistoryUseCase with sorting
    - Add logic to update history when items are checked
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 4.2 Create quantity and ordering use cases
    - Implement AdjustQuantityUseCase with validation
    - Implement ReorderItemsUseCase for drag-and-drop
    - Add numeric quantity validation
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [x] 4.3 Create shopping mode use cases
    - Implement ToggleShoppingModeUseCase
    - Add preferences storage for shopping mode settings
    - Implement mode state retrieval
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 4.4 Create section management use cases
    - Implement MoveItemToSectionUseCase with learning
    - Add section preference storage
    - Implement section learning logic
    - _Requirements: 5.1, 5.2, 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [x] 4.5 Create item enhancement use cases
    - Implement AddItemNoteUseCase
    - Implement TogglePriorityUseCase
    - Implement DuplicateItemUseCase
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [x] 4.6 Create template use cases
    - Implement SaveTemplateUseCase with validation
    - Implement LoadTemplateUseCase with merge logic
    - Add template last used tracking
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 4.7 Create budget tracking use cases
    - Implement UpdatePriceEstimateUseCase
    - Add budget summary calculation
    - Implement history price averaging
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [x] 4.8 Create voice input use case
    - Implement ProcessVoiceInputUseCase
    - Add voice text parsing logic
    - Handle quantity and unit extraction
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_
  
  - [x] 4.9 Create suggestions use case
    - Implement GetSuggestedItemsUseCase
    - Add rule-based suggestion logic
    - Implement meal plan analysis
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [x] 5. Extend ViewModel with new features
  - [x] 5.1 Add shopping mode state management
    - Add shopping mode toggle to UI state
    - Implement mode switching logic
    - Add filtered view for shopping mode
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 5.2 Add quick add functionality
    - Add frequent items state to ViewModel
    - Implement quick add action
    - Add history loading on init
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [x] 5.3 Add quantity adjustment actions
    - Implement increment quantity action
    - Implement decrement quantity action
    - Add validation for numeric quantities
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  
  - [x] 5.4 Add item reordering support
    - Implement reorder action
    - Add custom order state tracking
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [x] 5.5 Add section management actions
    - Implement move to section action
    - Add section list state
    - Implement section collapse toggle
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 6.1, 6.2, 6.3, 6.4, 6.5_
  
  - [x] 5.6 Add item enhancement actions
    - Implement add/edit note action
    - Implement toggle priority action
    - Implement duplicate item action
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 8.1, 8.2, 8.3, 8.4, 8.5, 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [x] 5.7 Add template management actions
    - Implement save template action
    - Implement load template action
    - Add template list state
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 5.8 Add budget tracking state
    - Add budget summary to UI state
    - Implement price update action
    - Add budget calculation
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [x] 5.9 Add voice input action
    - Implement voice input trigger
    - Add voice processing state
    - Handle voice input results
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_
  
  - [x] 5.10 Add suggestions state
    - Add suggested items to UI state
    - Implement suggestion loading
    - Add suggestion acceptance action
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [x] 6. Build enhanced UI components
  - [x] 6.1 Create QuickAddSheet composable
    - Build bottom sheet with frequent items grid
    - Display item names with icons
    - Implement tap to add functionality
    - Add loading state for history
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [x] 6.2 Enhance ShoppingListItemCard
    - Add increment/decrement buttons for numeric quantities
    - Add priority indicator badge
    - Add note indicator icon
    - Add price display when available
    - Implement long-press menu for actions
    - _Requirements: 2.1, 2.2, 2.3, 7.3, 7.4, 8.2, 12.1_
  
  - [x] 6.3 Create ShoppingModeScreen
    - Build simplified layout with large text
    - Hide checked items automatically
    - Show only essential information
    - Add quick toggle to exit mode
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 6.4 Create StoreSectionEditor
    - Build section list with drag handles
    - Implement section reordering
    - Add custom section creation dialog
    - Add section color picker
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 6.5 Create ItemHistoryScreen
    - Display history list with purchase counts
    - Show last purchased dates
    - Add tap to add functionality
    - Implement search/filter
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 6.6 Create TemplateManagerScreen
    - Display template list with metadata
    - Add create template dialog
    - Implement template load action
    - Add template delete confirmation
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_
  
  - [x] 6.7 Create ItemNoteDialog
    - Build note input dialog
    - Add multiline text field
    - Implement save and cancel actions
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [x] 6.8 Create BudgetSummaryCard
    - Display total estimated cost
    - Show checked items total
    - Display remaining budget
    - Add progress indicator
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5_
  
  - [x] 6.9 Create VoiceInputDialog
    - Build voice recording UI
    - Add microphone animation
    - Display recognized text
    - Implement cancel and confirm actions
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_
  
  - [x] 6.10 Create SuggestionsSection
    - Display suggested items list
    - Add tap to add functionality
    - Show suggestion reasoning
    - Implement dismiss action
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5_

- [x] 7. Implement advanced features
  - [x] 7.1 Add barcode scanning
    - Integrate CameraX for barcode scanning
    - Implement barcode detection
    - Add product lookup (mock for now)
    - Create BarcodeScanner composable
    - Handle camera permissions
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_
  
  - [x] 7.2 Add undo functionality
    - Implement undo state tracking
    - Create undo snackbar
    - Add 5-second timeout
    - Handle navigation cancellation
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_
  
  - [x] 7.3 Add export functionality
    - Implement text export format
    - Implement CSV export format
    - Implement JSON export format
    - Add clipboard copy option
    - Create export dialog with format selection
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_

- [ ] 8. Add navigation and integration
  - [ ] 8.1 Register new routes
    - Add ItemHistory screen route
    - Add TemplateManager screen route
    - Add StoreSectionEditor route
    - Add ShoppingMode screen route
    - _Requirements: 4.1, 5.1, 10.1, 11.1_
  
  - [ ] 8.2 Implement history tracking
    - Add observer to track checked items
    - Update history when items are checked
    - Increment purchase counts
    - Update average prices
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [ ] 8.3 Wire up dependency injection
    - Create module for new repositories
    - Bind repository interfaces
    - Provide DAOs from database
    - Register new use cases
    - _Requirements: 1.1, 5.1, 10.1, 11.1_

- [ ] 9. Implement permissions and system integration
  - [ ] 9.1 Add camera permission handling
    - Request camera permission for barcode scanning
    - Handle permission denial gracefully
    - Show rationale dialog
    - _Requirements: 14.5_
  
  - [ ] 9.2 Add microphone permission handling
    - Request microphone permission for voice input
    - Handle permission denial gracefully
    - Show rationale dialog
    - _Requirements: 16.5_
  
  - [ ] 9.3 Add location permission handling (future)
    - Request location permission for store notifications
    - Handle permission denial gracefully
    - Show rationale dialog
    - _Requirements: 13.1, 13.4_

- [ ] 10. Add error handling and edge cases
  - [ ] 10.1 Handle database errors
    - Add try-catch blocks in repositories
    - Map SQLite exceptions to domain errors
    - Display user-friendly error messages
    - _Requirements: 1.5, 2.4, 3.4, 6.5_
  
  - [ ] 10.2 Handle permission errors
    - Show error when camera unavailable
    - Show error when microphone unavailable
    - Provide fallback options
    - _Requirements: 14.4, 16.4_
  
  - [ ] 10.3 Handle voice parsing errors
    - Show error for unrecognized speech
    - Provide manual input fallback
    - Log parsing failures for improvement
    - _Requirements: 16.3, 16.4_
  
  - [ ] 10.4 Handle edge cases
    - Test with empty history
    - Test with 100+ history items
    - Test quantity adjustment with non-numeric values
    - Test template with empty list
    - Test voice input with unclear speech
    - _Requirements: 1.5, 2.5, 10.5, 11.5, 16.4_

- [ ] 11. Polish and optimize
  - [ ] 11.1 Add loading states
    - Show loading for history fetch
    - Show loading for template operations
    - Show loading for voice processing
    - Add skeleton screens
    - _Requirements: 1.5, 10.1, 11.4, 16.3_
  
  - [ ] 11.2 Optimize performance
    - Add database indices for new columns
    - Implement pagination for history
    - Cache frequent items in memory
    - Debounce voice input processing
    - Use stable keys in LazyColumn
    - _Requirements: 1.5, 2.4, 3.4, 10.5_
  
  - [ ] 11.3 Add animations
    - Animate shopping mode transition
    - Animate priority badge appearance
    - Animate quantity changes
    - Add drag-and-drop visual feedback
    - Animate section collapse/expand
    - _Requirements: 3.2, 4.3, 5.4, 8.2_
  
  - [ ] 11.4 Add visual polish
    - Style priority items distinctly
    - Add note indicator icon
    - Style shopping mode with larger text
    - Add section color coding
    - Implement Material3 design throughout
    - _Requirements: 4.3, 5.1, 7.3, 8.2_
