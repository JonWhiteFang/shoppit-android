# Navigation & Accessibility Guidelines

## Navigation Architecture

### Type-Safe Navigation

Use sealed classes for type-safe navigation routes:

```kotlin
sealed class Screen(val route: String) {
    data object MealList : Screen("meal_list")
    data object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long) = "meal_detail/$mealId"
    }
    data object AddMeal : Screen("add_meal")
    data object Planner : Screen("planner")
    data object Shopping : Screen("shopping")
}
```

### Bottom Navigation

- Use `NavigationBar` with `NavigationBarItem` for bottom navigation
- Maintain state across navigation with `rememberSaveable`
- Provide clear visual indicators for selected state
- Include content descriptions for accessibility

```kotlin
NavigationBar {
    NavigationBarItem(
        selected = currentRoute == Screen.MealList.route,
        onClick = { navController.navigate(Screen.MealList.route) },
        icon = { Icon(Icons.Default.Restaurant, contentDescription = "Meals") },
        label = { Text("Meals") },
        modifier = Modifier.semantics {
            contentDescription = "Meals tab"
            stateDescription = if (selected) "Selected" else "Not selected"
        }
    )
}
```

### Deep Links

Configure deep links in the manifest and navigation graph:

```kotlin
composable(
    route = Screen.MealDetail.route,
    arguments = listOf(navArgument("mealId") { type = NavType.LongType }),
    deepLinks = listOf(navDeepLink { uriPattern = "shoppit://meal/{mealId}" })
) { backStackEntry ->
    val mealId = backStackEntry.arguments?.getLong("mealId") ?: return@composable
    MealDetailScreen(mealId = mealId)
}
```

### State Preservation

- Use `rememberSaveable` for state that should survive configuration changes
- Save navigation state in ViewModel when appropriate
- Handle process death gracefully

## Accessibility Requirements

### Content Descriptions

**Always provide content descriptions** for:
- Icons and images
- Interactive elements without text labels
- Navigation items
- Action buttons

```kotlin
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = "Add new meal"
)
```

### Semantic Properties

Use semantic properties to communicate state and behavior:

```kotlin
Modifier.semantics {
    contentDescription = "Meals tab"
    stateDescription = if (selected) "Selected" else "Not selected"
    role = Role.Tab
}
```

### Screen Transition Announcements

Announce screen transitions for screen readers:

```kotlin
// In NavHost
var currentScreen by remember { mutableStateOf("") }

LaunchedEffect(currentRoute) {
    currentScreen = getScreenNameFromRoute(currentRoute)
}

Box(
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
        contentDescription = currentScreen
    }
) {
    // NavHost content
}
```

### Keyboard Navigation

Implement keyboard shortcuts for common actions:

```kotlin
@Composable
fun SetupKeyboardNavigation(
    navController: NavHostController,
    onNavigateToMeals: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToShopping: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    LaunchedEffect(Unit) {
        // Handle keyboard events
        // Alt+1 -> Meals, Alt+2 -> Planner, Alt+3 -> Shopping
        // Escape -> Back navigation
    }
}
```

### Focus Management

- Clear focus when navigating to new screens
- Ensure logical tab order (left to right, top to bottom)
- Provide visible focus indicators
- Move focus to appropriate element on new screen

```kotlin
val focusManager = LocalFocusManager.current

LaunchedEffect(currentRoute) {
    focusManager.clearFocus()
}
```

## Testing Requirements

### Accessibility Testing

Test with TalkBack and keyboard navigation:

```kotlin
@Test
fun navigationItemsHaveContentDescriptions() {
    composeTestRule.setContent {
        MainScreen()
    }
    
    composeTestRule
        .onNodeWithContentDescription("Meals tab")
        .assertExists()
        .assertHasClickAction()
}

@Test
fun screenTransitionsAreAnnounced() {
    composeTestRule.setContent {
        MainScreen()
    }
    
    composeTestRule
        .onNodeWithContentDescription("Planner tab")
        .performClick()
    
    // Verify screen transition announcement
    composeTestRule
        .onNodeWithText("Meal Planner", useUnmergedTree = true)
        .assertExists()
}
```

### Keyboard Navigation Testing

```kotlin
@Test
fun keyboardShortcutsNavigateBetweenSections() {
    // Test Alt+1, Alt+2, Alt+3 shortcuts
    // Test Escape for back navigation
    // Test Tab for focus movement
}
```

## Best Practices

### Navigation
- Use type-safe navigation routes
- Handle back stack properly
- Preserve state across navigation
- Support deep links for all screens
- Test navigation flows thoroughly

### Accessibility
- Provide content descriptions for all interactive elements
- Use semantic properties to communicate state
- Announce screen transitions
- Support keyboard navigation
- Test with TalkBack and keyboard
- Maintain logical focus order
- Ensure sufficient color contrast
- Support dynamic text sizing

### Performance
- Avoid recreating navigation graph unnecessarily
- Use `rememberSaveable` for state preservation
- Lazy load screens when possible
- Monitor navigation performance

## Common Patterns

### Bottom Navigation with State Preservation

```kotlin
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.MealList.route,
            modifier = Modifier.padding(padding)
        ) {
            // Composable destinations
        }
    }
}
```

### Accessible Navigation Item

```kotlin
@Composable
fun AccessibleNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = "$label navigation button"
            )
        },
        label = { Text(label) },
        modifier = Modifier.semantics {
            contentDescription = "$label tab"
            stateDescription = if (selected) "Selected" else "Not selected"
            role = Role.Tab
        }
    )
}
```

## Key Principles

### Navigation
- Always use type-safe routes (sealed classes)
- Preserve state across configuration changes
- Handle back stack properly (avoid duplicate destinations)
- Support deep links for all major screens
- Test navigation flows end-to-end

### Accessibility
- Every interactive element needs a content description
- Announce state changes for screen readers
- Support keyboard navigation with shortcuts
- Maintain logical focus order
- Test with TalkBack enabled
- Ensure minimum touch target size (48dp)
- Provide sufficient color contrast (WCAG AA)

## Documentation References

- [Navigation Architecture](../docs/navigation/NAVIGATION_ARCHITECTURE.md)
- [Deep Link Configuration](../docs/navigation/DEEP_LINK_CONFIGURATION.md)
- [Common Scenarios](../docs/navigation/COMMON_SCENARIOS.md)
- [Navigation Patterns](../docs/navigation/NAVIGATION_PATTERNS.md)
- [Navigation Accessibility](../docs/accessibility/NAVIGATION_ACCESSIBILITY.md)
- [Compose Patterns](compose-patterns.md)
