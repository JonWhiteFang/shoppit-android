# Implementation Plan: Fix Failing Unit Tests

- [x] 1. Fix MealPlannerViewModel error event emission and dialog state management
  - Add `_errorEvent` MutableSharedFlow and expose as `errorEvent` SharedFlow
  - Update `onMealSelected` to emit success/error events and close dialog on success
  - Update `deleteMealPlan` to emit success/error events
  - Update `selectSuggestion` to emit success/error events and hide suggestions on success
  - Add `dismissMealSelection` method to close dialog and clear selected slot
  - Add error logging with context to all three methods
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4_

- [x] 2. Fix MealViewModel data refresh on deletion
  - Update `deleteMeal` method to call `loadMeals()` after successful deletion
  - Ensure `loadMeals()` preserves search query and selected tags
  - Add error event emission for deletion success/failure
  - Add error logging with context
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 3. Integrate SavedStateHandle in MealViewModel
  - Update constructor to accept SavedStateHandle parameter
  - Replace `_searchQuery` with `savedStateHandle.getStateFlow("search_query", "")`
  - Replace `_selectedTags` with `savedStateHandle.getStateFlow("selected_tags", emptySet())`
  - Update `updateSearchQuery` to save to SavedStateHandle
  - Update `toggleTag` to save to SavedStateHandle
  - Ensure state persists across ViewModel recreations
  - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [x] 4. Create NavigationErrorHandler utility class
  - Create `NavigationErrorHandler` object in `presentation.ui.navigation.util` package
  - Implement `safeNavigate` method with try-catch and fallback navigation
  - Implement `handleInvalidArguments` method
  - Implement `handleMissingArguments` method
  - Implement `handleInvalidRoute` method
  - Implement `validateArguments` method
  - Add Timber logging for all error cases
  - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [x] 5. Verify all tests pass
  - Run `.\gradlew.bat test` to execute all unit tests
  - Verify all 14 previously failing tests now pass
  - Verify no new test failures were introduced
  - Check test reports for any warnings or issues
  - _Requirements: All_
