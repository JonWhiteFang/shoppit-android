# Implementation Plan

- [x] 1. Create domain models for meal suggestions
  - Create `MealSuggestion` data class with meal, score, reasons, and metadata
  - Create `SuggestionContext` data class with target date, meal type, filters, and search query
  - Create `SuggestionScore` data class with score breakdown (base, bonuses, penalties)
  - Create `MealPlanHistory` data class for tracking meal planning patterns
  - _Requirements: 1.1, 6.1_

- [x] 2. Implement suggestion scoring logic
  - [x] 2.1 Create `CalculateSuggestionScoreUseCase` with pure scoring function
    - Implement base score calculation (100 points)
    - Implement meal type match bonus (+100 for matching tag)
    - Implement frequency penalty (0-50 based on plan count in 30 days)
    - Implement recency penalty (-50 if planned within 7 days)
    - Return `SuggestionScore` with breakdown
    - _Requirements: 3.1-3.5, 4.1-4.3, 6.1-6.4_
  
  - [x] 2.2 Write unit tests for scoring logic
    - Test base score calculation
    - Test meal type bonus application
    - Test frequency penalty tiers (0, 1-2, 3-4, 5+ times)
    - Test recency penalty application
    - Test edge cases (no history, never planned)
    - _Requirements: 6.1-6.4_

- [x] 3. Implement meal plan history analysis
  - [x] 3.1 Create `GetMealPlanHistoryUseCase` to analyze past 30 days
    - Fetch meal plans from past 30 days using repository
    - Group plans by meal ID
    - Calculate plan count per meal
    - Determine last planned date per meal
    - Return map of meal ID to `MealPlanHistory`
    - _Requirements: 3.1-3.5_
  
  - [x] 3.2 Write unit tests for history analysis
    - Test history calculation with various plan patterns
    - Test 30-day date range filtering
    - Test grouping by meal ID
    - Test empty history handling
    - _Requirements: 3.1-3.5_

- [x] 4. Implement main suggestion generation use case
  - [x] 4.1 Create `GetMealSuggestionsUseCase` with orchestration logic
    - Inject `MealRepository`, `MealPlanRepository`, scoring and history use cases
    - Fetch all meals from repository as Flow
    - Fetch meal plans for the week to identify already-planned meals
    - Apply tag filters using `FilterMealsByTagsUseCase`
    - Apply search query filter (case-insensitive partial match)
    - Exclude meals already in the week's plan
    - Get meal plan history for scoring
    - Calculate score for each remaining meal
    - Sort by score descending, then alphabetically
    - Limit to top 10 results
    - Map to `MealSuggestion` with reasons
    - Return as Flow for reactive updates
    - _Requirements: 1.1-1.5, 2.1-2.5, 3.1-3.5, 4.1-4.5, 5.1-5.5, 6.1-6.5_
  
  - [x] 4.2 Write unit tests for suggestion generation
    - Test filtering by tags
    - Test filtering by search query
    - Test excluding already planned meals
    - Test score-based ranking
    - Test limit to 10 results
    - Test empty result scenarios (no meals, no matches)
    - Test error handling from repositories
    - _Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 6.1-6.5_

- [ ] 5. Create UI state management in ViewModel
  - [ ] 5.1 Enhance `MealPlannerViewModel` with suggestion state
    - Add `SuggestionUiState` sealed interface (Hidden, Loading, Success, Error, Empty)
    - Add `EmptyReason` enum (NO_MEALS, NO_MATCHES, ALL_PLANNED)
    - Add `_suggestionState` MutableStateFlow
    - Add `_suggestionContext` MutableStateFlow
    - Add `_selectedTags` MutableStateFlow for filter persistence
    - Add `_searchQuery` MutableStateFlow
    - Implement `showSuggestions(date, mealType)` function
    - Implement `updateTagFilter(tag)` function
    - Implement `updateSearchQuery(query)` function
    - Implement `selectSuggestion(meal)` function to add to plan
    - Implement `hideSuggestions()` function
    - Collect suggestions Flow and update UI state
    - Handle loading, success, error, and empty states
    - _Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 7.1-7.5, 8.1-8.5, 9.1-9.5_
  
  - [ ] 5.2 Write unit tests for ViewModel suggestion logic
    - Test showing suggestions updates state
    - Test tag filter updates
    - Test search query updates
    - Test selecting suggestion adds to plan
    - Test hiding suggestions
    - Test loading state transitions
    - Test error handling
    - Test empty state scenarios
    - _Requirements: 1.1-1.5, 8.1-8.5, 9.1-9.5_

- [ ] 6. Create suggestion UI components
  - [ ] 6.1 Create `SuggestionCard` composable
    - Display meal name with typography
    - Display meal type tags as chips
    - Display ingredient count
    - Display last planned date if applicable
    - Add visual indicator for high scores (>150)
    - Implement click handler for selection
    - Implement "View Details" button
    - Add content descriptions for accessibility
    - Apply Material3 styling
    - _Requirements: 4.4, 6.4, 10.1-10.5_
  
  - [ ] 6.2 Create `SuggestionFilters` composable
    - Display filter chips for all meal tags
    - Show meal count for each tag
    - Implement multi-select with visual feedback
    - Handle tag toggle events
    - Add content descriptions for accessibility
    - Apply Material3 styling with proper spacing
    - _Requirements: 2.1-2.5, 9.1-9.2_
  
  - [ ] 6.3 Create `MealSuggestionsBottomSheet` composable
    - Implement bottom sheet with Material3 ModalBottomSheet
    - Add search bar at top with real-time filtering
    - Add filter chips section below search
    - Display suggestion cards in scrollable list
    - Implement loading state with CircularProgressIndicator
    - Implement empty states (no meals, no matches, all planned)
    - Implement error state with retry button
    - Add "Browse All Meals" button
    - Handle meal selection and dismiss
    - Add keyboard navigation support
    - Add screen reader announcements
    - _Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 7.1-7.5, 8.1-8.5, 9.1-9.5, 10.1-10.5_
  
  - [ ] 6.4 Write UI tests for suggestion components
    - Test SuggestionCard displays meal information
    - Test SuggestionCard click handlers
    - Test SuggestionFilters tag selection
    - Test MealSuggestionsBottomSheet displays suggestions
    - Test search functionality
    - Test filter interaction
    - Test empty states
    - Test error states with retry
    - Test accessibility (content descriptions, semantics)
    - _Requirements: 1.1-1.5, 2.1-2.5, 5.1-5.5, 8.1-8.5_

- [ ] 7. Integrate suggestions into meal planning flow
  - [ ] 7.1 Update `MealPlannerScreen` to show suggestions
    - Add state collection for `suggestionState`
    - Show `MealSuggestionsBottomSheet` when state is not Hidden
    - Pass suggestion context from ViewModel
    - Handle meal selection callback
    - Handle dismiss callback
    - Handle browse all callback
    - _Requirements: 10.1-10.5_
  
  - [ ] 7.2 Update "Add Meal" action to trigger suggestions
    - Modify add meal button/action to call `showSuggestions()`
    - Pass selected date and meal type to ViewModel
    - Ensure smooth transition to bottom sheet
    - _Requirements: 1.1, 10.1-10.2_
  
  - [ ] 7.3 Write integration tests for suggestion flow
    - Test end-to-end suggestion display
    - Test meal selection adds to plan
    - Test filter and search integration
    - Test navigation flow
    - _Requirements: 10.1-10.5_

- [ ] 8. Add dependency injection configuration
  - Register `GetMealSuggestionsUseCase` in Hilt module
  - Register `CalculateSuggestionScoreUseCase` in Hilt module
  - Register `GetMealPlanHistoryUseCase` in Hilt module
  - Verify all dependencies are properly injected
  - _Requirements: All_

- [ ] 9. Implement performance optimizations
  - Add caching for meal plan history in current session
  - Implement cache invalidation when plans change
  - Use sequence operations for efficient filtering
  - Add loading indicator for operations >300ms
  - Verify performance targets (<500ms initial, <200ms filters)
  - _Requirements: 7.1-7.5_

- [ ] 10. Add accessibility enhancements
  - Add content descriptions to all interactive elements
  - Implement screen reader announcements for state changes
  - Add keyboard navigation support (Tab, Enter, Escape)
  - Ensure sufficient color contrast
  - Test with TalkBack enabled
  - Verify minimum touch target sizes (48dp)
  - _Requirements: All accessibility requirements_

- [ ] 11. Polish and refinement
  - Review and refine empty state messages
  - Review and refine error messages
  - Add suggestion reason text generation
  - Verify all UI states are handled
  - Test with various meal library sizes
  - Test with different planning patterns
  - Verify Material3 design consistency
  - _Requirements: 8.1-8.5_
