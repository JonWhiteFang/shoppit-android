# Deep Link Configuration

## Overview

Deep links allow users to navigate directly to specific screens in the Shoppit app from external sources such as notifications, web links, or other apps. The deep link system is configured in both the AndroidManifest.xml and the navigation graph.

## Supported Deep Links

| Deep Link | Destination | Parameters | Example |
|-----------|-------------|------------|---------|
| `shoppit://meal/{mealId}` | Meal Detail | mealId (Long) | `shoppit://meal/123` |
| `shoppit://planner?date={date}` | Meal Planner | date (String, optional) | `shoppit://planner?date=2024-01-15` |
| `shoppit://planner` | Meal Planner | None | `shoppit://planner` |
| `shoppit://shopping` | Shopping List | None | `shoppit://shopping` |
| `shoppit://shopping/mode` | Shopping Mode | None | `shoppit://shopping/mode` |

## Configuration

### 1. AndroidManifest.xml Configuration

Add intent filters to the main activity in `app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".presentation.MainActivity"
    android:exported="true">
    
    <!-- Main launcher intent -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    
    <!-- Deep link: shoppit://meal/{mealId} -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="shoppit"
            android:host="meal" />
    </intent-filter>
    
    <!-- Deep link: shoppit://planner -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="shoppit"
            android:host="planner" />
    </intent-filter>
    
    <!-- Deep link: shoppit://shopping -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="shoppit"
            android:host="shopping" />
    </intent-filter>
</activity>
```

### 2. Navigation Graph Configuration

Configure deep links in the navigation composable definitions in `ShoppitNavHost.kt`:

```kotlin
// Meal Detail with mealId parameter
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

// Meal Planner with optional date parameter
composable(
    route = Screen.MealPlanner.route,
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "shoppit://planner?date={date}"
        },
        navDeepLink {
            uriPattern = "shoppit://planner"
        }
    )
) { backStackEntry ->
    // Screen implementation
}

// Shopping List
composable(
    route = Screen.ShoppingList.route,
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "shoppit://shopping"
        }
    )
) { backStackEntry ->
    // Screen implementation
}

// Shopping Mode
composable(
    route = Screen.ShoppingMode.route,
    deepLinks = listOf(
        navDeepLink {
            uriPattern = "shoppit://shopping/mode"
        }
    )
) { backStackEntry ->
    // Screen implementation
}
```

## Deep Link Validation

All deep links are validated before navigation to ensure data integrity:

```kotlin
// Validate required arguments
val validationErrors = NavigationErrorHandler.validateArguments(
    arguments = backStackEntry.arguments,
    requiredArgs = listOf("mealId")
)

if (validationErrors.isNotEmpty()) {
    NavigationLogger.logNavigationError(
        message = "Validation failed for deep link",
        route = Screen.MealDetail.route,
        arguments = backStackEntry.arguments?.keyValueMap()
    )
    
    // Navigate to fallback screen
    NavigationErrorHandler.handleMissingArguments(
        navController = navController,
        route = Screen.MealDetail.route,
        requiredArgs = listOf("mealId"),
        fallbackRoute = Screen.MealList.route
    )
    return@composable
}

// Validate argument values
val mealId = backStackEntry.arguments?.getLong("mealId")
if (mealId == null || mealId <= 0) {
    NavigationLogger.logNavigationError(
        message = "Invalid mealId in deep link",
        route = Screen.MealDetail.route,
        arguments = backStackEntry.arguments?.keyValueMap()
    )
    
    // Navigate to fallback screen
    NavigationErrorHandler.handleInvalidArguments(
        navController = navController,
        route = Screen.MealDetail.route,
        arguments = backStackEntry.arguments?.keyValueMap() ?: emptyMap(),
        fallbackRoute = Screen.MealList.route
    )
    return@composable
}
```

## Back Stack Construction

Deep links automatically construct a proper back stack to allow logical navigation:

### Example: Meal Detail Deep Link

When a user opens `shoppit://meal/123`:

1. Navigation system creates back stack: `MealList -> MealDetail(123)`
2. User can press back to return to Meal List
3. State is preserved as if user navigated normally

### Example: Shopping Mode Deep Link

When a user opens `shoppit://shopping/mode`:

1. Navigation system creates back stack: `ShoppingList -> ShoppingMode`
2. User can press back to return to Shopping List
3. Shopping list state is preserved

## Testing Deep Links

### Using ADB (Android Debug Bridge)

Test deep links from the command line:

```bash
# Test meal detail deep link
adb shell am start -a android.intent.action.VIEW -d "shoppit://meal/123"

# Test planner deep link with date
adb shell am start -a android.intent.action.VIEW -d "shoppit://planner?date=2024-01-15"

# Test planner deep link without date
adb shell am start -a android.intent.action.VIEW -d "shoppit://planner"

# Test shopping list deep link
adb shell am start -a android.intent.action.VIEW -d "shoppit://shopping"

# Test shopping mode deep link
adb shell am start -a android.intent.action.VIEW -d "shoppit://shopping/mode"
```

### Using Android Studio

1. Run the app on a device or emulator
2. Open Run > Edit Configurations
3. Select your app configuration
4. In the "General" tab, find "Launch Options"
5. Set "Launch" to "URL"
6. Enter the deep link URL (e.g., `shoppit://meal/123`)
7. Run the app

### Programmatic Testing

Create a test activity or use instrumented tests:

```kotlin
@Test
fun testMealDetailDeepLink() {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("shoppit://meal/123")
    }
    
    val scenario = ActivityScenario.launch<MainActivity>(intent)
    
    // Verify meal detail screen is displayed
    onView(withId(R.id.meal_detail_content))
        .check(matches(isDisplayed()))
}
```

## Error Handling

### Invalid Parameters

If a deep link contains invalid parameters:

```kotlin
// Example: shoppit://meal/invalid
// Result: Navigate to MealList with error logged
```

### Missing Required Parameters

If a deep link is missing required parameters:

```kotlin
// Example: shoppit://meal/ (no mealId)
// Result: Navigate to MealList with error logged
```

### Non-Existent Data

If a deep link references data that doesn't exist:

```kotlin
// Example: shoppit://meal/999999 (meal doesn't exist)
// Result: ViewModel handles error, shows error message, allows navigation back
```

## Use Cases

### 1. Notification Deep Links

Create notifications that open specific screens:

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("shoppit://meal/$mealId")
}

val pendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_IMMUTABLE
)

val notification = NotificationCompat.Builder(context, channelId)
    .setContentTitle("Meal Reminder")
    .setContentText("Time to prepare ${mealName}")
    .setContentIntent(pendingIntent)
    .build()
```

### 2. Share Links

Generate shareable links for meals:

```kotlin
fun generateShareLink(mealId: Long): String {
    return "shoppit://meal/$mealId"
}

// Share via intent
val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, generateShareLink(mealId))
}
startActivity(Intent.createChooser(shareIntent, "Share meal"))
```

### 3. Widget Actions

Configure widgets to open specific screens:

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("shoppit://shopping")
}

val pendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_IMMUTABLE
)

remoteViews.setOnClickPendingIntent(R.id.widget_button, pendingIntent)
```

### 4. External App Integration

Allow other apps to open Shoppit screens:

```kotlin
// From another app
val intent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("shoppit://planner?date=2024-01-15")
}

if (intent.resolveActivity(packageManager) != null) {
    startActivity(intent)
}
```

## Best Practices

### Do's

✅ Always validate deep link parameters
✅ Provide fallback navigation for invalid links
✅ Log deep link events for analytics
✅ Test deep links on different Android versions
✅ Handle both app running and app not running scenarios
✅ Create proper back stacks for logical navigation
✅ Document all supported deep links
✅ Use type-safe parameter parsing

### Don'ts

❌ Don't assume parameters are always valid
❌ Don't skip error handling for deep links
❌ Don't create deep links without back stack
❌ Don't forget to test edge cases
❌ Don't expose sensitive data in deep links
❌ Don't create overly complex deep link patterns
❌ Don't forget to update manifest when adding new deep links

## Troubleshooting

### Deep Link Not Opening App

**Possible Causes:**
1. Intent filter not configured in AndroidManifest.xml
2. App not installed on device
3. Incorrect URI scheme or host

**Solution:**
- Verify intent filter configuration
- Check app installation
- Test with ADB command

### Deep Link Opens Wrong Screen

**Possible Causes:**
1. Multiple intent filters matching the same pattern
2. Navigation graph configuration mismatch
3. Parameter parsing error

**Solution:**
- Review intent filter specificity
- Verify navigation graph deep link patterns
- Add logging to track navigation flow

### Parameters Not Passed Correctly

**Possible Causes:**
1. Parameter name mismatch between manifest and navigation graph
2. Incorrect parameter type
3. URL encoding issues

**Solution:**
- Ensure parameter names match exactly
- Verify NavType matches parameter type
- URL encode special characters

### Back Navigation Doesn't Work

**Possible Causes:**
1. Back stack not constructed properly
2. Missing parent activity declaration
3. Task affinity issues

**Solution:**
- Verify back stack construction in navigation graph
- Check parent activity configuration
- Review task affinity settings

## Security Considerations

### Validate All Input

Always validate deep link parameters to prevent:
- SQL injection
- Path traversal
- Malicious data injection

```kotlin
// Validate mealId is positive
if (mealId <= 0) {
    // Handle invalid input
}

// Validate date format
try {
    LocalDate.parse(dateString)
} catch (e: DateTimeParseException) {
    // Handle invalid date
}
```

### Avoid Sensitive Data

Don't include sensitive information in deep links:
- User credentials
- Personal information
- Payment details
- Session tokens

### Rate Limiting

Consider implementing rate limiting for deep link handling to prevent abuse:

```kotlin
private val deepLinkTimestamps = mutableListOf<Long>()

fun shouldAllowDeepLink(): Boolean {
    val now = System.currentTimeMillis()
    deepLinkTimestamps.removeAll { it < now - 60000 } // Remove entries older than 1 minute
    
    return if (deepLinkTimestamps.size < 10) {
        deepLinkTimestamps.add(now)
        true
    } else {
        false // Too many deep links in short time
    }
}
```

## Related Documentation

- [Navigation Architecture](./NAVIGATION_ARCHITECTURE.md)
- [Navigation Error Handling](../../app/src/main/java/com/shoppit/app/presentation/ui/navigation/util/NavigationErrorHandler.kt)
- [Android Deep Links Guide](https://developer.android.com/training/app-links/deep-linking)
