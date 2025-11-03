---
inclusion: always
---

# System Environment & Available Commands

## System Overview

**Operating System**: Windows 10 (amd64)
**Shell**: PowerShell (cmd)
**Project Root**: `D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android`

## Development Environment

### Java Development Kit
- **Version**: Java 17.0.14 (Oracle JDK)
- **Location**: `C:\Program Files\Java\jdk-17`
- **JAVA_HOME**: Configured via Gradle
- **Note**: `JAVA_TOOL_OPTIONS` includes `-Dlog4j2.formatMsgNoLookups=true`

### Gradle Build System
- **Version**: Gradle 8.9
- **Kotlin**: 1.9.23 (Gradle embedded)
- **Groovy**: 3.0.21
- **Ant**: 1.10.13
- **Wrapper**: Use `.\gradlew.bat` for all Gradle commands
- **Configuration**: Optimized for performance (4GB heap, parallel execution, configuration cache)

### Android SDK
- **Location**: `D:\Users\jpawhite\AppData\Local\Android\Sdk`
- **Platform Tools**: Available at `D:\Users\jpawhite\AppData\Local\Android\Sdk\platform-tools`
- **ADB**: Available at `D:\Users\jpawhite\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- **Note**: ADB not in PATH, use full path or add to PATH

### Version Control
- **Git**: 2.51.0.windows.1
- **Location**: `C:\Program Files\Git\cmd\git.exe`

### Additional Tools
- **Python**: 3.13.9 (available via WindowsApps)
- **Node.js**: v22.20.0
- **Location**: `C:\Program Files\nodejs\node.exe`

## Essential Commands

### Gradle Commands (Use .\gradlew.bat)

#### Build & Install
```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Install debug APK on connected device
.\gradlew.bat installDebug

# Build and install
.\gradlew.bat installDebug

# Clean build
.\gradlew.bat clean

# Build release APK (requires signing config)
.\gradlew.bat assembleRelease
```

#### Testing
```powershell
# Run all unit tests
.\gradlew.bat test

# Run debug unit tests only
.\gradlew.bat testDebugUnitTest

# Run specific test class
.\gradlew.bat test --tests "MealViewModelTest"

# Run specific test method
.\gradlew.bat test --tests "MealViewModelTest.loads meals successfully"

# Run instrumented tests (requires device/emulator)
.\gradlew.bat connectedAndroidTest

# Run specific instrumented test
.\gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shoppit.app.data.local.dao.MealDaoTest
```

#### Code Quality
```powershell
# Run lint checks
.\gradlew.bat lint

# Generate lint report
.\gradlew.bat lintDebug

# Check dependencies
.\gradlew.bat dependencies

# List all available tasks
.\gradlew.bat tasks
```

#### Development
```powershell
# Generate Room/Hilt sources
.\gradlew.bat kspDebugKotlin

# Check for dependency updates
.\gradlew.bat dependencyUpdates

# Build with stacktrace for debugging
.\gradlew.bat build --stacktrace

# Build with info logging
.\gradlew.bat build --info
```

### Android Debug Bridge (ADB)

**Note**: ADB is not in PATH. Use full path or add to PATH first.

```powershell
# Add ADB to PATH for current session
$env:Path += ";D:\Users\jpawhite\AppData\Local\Android\Sdk\platform-tools"

# List connected devices
adb devices

# Install APK
adb install app\build\outputs\apk\debug\app-debug.apk

# Uninstall app
adb uninstall com.shoppit.app

# View logcat
adb logcat

# Filter logcat by tag
adb logcat -s "ShoppitApp"

# Clear app data
adb shell pm clear com.shoppit.app

# Start activity
adb shell am start -n com.shoppit.app/.MainActivity

# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen
adb shell screenrecord /sdcard/demo.mp4
```

### Git Commands

```powershell
# Check status
git status

# Create feature branch
git checkout -b feature/123-description

# Stage changes
git add .

# Commit with conventional commit message
git commit -m "feat(meal): add ingredient validation"

# Push to remote
git push -u origin feature/123-description

# Pull latest changes
git pull origin develop

# Rebase on develop
git rebase origin/develop

# View log
git log --oneline --graph --decorate

# Stash changes
git stash
git stash pop
```

### File System Operations

```powershell
# List directory contents
dir
Get-ChildItem

# List with details
Get-ChildItem -Force

# Change directory (avoid in scripts, use -path parameter instead)
cd path\to\directory
Set-Location path\to\directory

# Create directory
mkdir new-directory
New-Item -ItemType Directory -Path new-directory

# Copy file
copy source.txt destination.txt
Copy-Item source.txt destination.txt

# Copy directory recursively
Copy-Item -Path source-dir -Destination dest-dir -Recurse

# Move/rename file
move old.txt new.txt
Move-Item old.txt new.txt

# Delete file
del file.txt
Remove-Item file.txt

# Delete directory recursively
rmdir /s /q directory
Remove-Item -Recurse -Force directory

# View file content
type file.txt
Get-Content file.txt

# View first/last lines
Get-Content file.txt -Head 10
Get-Content file.txt -Tail 10

# Find in files
findstr /s /i "search term" *.kt
Select-String -Path *.kt -Pattern "search term"

# Find files by name
Get-ChildItem -Recurse -Filter "*.kt"

# Get current directory
cd
pwd
Get-Location
```

### Python Commands

```powershell
# Check Python version
python --version

# Run Python script
python script.py

# Install package
pip install package-name

# List installed packages
pip list

# Create virtual environment
python -m venv venv

# Activate virtual environment
.\venv\Scripts\activate

# Deactivate virtual environment
deactivate
```

### Node.js Commands

```powershell
# Check Node version
node --version

# Check npm version
npm --version

# Install package globally
npm install -g package-name

# Install package locally
npm install package-name

# Run script from package.json
npm run script-name
```

## Project-Specific Paths

### Source Code
- **Main source**: `app\src\main\java\com\shoppit\app`
- **Test source**: `app\src\test\java\com\shoppit\app`
- **Instrumented tests**: `app\src\androidTest\java\com\shoppit\app`
- **Resources**: `app\src\main\res`
- **Manifest**: `app\src\main\AndroidManifest.xml`

### Build Outputs
- **Debug APK**: `app\build\outputs\apk\debug\app-debug.apk`
- **Release APK**: `app\build\outputs\apk\release\app-release.apk`
- **Test reports**: `app\build\reports\tests`
- **Lint reports**: `app\build\reports\lint-results.html`

### Configuration Files
- **Gradle build**: `build.gradle.kts`, `app\build.gradle.kts`
- **Version catalog**: `gradle\libs.versions.toml`
- **Gradle properties**: `gradle.properties`
- **Local properties**: `local.properties` (not in VCS)
- **Settings**: `settings.gradle.kts`

### Documentation
- **Main docs**: `docs\`
- **README**: `README.md`
- **Changelog**: `CHANGELOG.md`
- **Security**: `SECURITY_ISSUES.md`
- **Contributing**: `CONTRIBUTING.md`

### Steering & Configuration
- **Steering rules**: `.kiro\steering\`
- **Specs**: `.kiro\specs\`
- **Settings**: `.kiro\settings\`

## Gradle Performance Configuration

The project is configured for optimal performance:

- **Heap size**: 4GB (`-Xmx4096m`)
- **Metaspace**: 1GB (`-XX:MaxMetaspaceSize=1024m`)
- **Parallel execution**: Enabled (4 workers)
- **Configuration cache**: Enabled
- **Build cache**: Enabled
- **Incremental compilation**: Enabled for Kotlin
- **KSP**: Incremental mode enabled
- **R8 full mode**: Enabled for release builds

## Common Workflows

### Starting Development
```powershell
# 1. Pull latest changes
git pull origin develop

# 2. Create feature branch
git checkout -b feature/123-description

# 3. Build project
.\gradlew.bat build

# 4. Run tests
.\gradlew.bat test
```

### Running on Device
```powershell
# 1. Connect device via USB or start emulator

# 2. Verify device connection
adb devices

# 3. Build and install
.\gradlew.bat installDebug

# 4. View logs
adb logcat -s "ShoppitApp"
```

### Before Committing
```powershell
# 1. Run tests
.\gradlew.bat test

# 2. Run lint
.\gradlew.bat lint

# 3. Run security scan (MANDATORY)
pwd  # Get absolute path
# Then run Snyk scans as per mandatory-security-workflow.md

# 4. Update SECURITY_ISSUES.md

# 5. Stage and commit
git add .
git commit -m "feat(feature): description"
```

### Troubleshooting Build Issues
```powershell
# Clean and rebuild
.\gradlew.bat clean build

# Clear Gradle cache
.\gradlew.bat clean --no-build-cache

# Invalidate caches and restart (if using Android Studio)
# File > Invalidate Caches / Restart

# Check for dependency conflicts
.\gradlew.bat dependencies

# Build with stacktrace
.\gradlew.bat build --stacktrace

# Build with debug info
.\gradlew.bat build --debug
```

## Environment Variables

### Current Configuration
- `JAVA_TOOL_OPTIONS`: `-Dlog4j2.formatMsgNoLookups=true`
- `ANDROID_HOME`: Not set (configured in `local.properties`)

### Recommended Setup
```powershell
# Add to PowerShell profile for persistent configuration
# Edit: notepad $PROFILE

# Add Android SDK to PATH
$env:ANDROID_HOME = "D:\Users\jpawhite\AppData\Local\Android\Sdk"
$env:Path += ";$env:ANDROID_HOME\platform-tools"
$env:Path += ";$env:ANDROID_HOME\tools"
$env:Path += ";$env:ANDROID_HOME\tools\bin"
```

## IDE Integration

### Android Studio
- **Recommended version**: Latest stable (Hedgehog or newer)
- **JDK**: Use embedded JDK 17 or configured JDK 17
- **Gradle JVM**: Use project JDK (Java 17)
- **Build tools**: Use Gradle wrapper (`.\gradlew.bat`)

### VS Code (Kiro)
- **Extensions**: Kotlin, Gradle, Android
- **Terminal**: PowerShell
- **Tasks**: Use `.\gradlew.bat` commands
- **Debugging**: Configure launch.json for Android debugging

## Security Tools

### Snyk MCP
- **Authentication**: `snyk_auth()`
- **SAST scan**: `snyk_code_scan(path = "absolute-path")`
- **SCA scan**: `snyk_sca_scan(path = "absolute-path", all_projects = true)`
- **Version check**: `snyk_version()`
- **Logout**: `snyk_logout()`

**CRITICAL**: Always use absolute paths for Snyk scans. Get path with `pwd` first.

## Key Dependencies (from libs.versions.toml)

### Core
- **Kotlin**: 2.1.0
- **Compose BOM**: 2023.10.01
- **Hilt**: 2.56
- **Room**: 2.6.0
- **Retrofit**: 2.9.0
- **OkHttp**: 4.12.0
- **Coroutines**: 1.7.3
- **Navigation**: 2.7.4
- **Lifecycle**: 2.7.0
- **Timber**: 5.0.1

### Testing
- **JUnit**: 4.13.2
- **MockK**: 1.13.8
- **Espresso**: 3.5.1
- **AndroidX Test**: 1.5.0
- **Robolectric**: 4.11.1

### Build
- **Android Gradle Plugin**: 8.7.3
- **KSP**: 2.1.0-1.0.29

### Additional
- **Security Crypto**: 1.1.0-alpha06
- **Work Manager**: 2.9.0
- **DataStore**: 1.0.0

## Notes & Best Practices

### Windows-Specific
- Use `.\gradlew.bat` instead of `./gradlew`
- Use backslashes `\` for paths in commands
- Use forward slashes `/` in Gradle files
- PowerShell is case-insensitive for commands
- Use `&` to chain commands in cmd, `;` in PowerShell

### Path Handling
- Always use absolute paths for Snyk scans
- Use `pwd` or `Get-Location` to get current directory
- Escape backslashes in strings: `D:\\Users\\...`
- Use raw strings in Kotlin: `"""D:\Users\..."""`

### Performance
- Gradle daemon is enabled (faster builds)
- Configuration cache speeds up subsequent builds
- Parallel execution uses 4 workers
- Incremental compilation reduces build time

### Security
- Never commit `local.properties`
- Never commit API keys or secrets
- Always run security scans before committing
- Update `SECURITY_ISSUES.md` after every scan

## Quick Reference Card

```powershell
# Build & Test
.\gradlew.bat build                    # Full build
.\gradlew.bat test                     # Unit tests
.\gradlew.bat installDebug             # Install on device

# Git
git status                             # Check status
git add .                              # Stage all
git commit -m "type: message"          # Commit
git push                               # Push to remote

# ADB (add to PATH first)
adb devices                            # List devices
adb logcat                             # View logs
adb install app-debug.apk              # Install APK

# Security (MANDATORY)
pwd                                    # Get absolute path
snyk_code_scan(path = "abs-path")      # SAST scan
snyk_sca_scan(path = "abs-path")       # SCA scan

# File Operations
dir                                    # List files
Get-ChildItem                          # List files (PowerShell)
cd path                                # Change directory
type file.txt                          # View file
Get-Content file.txt                   # View file (PowerShell)
```

## Troubleshooting

### Common Issues

**Issue**: `gradlew: command not found`
**Solution**: Use `.\gradlew.bat` on Windows

**Issue**: `adb: command not found`
**Solution**: Add Android SDK platform-tools to PATH or use full path

**Issue**: Gradle build fails with "Out of memory"
**Solution**: Increase heap size in `gradle.properties` (already set to 4GB)

**Issue**: Tests fail with "No connected devices"
**Solution**: Connect device or start emulator, verify with `adb devices`

**Issue**: Snyk scan fails
**Solution**: Ensure using absolute path, authenticate with `snyk_auth()`

**Issue**: Git push rejected
**Solution**: Pull latest changes first: `git pull --rebase origin develop`

**Issue**: PowerShell execution policy blocks scripts
**Solution**: Run `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

**Issue**: Gradle daemon not starting
**Solution**: Check `gradle.properties` settings, restart daemon with `.\gradlew.bat --stop`

## Additional Resources

- **Android Developer Docs**: https://developer.android.com
- **Gradle Docs**: https://docs.gradle.org
- **Kotlin Docs**: https://kotlinlang.org/docs
- **Compose Docs**: https://developer.android.com/jetpack/compose
- **Snyk Docs**: https://docs.snyk.io

## Maintenance

This document should be updated when:
- Development tools are upgraded
- New tools are installed
- Environment configuration changes
- New workflows are established
- Common issues are discovered and resolved
