# Independent Back Stacks for Bottom Navigation

## Overview

The Shoppit app implements independent back stacks for each bottom navigation item (Meals, Planner, Shopping). This means that each section maintains its own navigation history, and switching between tabs preserves the state and navigation stack of each section.

## Implementation

### MainScreen.kt

The key implementation is in `MainScreen.kt` where bottom navigation items are configured:

```kotlin
navController.navigate(item.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true  // Save the back stack when navigating away
    }
    launchSingleTop = true  // Prevent duplicate destinations
    restoreState = true     // Restore the back stack when returning
}
```

### How It Works

1. **saveState = true**: When navigating away from a section (e.g., from Meals to Planner), the entire back stack of that section is saved, including:
   - Navigation history (which screens were visited)
   - Scroll positions (via `rememberLazyListState`)
   - ViewModel state (via `SavedStateHandle`)
   - UI state (via `rememberSaveable`)

2. **restoreState = true**: When returning to a previously visited section, the saved back stack is restored, bringing back:
   - The exact screen the user was on
   - The scroll position
   - Any form inputs or filters
   - The complete navigation history

3. **launchSingleTop = true**: Prevents creating duplicate instances of the same destination

## Example User Flow

### Scenario 1: Basic Navigation
1. User starts on Meals tab (MealList screen)
2. User taps a meal → navigates to MealDetail screen
3. User taps Planner tab → switches to Planner section
   - Meals back stack is saved: [MealList, MealDetail]
4. User taps Meals tab again → returns to MealDetail screen
   - Meals back stack is restored: [MealList, MealDetail]
5. User presses back → returns to MealList screen

### Scenario 2: Multiple Sections
1. User on Meals tab: MealList → MealDetail → EditMeal
   - Meals stack: [MealList, MealDetail, EditMeal]
2. User switches to Planner tab
   - Meals stack saved
   - Planner stack: [MealPlanner]
3. User navigates in Planner: MealPlanner → MealDetail
   - Planner stack: [MealPlanner, MealDetail]
4. User switches to Shopping tab
   - Planner stack saved
   - Shopping stack: [ShoppingList]
5. User switches back to Meals tab
   - Returns to EditMeal screen (where they left off)
   - Back navigation works: EditMeal → MealDetail → MealList

### Scenario 3: State Preservation
1. User on Meals tab, scrolls down the meal list
2. User taps a meal → MealDetail screen
3. User switches to Planner tab
4. User switches back to Meals tab
5. User presses back → returns to MealList
   - **Scroll position is preserved** (user is still scrolled down)

## State Preservation Mechanisms

### 1. Navigation Back Stack
- Managed by Navigation Compose
- Saved/restored automatically with `saveState`/`restoreState`

### 2. Scroll Positions
- Implemented using `rememberLazyListState()` in screens
- Automatically saved/restored by Compose

### 3. ViewModel State
- Critical state saved in `SavedStateHandle`
- Examples:
  - Current week in MealPlanner
  - Search query in MealList
  - Filter settings in ShoppingList

### 4. Form Inputs
- Saved in ViewModel state
- Persists across configuration changes and process death

## Testing Independent Back Stacks

### Manual Testing Steps

1. **Test Basic Back Stack**:
   - Navigate: Meals → MealDetail → EditMeal
   - Switch to Planner tab
   - Switch back to Meals tab
   - Verify: You're on EditMeal screen
   - Press back twice
   - Verify: You're on MealList screen

2. **Test State Preservation**:
   - On Meals tab, scroll down the list
   - Tap a meal to view details
   - Switch to Shopping tab
   - Switch back to Meals tab
   - Press back
   - Verify: Scroll position is preserved

3. **Test Multiple Sections**:
   - Navigate in Meals: MealList → MealDetail
   - Switch to Planner, navigate: MealPlanner → MealDetail
   - Switch to Shopping, navigate: ShoppingList → ItemHistory
   - Switch back to Meals
   - Verify: You're on MealDetail (Meals section)
   - Switch to Planner
   - Verify: You're on MealDetail (Planner section)
   - Switch to Shopping
   - Verify: You're on ItemHistory

4. **Test Process Death**:
   - Navigate to a deep screen (e.g., EditMeal)
   - Enable "Don't keep activities" in Developer Options
   - Switch to another app
   - Return to Shoppit
   - Verify: State is restored correctly

## Requirements Satisfied

- **Requirement 6.5**: Each bottom nav item maintains its own back stack ✓
- **Requirement 1.3**: State preservation when switching between tabs ✓
- **Requirement 1.4**: Back navigation works correctly within each section ✓
- **Requirement 6.1**: Preserve scroll position and UI state across navigation ✓
- **Requirement 6.2**: Save form input state in ViewModels ✓
- **Requirement 6.3**: Restore state after process death ✓

## Implementation Files

- `MainScreen.kt`: Bottom navigation configuration
- `ShoppitNavHost.kt`: Navigation graph and routes
- `MealViewModel.kt`: State preservation for Meals section
- `MealPlannerViewModel.kt`: State preservation for Planner section
- `ShoppingListViewModel.kt`: State preservation for Shopping section
- `MealListScreen.kt`: Scroll position preservation
- `AddEditMealViewModel.kt`: Form state preservation

## Known Limitations

1. **Deep Links**: Deep links create a new back stack, not restoring the previous state
2. **Memory Constraints**: On low-memory devices, saved state may be cleared by the system
3. **Large Back Stacks**: Very deep navigation stacks may impact performance

## Future Enhancements

1. Add analytics to track navigation patterns
2. Implement back stack size limits to prevent memory issues
3. Add user preference for back stack behavior
4. Implement smart back stack clearing for unused sections
