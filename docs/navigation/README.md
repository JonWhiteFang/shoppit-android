# Navigation Documentation

## Overview

This directory contains comprehensive documentation for the Shoppit app's navigation system. The navigation architecture uses Jetpack Compose Navigation with Material3 components, providing a modern, type-safe navigation experience.

## Documentation Structure

### Core Documentation

1. **[Navigation Architecture](./NAVIGATION_ARCHITECTURE.md)**
   - Complete overview of the navigation system
   - Core components and their responsibilities
   - Navigation patterns and best practices
   - Error handling and performance optimization
   - Accessibility features
   - Testing strategies

2. **[Deep Link Configuration](./DEEP_LINK_CONFIGURATION.md)**
   - Supported deep links and their parameters
   - AndroidManifest.xml configuration
   - Navigation graph setup
   - Deep link validation and error handling
   - Testing deep links
   - Security considerations

3. **[Common Navigation Scenarios](./COMMON_SCENARIOS.md)**
   - Practical examples for common navigation patterns
   - Code samples for each scenario
   - Best practices and tips
   - Troubleshooting common issues

4. **[Navigation State Management Patterns](./NAVIGATION_PATTERNS.md)**
   - State preservation across navigation
   - Bottom navigation state management
   - Process death and recreation handling
   - Configuration change handling
   - State clearing strategies

## Quick Start

### Basic Navigation

```kotlin
// Navigate to a screen
navController.navigate(Screen.MealList.route)

// Navigate with arguments
navController.navigate(Screen.MealDetail.createRoute(mealId))

// Navigate back
navController.popBackStack()
```

### Safe Navigation with Error Handling

```kotlin
NavigationErrorHandler.safeNavigate(
    navController = navController,
    route = Screen.MealDetail.createRoute(mealId),
    arguments = mapOf("mealId" to mealId),
    fallbackRoute = Screen.MealList.route
)
```

### Bottom Navigation with State Preservation

```kotlin
navController.navigate(item.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

## Navigation Components

### Core Files

| File | Location | Purpose |
|------|----------|---------|
| Screen.kt | `presentation/ui/navigation/` | Type-safe route definitions |
| BottomNavigationItem.kt | `presentation/ui/navigation/` | Bottom nav item definitions |
| ShoppitNavHost.kt | `presentation/ui/navigation/` | Central navigation host |
| MainScreen.kt | `presentation/ui/navigation/` | Root screen with bottom nav |

### Utility Files

| File | Location | Purpose |
|------|----------|---------|
| NavigationErrorHandler.kt | `presentation/ui/navigation/util/` | Error handling and recovery |
| DeepLinkHandler.kt | `presentation/ui/navigation/util/` | Deep link processing |
| BackPressHandler.kt | `presentation/ui/navigation/util/` | Back button handling |
| NavigationLogger.kt | `presentation/ui/navigation/util/` | Navigation event logging |
| NavigationAnalytics.kt | `presentation/ui/navigation/util/` | Analytics tracking |
| NavigationPerformanceMonitor.kt | `presentation/ui/navigation/util/` | Performance monitoring |

## Navigation Architecture

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

## Supported Deep Links

| Deep Link | Destination | Example |
|-----------|-------------|---------|
| `shoppit://meal/{mealId}` | Meal Detail | `shoppit://meal/123` |
| `shoppit://planner?date={date}` | Meal Planner | `shoppit://planner?date=2024-01-15` |
| `shoppit://planner` | Meal Planner | `shoppit://planner` |
| `shoppit://shopping` | Shopping List | `shoppit://shopping` |
| `shoppit://shopping/mode` | Shopping Mode | `shoppit://shopping/mode` |

## Key Features

### Type-Safe Navigation

All navigation routes are defined in a sealed class for compile-time safety:

```kotlin
sealed class Screen(val route: String) {
    data object MealList : Screen("meal_list")
    data object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long): String = "meal_detail/$mealId"
    }
}
```

### Error Handling

Comprehensive error handling with automatic recovery:

- Missing arguments detection
- Invalid argument validation
- Navigation failure recovery
- Corrupted back stack handling
- Circular loop prevention

### State Preservation

Automatic state preservation across:

- Bottom navigation tab switches
- Detail screen navigation
- Process death and recreation
- Configuration changes (rotation)

### Performance Monitoring

Built-in performance tracking:

- Navigation transition times
- Frame rate monitoring
- Memory usage tracking
- Loading indicator management

### Accessibility

Full accessibility support:

- Screen reader announcements
- Keyboard navigation shortcuts
- Focus management
- Content descriptions

## Common Tasks

### Adding a New Screen

1. Add route to `Screen.kt`:
```kotlin
data object NewScreen : Screen("new_screen")
```

2. Add composable to `ShoppitNavHost.kt`:
```kotlin
composable(Screen.NewScreen.route) {
    NewScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

3. Navigate to the screen:
```kotlin
navController.navigate(Screen.NewScreen.route)
```

### Adding a New Deep Link

1. Add intent filter to `AndroidManifest.xml`:
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data
        android:scheme="shoppit"
        android:host="newscreen" />
</intent-filter>
```

2. Add deep link to composable:
```kotlin
composable(
    route = Screen.NewScreen.route,
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "shoppit://newscreen"
        }
    )
) {
    NewScreen()
}
```

3. Test with ADB:
```bash
adb shell am start -a android.intent.action.VIEW -d "shoppit://newscreen"
```

### Adding State Preservation

1. Use `SavedStateHandle` in ViewModel:
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var data by savedStateHandle.saveable { mutableStateOf("") }
}
```

2. Use `rememberSaveable` in Composable:
```kotlin
var textValue by rememberSaveable { mutableStateOf("") }
```

3. Enable state preservation in navigation:
```kotlin
navController.navigate(route) {
    popUpTo(startDestination) {
        saveState = true
    }
    restoreState = true
}
```

## Testing

### Unit Tests

Test navigation logic and route generation:

```kotlin
@Test
fun `createRoute generates correct route`() {
    val route = Screen.MealDetail.createRoute(123)
    assertEquals("meal_detail/123", route)
}
```

### Integration Tests

Test navigation flows:

```kotlin
@Test
fun `navigating to detail and back preserves state`() {
    navController.navigate(Screen.MealDetail.createRoute(1))
    navController.popBackStack()
    assertEquals(Screen.MealList.route, navController.currentDestination?.route)
}
```

### UI Tests

Test navigation interactions:

```kotlin
@Test
fun `clicking meal navigates to detail`() {
    composeTestRule.onNodeWithText("Pasta").performClick()
    composeTestRule.onNodeWithText("Meal Details").assertIsDisplayed()
}
```

## Troubleshooting

### Common Issues

1. **State not preserved when switching tabs**
   - Ensure `saveState = true` and `restoreState = true`
   - See [Navigation Patterns](./NAVIGATION_PATTERNS.md#scenario-1-switching-between-bottom-navigation-tabs)

2. **Navigation arguments missing**
   - Always validate arguments before use
   - Use `NavigationErrorHandler.validateArguments()`
   - See [Common Scenarios](./COMMON_SCENARIOS.md#error-handling)

3. **Deep links not working**
   - Verify AndroidManifest.xml configuration
   - Check navigation graph deep link patterns
   - Test with ADB command
   - See [Deep Link Configuration](./DEEP_LINK_CONFIGURATION.md#troubleshooting)

4. **Back stack becomes corrupted**
   - Use `launchSingleTop = true` to prevent duplicates
   - Validate back stack with `BackStackValidator`
   - See [Navigation Architecture](./NAVIGATION_ARCHITECTURE.md#error-handling)

## Best Practices

### Do's

✅ Use `Screen` sealed class for type-safe navigation
✅ Always provide fallback routes for error handling
✅ Validate navigation arguments before use
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

## Performance Guidelines

- **Navigation Transition Time:** Target < 300ms
- **Frame Rate:** Maintain 60fps during transitions
- **Memory Usage:** Monitor for leaks and excessive consumption
- **Loading Indicators:** Show for operations > 100ms

## Accessibility Guidelines

- Provide content descriptions for all navigation elements
- Announce screen transitions with screen readers
- Support keyboard navigation shortcuts
- Manage focus during screen transitions
- Maintain logical focus order

## Related Documentation

### Project Documentation

- [Project Structure](../../.kiro/steering/structure.md)
- [Compose Patterns](../../.kiro/steering/compose-patterns.md)
- [Testing Strategy](../../.kiro/steering/testing-strategy.md)
- [Technology Stack](../../.kiro/steering/tech.md)

### Code Documentation

- [Back Stack Management](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/README_BACK_STACK.md)
- [Performance Monitoring](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/util/README_PERFORMANCE.md)
- [Analytics](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/util/README_ANALYTICS.md)

### External Resources

- [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- [Material3 Navigation](https://m3.material.io/components/navigation-bar/overview)
- [Android Deep Links](https://developer.android.com/training/app-links/deep-linking)
- [SavedStateHandle](https://developer.android.com/topic/libraries/architecture/viewmodel-savedstate)

## Contributing

When adding new navigation features:

1. Update the relevant documentation files
2. Add code examples to Common Scenarios
3. Update the architecture diagram if structure changes
4. Add tests for new navigation flows
5. Update this README if new files are added

## Support

For questions or issues related to navigation:

1. Check the troubleshooting sections in the documentation
2. Review the common scenarios for examples
3. Check the inline code documentation (KDoc comments)
4. Review the test files for usage examples
