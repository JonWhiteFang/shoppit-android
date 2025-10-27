# Gradle Commands Reference

Quick reference for common Gradle commands used in the Shoppit Android project.

## Build Commands

### Build APK

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build all variants
./gradlew assemble
```

### Install on Device

```bash
# Install debug build
./gradlew installDebug

# Install release build
./gradlew installRelease

# Uninstall
./gradlew uninstallDebug
./gradlew uninstallRelease
```

### Clean Build

```bash
# Clean build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDebug

# Clean and install
./gradlew clean installDebug
```

## Test Commands

### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run debug unit tests only
./gradlew testDebugUnitTest

# Run release unit tests only
./gradlew testReleaseUnitTest

# Run specific test class
./gradlew test --tests "MealViewModelTest"

# Run specific test method
./gradlew test --tests "MealViewModelTest.loads meals successfully when repository returns data"

# Run tests with info logging
./gradlew test --info

# Run tests with stack traces
./gradlew test --stacktrace
```

### Instrumented Tests

```bash
# Run all instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run debug instrumented tests
./gradlew connectedDebugAndroidTest

# Run specific instrumented test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest

# Run specific test method
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest#insertAndRetrieveMeal
```

### Test Coverage

```bash
# Generate test coverage report
./gradlew testDebugUnitTest jacocoTestReport

# View report at: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Code Quality Commands

### Lint

```bash
# Run lint checks
./gradlew lint

# Run lint on debug variant
./gradlew lintDebug

# Run lint on release variant
./gradlew lintRelease

# Generate lint report
# Report location: app/build/reports/lint-results.html
```

### Code Style (ktlint)

```bash
# Check code style
./gradlew ktlintCheck

# Auto-format code
./gradlew ktlintFormat
```

## Development Commands

### Generate Sources

```bash
# Generate Room and Hilt sources with KSP
./gradlew kspDebugKotlin

# Generate release sources
./gradlew kspReleaseKotlin
```

### Dependencies

```bash
# Show dependency tree
./gradlew dependencies

# Show dependencies for specific configuration
./gradlew dependencies --configuration debugRuntimeClasspath

# Check for dependency updates
./gradlew dependencyUpdates
```

### Build Analysis

```bash
# Show all tasks
./gradlew tasks

# Show all tasks with details
./gradlew tasks --all

# Show project properties
./gradlew properties

# Build with build scan
./gradlew build --scan
```

## Troubleshooting Commands

### Cache Management

```bash
# Clear Gradle cache
./gradlew cleanBuildCache

# Refresh dependencies
./gradlew build --refresh-dependencies

# Clear all caches and rebuild
./gradlew clean cleanBuildCache
rm -rf .gradle
./gradlew build
```

### Debugging Builds

```bash
# Build with debug output
./gradlew assembleDebug --debug

# Build with info output
./gradlew assembleDebug --info

# Build with stack trace
./gradlew assembleDebug --stacktrace

# Build with full stack trace
./gradlew assembleDebug --full-stacktrace

# Dry run (show what would be executed)
./gradlew assembleDebug --dry-run
```

### Daemon Management

```bash
# Stop Gradle daemon
./gradlew --stop

# Run without daemon
./gradlew assembleDebug --no-daemon

# Show daemon status
./gradlew --status
```

## Performance Commands

### Build Performance

```bash
# Build with build cache
./gradlew assembleDebug --build-cache

# Build with parallel execution
./gradlew assembleDebug --parallel

# Build with max workers
./gradlew assembleDebug --max-workers=4

# Profile build
./gradlew assembleDebug --profile
# Report location: build/reports/profile/
```

### Continuous Build

```bash
# Watch for changes and rebuild
./gradlew build --continuous

# Watch tests
./gradlew test --continuous
```

## Release Commands

### Build Release

```bash
# Build release APK (requires signing config)
./gradlew assembleRelease

# Build release bundle (for Play Store)
./gradlew bundleRelease

# Install release build
./gradlew installRelease
```

### Signing

```bash
# Build signed release
./gradlew assembleRelease -Pandroid.injected.signing.store.file=/path/to/keystore
./gradlew assembleRelease -Pandroid.injected.signing.store.password=password
./gradlew assembleRelease -Pandroid.injected.signing.key.alias=alias
./gradlew assembleRelease -Pandroid.injected.signing.key.password=password
```

## Common Workflows

### Development Workflow

```bash
# 1. Clean and build
./gradlew clean assembleDebug

# 2. Run tests
./gradlew test

# 3. Check code style
./gradlew ktlintCheck

# 4. Run lint
./gradlew lintDebug

# 5. Install on device
./gradlew installDebug
```

### Pre-Commit Workflow

```bash
# Run all checks before committing
./gradlew clean test ktlintCheck lintDebug
```

### CI/CD Workflow

```bash
# Full CI build
./gradlew clean test connectedAndroidTest lintDebug assembleRelease
```

### Quick Iteration

```bash
# Fast rebuild and install
./gradlew installDebug

# With tests
./gradlew test installDebug
```

## Gradle Properties

### Common Properties

Add to `gradle.properties`:

```properties
# Enable parallel execution
org.gradle.parallel=true

# Enable build cache
org.gradle.caching=true

# Configure JVM memory
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m

# Enable configuration cache (experimental)
org.gradle.configuration-cache=true

# Enable Kotlin incremental compilation
kotlin.incremental=true

# Enable KSP incremental processing
ksp.incremental=true
```

### Project Properties

```properties
# Android SDK location
sdk.dir=/path/to/android/sdk

# Signing configuration
RELEASE_STORE_FILE=/path/to/keystore
RELEASE_STORE_PASSWORD=password
RELEASE_KEY_ALIAS=alias
RELEASE_KEY_PASSWORD=password
```

## Command Options

### Common Options

```bash
# Show help
./gradlew --help

# Show version
./gradlew --version

# Quiet output
./gradlew assembleDebug --quiet

# Warn output level
./gradlew assembleDebug --warn

# Continue on failure
./gradlew test --continue

# Fail fast
./gradlew test --fail-fast

# Offline mode
./gradlew assembleDebug --offline
```

## Useful Aliases

Add to your shell profile (`.bashrc`, `.zshrc`, etc.):

```bash
# Gradle shortcuts
alias gw='./gradlew'
alias gwc='./gradlew clean'
alias gwb='./gradlew assembleDebug'
alias gwi='./gradlew installDebug'
alias gwt='./gradlew test'
alias gwl='./gradlew lintDebug'
alias gwk='./gradlew ktlintCheck'
alias gwf='./gradlew ktlintFormat'

# Combined commands
alias gwci='./gradlew clean installDebug'
alias gwct='./gradlew clean test'
alias gwcheck='./gradlew test ktlintCheck lintDebug'
```

## Task Dependencies

Understanding task dependencies:

```bash
# Show task dependencies
./gradlew assembleDebug --dry-run

# Common dependency chains:
# assembleDebug depends on:
#   - compileDebugKotlin
#   - kspDebugKotlin (generates Room/Hilt code)
#   - processDebugResources
#   - mergeDebugAssets

# test depends on:
#   - compileDebugUnitTestKotlin
#   - kspDebugKotlin
#   - compileDebugKotlin

# connectedAndroidTest depends on:
#   - assembleDebug
#   - assembleDebugAndroidTest
#   - installDebug
#   - installDebugAndroidTest
```

## Troubleshooting Guide

### Build Fails with "Cannot find symbol"

```bash
# Solution: Regenerate KSP sources
./gradlew clean
./gradlew kspDebugKotlin
./gradlew assembleDebug
```

### Tests Fail to Run

```bash
# Solution: Clean test results and rebuild
./gradlew cleanTest
./gradlew test --rerun-tasks
```

### Gradle Daemon Issues

```bash
# Solution: Stop daemon and rebuild
./gradlew --stop
./gradlew clean assembleDebug
```

### Out of Memory Errors

```bash
# Solution: Increase heap size in gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m

# Or run with increased memory
./gradlew assembleDebug -Dorg.gradle.jvmargs=-Xmx4096m
```

### Slow Builds

```bash
# Solution: Enable parallel execution and build cache
# Add to gradle.properties:
org.gradle.parallel=true
org.gradle.caching=true

# Profile build to find bottlenecks
./gradlew assembleDebug --profile
```

## Version Information

Current project configuration:

- **Gradle**: 8.9 (via wrapper)
- **Android Gradle Plugin**: 8.7.3
- **Kotlin**: 2.0.21
- **KSP**: 2.0.21-1.0.28
- **Java**: 17

Check versions:

```bash
# Gradle version
./gradlew --version

# Project versions
./gradlew dependencies | grep -E "kotlin|gradle|ksp"
```

## Further Reading

- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [Android Gradle Plugin](https://developer.android.com/build)
- [Gradle Build Scans](https://scans.gradle.com/)
- [Getting Started Guide](../guides/getting-started.md) - Project setup
- [Tech Stack Details](../../.kiro/steering/tech.md) - Complete tech stack information
