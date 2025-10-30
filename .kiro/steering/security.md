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

6. **Repeat Until Clean**
   - Continue fixing until no new issues
   - Document any accepted risks

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

## Common Security Issues in Android

### Code-Level Issues (SAST)
- **SQL Injection**: Use Room with parameterized queries (already implemented)
- **Hardcoded Secrets**: Never commit API keys, tokens, or passwords
- **Insecure Data Storage**: Use EncryptedSharedPreferences for sensitive data
- **Weak Cryptography**: Use Android Keystore for encryption
- **Path Traversal**: Validate file paths before access
- **XSS in WebViews**: Sanitize user input if using WebViews

### Dependency Issues (SCA)
- **Outdated Libraries**: Keep Compose, Hilt, Room, Retrofit up to date
- **Transitive Dependencies**: Monitor indirect dependencies
- **License Compliance**: Check for incompatible licenses

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
- Scan all new/modified code before committing
- Run SCA scans after dependency updates
- Fix high/critical issues immediately
- Use Snyk Learn to understand vulnerabilities
- Document accepted risks in `.snyk` file
- Keep Snyk CLI updated: `snyk_version()`
- Scan with appropriate severity thresholds

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
