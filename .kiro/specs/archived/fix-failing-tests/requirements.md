# Requirements Document: Fix Failing Unit Tests

## Introduction

This spec addresses 14 failing unit tests across 3 test classes in the Shoppit Android application. The tests are failing due to missing or incorrect implementations in the ViewModels and navigation utilities. All tests are well-written and follow the project's testing standards - the implementations need to be fixed to match the expected behavior.

## Glossary

- **System**: The Shoppit Android application
- **ViewModel**: Android Architecture Component that manages UI-related data
- **ErrorEvent**: Domain event emitted to communicate success/error states to the UI
- **SavedStateHandle**: Android component for preserving state across process death
- **NavigationErrorHandler**: Utility class for handling navigation errors gracefully

## Requirements

### Requirement 1: MealPlannerViewModel Error Event Emission

**User Story:** As a developer, I want the MealPlannerViewModel to emit error and success events, so that the UI can display appropriate feedback to users.

#### Acceptance Criteria

1. WHEN a meal is successfully assigned to a plan, THE System SHALL emit an ErrorEvent.Success with message "Meal added to plan"
2. WHEN meal assignment fails, THE System SHALL emit an ErrorEvent.Error with the failure message
3. WHEN a meal plan is successfully deleted, THE System SHALL emit an ErrorEvent.Success with message "Meal removed from plan"
4. WHEN meal plan deletion fails, THE System SHALL emit an ErrorEvent.Error with the failure message
5. WHEN a suggestion is successfully selected, THE System SHALL emit an ErrorEvent.Success with message "Meal added to plan"
6. WHEN suggestion selection fails, THE System SHALL emit an ErrorEvent.Error with the failure message

### Requirement 2: MealPlannerViewModel Error Logging

**User Story:** As a developer, I want errors to be logged with proper context, so that I can debug issues in production.

#### Acceptance Criteria

1. WHEN meal assignment fails, THE System SHALL log the error with context "MealPlannerViewModel.onMealSelected"
2. WHEN meal plan deletion fails, THE System SHALL log the error with context "MealPlannerViewModel.deleteMealPlan"
3. WHEN suggestion selection fails, THE System SHALL log the error with context "MealPlannerViewModel.selectSuggestion"
4. WHEN logging errors, THE System SHALL include additional data about the operation

### Requirement 3: MealPlannerViewModel Dialog State Management

**User Story:** As a user, I want the meal selection dialog to close properly, so that I can continue using the app without UI glitches.

#### Acceptance Criteria

1. WHEN dismissMealSelection is called, THE System SHALL set showMealSelection to false
2. WHEN dismissMealSelection is called, THE System SHALL set selectedSlot to null
3. WHEN a meal is successfully selected, THE System SHALL close the meal selection dialog
4. WHEN a meal is successfully selected, THE System SHALL clear the selected slot

### Requirement 4: MealPlannerViewModel Async Operation Handling

**User Story:** As a developer, I want async operations to complete before assertions, so that tests are reliable and not flaky.

#### Acceptance Criteria

1. WHEN onSlotClick is called, THE System SHALL complete all async operations before returning
2. WHEN onMealSelected is called, THE System SHALL complete all async operations before updating state
3. WHEN selectSuggestion is called, THE System SHALL complete all async operations before emitting events
4. WHEN deleteMealPlan is called, THE System SHALL complete all async operations before emitting events

### Requirement 5: MealViewModel State Updates on Data Changes

**User Story:** As a user, I want the meal list to update automatically when meals are deleted, so that I always see current data.

#### Acceptance Criteria

1. WHEN a meal is deleted, THE System SHALL reload the meal list
2. WHEN the meal list is reloaded, THE System SHALL update the UI state with the new data
3. WHEN state is updated after deletion, THE System SHALL maintain other state properties (search query, selected tags)
4. WHEN deletion fails, THE System SHALL display an error without corrupting the state

### Requirement 6: MealViewModel SavedStateHandle Integration

**User Story:** As a user, I want my search and filter settings preserved across app restarts, so that I don't lose my work.

#### Acceptance Criteria

1. WHEN the ViewModel is created with an empty SavedStateHandle, THE System SHALL use default values for all state
2. WHEN the ViewModel is created with partial SavedStateHandle data, THE System SHALL use saved values where available and defaults for missing values
3. WHEN state is updated, THE System SHALL save the new values to SavedStateHandle
4. WHEN an error occurs, THE System SHALL preserve state in SavedStateHandle without corruption

### Requirement 7: NavigationErrorHandler Implementation

**User Story:** As a user, I want navigation errors to be handled gracefully, so that the app doesn't crash when I navigate to invalid screens.

#### Acceptance Criteria

1. WHEN safeNavigate is called with a valid route, THE System SHALL navigate to that route
2. WHEN safeNavigate is called with an invalid route, THE System SHALL catch the exception and navigate to the fallback route
3. WHEN navigation fails, THE System SHALL log the error for debugging
4. WHEN navigation succeeds, THE System SHALL not navigate to the fallback route
