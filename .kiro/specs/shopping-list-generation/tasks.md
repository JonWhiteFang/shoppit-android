# Implementation Plan

- [x] 1. Create domain models and enums
  - Define ShoppingListItem data class with all required fields
  - Create ItemCategory enum with display names
  - Define IngredientSource data class for meal tracking
  - Create ShoppingListData wrapper class for grouped data
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 10.1, 10.2_

- [x] 2. Implement data layer foundation
  - [x] 2.1 Create Room entity and DAO
    - Define ShoppingListItemEntity with proper indices
    - Implement ShoppingListDao with all query methods
    - Add type converters for meal ID list serialization
    - Update AppDatabase to include ShoppingListDao
    - _Requirements: 3.3, 4.4, 5.4, 12.1, 12.3_
  
  - [x] 2.2 Create repository interface and implementation
    - Define ShoppingListRepository interface with Flow-based queries
    - Implement ShoppingListRepositoryImpl with error handling
    - Create mapper functions for entity/domain conversions
    - _Requirements: 1.1, 1.2, 3.3, 12.1, 12.3, 12.4_
  
  - [x] 2.3 Wire up dependency injection
    - Create RepositoryModule binding for ShoppingListRepository
    - Ensure database module provides ShoppingListDao
    - _Requirements: 12.1_

- [x] 3. Implement core use cases
  - [x] 3.1 Create GenerateShoppingListUseCase
    - Implement logic to fetch current week's meal plans
    - Aggregate ingredients by name (case-insensitive)
    - Categorize ingredients using keyword matching
    - Handle quantity aggregation with "+" separator
    - Delete existing auto-generated items before inserting new ones
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 4.1, 4.2, 4.3, 10.1, 10.2_
  
  - [x] 3.2 Create GetShoppingListUseCase
    - Implement Flow-based shopping list retrieval
    - Group items by category
    - Calculate total and checked item counts
    - _Requirements: 1.5, 10.1, 10.3, 11.1, 11.2_
  
  - [x] 3.3 Create item management use cases
    - Implement ToggleItemCheckedUseCase for check/uncheck
    - Implement AddManualItemUseCase with validation
    - Implement UpdateManualItemUseCase with manual-only check
    - Implement DeleteManualItemUseCase with manual-only check
    - _Requirements: 3.1, 3.2, 3.3, 5.1, 5.2, 5.3, 5.4, 6.1, 6.2, 6.3, 6.4, 7.1, 7.2, 7.3, 7.4_
  
  - [x] 3.4 Create bulk operation use cases
    - Implement ClearCheckedItemsUseCase
    - Implement UncheckAllItemsUseCase
    - Implement GetItemSourcesUseCase for meal tracking
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 9.1, 9.2, 9.3, 17.1, 17.2, 17.3, 17.4_

- [x] 4. Build ViewModel and UI state
  - [x] 4.1 Create UI state models
    - Define ShoppingListUiState data class
    - Include loading, error, and dialog states
    - Add filter and search state properties
    - _Requirements: 14.1, 14.2, 14.3, 15.1, 15.2_
  
  - [x] 4.2 Implement ShoppingListViewModel
    - Set up StateFlow for UI state management
    - Implement loadShoppingList with Flow collection
    - Create generateShoppingList action
    - Implement toggleItemChecked action
    - Add manual item CRUD operations
    - Implement bulk operations (clear checked, uncheck all)
    - Add filter and search functionality
    - Handle item detail display with source loading
    - _Requirements: 1.1, 3.1, 3.2, 4.1, 4.2, 4.3, 5.1, 6.1, 7.1, 8.1, 9.1, 14.1, 14.2, 15.1, 15.2, 17.1_

- [x] 5. Create UI components
  - [x] 5.1 Build ShoppingListScreen composable
    - Create main screen layout with Scaffold
    - Add top app bar with actions (generate, filter, share)
    - Display summary header with counts and progress
    - Implement category-grouped LazyColumn
    - Add floating action button for manual items
    - Handle empty state when no items exist
    - _Requirements: 1.5, 5.1, 8.1, 10.3, 10.4, 10.5, 11.1, 11.2, 11.3, 13.1, 14.1, 16.2_
  
  - [x] 5.2 Create shopping list item composable
    - Build ShoppingListItemCard with checkbox
    - Display item name, quantity, and unit
    - Show category badge
    - Add meal count badge for multi-meal items
    - Apply checked styling (strikethrough, opacity)
    - Handle click for detail view
    - Add manual item indicator
    - _Requirements: 3.1, 3.4, 5.5, 6.5, 18.1, 18.2, 18.3_
  
  - [x] 5.3 Build AddItemDialog composable
    - Create dialog with input fields for name, quantity, unit
    - Add validation for required fields
    - Implement confirm and cancel actions
    - _Requirements: 5.1, 5.2, 5.3_
  
  - [x] 5.4 Build ItemDetailDialog composable
    - Display item details (name, quantity, unit)
    - Show list of meals using the ingredient
    - Display quantity per meal
    - Add navigation to meal detail
    - Show "Manually added" for manual items
    - Add delete button for manual items only
    - _Requirements: 6.1, 7.1, 9.1, 9.2, 9.3, 9.4, 9.5_
  
  - [x] 5.5 Implement search and filter UI
    - Add search bar to top app bar
    - Implement real-time search filtering
    - Add filter toggle for unchecked items only
    - Update item counts based on active filters
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 15.1, 15.2, 15.3, 15.4, 15.5_

- [ ] 6. Add navigation and integration
  - [ ] 6.1 Register shopping list route
    - Add ShoppingList screen to navigation graph
    - Define route constants
    - _Requirements: 1.1_
  
  - [ ] 6.2 Implement auto-generation trigger
    - Add observer in meal planning screen to trigger regeneration
    - Call generateShoppingList when meal plans change
    - Ensure generation completes within 1 second
    - _Requirements: 4.1, 4.2, 4.3, 4.5_
  
  - [ ] 6.3 Add share functionality
    - Implement share intent with formatted text
    - Include only unchecked items by default
    - Format with category headers
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5_

- [ ] 7. Implement error handling and edge cases
  - [ ] 7.1 Add comprehensive error handling
    - Display user-friendly error messages
    - Handle empty meal plan scenario
    - Log detailed errors for debugging
    - Add retry capability for failed operations
    - Clear errors on successful operations
    - _Requirements: 16.1, 16.2, 16.3, 16.4, 16.5_
  
  - [ ] 7.2 Handle edge cases
    - Test with empty ingredient lists
    - Handle meals with no ingredients
    - Test ingredient name case variations
    - Verify checked status preservation during regeneration
    - Test with 100+ items for performance
    - _Requirements: 2.2, 4.4, 17.5_

- [ ] 8. Add confirmation dialogs
  - Create ConfirmationDialog for clear checked items
  - Create ConfirmationDialog for uncheck all items
  - Create ConfirmationDialog for delete manual item
  - Show item counts in confirmation messages
  - _Requirements: 7.2, 8.2, 17.2_

- [ ] 9. Polish and optimize
  - [ ] 9.1 Add loading states
    - Show loading indicator during list generation
    - Display loading state during initial load
    - Add progress indicator for bulk operations
    - _Requirements: 4.5, 7.3, 17.5_
  
  - [ ] 9.2 Optimize performance
    - Use stable keys in LazyColumn
    - Implement remember for expensive computations
    - Test with large shopping lists (100+ items)
    - Ensure database operations complete within specified timeframes
    - _Requirements: 3.3, 4.5, 7.3, 12.4, 17.5_
  
  - [ ] 9.3 Add visual polish
    - Implement Material3 design throughout
    - Add smooth animations for check/uncheck
    - Style checked items with strikethrough and reduced opacity
    - Add category section headers with collapse/expand
    - Display progress bar for shopping completion
    - _Requirements: 3.4, 10.3, 10.4, 11.3_
