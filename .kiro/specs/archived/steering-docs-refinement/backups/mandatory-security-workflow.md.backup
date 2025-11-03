---
inclusion: always
---

# MANDATORY Security Workflow

## CRITICAL RULE: Security Scanning is REQUIRED

**YOU MUST run security scans after creating or modifying code files. This is NOT optional.**

## Mandatory Workflow for ALL Code Changes

### Step 1: Create/Modify Code
When you create or modify any `.kt`, `.java`, or build files.

### Step 2: IMMEDIATELY Run Security Scan
**DO NOT SKIP THIS STEP**

#### For Kotlin/Java Code Changes:
```bash
# Get absolute path first
pwd

# Run SAST scan on modified directory
snyk_code_scan(path = "<absolute-path-to-modified-directory>")
```

#### For Dependency Changes (build.gradle.kts, libs.versions.toml):
```bash
# Get absolute path first
pwd

# Run SCA scan on project root
snyk_sca_scan(path = "<absolute-path-to-project-root>", all_projects = true)
```

### Step 3: Review and Fix Issues
- Review all findings, especially Critical and High severity
- Fix issues or document why they're accepted risks
- Re-run scan to verify fixes

### Step 4: Update SECURITY_ISSUES.md
**REQUIRED** - Update the security documentation:
- Add scan date and results summary
- Document any new issues found
- Document any issues fixed
- Document any accepted risks with justification

### Step 5: Verify Before Completion
- Confirm scan completed successfully
- Confirm SECURITY_ISSUES.md is updated
- Confirm no unresolved Critical/High issues (or documented as accepted)

## Workflow Checklist

Use this checklist for EVERY code change:

- [ ] Code created/modified
- [ ] `pwd` executed to get absolute path
- [ ] Security scan executed (SAST or SCA)
- [ ] Scan results reviewed
- [ ] Critical/High issues fixed or documented
- [ ] SECURITY_ISSUES.md updated
- [ ] Re-scan completed (if fixes were made)
- [ ] Ready to commit

## Examples

### Example 1: Creating New ViewModel
```bash
# 1. Create MealViewModel.kt
# 2. Get absolute path
pwd
# Output: C:\Users\user\shoppit

# 3. Run SAST scan
snyk_code_scan(path = "C:\\Users\\user\\shoppit\\app\\src\\main\\java\\com\\shoppit\\app\\ui\\meal")

# 4. Review results, fix any issues
# 5. Update SECURITY_ISSUES.md
# 6. Re-scan if needed
```

### Example 2: Updating Dependencies
```bash
# 1. Modify build.gradle.kts or libs.versions.toml
# 2. Get absolute path
pwd
# Output: C:\Users\user\shoppit

# 3. Run SCA scan
snyk_sca_scan(path = "C:\\Users\\user\\shoppit", all_projects = true)

# 4. Review CVEs, update dependencies if needed
# 5. Update SECURITY_ISSUES.md
# 6. Re-scan to verify
```

### Example 3: Creating Multiple Files
```bash
# 1. Create MealRepository.kt, MealRepositoryImpl.kt, MealDao.kt
# 2. Get absolute path
pwd
# Output: C:\Users\user\shoppit

# 3. Run SAST scan on entire data layer
snyk_code_scan(path = "C:\\Users\\user\\shoppit\\app\\src\\main\\java\\com\\shoppit\\app\\data")

# 4. Review results
# 5. Update SECURITY_ISSUES.md
```

## What to Scan

### Always Scan:
- New `.kt` or `.java` files
- Modified `.kt` or `.java` files
- Changes to `build.gradle.kts`
- Changes to `gradle/libs.versions.toml`
- Changes to `settings.gradle.kts`

### Scan Scope:
- **Single file change**: Scan the containing directory
- **Multiple files in same feature**: Scan the feature directory
- **Multiple files across features**: Scan `app/src/main/java`
- **Dependency changes**: Scan project root with `all_projects = true`

## Handling Scan Results

### Critical/High Severity:
- **MUST FIX** before completing the task
- If cannot fix immediately, document as accepted risk with:
  - Clear justification
  - Mitigation plan
  - Review date
  - Approver (if applicable)

### Medium Severity:
- Fix if straightforward
- Document if accepting risk
- Plan to fix before release

### Low Severity:
- Review and document
- Fix when convenient
- Can be accepted with brief justification

## Integration with Task Completion

**A task is NOT complete until:**
1. ✅ Code is written and working
2. ✅ Security scan is executed
3. ✅ Scan results are reviewed
4. ✅ Critical/High issues are fixed or documented
5. ✅ SECURITY_ISSUES.md is updated
6. ✅ Re-scan confirms fixes (if applicable)

## Failure to Follow This Workflow

If you complete a task without running security scans:
- The task is considered INCOMPLETE
- You must go back and run the scans
- You must update SECURITY_ISSUES.md
- You must address any findings

## Quick Command Reference

```bash
# Get absolute path (ALWAYS run first)
pwd

# SAST scan (code changes)
snyk_code_scan(path = "<absolute-path>")

# SCA scan (dependency changes)
snyk_sca_scan(path = "<absolute-path>", all_projects = true)

# High/Critical only
snyk_code_scan(path = "<absolute-path>", severity_threshold = "high")

# Check Snyk version
snyk_version()

# Authenticate (if needed)
snyk_auth()
```

## Remember

🔒 **Security is not optional**
🔒 **Scans must run after EVERY code change**
🔒 **Documentation must be updated EVERY time**
🔒 **Critical/High issues must be addressed**

This workflow protects the codebase, users, and the project's reputation. Follow it religiously.
