# Implementation Plan

## Overview

This implementation plan breaks down the error handling and validation feature into discrete, manageable coding tasks. Each task builds incrementally on previous work, ensuring the system remains functional throughout development.

## Task List

- [x] 1. Enhance core error infrastructure
  - Update AppError sealed class to include field and cause parameters for all error types
  - Create extension functions for ValidationResult to field-error map conversion
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_

- [x] 2. Implement centralized error logging
  - [x] 2.1 Create ErrorLogger interface and implementation
    - Define ErrorLogger interface with logError, logWarning, and logInfo methods
    - Implement ErrorLoggerImpl with Timber integration
    - Add conditional crash reporting for production builds
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  
  - [x] 2.2 Create Hilt module for ErrorLogger
    - Create ErrorLoggerModule to provide ErrorLogger instance
    - Bind ErrorLoggerImpl to ErrorLogger interface
    - _Requirements: 10.1_

- [x] 3. Create ingredient validation
  - [x] 3.1 Implement IngredientValidator
    - Create IngredientValidator class implementing DataValidator<Ingredient>
    - Validate ingredient name is not blank
    - Validate quantity is not blank and is a valid positive number
    - Return ValidationResult with field-specific errors
    - _Requirements: 3.5, 3.6, 3.7_
  
  - [x] 3.2 Write unit tests for IngredientValidator
    - Test empty ingredient name validation
    - Test empty quantity validation
    - Test non-numeric quantity validation
    - Test zero or negative quantity validation
    - Test valid ingredient passes validation
    - _Requirements: 3.5, 3.6, 3.7_

- [x] 4. Enhance meal validation
  - [x] 4.1 Update MealValidator to validate ingredients individually
    - Inject IngredientValidator into MealValidator
    - Validate each ingredient using IngredientValidator
    - Collect ingredient-specific validation errors with indexed field names
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  
  - [x] 4.2 Write unit tests for enhanced MealValidator
    - Test meal with invalid ingredients shows ingredient-specific errors
    - Test meal with multiple invalid ingredients shows all errors
    - Test valid meal with valid ingredients passes validation
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 5. Create reusable UI components for error display
  - [x] 5.1 Create ValidatedTextField component
    - Build OutlinedTextField wrapper with error state support
    - Display error message below field when error is present
    - Apply error color styling to field border
    - Support all standard TextField parameters
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [x] 5.2 Create ErrorSnackbarHandler component
    - Create composable that observes ErrorEvent SharedFlow
    - Display error messages with long duration and dismiss action
    - Display success messages with short duration
    - Handle snackbar queueing for multiple messages
    - _Requirements: 1.1, 9.1, 9.2, 9.3, 9.4_
  
  - [x] 5.3 Create LoadingOverlay component
    - Build semi-transparent overlay with CircularProgressIndicator
    - Block user interaction when loading
    - Center loading indicator
    - _Requirements: 8.1, 8.2, 8.3, 8.4_
  
  - [x] 5.4 Enhance ErrorScreen component
    - Add support for retry callback
    - Improve error icon and message styling
    - Add accessibility content descriptions
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4_

- [x] 6. Update AddEditMealViewModel with comprehensive error handling
  - [x] 6.1 Add ErrorLogger injection and error event flow
    - Inject ErrorLogger into AddEditMealViewModel
    - Add private MutableSharedFlow for ErrorEvent
    - Expose public SharedFlow for UI observation
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 6.2 Implement field-specific validation error tracking
    - Update saveMeal to extract field-specific errors from ValidationResult
    - Store validation errors in Map<String, String> in UI state
    - Clear field errors when user updates that field
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 4.1, 4.2, 4.3, 4.4_
  
  - [x] 6.3 Add success message emission
    - Emit success ErrorEvent when meal is saved successfully
    - Emit success ErrorEvent when meal is updated successfully
    - _Requirements: 9.1, 9.2_
  
  - [x] 6.4 Integrate error logging in error handling
    - Log all errors with context using ErrorLogger
    - Include operation name in log context
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 6.5 Write unit tests for ViewModel error handling
    - Test validation errors are mapped to field-specific errors
    - Test error events are emitted on failure
    - Test success events are emitted on success
    - Test errors are logged with context
    - _Requirements: 1.1, 3.1, 3.2, 3.3, 3.4, 9.1, 10.1_

- [x] 7. Update AddEditMealScreen with inline validation errors
  - [x] 7.1 Replace TextField with ValidatedTextField for meal name
    - Use ValidatedTextField component for meal name input
    - Pass validation error from uiState.validationErrors["name"]
    - Clear error when user types
    - _Requirements: 4.1, 4.2, 4.3_
  
  - [x] 7.2 Add ValidatedTextField for ingredient fields
    - Use ValidatedTextField for ingredient name input
    - Use ValidatedTextField for ingredient quantity input
    - Pass indexed validation errors (e.g., "ingredients[0].name")
    - _Requirements: 4.1, 4.2, 4.3, 4.4_
  
  - [x] 7.3 Add ErrorSnackbarHandler to screen
    - Add SnackbarHost to Scaffold
    - Integrate ErrorSnackbarHandler observing ViewModel errorEvent
    - _Requirements: 1.1, 9.1, 9.2, 9.3, 9.4_
  
  - [x] 7.4 Add LoadingOverlay for save operation
    - Display LoadingOverlay when isSaving is true
    - Disable save button when saving
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 8. Update MealViewModel with error handling
  - [x] 8.1 Add ErrorLogger and error event flow
    - Inject ErrorLogger into MealViewModel
    - Add error event SharedFlow for snackbar messages
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 8.2 Implement error handling in loadMeals
    - Catch errors from getMealsUseCase
    - Map errors to user-friendly messages
    - Emit error events for snackbar display
    - Log errors with context
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 2.4, 10.1_
  
  - [x] 8.3 Implement error handling in deleteMeal
    - Catch errors from deleteMealUseCase
    - Emit error or success events
    - Log errors with context
    - _Requirements: 1.1, 9.2, 10.1_
  
  - [x] 8.4 Write unit tests for MealViewModel error handling
    - Test error state is set when loading fails
    - Test error events are emitted
    - Test success events are emitted on delete
    - _Requirements: 1.1, 2.1, 9.2, 10.1_

- [x] 9. Update MealListScreen with error feedback
  - [x] 9.1 Add ErrorSnackbarHandler to MealListScreen
    - Add SnackbarHost to Scaffold
    - Integrate ErrorSnackbarHandler observing ViewModel errorEvent
    - _Requirements: 1.1, 9.2_
  
  - [x] 9.2 Enhance error screen with retry action
    - Pass retry callback to ErrorScreen when in error state
    - Retry callback should call viewModel.loadMeals()
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 10. Update MealPlannerViewModel with error handling
  - [x] 10.1 Add ErrorLogger and error event flow
    - Inject ErrorLogger into MealPlannerViewModel
    - Add error event SharedFlow
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 10.2 Implement error handling in addMealToPlan
    - Catch errors from addMealPlanUseCase
    - Emit error or success events
    - Log errors with context
    - _Requirements: 1.1, 9.3, 10.1_
  
  - [x] 10.3 Implement error handling in removeMealFromPlan
    - Catch errors from removeMealPlanUseCase
    - Emit error or success events
    - Log errors with context
    - _Requirements: 1.1, 10.1_
  
  - [x] 10.4 Write unit tests for MealPlannerViewModel error handling
    - Test error events are emitted on failure
    - Test success events are emitted on success
    - Test errors are logged
    - _Requirements: 1.1, 9.3, 10.1_

- [x] 11. Update MealPlannerScreen with error feedback
  - [x] 11.1 Add ErrorSnackbarHandler to MealPlannerScreen
    - Add SnackbarHost to Scaffold
    - Integrate ErrorSnackbarHandler observing ViewModel errorEvent
    - _Requirements: 1.1, 9.3_

- [x] 12. Update ShoppingListViewModel with error handling
  - [x] 12.1 Add ErrorLogger and error event flow
    - Inject ErrorLogger into ShoppingListViewModel
    - Add error event SharedFlow
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  
  - [x] 12.2 Implement error handling in toggleItemChecked
    - Catch errors from toggleShoppingListItemUseCase
    - Emit error events on failure
    - Log errors with context
    - _Requirements: 1.1, 10.1_
  
  - [x] 12.3 Implement error handling in clearCheckedItems
    - Catch errors from clearCheckedItemsUseCase
    - Emit success event on completion
    - Log errors with context
    - _Requirements: 1.1, 10.1_
  
  - [x] 12.4 Write unit tests for ShoppingListViewModel error handling
    - Test error events are emitted on failure
    - Test errors are logged
    - _Requirements: 1.1, 10.1_

- [x] 13. Update ShoppingListScreen with error feedback
  - [x] 13.1 Add ErrorSnackbarHandler to ShoppingListScreen
    - Add SnackbarHost to Scaffold
    - Integrate ErrorSnackbarHandler observing ViewModel errorEvent
    - _Requirements: 1.1_

- [x] 14. Update all repositories with error logging
  - [x] 14.1 Add ErrorLogger to MealRepositoryImpl
    - Inject ErrorLogger into MealRepositoryImpl
    - Log all caught exceptions with context
    - Add cause parameter to all AppError instances
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 10.1, 10.3, 10.4_
  
  - [x] 14.2 Add ErrorLogger to MealPlanRepositoryImpl
    - Inject ErrorLogger into MealPlanRepositoryImpl
    - Log all caught exceptions with context
    - Add cause parameter to all AppError instances
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 10.1, 10.3, 10.4_
  
  - [x] 14.3 Add ErrorLogger to ShoppingListRepositoryImpl
    - Inject ErrorLogger into ShoppingListRepositoryImpl
    - Log all caught exceptions with context
    - Add cause parameter to all AppError instances
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 10.1, 10.3, 10.4_
  
  - [x] 14.4 Write unit tests for repository error logging
    - Test errors are logged when exceptions occur
    - Test AppError instances include cause parameter
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 10.1_

- [x] 15. Implement offline error handling
  - [x] 15.1 Update SyncViewModel with offline error handling
    - Display user-friendly message when sync fails due to network
    - Show "Using offline data" message in snackbar
    - Continue displaying cached data on sync failure
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [x] 15.2 Add network status indicator to main screens
    - Show subtle indicator when app is offline
    - Update indicator when network status changes
    - _Requirements: 5.1, 5.2_

- [x] 16. Add validation for meal plan operations
  - [x] 16.1 Create MealPlanValidator
    - Implement MealPlanValidator with date and meal type validation
    - Validate date is not in the past (optional business rule)
    - Validate meal type is one of allowed values
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [x] 16.2 Integrate MealPlanValidator in AddMealPlanUseCase
    - Inject MealPlanValidator into use case
    - Validate meal plan before repository call
    - Return validation errors if validation fails
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  
  - [x] 16.3 Write unit tests for MealPlanValidator
    - Test invalid date validation
    - Test invalid meal type validation
    - Test valid meal plan passes validation
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 17. Final integration and polish
  - [ ] 17.1 Verify all error messages are user-friendly
    - Review all error messages across the app
    - Ensure no technical details are exposed to users
    - Update ErrorMapper if needed
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  
  - [ ] 17.2 Verify all success messages are displayed
    - Test all CRUD operations show success messages
    - Verify snackbar durations are appropriate
    - _Requirements: 9.1, 9.2, 9.3, 9.4_
  
  - [ ] 17.3 Verify error logging is comprehensive
    - Check all ViewModels log errors
    - Check all repositories log errors
    - Verify log messages include context
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 17.4 Add accessibility content descriptions to error UI
    - Add content descriptions to error icons
    - Ensure error messages are announced by TalkBack
    - Test with TalkBack enabled
    - _Requirements: 1.1, 4.1, 4.2_
  
  - [ ] 17.5 Run full test suite and fix any failures
    - Run all unit tests
    - Run all integration tests
    - Fix any test failures
    - Verify test coverage meets 80% target
    - _Requirements: All_

## Notes

- All tasks including tests are required for comprehensive implementation
- Each task should be completed and tested before moving to the next
- Validation should always occur before repository calls in use cases
- All errors should be logged with context for debugging
- User-facing error messages should be clear and actionable
