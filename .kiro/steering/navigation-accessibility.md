---
inclusion: always
---

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

Deep links allow users to navigate directly to specific screens from external sources (notifications, web links, etc.).

#### AndroidManifest.xml Configuration

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">
    
    <!-- App launch intent -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link: shoppit://meal/123 -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="shoppit"
            android:host="meal" />
    </intent-filter>
    
    <!-- Web link: https://shoppit.app/meal/123 -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="https"
            android:host="shoppit.app"
            android:pathPrefix="/meal" />
    </intent-filter>
</activity>
```

#### Navigation Graph Configuration

```kotlin
composable(
    route = Screen.MealDetail.route,
    arguments = listOf(navArgument("mealId") { type = NavType.LongType }),
    deepLinks = listOf(
        navDeepLink { uriPattern = "shoppit://meal/{mealId}" },
        navDeepLink { uriPattern = "https://shoppit.app/meal/{mealId}" }
    )
) { backStackEntry ->
    val mealId = backStackEntry.arguments?.getLong("mealId") ?: return@composable
    MealDetailScreen(mealId = mealId)
}
```

#### Testing Deep Links

```bash
# Test deep link via ADB
adb shell am start -W -a android.intent.action.VIEW -d "shoppit://meal/123" com.shoppit.app

# Test web link
adb shell am start -W -a android.intent.action.VIEW -d "https://shoppit.app/meal/123" com.shoppit.app
```

#### Handling Deep Links in Activity

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ShoppitTheme {
                val navController = rememberNavController()
                
                // Handle deep link intent
                LaunchedEffect(intent) {
                    handleDeepLink(intent, navController)
                }
                
                ShoppitNavHost(navController = navController)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { setIntent(it) }
    }
    
    private fun handleDeepLink(intent: Intent, navController: NavHostController) {
        val data = intent.data ?: return
        
        when (data.host) {
            "meal" -> {
                val mealId = data.lastPathSegment?.toLongOrNull()
                if (mealId != null) {
                    navController.navigate(Screen.MealDetail.createRoute(mealId))
                }
            }
            // Handle other deep links
        }
    }
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
- Decorative vs informative images

#### Basic Content Descriptions

```kotlin
// Action button
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = "Add new meal"
)

// Decorative image (no content description needed)
Image(
    painter = painterResource(R.drawable.decorative_pattern),
    contentDescription = null // Explicitly null for decorative
)

// Informative image
Image(
    painter = painterResource(R.drawable.meal_photo),
    contentDescription = "Photo of spaghetti carbonara"
)
```

#### Context-Aware Content Descriptions

```kotlin
// Dynamic content description based on state
IconButton(
    onClick = { isFavorite = !isFavorite },
    modifier = Modifier.semantics {
        contentDescription = if (isFavorite) {
            "Remove from favorites"
        } else {
            "Add to favorites"
        }
    }
) {
    Icon(
        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
        contentDescription = null // Handled by parent
    )
}
```

#### List Item Content Descriptions

```kotlin
@Composable
fun MealListItem(
    meal: Meal,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Meal: ${meal.name}, " +
                    "${meal.ingredients.size} ingredients, " +
                    "created ${formatDate(meal.createdAt)}"
            }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(meal.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Text("${meal.ingredients.size} items")
        }
    }
}
```

### Semantic Properties

Use semantic properties to communicate state and behavior to assistive technologies.

#### Basic Semantic Properties

```kotlin
Modifier.semantics {
    contentDescription = "Meals tab"
    stateDescription = if (selected) "Selected" else "Not selected"
    role = Role.Tab
}
```

#### Advanced Semantic Properties

```kotlin
// Checkbox with custom semantics
Row(
    modifier = Modifier
        .toggleable(
            value = isChecked,
            onValueChange = { isChecked = it },
            role = Role.Checkbox
        )
        .semantics {
            contentDescription = "${item.name}, ${item.quantity} ${item.unit}"
            stateDescription = if (isChecked) "Checked" else "Unchecked"
        }
) {
    Checkbox(checked = isChecked, onCheckedChange = null)
    Text("${item.name} - ${item.quantity} ${item.unit}")
}
```

#### Custom Actions

```kotlin
// Add custom accessibility actions
Card(
    modifier = Modifier
        .fillMaxWidth()
        .semantics {
            contentDescription = "Meal: ${meal.name}"
            
            // Add custom actions for TalkBack
            customActions = listOf(
                CustomAccessibilityAction("Edit") {
                    onEdit(meal)
                    true
                },
                CustomAccessibilityAction("Delete") {
                    onDelete(meal)
                    true
                },
                CustomAccessibilityAction("Share") {
                    onShare(meal)
                    true
                }
            )
        }
) {
    // Card content
}
```

#### Heading Semantics

```kotlin
// Mark text as heading for screen readers
Text(
    text = "Meal Categories",
    style = MaterialTheme.typography.headlineMedium,
    modifier = Modifier.semantics {
        heading()
    }
)
```

#### Live Regions

```kotlin
// Announce dynamic content changes
Text(
    text = "${itemsRemaining} items remaining",
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Polite
    }
)

// For urgent announcements
Text(
    text = errorMessage,
    modifier = Modifier.semantics {
        liveRegion = LiveRegionMode.Assertive
    }
)
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

Keyboard navigation is essential for accessibility and power users.

#### Keyboard Shortcuts Implementation

```kotlin
@Composable
fun SetupKeyboardNavigation(
    navController: NavHostController,
    onNavigateToMeals: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToShopping: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    // Intercept key events at root level
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        // Alt+1 -> Meals
                        keyEvent.isAltPressed && keyEvent.key == Key.One -> {
                            onNavigateToMeals()
                            true
                        }
                        // Alt+2 -> Planner
                        keyEvent.isAltPressed && keyEvent.key == Key.Two -> {
                            onNavigateToPlanner()
                            true
                        }
                        // Alt+3 -> Shopping
                        keyEvent.isAltPressed && keyEvent.key == Key.Three -> {
                            onNavigateToShopping()
                            true
                        }
                        // Escape -> Back
                        keyEvent.key == Key.Escape -> {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                                true
                            } else {
                                false
                            }
                        }
                        // Ctrl+N -> New meal (example)
                        keyEvent.isCtrlPressed && keyEvent.key == Key.N -> {
                            navController.navigate(Screen.AddMeal.route)
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Content
    }
}
```

#### Keyboard Shortcuts Reference

| Shortcut | Action |
|----------|--------|
| Alt+1 | Navigate to Meals |
| Alt+2 | Navigate to Planner |
| Alt+3 | Navigate to Shopping List |
| Ctrl+N | Add new meal |
| Escape | Go back |
| Tab | Move focus forward |
| Shift+Tab | Move focus backward |
| Enter/Space | Activate focused element |
| Arrow keys | Navigate within lists |

#### Custom Focus Indicators

```kotlin
@Composable
fun FocusableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    Button(
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable(interactionSource = interactionSource)
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        interactionSource = interactionSource
    ) {
        Text(text)
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
    composeTestRule.setContent {
        MainScreen()
    }
    
    // Test Alt+1 navigates to Meals
    composeTestRule.onRoot().performKeyPress(KeyEvent(Key.One, KeyEventType.KeyDown, isAltPressed = true))
    composeTestRule.onNodeWithText("Meals").assertIsDisplayed()
    
    // Test Alt+2 navigates to Planner
    composeTestRule.onRoot().performKeyPress(KeyEvent(Key.Two, KeyEventType.KeyDown, isAltPressed = true))
    composeTestRule.onNodeWithText("Meal Planner").assertIsDisplayed()
    
    // Test Escape for back navigation
    composeTestRule.onRoot().performKeyPress(KeyEvent(Key.Escape, KeyEventType.KeyDown))
    composeTestRule.onNodeWithText("Meals").assertIsDisplayed()
}

@Test
fun tabNavigationMovesForward() {
    composeTestRule.setContent {
        MealListScreen()
    }
    
    // Press Tab to move focus
    composeTestRule.onRoot().performKeyPress(KeyEvent(Key.Tab, KeyEventType.KeyDown))
    
    // Verify focus moved to first focusable element
    composeTestRule.onNodeWithTag("add_meal_button").assertIsFocused()
}

@Test
fun enterKeyActivatesFocusedElement() {
    var clicked = false
    
    composeTestRule.setContent {
        Button(
            onClick = { clicked = true },
            modifier = Modifier.testTag("test_button")
        ) {
            Text("Click Me")
        }
    }
    
    // Focus the button
    composeTestRule.onNodeWithTag("test_button").requestFocus()
    
    // Press Enter
    composeTestRule.onRoot().performKeyPress(KeyEvent(Key.Enter, KeyEventType.KeyDown))
    
    // Verify button was clicked
    assertTrue(clicked)
}
```

### TalkBack Testing

```kotlin
@Test
fun talkBackAnnouncesListItems() {
    composeTestRule.setContent {
        MealListScreen(
            uiState = MealUiState.Success(
                meals = listOf(
                    Meal(id = 1, name = "Pasta", ingredients = listOf())
                )
            )
        )
    }
    
    // Verify content description includes all relevant info
    composeTestRule
        .onNodeWithContentDescription("Meal: Pasta, 0 ingredients", substring = true)
        .assertExists()
}

@Test
fun customActionsAreAvailable() {
    composeTestRule.setContent {
        MealCard(
            meal = Meal(id = 1, name = "Pasta"),
            onEdit = {},
            onDelete = {},
            onShare = {}
        )
    }
    
    // Verify custom actions are available
    composeTestRule
        .onNode(hasCustomAction("Edit"))
        .assertExists()
    
    composeTestRule
        .onNode(hasCustomAction("Delete"))
        .assertExists()
}
```

### Touch Target Size Testing

```kotlin
@Test
fun interactiveElementsMeetMinimumTouchTarget() {
    composeTestRule.setContent {
        IconButton(onClick = {}) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
    
    // Verify minimum touch target size (48dp)
    composeTestRule
        .onNodeWithContentDescription("Delete")
        .assertHeightIsAtLeast(48.dp)
        .assertWidthIsAtLeast(48.dp)
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

## Accessibility Checklist

### Before Releasing a Screen

- [ ] All interactive elements have content descriptions
- [ ] Content descriptions are meaningful and context-aware
- [ ] Decorative images have `contentDescription = null`
- [ ] Touch targets are at least 48dp × 48dp
- [ ] Color contrast meets WCAG AA standards (4.5:1 for text)
- [ ] Screen works with TalkBack enabled
- [ ] Keyboard navigation is functional
- [ ] Focus indicators are visible
- [ ] Tab order is logical (left-to-right, top-to-bottom)
- [ ] Custom actions are provided for complex interactions
- [ ] State changes are announced (live regions)
- [ ] Error messages are accessible
- [ ] Loading states are announced
- [ ] Dynamic content updates are announced
- [ ] Text can scale up to 200% without breaking layout

### Testing Checklist

- [ ] Test with TalkBack enabled
- [ ] Test with keyboard only (no mouse/touch)
- [ ] Test with large font sizes (Settings > Display > Font size)
- [ ] Test with display scaling (Settings > Display > Display size)
- [ ] Test in dark mode
- [ ] Test with color inversion (Settings > Accessibility)
- [ ] Test with high contrast mode
- [ ] Verify all interactive elements are reachable
- [ ] Verify all content is announced correctly
- [ ] Verify custom actions work as expected

## Common Accessibility Issues

### Issue 1: Missing Content Descriptions

**Problem:**
```kotlin
// Bad - no content description
Icon(Icons.Default.Delete, contentDescription = null)
```

**Solution:**
```kotlin
// Good - descriptive content description
Icon(Icons.Default.Delete, contentDescription = "Delete meal")
```

### Issue 2: Touch Targets Too Small

**Problem:**
```kotlin
// Bad - icon is only 24dp
Icon(
    Icons.Default.Delete,
    contentDescription = "Delete",
    modifier = Modifier.size(24.dp)
)
```

**Solution:**
```kotlin
// Good - IconButton provides 48dp touch target
IconButton(onClick = { onDelete() }) {
    Icon(Icons.Default.Delete, contentDescription = "Delete meal")
}
```

### Issue 3: Poor Color Contrast

**Problem:**
```kotlin
// Bad - light gray on white (poor contrast)
Text(
    text = "Secondary info",
    color = Color(0xFFCCCCCC)
)
```

**Solution:**
```kotlin
// Good - use theme colors with sufficient contrast
Text(
    text = "Secondary info",
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

### Issue 4: Unlabeled Form Fields

**Problem:**
```kotlin
// Bad - no label
TextField(
    value = mealName,
    onValueChange = { mealName = it }
)
```

**Solution:**
```kotlin
// Good - clear label
TextField(
    value = mealName,
    onValueChange = { mealName = it },
    label = { Text("Meal name") },
    modifier = Modifier.semantics {
        contentDescription = "Meal name input field"
    }
)
```

### Issue 5: State Changes Not Announced

**Problem:**
```kotlin
// Bad - loading state change not announced
if (isLoading) {
    CircularProgressIndicator()
}
```

**Solution:**
```kotlin
// Good - announce loading state
if (isLoading) {
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "Loading meals"
        }
    ) {
        CircularProgressIndicator()
    }
}
```

## Documentation References

- [Navigation Architecture](../docs/navigation/NAVIGATION_ARCHITECTURE.md)
- [Deep Link Configuration](../docs/navigation/DEEP_LINK_CONFIGURATION.md)
- [Common Scenarios](../docs/navigation/COMMON_SCENARIOS.md)
- [Navigation Patterns](../docs/navigation/NAVIGATION_PATTERNS.md)
- [Navigation Accessibility](../docs/accessibility/NAVIGATION_ACCESSIBILITY.md)
- [Compose Patterns](compose-patterns.md)
- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility)
- [Material Design Accessibility](https://m3.material.io/foundations/accessible-design/overview)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
