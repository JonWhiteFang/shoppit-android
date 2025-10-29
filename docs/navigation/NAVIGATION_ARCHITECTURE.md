# Navigation Architecture

## Overview

Shoppit uses Jetpack Compose Navigation with a hierarchical structure featuring bottom navigation for primary sections and stack-based navigation for detail screens. The navigation system is built around three main sections (Meals, Planner, Shopping), each maintaining its own navigation stack and state preservation.

## Architecture Diagram

```
MainScreen (Scaffold with Bottom Navigation)
├── MealList (Bottom Nav Item 1)
│   ├── MealDetail
│   ├── AddMeal
│   └── EditMeal
├── MealPlanner (Bottom Nav Item 2)
│   └── MealDetail (shared)
└── ShoppingList (Bottom Nav Item 3)
    ├── MealDetail (shared)
    ├── ItemHistory
    ├── TemplateManager
    ├── StoreSectionEditor
    └── ShoppingMode
```

## Core Components

### 1. Screen (Sealed Class)

**Location:** `app/src/main/java/com/shoppit/app/presentation/ui/navigation/Screen.kt`

**Purpose:** Type-safe navigation destinations with route definitions.

**Key Features:**
- Sealed class for compile-time safety
- Parameterized routes use companion object functions for type safety
- Route strings follow snake_case convention
- Each screen has a unique, descriptive route identifier

**Example:**
```kotlin
sealed class Screen(val route: String) {
    data object MealList : Screen("meal_list")
    
    data object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long): String = "meal_detail/$mealId"
    }
}
```

### 2. BottomNavigationItem (Sealed Class)

**Location:** `app/src/main/java/com/shoppit/app/presentation/ui/navigation/BottomNavigationItem.kt`

**Purpose:** Define bottom navigation bar items with icons and labels.

**Key Features:**
- Three primary navigation items (Meals, Planner, Shopping)
- Material Icons for consistent visual language
- Companion object provides easy iteration for UI rendering
- Routes map directly to Screen objects for consistency

**Example:**
```kotlin
sealed class BottomNavigationItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Meals : BottomNavigationItem(
        route = Screen.MealList.route,
        title = "Meals",
        icon = Icons.Default.Restaurant
    )
    
    companion object {
        val items = listOf(Meals, Planner, Shopping)
    }
}
```

### 3. ShoppitNavHost (Composable)

**Location:** `app/src/main/java/com/shoppit/app/presentation/ui/navigation/ShoppitNavHost.kt`

**Purpose:** Central navigation host managing all app destinations.

**Key Responsibilities:**
- Define all navigation routes and their composable screens
- Handle navigation arguments (e.g., mealId)
- Manage navigation callbacks between screens
- Coordinate with ViewModels for data flow
- Integrate error handling and performance monitoring
- Support deep links for external navigation

### 4. MainScreen (Composable)

**Location:** `app/src/main/java/com/shoppit/app/presentation/ui/navigation/MainScreen.kt`

**Purpose:** Root screen with bottom navigation and content area.

**Key Features:**
- Scaffold provides Material3 layout structure
- Bottom bar visibility controlled by current destination
- Inner padding ensures content doesn't overlap with navigation bar
- NavController scoped to MainScreen for proper lifecycle management
- Independent back stacks for each bottom navigation item

## Navigation Patterns

### Simple Navigation

Navigate to a screen without arguments:

```kotlin
navController.navigate(Screen.MealList.route)
```

### Navigation with Arguments

Navigate to a screen with required parameters:

```kotlin
// Create route with argument
val route = Screen.MealDetail.createRoute(mealId = 123)
navController.navigate(route)
```

### Bottom Navigation with State Preservation

Navigate between bottom navigation items while preserving state:

```kotlin
navController.navigate(item.route) {
    // Pop up to start destination to avoid large stack
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true  // Save state when navigating away
    }
    launchSingleTop = true  // Avoid duplicate destinations
    restoreState = true     // Restore state when returning
}
```

### Safe Navigation with Error Handling

Navigate with automatic error handling and fallback:

```kotlin
NavigationErrorHandler.safeNavigate(
    navController = navController,
    route = Screen.MealDetail.createRoute(mealId),
    arguments = mapOf("mealId" to mealId),
    fallbackRoute = Screen.MealList.route
)
```

### Pop Back Stack

Navigate back to the previous screen:

```kotlin
navController.popBackStack()
```

## State Management

### Bottom Navigation State

Each bottom navigation item maintains an independent back stack:

- **State Saved:** When switching between tabs using `saveState = true`
- **State Restored:** When returning to a tab using `restoreState = true`
- **Back Stack Preserved:** Each section maintains its own navigation history

### Screen State

State is preserved across navigation using:

- **ViewModels:** Survive configuration changes and navigation
- **rememberSaveable:** Preserves scroll positions and UI state
- **SavedStateHandle:** Persists critical state across process death
- **Database Queries:** Cached in repositories for quick restoration

### Back Stack Management

- **Single Top Launch Mode:** Prevents duplicate screens in the stack
- **Pop Up To Start Destination:** Keeps back stack manageable for bottom nav items
- **Proper Back Stack Construction:** Deep links create logical navigation paths
- **Clear Back Stack:** On logout or data reset

## Deep Link Support

### Configuration

Deep links are configured in two places:

1. **AndroidManifest.xml:** Intent filters for external navigation
2. **Navigation Graph:** Deep link patterns in composable definitions

### Supported Deep Links

| Deep Link | Destination | Parameters |
|-----------|-------------|------------|
| `shoppit://meal/{mealId}` | Meal Detail | mealId (Long) |
| `shoppit://planner?date={date}` | Meal Planner | date (String, optional) |
| `shoppit://planner` | Meal Planner | None |
| `shoppit://shopping` | Shopping List | None |
| `shoppit://shopping/mode` | Shopping Mode | None |

### Example Deep Link Configuration

```kotlin
composable(
    route = Screen.MealDetail.route,
    arguments = listOf(
        navArgument("mealId") {
            type = NavType.LongType
        }
    ),
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "shoppit://meal/{mealId}"
        }
    )
) { backStackEntry ->
    // Screen implementation
}
```

### Deep Link Handling

Deep links are validated and handled with proper error recovery:

```kotlin
// Validate arguments
val validationErrors = NavigationErrorHandler.validateArguments(
    arguments = arguments,
    requiredArgs = listOf("mealId")
)

if (validationErrors.isNotEmpty()) {
    // Handle missing or invalid arguments
    NavigationErrorHandler.handleMissingArguments(
        navController = navController,
        route = Screen.MealDetail.route,
        requiredArgs = listOf("mealId"),
        fallbackRoute = Screen.MealList.route
    )
    return@composable
}
```

## Error Handling

### Navigation Error Types

1. **Missing Arguments:** Required parameters not provided
2. **Invalid Arguments:** Parameters with incorrect values or types
3. **Navigation Failures:** Exceptions during navigation operations
4. **Corrupted Back Stack:** Invalid navigation state

### Error Recovery Strategies

**Missing Arguments:**
```kotlin
NavigationErrorHandler.handleMissingArguments(
    navController = navController,
    route = currentRoute,
    requiredArgs = listOf("mealId"),
    fallbackRoute = Screen.MealList.route
)
```

**Invalid Arguments:**
```kotlin
NavigationErrorHandler.handleInvalidArguments(
    navController = navController,
    route = currentRoute,
    arguments = arguments,
    fallbackRoute = Screen.MealList.route
)
```

**Navigation Failures:**
```kotlin
try {
    navController.navigate(route)
} catch (e: Exception) {
    NavigationLogger.logNavigationError(
        message = "Navigation failed",
        exception = e
    )
    NavigationErrorHandler.handleNavigationFailure(navController, e)
}
```

## Performance Optimization

### Navigation Performance Monitoring

The navigation system includes comprehensive performance monitoring:

```kotlin
// Start monitoring before navigation
NavigationPerformanceAnalytics.startMonitoring(route)

// Navigation occurs...

// Stop monitoring after navigation completes
NavigationPerformanceAnalytics.stopMonitoring(route)
```

### Preloading Strategy

Frequently accessed screens can be preloaded:

```kotlin
RecordNavigationForPreloading(
    currentRoute = currentRoute,
    previousRoute = previousRoute
)
```

### Performance Metrics

- **Navigation Transition Time:** Target < 300ms
- **Frame Rate:** Maintained at 60fps during transitions
- **Memory Usage:** Monitored for leaks and excessive consumption
- **Loading Indicators:** Shown for operations > 100ms

## Accessibility

### Screen Reader Support

Navigation components provide comprehensive accessibility:

```kotlin
// Announce screen transitions
modifier = Modifier.semantics {
    liveRegion = LiveRegionMode.Polite
}

// Content descriptions for navigation items
Icon(
    imageVector = item.icon,
    contentDescription = "${item.title} navigation button"
)

// State descriptions for selected items
modifier = Modifier.semantics {
    contentDescription = "${item.title} tab"
    stateDescription = if (selected) "Selected" else "Not selected"
}
```

### Keyboard Navigation

Keyboard shortcuts are supported for navigation:

```kotlin
SetupKeyboardNavigation(navController)

modifier = Modifier.keyboardNavigationShortcuts(navController)
```

### Focus Management

Focus is automatically managed during screen transitions:

```kotlin
FocusManagementEffect(currentRoute)
```

## Analytics Integration

### Navigation Tracking

All navigation events are tracked for analytics:

```kotlin
// Track screen views
NavigationAnalytics.trackScreenView(route, arguments)

// Track navigation paths
NavigationAnalytics.trackNavigationPath(
    fromRoute = previousRoute,
    toRoute = currentRoute,
    arguments = arguments
)
```

### Logging

Navigation events are logged for debugging:

```kotlin
// Success logging
NavigationLogger.logNavigationSuccess(
    route = route,
    arguments = arguments
)

// Error logging
NavigationLogger.logNavigationError(
    message = "Navigation failed",
    route = route,
    arguments = arguments,
    exception = exception
)
```

## Best Practices

### Do's

✅ Use `Screen` sealed class for type-safe navigation
✅ Always provide fallback routes for error handling
✅ Validate navigation arguments before use
✅ Use `safeNavigate` for navigation with error handling
✅ Preserve state when switching between bottom nav items
✅ Wrap navigation calls in try-catch blocks
✅ Log navigation events for debugging and analytics
✅ Provide content descriptions for accessibility
✅ Test navigation flows with different scenarios

### Don'ts

❌ Don't navigate without validating arguments
❌ Don't ignore navigation exceptions
❌ Don't create circular navigation loops
❌ Don't forget to handle back navigation
❌ Don't skip accessibility annotations
❌ Don't navigate from ViewModels (use events/callbacks)
❌ Don't create duplicate destinations in back stack
❌ Don't forget to test deep link handling

## Testing Navigation

### Unit Tests

Test navigation logic and route generation:

```kotlin
@Test
fun `createRoute generates correct route with mealId`() {
    val mealId = 123L
    val route = Screen.MealDetail.createRoute(mealId)
    assertEquals("meal_detail/123", route)
}
```

### Integration Tests

Test navigation flows between screens:

```kotlin
@Test
fun `navigating from meal list to detail preserves state`() {
    // Navigate to detail
    navController.navigate(Screen.MealDetail.createRoute(1))
    
    // Verify destination
    assertEquals("meal_detail/1", navController.currentDestination?.route)
    
    // Navigate back
    navController.popBackStack()
    
    // Verify back at list
    assertEquals(Screen.MealList.route, navController.currentDestination?.route)
}
```

### UI Tests

Test navigation interactions:

```kotlin
@Test
fun `clicking bottom navigation item navigates to correct screen`() {
    composeTestRule.onNodeWithText("Planner").performClick()
    composeTestRule.onNodeWithText("Meal Planner").assertIsDisplayed()
}
```

## Common Scenarios

### Scenario 1: Navigate to Meal Detail from List

```kotlin
// In MealListScreen
MealListScreen(
    onMealClick = { mealId ->
        NavigationErrorHandler.safeNavigate(
            navController = navController,
            route = Screen.MealDetail.createRoute(mealId),
            arguments = mapOf("mealId" to mealId),
            fallbackRoute = Screen.MealList.route
        )
    }
)
```

### Scenario 2: Edit Meal and Return

```kotlin
// In MealDetailScreen
MealDetailScreen(
    onEditClick = { mealId ->
        NavigationErrorHandler.safeNavigate(
            navController = navController,
            route = Screen.EditMeal.createRoute(mealId),
            arguments = mapOf("mealId" to mealId),
            fallbackRoute = Screen.MealList.route
        )
    }
)

// In AddEditMealScreen (edit mode)
AddEditMealScreen(
    onMealSaved = {
        navController.popBackStack()  // Return to detail screen
    }
)
```

### Scenario 3: Switch Between Bottom Nav Items

```kotlin
// In MainScreen
NavigationBarItem(
    selected = selected,
    onClick = {
        navController.navigate(item.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
)
```

### Scenario 4: Handle Deep Link

```kotlin
// Deep link: shoppit://meal/123
// Automatically handled by navigation system
// Creates proper back stack: MealList -> MealDetail(123)
```

## Troubleshooting

### Issue: State Not Preserved When Switching Tabs

**Solution:** Ensure `saveState = true` and `restoreState = true` in navigation options:

```kotlin
navController.navigate(route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    restoreState = true
}
```

### Issue: Navigation Arguments Missing

**Solution:** Always validate arguments before use:

```kotlin
val validationErrors = NavigationErrorHandler.validateArguments(
    arguments = arguments,
    requiredArgs = listOf("mealId")
)

if (validationErrors.isNotEmpty()) {
    // Handle error
}
```

### Issue: Back Stack Becomes Corrupted

**Solution:** Use `launchSingleTop = true` to prevent duplicates:

```kotlin
navController.navigate(route) {
    launchSingleTop = true
}
```

### Issue: Deep Links Not Working

**Solution:** Verify both AndroidManifest.xml and navigation graph configuration:

1. Check intent filter in manifest
2. Verify deep link pattern in composable
3. Test with `adb shell am start -a android.intent.action.VIEW -d "shoppit://meal/123"`

## Related Documentation

- [Deep Link Configuration](./DEEP_LINK_CONFIGURATION.md)
- [Navigation Performance](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/util/README_PERFORMANCE.md)
- [Navigation Analytics](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/util/README_ANALYTICS.md)
- [Back Stack Management](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/README_BACK_STACK.md)
- [Compose Patterns](../../.kiro/steering/compose-patterns.md)
- [Project Structure](../../.kiro/steering/structure.md)
