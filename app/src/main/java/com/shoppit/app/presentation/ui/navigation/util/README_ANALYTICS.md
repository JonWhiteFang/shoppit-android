# Navigation Analytics Dashboard

## Overview

The Navigation Analytics Dashboard provides comprehensive insights into navigation patterns, performance metrics, and potential issues within the Shoppit app. It aggregates data from multiple monitoring systems to give developers a complete picture of navigation health.

## Features

### 1. Overview Tab
- **Session Summary**: Duration, total navigations, error count, and error rate
- **Performance Score**: Overall performance rating with breakdown by category
- **Top Screens**: Most frequently viewed screens
- **Common Paths**: Most common navigation patterns

### 2. Screens Tab
- Complete list of all screens with view counts
- Sorted by popularity
- Helps identify most and least used features

### 3. Paths Tab
- Navigation flow analysis
- Shows common user journeys through the app
- Identifies typical navigation patterns

### 4. Performance Tab
- **Overall Performance Score**: Composite score (0-100) with rating
- **Timing Metrics**: Average, fastest, and slowest transitions
- **Frame Rate Metrics**: FPS, frame drops, and smoothness
- **Memory Metrics**: Usage, delta, and available memory

### 5. Issues Tab
- **Performance Issues**: Detected problems with severity levels
- **Screens with Errors**: Routes experiencing the most errors
- **Common Failures**: Most frequent failure types

## Accessing the Dashboard

### From Code
Navigate to the dashboard programmatically:

```kotlin
navController.navigate(Screen.AnalyticsDashboard.route)
```

### Debug Menu (Recommended)
Add a debug menu item in development builds:

```kotlin
// In MainScreen or a debug menu
if (BuildConfig.DEBUG) {
    IconButton(onClick = { 
        navController.navigate(Screen.AnalyticsDashboard.route) 
    }) {
        Icon(Icons.Default.Analytics, "Analytics")
    }
}
```

### Shake Gesture (Optional)
Implement shake detection to open the dashboard:

```kotlin
// In MainActivity
private val shakeDetector = ShakeDetector {
    // Navigate to analytics dashboard
}
```

## Performance Scoring

### Overall Score (0-100)
Weighted average of three components:
- **Timing Score (40%)**: Based on navigation transition times
- **Frame Rate Score (40%)**: Based on FPS and frame drops
- **Memory Score (20%)**: Based on memory usage

### Rating Categories
- **Excellent (90-100)**: Outstanding performance
- **Good (75-89)**: Acceptable performance
- **Fair (60-74)**: Needs attention
- **Poor (40-59)**: Significant issues
- **Critical (0-39)**: Severe problems

### Timing Score Thresholds
- ≤200ms: 100 points
- ≤300ms: 90 points (target)
- ≤400ms: 75 points
- ≤500ms: 60 points
- ≤700ms: 40 points
- >700ms: 20 points

### Frame Rate Score Thresholds
- ≥58 FPS: 100 points
- ≥55 FPS: 90 points
- ≥50 FPS: 75 points
- ≥45 FPS: 60 points
- ≥40 FPS: 40 points
- <40 FPS: 20 points

Penalty: -2 points per 1% frame drop rate

### Memory Score Thresholds
- ≤50%: 100 points
- ≤60%: 90 points
- ≤70%: 75 points
- ≤80%: 60 points
- ≤90%: 40 points
- >90%: 20 points

## Issue Detection

### Performance Issues
The dashboard automatically detects and reports:

1. **Slow Transitions**
   - Trigger: >300ms average transition time
   - Severity: HIGH if >500ms, MEDIUM otherwise

2. **Low Frame Rate**
   - Trigger: <55 FPS average
   - Severity: HIGH if <45 FPS, MEDIUM otherwise

3. **Frame Drops**
   - Trigger: >5% frame drop rate
   - Severity: HIGH if >10%, MEDIUM otherwise

4. **High Memory Usage**
   - Trigger: >80% memory usage
   - Severity: HIGH if >90%, MEDIUM otherwise

5. **Memory Leaks**
   - Trigger: >50MB memory increase during navigation
   - Severity: HIGH if >100MB, MEDIUM otherwise

## Data Management

### Resetting Analytics
Tap the refresh icon in the top bar to reset all analytics data. This:
- Clears all navigation event history
- Resets performance metrics
- Clears detected issues
- Starts a new session

### Data Persistence
Analytics data is stored in memory and resets when:
- The app is killed
- Analytics are manually reset
- The app crashes

For persistent analytics, consider:
- Exporting data periodically
- Logging to analytics services (Firebase, etc.)
- Storing in local database

## Exporting Data

### Programmatic Export
```kotlin
// Export analytics data
val export = NavigationAnalytics.exportData()

// Generate text report
val report = NavigationAnalytics.generateReport()

// Get comprehensive performance report
val perfReport = NavigationPerformanceAnalytics.getComprehensiveReport(context)
```

### Use Cases
- Bug reports: Include analytics data
- Performance analysis: Export for detailed review
- CI/CD: Automated performance testing
- User feedback: Attach analytics to support tickets

## Integration with Monitoring Services

### Firebase Analytics
```kotlin
// Track navigation events
NavigationAnalytics.analyticsState.collect { state ->
    firebaseAnalytics.logEvent("navigation_session") {
        param("total_navigations", state.totalNavigations.toLong())
        param("error_rate", state.errorRate.toDouble())
    }
}
```

### Crashlytics
```kotlin
// Log navigation context with crashes
NavigationAnalytics.getRecentEvents(5).forEach { event ->
    FirebaseCrashlytics.getInstance().log(
        "Navigation: ${event.type} - ${event.route}"
    )
}
```

### Custom Analytics
```kotlin
// Send to custom backend
val export = NavigationAnalytics.exportData()
analyticsService.sendNavigationMetrics(export)
```

## Best Practices

### Development
1. **Monitor Regularly**: Check dashboard during development
2. **Set Targets**: Aim for "Good" or better performance scores
3. **Fix Issues**: Address HIGH severity issues immediately
4. **Test Flows**: Verify common navigation paths are smooth

### Testing
1. **Performance Tests**: Use analytics to validate performance requirements
2. **Regression Tests**: Compare scores across builds
3. **Load Tests**: Monitor under heavy navigation load
4. **Device Tests**: Check performance on low-end devices

### Production
1. **Remove Debug Access**: Hide dashboard in release builds
2. **Export Metrics**: Send to analytics service
3. **Alert on Issues**: Set up monitoring for critical issues
4. **Track Trends**: Monitor performance over time

## Troubleshooting

### Dashboard Not Showing Data
- Ensure you've navigated between screens
- Check that analytics tracking is enabled
- Verify NavigationAnalytics is being called

### Performance Score is 0
- No navigation events have been recorded yet
- Navigate between screens to generate data
- Check that performance monitoring is active

### High Error Rate
- Review "Issues" tab for specific problems
- Check logs for navigation errors
- Verify navigation routes are correct
- Test on different devices

### Memory Issues
- Check for memory leaks in ViewModels
- Verify proper cleanup in composables
- Review image loading and caching
- Test with memory profiler

## Requirements Satisfied

This implementation satisfies the following requirements:

- **7.5**: Monitor navigation performance metrics
  - Comprehensive performance scoring
  - Real-time metric collection
  - Historical data analysis

- **10.1**: Log navigation errors and failures
  - Error tracking by route
  - Failure type categorization
  - Recent event history

## Future Enhancements

Potential improvements for the analytics dashboard:

1. **Charts and Graphs**: Visual representation of metrics over time
2. **Export Formats**: JSON, CSV, PDF reports
3. **Filtering**: Filter by date range, route, or issue type
4. **Comparison**: Compare sessions or builds
5. **Alerts**: Real-time notifications for critical issues
6. **Remote Monitoring**: Send data to backend service
7. **A/B Testing**: Compare navigation patterns between variants
8. **User Segmentation**: Analyze by user type or device

## Related Files

- `NavigationAnalytics.kt`: Core analytics tracking
- `NavigationPerformanceAnalytics.kt`: Performance scoring
- `NavigationPerformanceMonitor.kt`: Timing metrics
- `NavigationFrameRateMonitor.kt`: Frame rate tracking
- `NavigationMemoryMonitor.kt`: Memory monitoring
- `NavigationLogger.kt`: Event logging
- `NavigationAnalyticsDashboard.kt`: UI implementation
