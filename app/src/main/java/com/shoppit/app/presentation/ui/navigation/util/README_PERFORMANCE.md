# Navigation Performance Monitoring

This document describes the navigation performance monitoring system implemented for the Shoppit app.

## Overview

The performance monitoring system tracks three key metrics during navigation transitions:
1. **Transition Timing** - Ensures navigation completes within 300ms target
2. **Frame Rate** - Monitors 60fps performance and frame drops
3. **Memory Usage** - Tracks memory consumption and detects potential leaks

## Components

### NavigationPerformanceMonitor
Tracks navigation transition times and provides timing metrics.

**Key Features:**
- Records transition duration for each navigation
- Alerts when transitions exceed 300ms target
- Maintains history of recent transitions
- Provides performance reports

### NavigationFrameRateMonitor
Monitors frame rates during navigation transitions using Choreographer API.

**Key Features:**
- Tracks actual FPS during transitions
- Detects frame drops (frames taking >32ms)
- Calculates average and max frame times
- Provides frame rate reports

### NavigationMemoryMonitor
Monitors memory usage during navigation to detect leaks and excessive allocations.

**Key Features:**
- Tracks memory delta during transitions
- Monitors overall memory usage percentage
- Provides detailed memory information
- Alerts on high memory usage or large increases

### NavigationPerformanceAnalytics
Aggregates all metrics and provides comprehensive performance analysis.

**Key Features:**
- Combines timing, frame rate, and memory metrics
- Calculates overall performance score (0-100)
- Detects and categorizes performance issues
- Provides comprehensive performance reports
- Assigns performance ratings (Excellent, Good, Fair, Poor, Critical)

### NavigationPreloader
Tracks navigation patterns and enables preloading of frequently accessed screens.

**Key Features:**
- Records navigation paths and frequencies
- Identifies frequently accessed destinations
- Predicts likely next destinations
- Supports ViewModel caching for faster navigation

### NavigationLoadingIndicator
Shows loading indicators for slow screen loads (>100ms).

**Key Features:**
- Delayed appearance (100ms) to avoid flicker
- Smooth fade in/out animations
- Accessibility support

### NavigationTransitions
Provides optimized transition animations for smooth 60fps performance.

**Key Features:**
- 250ms transition duration (within 300ms target)
- FastOutSlowInEasing for natural motion
- Separate animations for push/pop navigation
- Optimized fade transitions for bottom navigation

## Usage

### Automatic Monitoring

Performance monitoring is automatically enabled for all navigation events. The system:
1. Starts monitoring when navigation begins
2. Tracks metrics during the transition
3. Stops monitoring when the destination screen is displayed
4. Analyzes performance and logs any issues

### Accessing Metrics

#### Get Current Performance Score
```kotlin
val performanceScore = NavigationPerformanceAnalytics.performanceScore.collectAsState()
// performanceScore.value contains overall score and breakdown
```

#### Get Comprehensive Report
```kotlin
val report = NavigationPerformanceAnalytics.getComprehensiveReport(context)
Timber.d(report)
```

#### Get Recent Performance Issues
```kotlin
val issues = NavigationPerformanceAnalytics.getRecentIssues(count = 10)
issues.forEach { issue ->
    Timber.w("${issue.type}: ${issue.description}")
}
```

#### Get Navigation Statistics
```kotlin
val stats = NavigationPreloader.getStatistics()
Timber.d(stats)
```

### Manual Monitoring (Advanced)

For custom monitoring scenarios:

```kotlin
// Start monitoring
NavigationPerformanceAnalytics.startMonitoring(route)

// ... perform navigation ...

// Stop monitoring
NavigationPerformanceAnalytics.stopMonitoring(route)
```

## Performance Targets

### Timing
- **Target:** 300ms or less per transition
- **Good:** 200ms or less
- **Warning:** 300-500ms
- **Critical:** >500ms

### Frame Rate
- **Target:** 60 FPS
- **Good:** 55+ FPS
- **Warning:** 45-55 FPS
- **Critical:** <45 FPS

### Frame Drops
- **Target:** <2% frame drop rate
- **Good:** <5% frame drop rate
- **Warning:** 5-10% frame drop rate
- **Critical:** >10% frame drop rate

### Memory
- **Target:** <50% memory usage
- **Good:** <70% memory usage
- **Warning:** 70-80% memory usage
- **Critical:** >80% memory usage

## Performance Scoring

The system calculates an overall performance score (0-100) based on:
- **Timing Score (40%):** Based on transition duration
- **Frame Rate Score (40%):** Based on FPS and frame drops
- **Memory Score (20%):** Based on memory usage

### Performance Ratings
- **Excellent:** 90-100 points
- **Good:** 75-89 points
- **Fair:** 60-74 points
- **Poor:** 40-59 points
- **Critical:** 0-39 points

## Issue Detection

The system automatically detects and logs the following issues:

### Slow Transitions
- **Medium Severity:** 300-500ms
- **High Severity:** >500ms

### Low Frame Rate
- **Medium Severity:** 45-55 FPS
- **High Severity:** <45 FPS

### Frame Drops
- **Medium Severity:** 5-10% drop rate
- **High Severity:** >10% drop rate

### High Memory Usage
- **Medium Severity:** 80-90% usage
- **High Severity:** >90% usage

### Memory Leaks
- **Medium Severity:** 50-100MB increase
- **High Severity:** >100MB increase

## Debugging Performance Issues

### View Performance Report
```kotlin
val report = NavigationPerformanceAnalytics.getComprehensiveReport(context)
Timber.d(report)
```

### Check Specific Route Performance
```kotlin
val issues = NavigationPerformanceAnalytics.getIssuesForRoute("meal_detail/{mealId}")
issues.forEach { issue ->
    Timber.w("Issue: ${issue.description}")
}
```

### Monitor Frame Rate in Real-Time
```kotlin
NavigationFrameRateMonitor.metrics.collect { metrics ->
    Timber.d("Current FPS: ${metrics.averageFps}")
}
```

### Check Memory Usage
```kotlin
val memoryReport = NavigationMemoryMonitor.getMemoryReport(context)
Timber.d(memoryReport)
```

## Best Practices

1. **Monitor in Debug Builds:** Enable detailed logging in debug builds
2. **Profile Regularly:** Use Android Profiler alongside these metrics
3. **Address High Severity Issues:** Prioritize fixing high severity performance issues
4. **Optimize Heavy Screens:** Focus on screens with consistently poor performance
5. **Test on Low-End Devices:** Performance issues are more apparent on slower devices
6. **Clear Metrics Periodically:** Use `clearAllData()` to reset metrics during testing

## Integration with Android Profiler

For detailed analysis, use these metrics alongside Android Profiler:
1. Start navigation performance monitoring
2. Open Android Profiler
3. Navigate through the app
4. Compare metrics from both systems
5. Use frame rate and memory data to identify bottlenecks

## Clearing Data

To reset all performance data:
```kotlin
NavigationPerformanceAnalytics.clearAllData()
```

This clears:
- All timing metrics
- Frame rate history
- Memory usage data
- Navigation patterns
- Performance issues
- ViewModel cache

## Requirements Mapping

This implementation satisfies the following requirements:

- **7.1:** Navigation transitions complete within 300ms (monitored by NavigationPerformanceMonitor)
- **7.2:** Smooth animations without frame drops (monitored by NavigationFrameRateMonitor)
- **7.3:** Loading indicators for slow loads (NavigationLoadingIndicator)
- **7.4:** Screen preloading for frequent destinations (NavigationPreloader)
- **7.5:** Performance monitoring and metrics (NavigationPerformanceAnalytics)
