---
inclusion: always
---

# Security Best Practices with Snyk

## Overview

This project uses Snyk MCP server for continuous security scanning. Snyk helps identify and fix vulnerabilities in code, dependencies, containers, and infrastructure.

## When to Run Scans

### Automatic Scanning Triggers
- **After generating new code** - Always scan newly created Kotlin/Java files
- **After modifying dependencies** - Scan when updating `build.gradle.kts` or `libs.versions.toml`
- **Before committing** - Run scans as part of pre-commit workflow
- **After pulling changes** - Scan when integrating code from other developers

### Scan Types for This Project

#### 1. SAST (Static Application Security Testing)
**Tool**: `snyk_code_scan`
**Use for**: Kotlin/Java source code analysis
**When**: After creating/modifying any `.kt` or `.java` files

```kotlin
// Scan entire app module
snyk_code_scan(path = "/absolute/path/to/app/src")

// Scan specific feature
snyk_code_scan(path = "/absolute/path/to/app/src/main/java/com/shoppit/app/ui/meal")

// Scan with severity filter (only high/critical)
snyk_code_scan(
    path = "/absolute/path/to/app/src",
    severity_threshold = "high"
)
```

#### 2. SCA (Software Composition Analysis)
**Tool**: `snyk_sca_scan`
**Use for**: Open-source dependency vulnerabilities
**When**: After updating dependencies in Gradle files

```kotlin
// Scan all projects (recommended for multi-module)
snyk_sca_scan(
    path = "/absolute/path/to/project",
    all_projects = true
)

// Scan including dev dependencies
snyk_sca_scan(
    path = "/absolute/path/to/project",
    dev = true
)
```

#### 3. Container Scanning (Future)
**Tool**: `snyk_container_scan`
**Use for**: Docker images (when containerizing the app)
**When**: Before deploying containerized builds

#### 4. IaC Scanning (If Applicable)
**Tool**: `snyk_iac_scan`
**Use for**: Terraform, CloudFormation, Kubernetes configs
**When**: Managing cloud infrastructure

## Security Documentation

### SECURITY_ISSUES.md Maintenance

**CRITICAL**: After every security scan, update `SECURITY_ISSUES.md` to reflect current security status.

#### When to Update
- After completing any Snyk scan (SAST or SCA)
- After fixing security vulnerabilities
- After accepting/documenting risks
- Before committing code changes
- During code reviews

#### What to Document
1. **Scan Results Summary**
   - Date of scan
   - Scan type (SAST/SCA)
   - Total issues found by severity
   - Issues fixed vs remaining

2. **Active Vulnerabilities**
   - Issue ID (CVE/CWE/Snyk ID)
   - Severity level
   - Affected component/file
   - Brief description
   - Status (Open/In Progress/Accepted Risk)

3. **Fixed Issues**
   - Issue ID
   - Date fixed
   - Fix description
   - Commit reference

4. **Accepted Risks**
   - Issue ID
   - Reason for acceptance
   - Mitigation measures
   - Review date
   - Approver

#### Update Process
```kotlin
// 1. Run scan
snyk_code_scan(path = "/absolute/path/to/app/src")

// 2. Review results and fix issues
// 3. Update SECURITY_ISSUES.md with:
//    - New scan date
//    - Current vulnerability count
//    - Fixed issues
//    - Remaining issues
//    - Any accepted risks

// 4. Commit both code fixes and documentation
git add SECURITY_ISSUES.md
git commit -m "security: fix SQL injection vulnerability (SNYK-001)"
```

#### Template Structure
```markdown
# Security Issues

Last Updated: YYYY-MM-DD
Last Scan: YYYY-MM-DD

## Summary
- Critical: 0
- High: 0
- Medium: 2 (2 accepted risks)
- Low: 5

## Active Issues

### Medium Severity

#### [SNYK-KOTLIN-001] Potential Path Traversal
- **Status**: Accepted Risk
- **Location**: `app/src/main/java/com/shoppit/app/data/FileManager.kt`
- **Reason**: Input is validated at UI layer, not user-facing
- **Mitigation**: Additional validation added in v1.2.0
- **Review Date**: 2025-12-31
- **Approver**: Security Team

## Recently Fixed

### 2025-10-30
- [CVE-2023-12345] Updated OkHttp to 4.12.0 (commit: abc123)
- [SNYK-JAVA-002] Fixed SQL injection in MealDao (commit: def456)

## Scan History

### 2025-10-30 - SAST Scan
- Total Issues: 7
- Fixed: 2
- Accepted: 2
- Remaining: 3 (all low severity)
```

## Workflow Integration

### Development Workflow

1. **Create/Modify Code**
   ```
   Write new feature → Save files
   ```

2. **Run SAST Scan**
   ```
   snyk_code_scan(path = "/absolute/path/to/modified/directory")
   ```

3. **Review Results**
   - Check severity levels (Low, Medium, High, Critical)
   - Review CWE/CVE references
   - Read remediation advice

4. **Fix Issues**
   - Apply suggested fixes
   - Refactor vulnerable code patterns
   - Use Snyk Learn for education: `snyk_open_learn_lesson`

5. **Rescan**
   ```
   snyk_code_scan(path = "/absolute/path/to/modified/directory")
   ```

6. **Update SECURITY_ISSUES.md**
   - Document scan results
   - List fixed issues
   - Note any accepted risks

7. **Commit Changes**
   ```
   git add SECURITY_ISSUES.md [fixed files]
   git commit -m "security: [description]"
   ```

### Dependency Update Workflow

1. **Update Dependencies**
   ```
   Modify build.gradle.kts or libs.versions.toml
   ```

2. **Run SCA Scan**
   ```
   snyk_sca_scan(path = "/absolute/path/to/project")
   ```

3. **Review Vulnerabilities**
   - Check for known CVEs in dependencies
   - Review upgrade paths
   - Check for breaking changes

4. **Upgrade or Patch**
   - Update to patched versions
   - Apply Snyk patches if available
   - Consider alternative libraries if needed

5. **Rescan**
   ```
   snyk_sca_scan(path = "/absolute/path/to/project")
   ```

6. **Update SECURITY_ISSUES.md**
   - Document dependency updates
   - List resolved CVEs
   - Note any remaining vulnerabilities

## Common Security Issues in Android

### Code-Level Issues (SAST)

#### SQL Injection
**Risk:** Malicious SQL queries can access/modify database
**Prevention:** Use Room with parameterized queries (already implemented)

```kotlin
// Bad - vulnerable to SQL injection
@Query("SELECT * FROM meals WHERE name = '" + userInput + "'")
fun searchMeals(userInput: String): List<MealEntity>

// Good - parameterized query
@Query("SELECT * FROM meals WHERE name = :name")
fun searchMeals(name: String): List<MealEntity>
```

#### Hardcoded Secrets
**Risk:** API keys, tokens, passwords exposed in source code
**Prevention:** Use BuildConfig, environment variables, or secure storage

```kotlin
// Bad - hardcoded API key
const val API_KEY = "sk_live_1234567890abcdef"

// Good - from BuildConfig
val apiKey = BuildConfig.API_KEY

// Good - from secure storage
val apiKey = securePreferences.getString("api_key", "")
```

#### Insecure Data Storage
**Risk:** Sensitive data stored in plain text
**Prevention:** Use EncryptedSharedPreferences for sensitive data

```kotlin
// Bad - plain SharedPreferences
val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
prefs.edit().putString("auth_token", token).apply()

// Good - EncryptedSharedPreferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

encryptedPrefs.edit().putString("auth_token", token).apply()
```

#### Weak Cryptography
**Risk:** Weak encryption algorithms can be broken
**Prevention:** Use Android Keystore for encryption

```kotlin
// Bad - weak encryption
val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")

// Good - strong encryption with Keystore
val keyGenerator = KeyGenerator.getInstance(
    KeyProperties.KEY_ALGORITHM_AES,
    "AndroidKeyStore"
)

val keyGenParameterSpec = KeyGenParameterSpec.Builder(
    "my_key_alias",
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .build()

keyGenerator.init(keyGenParameterSpec)
val secretKey = keyGenerator.generateKey()
```

#### Path Traversal
**Risk:** Malicious file paths can access unauthorized files
**Prevention:** Validate and sanitize file paths

```kotlin
// Bad - no validation
fun readFile(filename: String): String {
    return File("/data/app/files/$filename").readText()
}

// Good - validate path
fun readFile(filename: String): String {
    require(!filename.contains("..")) { "Invalid filename" }
    require(!filename.startsWith("/")) { "Invalid filename" }
    
    val file = File(context.filesDir, filename)
    require(file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
        "Path traversal detected"
    }
    
    return file.readText()
}
```

#### XSS in WebViews
**Risk:** Malicious scripts can execute in WebView
**Prevention:** Sanitize user input, disable JavaScript if not needed

```kotlin
// Bad - JavaScript enabled without sanitization
webView.settings.javaScriptEnabled = true
webView.loadData(userInput, "text/html", "UTF-8")

// Good - sanitize input or disable JavaScript
webView.settings.javaScriptEnabled = false
val sanitizedInput = Html.escapeHtml(userInput)
webView.loadData(sanitizedInput, "text/html", "UTF-8")
```

#### Insecure Network Communication
**Risk:** Data transmitted over unencrypted connections
**Prevention:** Use HTTPS, implement certificate pinning

```kotlin
// Bad - allows cleartext traffic
<application android:usesCleartextTraffic="true">

// Good - enforce HTTPS
<application android:usesCleartextTraffic="false">

// Network security config
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.shoppit.app</domain>
    </domain-config>
</network-security-config>
```

#### Intent Redirection
**Risk:** Malicious apps can intercept intents
**Prevention:** Use explicit intents, validate intent data

```kotlin
// Bad - implicit intent
val intent = Intent(Intent.ACTION_VIEW)
intent.data = Uri.parse(userInput)
startActivity(intent)

// Good - explicit intent with validation
val uri = Uri.parse(userInput)
require(uri.scheme == "https") { "Only HTTPS URLs allowed" }
require(uri.host == "shoppit.app") { "Only shoppit.app URLs allowed" }

val intent = Intent(Intent.ACTION_VIEW, uri)
startActivity(intent)
```

#### Logging Sensitive Data
**Risk:** Sensitive data exposed in logs
**Prevention:** Never log sensitive information

```kotlin
// Bad - logging sensitive data
Log.d(TAG, "User password: $password")
Log.d(TAG, "Auth token: $token")

// Good - log only non-sensitive data
Log.d(TAG, "User logged in successfully")
Log.d(TAG, "Auth token received (length: ${token.length})")

// Use Timber with custom tree for production
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(CrashReportingTree())
}
```

### Dependency Issues (SCA)

#### Outdated Libraries
**Risk:** Known vulnerabilities in old versions
**Prevention:** Keep dependencies up to date

```kotlin
// Check current versions
.\gradlew.bat dependencies

// Update in libs.versions.toml
[versions]
compose-bom = "2023.10.01"  # Update to latest
hilt = "2.56"                # Update to latest
room = "2.6.0"               # Update to latest
```

#### Transitive Dependencies
**Risk:** Vulnerabilities in indirect dependencies
**Prevention:** Monitor and update transitive dependencies

```bash
# View dependency tree
.\gradlew.bat app:dependencies

# Scan for vulnerabilities
snyk_sca_scan(path = "absolute-path", all_projects = true)
```

#### License Compliance
**Risk:** Using libraries with incompatible licenses
**Prevention:** Check licenses before adding dependencies

```kotlin
// Check license compatibility
// GPL - Requires open-sourcing your code
// Apache 2.0 - Permissive, commercial use allowed
// MIT - Permissive, commercial use allowed
```

### Android-Specific Security Issues

#### Exported Components
**Risk:** Unprotected components accessible to other apps
**Prevention:** Only export components that need to be public

```xml
<!-- Bad - exported without protection -->
<activity
    android:name=".SecretActivity"
    android:exported="true" />

<!-- Good - not exported -->
<activity
    android:name=".SecretActivity"
    android:exported="false" />

<!-- Good - exported with permission -->
<activity
    android:name=".PublicActivity"
    android:exported="true"
    android:permission="com.shoppit.app.permission.ACCESS_ACTIVITY" />
```

#### Backup Configuration
**Risk:** Sensitive data included in backups
**Prevention:** Exclude sensitive files from backup

```xml
<!-- backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="secure_prefs.xml" />
    <exclude domain="database" path="sensitive.db" />
</full-backup-content>
```

#### Debug Mode in Production
**Risk:** Debug features exposed in production
**Prevention:** Disable debug features in release builds

```kotlin
// Check build type
if (BuildConfig.DEBUG) {
    // Debug-only features
    Timber.plant(Timber.DebugTree())
    StrictMode.enableDefaults()
}

// Disable debugging in manifest
<application
    android:debuggable="false"
    android:allowBackup="false">
```

## Snyk Authentication

### First-Time Setup
```
snyk_auth()
```
Follow the browser prompt to authenticate with your Snyk account.

### Logout (When Switching Accounts)
```
snyk_logout()
```

## Advanced Usage

### Filtering Results
```kotlin
// Only show high and critical issues
snyk_code_scan(
    path = "/absolute/path/to/app",
    severity_threshold = "high"
)

// Exclude specific directories
snyk_code_scan(
    path = "/absolute/path/to/app",
    exclude = "build,test"
)
```

### Organization Management
```kotlin
// Scan under specific Snyk organization
snyk_code_scan(
    path = "/absolute/path/to/app",
    org = "my-org-id"
)
```

### Ignoring Issues
Use `.snyk` policy file to document accepted risks:
```yaml
# .snyk file
version: v1.22.0
ignore:
  'SNYK-JAVA-ORGSPRINGFRAMEWORK-12345':
    - '*':
        reason: 'Not applicable to our use case'
        expires: '2025-12-31'
```

## Learning Resources

### Snyk Learn Integration
When issues are found, use Snyk Learn for education:

```kotlin
// Open lesson for specific vulnerability
snyk_open_learn_lesson(
    issueType = "sast",
    rule = "java/sql-injection",
    ecosystem = "java"
)

// Open lesson from scan results
snyk_open_learn_lesson(
    issueType = "sca",
    cves = "CVE-2023-12345",
    ecosystem = "maven"
)
```

## CI/CD Integration (Future)

### GitHub Actions Example
```yaml
- name: Snyk Code Scan
  run: snyk code test --severity-threshold=high

- name: Snyk Dependency Scan
  run: snyk test --all-projects
```

## Best Practices Summary

### Do ✅
- **Update SECURITY_ISSUES.md after every scan**
- Scan all new/modified code before committing
- Run SCA scans after dependency updates
- Fix high/critical issues immediately
- Use Snyk Learn to understand vulnerabilities
- Document accepted risks in `.snyk` file and SECURITY_ISSUES.md
- Keep Snyk CLI updated: `snyk_version()`
- Scan with appropriate severity thresholds
- Commit security documentation with code fixes

### Don't ❌
- Commit code with unresolved high/critical issues
- Ignore security warnings without investigation
- Hardcode secrets or API keys
- Skip scans to save time
- Use outdated dependencies with known CVEs
- Disable security features without documentation

## Quick Reference

### Essential Commands
```kotlin
// Authenticate
snyk_auth()

// Scan code (SAST)
snyk_code_scan(path = "/absolute/path/to/app/src")

// Scan dependencies (SCA)
snyk_sca_scan(path = "/absolute/path/to/project", all_projects = true)

// Open learning resource
snyk_open_learn_lesson(issueType = "sast", rule = "rule-id")

// Check version
snyk_version()

// Logout
snyk_logout()
```

### Path Requirements
- **Always use absolute paths** for scan commands
- Get absolute path: Run `pwd` in the working directory
- Linux/macOS format: `/home/user/project/app`
- Windows format: `C:\\Users\\user\\project\\app`

## Project-Specific Notes

### Scan Targets
- **Primary**: `app/src/main/java/com/shoppit/app/`
- **Tests**: `app/src/test/` and `app/src/androidTest/`
- **Build files**: `build.gradle.kts`, `gradle/libs.versions.toml`

### Key Dependencies to Monitor
- Jetpack Compose (BOM 2023.10.01)
- Hilt (2.48)
- Room (2.6.0)
- Retrofit (2.9.0)
- OkHttp (4.12.0)
- Kotlin (1.9.20)

### Security Priorities
1. **Critical/High** - Fix immediately, block commits
2. **Medium** - Fix before release
3. **Low** - Fix when convenient or document acceptance
