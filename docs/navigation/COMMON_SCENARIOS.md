# Common Navigation Scenarios

## Overview

This guide provides practical examples for implementing common navigation patterns in the Shoppit app. Each scenario includes complete code examples and explanations.

## Table of Contents

1. [Basic Navigation](#basic-navigation)
2. [Navigation with Arguments](#navigation-with-arguments)
3. [Bottom Navigation](#bottom-navigation)
4. [Back Navigation](#back-navigation)
5. [Form Navigation](#form-navigation)
6. [Deep Link Navigation](#deep-link-navigation)
7. [Error Handling](#error-handling)
8. [State Preservation](#state-preservation)

## Basic Navigation

### Scenario: Navigate from Meal List to Add Meal

**Use Case:** User taps the floating action button to add a new meal.

**Implementation:**

```kotlin
@Composable
fun MealListScreen(
    onAddMealClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMealClick) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { padding ->
        // Screen content
    }
}

// In ShoppitNavHost
composable(Screen.MealList.route) {
    MealListScreen(
        onAddMealClick = {
            NavigationErrorHandler.safeNavigate(
                navController = navController,
                route = Screen.AddMeal.route,
                fallbackRoute = Screen.MealList.route
            )
        }
    )
}
```

**Key Points:**
- Use callback pattern for navigation actions
- Wrap navigation in error handler for safety
- Provide fallback route for error recovery

## Navigation with Arguments

### Scenario: Navigate to Meal Detail with Meal ID

**Use Case:** User taps a meal in the list to view its details.

**Implementation:**

```kotlin
@Composable
fun MealListScreen(
    meals: List<Meal>,
    onMealClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(meals, key = { it.id }) { meal ->
            MealCard(
                meal = meal,
                onClick = { onMealClick(meal.id) }
            )
        }
    }
}

// In ShoppitNavHost
composable(Screen.MealList.route) {
    MealListScreen(
        meals = meals,
        onMealClick = { mealId ->
            NavigationErrorHandler.safeNavigate(
                navController = navController,
                route = Screen.MealDetail.createRoute(mealId),
                arguments = mapOf("mealId" to mealId),
                fallbackRoute = Screen.MealList.route
            )
        }
    )
}

// Meal Detail composable with argument handling
composable(
    route = Screen.MealDetail.route,
    arguments = listOf(
        navArgument("mealId") {
            type = NavType.LongType
        }
    )
) { backStackEntry ->
    // Validate arguments
    val mealId = backStackEntry.arguments?.getLong("mealId")
    if (mealId == null || mealId <= 0) {
        // Handle invalid argument
        NavigationErrorHandler.handleInvalidArguments(
            navController = navController,
            route = Screen.MealDetail.route,
            arguments = backStackEntry.arguments?.keyValueMap() ?: emptyMap(),
            fallbackRoute = Screen.MealList.route
        )
        return@composable
    }
    
    MealDetailScreen(
        mealId = mealId,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

**Key Points:**
- Use `createRoute()` function for type-safe argument passing
- Always validate arguments before use
- Handle invalid arguments gracefully
- Pass arguments to error handler for logging

## Bottom Navigation

### Scenario: Switch Between Main Sections

**Use Case:** User taps a bottom navigation item to switch sections.

**Implementation:**

```kotlin
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavigationItem.items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { 
                        it.route == item.route 
                    } == true
                    
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to start destination
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true  // Save current state
                                }
                                launchSingleTop = true  // Avoid duplicates
                                restoreState = true     // Restore previous state
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        ShoppitNavHost(
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}
```

**Key Points:**
- Use `saveState = true` to preserve section state
- Use `restoreState = true` to restore previous state
- Use `launchSingleTop = true` to prevent duplicates
- Pop up to start destination to manage back stack

### Scenario: Hide Bottom Bar on Detail Screens

**Use Case:** Show more content space on detail screens by hiding bottom bar.

**Implementation:**

```kotlin
@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom bar should be visible
    val shouldShowBottomBar by remember(currentRoute) {
        derivedStateOf {
            when {
                currentRoute == null -> true
                currentRoute == Screen.MealList.route -> true
                currentRoute == Screen.MealPlanner.route -> true
                currentRoute == Screen.ShoppingList.route -> true
                currentRoute.startsWith("meal_detail") -> false
                currentRoute.startsWith("edit_meal") -> false
                currentRoute == Screen.AddMeal.route -> false
                else -> true
            }
        }
    }
    
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar {
                    // Bottom navigation items
                }
            }
        }
    ) { padding ->
        ShoppitNavHost(navController, Modifier.padding(padding))
    }
}
```

**Key Points:**
- Use `derivedStateOf` for computed visibility
- Use `AnimatedVisibility` for smooth transitions
- Check route patterns for detail screens
- Default to showing bottom bar for unknown routes

## Back Navigation

### Scenario: Navigate Back from Detail Screen

**Use Case:** User presses back button or top bar back icon.

**Implementation:**

```kotlin
@Composable
fun MealDetailScreen(
    mealId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { padding ->
        // Screen content
    }
}

// In ShoppitNavHost
composable(Screen.MealDetail.route) {
    MealDetailScreen(
        mealId = mealId,
        onNavigateBack = {
            try {
                navController.popBackStack()
            } catch (e: Exception) {
                NavigationLogger.logNavigationError(
                    message = "Failed to pop back stack",
                    exception = e
                )
                NavigationErrorHandler.handleNavigationFailure(navController, e)
            }
        }
    )
}
```

**Key Points:**
- Wrap `popBackStack()` in try-catch
- Log navigation errors for debugging
- Use error handler for recovery
- Provide clear back button icon

### Scenario: Handle System Back Button

**Use Case:** User presses Android system back button.

**Implementation:**

```kotlin
@Composable
fun MealDetailScreen(
    mealId: Long,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle system back button
    BackHandler(onBack = onNavigateBack)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { padding ->
        // Screen content
    }
}
```

**Key Points:**
- Use `BackHandler` to intercept system back button
- Provide same behavior as top bar back button
- Can add custom logic before navigating back

## Form Navigation

### Scenario: Save Form and Navigate Back

**Use Case:** User saves a meal form and returns to previous screen.

**Implementation:**

```kotlin
@Composable
fun AddEditMealScreen(
    onNavigateBack: () -> Unit,
    onMealSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Meal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveMeal()
                        },
                        enabled = uiState.isValid
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        // Form content
    }
    
    // Navigate back after successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onMealSaved()
        }
    }
}

// In ShoppitNavHost
composable(Screen.AddMeal.route) {
    AddEditMealScreen(
        onNavigateBack = {
            navController.popBackStack()
        },
        onMealSaved = {
            navController.popBackStack()
        }
    )
}
```

**Key Points:**
- Separate callbacks for cancel and save
- Use `LaunchedEffect` to trigger navigation after save
- Disable save button when form is invalid
- Show close icon for cancel action

### Scenario: Confirm Before Discarding Changes

**Use Case:** Warn user about unsaved changes when navigating back.

**Implementation:**

```kotlin
@Composable
fun AddEditMealScreen(
    onNavigateBack: () -> Unit,
    onMealSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDiscardDialog by remember { mutableStateOf(false) }
    
    // Handle back button with confirmation
    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showDiscardDialog = true
    }
    
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Meal") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.hasUnsavedChanges) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { padding ->
        // Form content
    }
}
```

**Key Points:**
- Track unsaved changes in ViewModel state
- Show confirmation dialog before discarding
- Handle both back button and close icon
- Allow user to cancel discard action

## Deep Link Navigation

### Scenario: Open Meal Detail from Notification

**Use Case:** User taps notification to view meal details.

**Implementation:**

```kotlin
// Create notification with deep link
fun createMealReminderNotification(
    context: Context,
    mealId: Long,
    mealName: String
): Notification {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("shoppit://meal/$mealId")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    
    val pendingIntent = PendingIntent.getActivity(
        context,
        mealId.toInt(),
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Meal Reminder")
        .setContentText("Time to prepare $mealName")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()
}

// Deep link configuration in ShoppitNavHost
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
    // Validate and handle deep link
    val mealId = backStackEntry.arguments?.getLong("mealId")
    if (mealId != null && mealId > 0) {
        MealDetailScreen(
            mealId = mealId,
            onNavigateBack = { navController.popBackStack() }
        )
    } else {
        // Handle invalid deep link
        NavigationErrorHandler.handleInvalidArguments(
            navController = navController,
            route = Screen.MealDetail.route,
            arguments = backStackEntry.arguments?.keyValueMap() ?: emptyMap(),
            fallbackRoute = Screen.MealList.route
        )
    }
}
```

**Key Points:**
- Use `Intent.ACTION_VIEW` for deep links
- Set appropriate intent flags
- Use `PendingIntent.FLAG_IMMUTABLE` for security
- Validate deep link parameters
- Provide fallback for invalid links

## Error Handling

### Scenario: Handle Missing Navigation Arguments

**Use Case:** Navigation argument is missing or null.

**Implementation:**

```kotlin
composable(
    route = Screen.MealDetail.route,
    arguments = listOf(
        navArgument("mealId") {
            type = NavType.LongType
        }
    )
) { backStackEntry ->
    // Validate arguments exist
    val arguments = backStackEntry.arguments
    val validationErrors = NavigationErrorHandler.validateArguments(
        arguments = arguments,
        requiredArgs = listOf("mealId")
    )
    
    if (validationErrors.isNotEmpty()) {
        NavigationLogger.logNavigationError(
            message = "Missing required arguments",
            route = Screen.MealDetail.route,
            arguments = arguments?.keyValueMap()
        )
        
        NavigationErrorHandler.handleMissingArguments(
            navController = navController,
            route = Screen.MealDetail.route,
            requiredArgs = listOf("mealId"),
            fallbackRoute = Screen.MealList.route
        )
        return@composable
    }
    
    // Arguments are valid, proceed with screen
    val mealId = arguments!!.getLong("mealId")
    MealDetailScreen(mealId = mealId)
}
```

**Key Points:**
- Validate arguments before accessing
- Log validation errors for debugging
- Use error handler for consistent recovery
- Navigate to appropriate fallback screen

### Scenario: Handle Navigation Failure

**Use Case:** Navigation operation throws an exception.

**Implementation:**

```kotlin
fun navigateToMealDetail(
    navController: NavHostController,
    mealId: Long
) {
    try {
        val route = Screen.MealDetail.createRoute(mealId)
        navController.navigate(route)
        
        NavigationLogger.logNavigationSuccess(
            route = route,
            arguments = mapOf("mealId" to mealId)
        )
    } catch (e: Exception) {
        NavigationLogger.logNavigationError(
            message = "Failed to navigate to meal detail",
            route = Screen.MealDetail.route,
            arguments = mapOf("mealId" to mealId),
            exception = e
        )
        
        NavigationErrorHandler.handleNavigationFailure(navController, e)
    }
}
```

**Key Points:**
- Wrap navigation in try-catch
- Log both success and failure
- Use error handler for recovery
- Include context in error logs

## State Preservation

### Scenario: Preserve Scroll Position When Switching Tabs

**Use Case:** User scrolls meal list, switches to planner, then returns to meal list.

**Implementation:**

```kotlin
@Composable
fun MealListScreen(
    meals: List<Meal>,
    onMealClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Scroll state is automatically preserved with rememberSaveable
    val listState = rememberLazyListState()
    
    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(meals, key = { it.id }) { meal ->
            MealCard(
                meal = meal,
                onClick = { onMealClick(meal.id) }
            )
        }
    }
}

// In MainScreen, ensure state preservation is enabled
NavigationBarItem(
    selected = selected,
    onClick = {
        navController.navigate(item.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true  // This preserves scroll state
            }
            launchSingleTop = true
            restoreState = true  // This restores scroll state
        }
    }
)
```

**Key Points:**
- Use `rememberLazyListState()` for scroll position
- Enable `saveState` and `restoreState` in navigation
- Scroll state is automatically preserved
- Works across configuration changes

### Scenario: Preserve Form State During Navigation

**Use Case:** User partially fills form, navigates away, then returns.

**Implementation:**

```kotlin
@HiltViewModel
class AddEditMealViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addMealUseCase: AddMealUseCase
) : ViewModel() {
    
    // Use SavedStateHandle for state that survives process death
    var mealName by savedStateHandle.saveable { mutableStateOf("") }
        private set
    
    var ingredients by savedStateHandle.saveable { mutableStateOf(listOf<Ingredient>()) }
        private set
    
    fun updateMealName(name: String) {
        mealName = name
    }
    
    fun addIngredient(ingredient: Ingredient) {
        ingredients = ingredients + ingredient
    }
}

@Composable
fun AddEditMealScreen(
    viewModel: AddEditMealViewModel = hiltViewModel()
) {
    // Form state is preserved in ViewModel
    OutlinedTextField(
        value = viewModel.mealName,
        onValueChange = viewModel::updateMealName,
        label = { Text("Meal Name") }
    )
}
```

**Key Points:**
- Use `SavedStateHandle` for critical form state
- State survives process death
- State preserved during navigation
- ViewModel survives configuration changes

## Related Documentation

- [Navigation Architecture](./NAVIGATION_ARCHITECTURE.md)
- [Deep Link Configuration](./DEEP_LINK_CONFIGURATION.md)
- [Compose Patterns](../../.kiro/steering/compose-patterns.md)
