# Performance Testing Guide

## Quick Start

This guide provides instructions for running performance tests on the Shoppit Android application.

## Prerequisites

- Android device or emulator (API 24+)
- For macrobenchmarks: API 29+ recommended
- Gradle 8.9
- Java 17

## Test Types

### 1. Unit Tests (Fast, No Device Required)

**Location:** `app/src/test/java/com/shoppit/app/performance/`

**Run all unit tests:**
```powershell
.\gradlew.bat test
```

**Run specific performance tests:**
```powershell
# Startup tests
.\gradlew.bat test --tests "StartupPerformanceTest"

# Database tests
.\gradlew.bat test --tests "DatabasePerformanceTest"

# UI tests
.\gradlew.bat test --tests "UiPerformanceTest"

# Memory tests
.\gradlew.bat test --tests "MemoryPerformanceTest"

# Cache tests
.\gradlew.bat test --tests "CachePerformanceTest"
```

**View results:**
```
app/build/reports/tests/testDebugUnitTest/index.html
```

### 2. Instrumented Tests (Requires Device)

**Location:** `app/src/androidTest/java/com/shoppit/app/performance/`

**Run all instrumented tests:**
```powershell
.\gradlew.bat connectedAndroidTest
```

**Run specific performance tests:**
```powershell
# All performance tests
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.performance.*

# Specific test class
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.performance.DatabasePerformanceInstrumentedTest
```

**View results:**
```
app/build/reports/androidTests/connected/index.html
```

### 3. Macrobenchmark Tests (Comprehensive, Requires Device)

**Location:** `macrobenchmark/src/main/java/com/shoppit/app/macrobenchmark/`

**Build benchmark variant:**
```powershell
.\gradlew.bat :app:assembleBenchmark
```

**Run all benchmarks:**
```powershell
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest
```

**Run specific benchmarks:**
```powershell
# Startup benchmark
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.macrobenchmark.StartupBenchmark

# Scroll benchmark
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.macrobenchmark.ScrollBenchmark

# Navigation benchmark
.\gradlew.bat :macrobenchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.macrobenchmark.NavigationBenchmark
```

**View results:**
```
macrobenchmark/build/outputs/connected_android_test_additional_output/benchmarkBenchmarkAndroidTest/connected/
```

## Performance Targets

| Test | Target | Command |
|------|--------|---------|
| Cold Start | < 2000ms | StartupBenchmark |
| Warm Start | < 1000ms | StartupBenchmark |
| Hot Start | < 500ms | StartupBenchmark |
| Query Time | < 50ms | DatabasePerformanceTest |
| Frame Rate | 60 FPS | ScrollBenchmark |
| Memory Usage | < 100MB | MemoryPerformanceTest |
| Cache Hit Rate | > 80% | CachePerformanceTest |
| Navigation | < 100ms | NavigationBenchmark |

## Interpreting Results

### Unit Test Results

Unit tests provide pass/fail results with assertions:
- ✅ Green: Test passed, performance target met
- ❌ Red: Test failed, performance target not met

Check test output for specific metrics and failure reasons.

### Macrobenchmark Results

Macrobenchmark results include:

1. **JSON Files** - Detailed metrics in JSON format
2. **Trace Files** - Perfetto traces for profiling
3. **Summary Reports** - Human-readable summaries

**Key Metrics:**

- **StartupTimingMetric**: Time to first frame
- **FrameTimingMetric**: Frame rendering times
  - P50: Median frame time
  - P90: 90th percentile frame time
  - P95: 95th percentile frame time
  - P99: 99th percentile frame time

**Good Performance:**
- P50 < 16.67ms (60 FPS)
- P90 < 20ms
- P95 < 25ms
- P99 < 50ms

## Troubleshooting

### Macrobenchmark Tests Fail to Run

**Issue:** Tests don't start or fail immediately

**Solutions:**
1. Ensure device is API 29+
2. Build benchmark variant first: `.\gradlew.bat :app:assembleBenchmark`
3. Check device has sufficient storage
4. Disable animations on device:
   ```
   adb shell settings put global window_animation_scale 0
   adb shell settings put global transition_animation_scale 0
   adb shell settings put global animator_duration_scale 0
   ```

### Tests Run Slowly

**Issue:** Tests take very long to complete

**Solutions:**
1. Reduce iterations in benchmark configuration
2. Run specific tests instead of all tests
3. Use faster device or emulator
4. Close other apps on device

### Inconsistent Results

**Issue:** Results vary significantly between runs

**Solutions:**
1. Ensure device is not under load
2. Close background apps
3. Use physical device instead of emulator
4. Increase number of iterations for more stable results
5. Ensure device is plugged in and not in battery saver mode

### Out of Memory Errors

**Issue:** Tests fail with OOM errors

**Solutions:**
1. Increase Gradle heap size in `gradle.properties`
2. Run fewer tests at once
3. Clear build cache: `.\gradlew.bat clean`
4. Restart Gradle daemon: `.\gradlew.bat --stop`

## Best Practices

### Before Running Tests

1. **Clean Build**
   ```powershell
   .\gradlew.bat clean
   ```

2. **Ensure Device is Ready**
   - Fully charged or plugged in
   - No background apps running
   - Animations disabled (for benchmarks)
   - Sufficient storage available

3. **Close Other Apps**
   - Close Android Studio
   - Close other resource-intensive apps

### During Testing

1. **Don't Touch Device**
   - Let tests run without interaction
   - Don't lock/unlock screen
   - Don't press buttons

2. **Monitor Progress**
   - Watch Gradle output for progress
   - Check for errors or warnings

3. **Be Patient**
   - Benchmarks can take 10-30 minutes
   - Multiple iterations ensure accuracy

### After Testing

1. **Review Results**
   - Check all metrics against targets
   - Look for regressions
   - Identify bottlenecks

2. **Document Findings**
   - Update PERFORMANCE_VALIDATION_REPORT.md
   - Note any issues or concerns
   - Track improvements over time

3. **Re-enable Animations**
   ```
   adb shell settings put global window_animation_scale 1
   adb shell settings put global transition_animation_scale 1
   adb shell settings put global animator_duration_scale 1
   ```

## Continuous Integration

### GitHub Actions Example

```yaml
name: Performance Tests

on:
  pull_request:
    branches: [ develop, main ]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run unit tests
        run: ./gradlew test

  instrumented-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          script: ./gradlew connectedAndroidTest
```

## Performance Monitoring in Production

### Firebase Performance Monitoring

Add Firebase Performance Monitoring for production metrics:

1. Add dependency to `app/build.gradle.kts`
2. Initialize in Application class
3. Add custom traces for key operations
4. Monitor metrics in Firebase Console

### Custom Metrics

Track custom performance metrics:

```kotlin
// In PerformanceMonitor
fun trackCustomMetric(name: String, value: Long) {
    // Log to Timber
    Timber.tag("Performance").d("$name: ${value}ms")
    
    // Send to Firebase (if enabled)
    // FirebasePerformance.getInstance()
    //     .newTrace(name)
    //     .putMetric("duration", value)
}
```

## Resources

- [Android Performance Documentation](https://developer.android.com/topic/performance)
- [Macrobenchmark Guide](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview)
- [Jetpack Compose Performance](https://developer.android.com/jetpack/compose/performance)
- [Room Performance Best Practices](https://developer.android.com/training/data-storage/room/performance)

## Support

For issues or questions:
1. Check troubleshooting section above
2. Review test logs in build output
3. Consult Android documentation
4. Ask team for assistance

---

**Last Updated:** 2025-11-05  
**Version:** 1.0
