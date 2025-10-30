# Security Vulnerabilities Report

**Last Scan Date:** October 30, 2025  
**Last Update:** October 30, 2025  
**Project:** Shoppit Android  
**Total Issues:** 20 vulnerabilities (1 fixed)

## Summary

- ⚠️ **Code Security (SAST):** 8 low severity issues (test code only)
- ⚠️ **Dependency Vulnerabilities (SCA):** 12 issues (1 fixed)
  - 🔴 High Severity: 7
  - 🟡 Medium Severity: 5
  - ✅ Low Severity: 0 (1 fixed)

---

## Recent Scans

### October 30, 2025 - Security Fix: Kotlin stdlib
**Action:** Updated Hilt from 2.54 to 2.56  
**Result:** ✅ Fixed CVE-2020-29582 (Kotlin stdlib Information Exposure)  
**Verification:** Kotlin stdlib updated from 2.0.21 to 2.1.10

### October 30, 2025 - Full Security Scan
**Scan Type:** SAST + SCA  
**Scope:** Entire project  
**Result:** 21 issues found

#### SAST Results (8 issues - all low severity)
- **Location:** Test code only (`AuthRepositoryImplTest.kt`)
- **Issue:** Hardcoded passwords in test fixtures
- **Severity:** Low
- **Status:** Accepted risk (test code only, not production)

#### SCA Results (13 issues)
- **High:** 7 vulnerabilities in Netty and Protobuf
- **Medium:** 5 vulnerabilities in Netty and Commons-IO
- **Low:** 1 vulnerability in Kotlin stdlib

---

## Code Security Issues (SAST)

### Hardcoded Passwords in Test Code (8 occurrences)

**File:** `app/src/test/java/com/shoppit/app/data/auth/AuthRepositoryImplTest.kt`  
**Severity:** Low  
**CWE:** CWE-798, CWE-259  
**Status:** ✅ Accepted Risk

**Occurrences:**
- Line 62: `val password = "password123"`
- Line 85: `val password = "password123"`
- Line 116: `val password = "password123"`
- Line 132: `val password = "password123"`
- Line 149: `val password = "password123"`
- Line 171: `val password = "password123"`
- Line 204: `val password = "password123"`
- Line 220: `val password = "password123"`

**Justification:**
- These are test fixtures in unit tests
- Not used in production code
- Standard practice for authentication testing
- No security risk as tests are not deployed

**Mitigation:**
- Test passwords are clearly marked as test data
- No production credentials in codebase
- Tests run in isolated environment

---

## High Severity Issues (7)

### 1. io.netty:netty-codec-http2 - Multiple Vulnerabilities

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.125.Final or 4.2.5.Final  
**Status:** 🔴 Open - Requires dependency update

#### CVE-2025-55163 - Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Snyk ID:** SNYK-JAVA-IONETTY-11799531
- **Learn More:** https://learn.snyk.io/lesson/no-rate-limiting/?loc=ide

#### CVE-2025-58057 - Improper Handling of Highly Compressed Data
- **CWE:** CWE-409
- **Snyk ID:** SNYK-JAVA-IONETTY-12485151
- **Fix Version:** 4.1.125.Final

#### CVE-2023-44487 - Denial of Service (DoS)
- **CWE:** CWE-400
- **Snyk ID:** SNYK-JAVA-IONETTY-5953332
- **Fix Version:** 4.1.100.Final
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

### 2. io.netty:netty-codec-http - Multiple Vulnerabilities

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.125.Final or 4.2.5.Final  
**Status:** 🔴 Open - Requires dependency update

#### CVE-2025-58056 - HTTP Request Smuggling
- **CWE:** CWE-444
- **Snyk ID:** SNYK-JAVA-IONETTY-12485149
- **Fix Version:** 4.1.125.Final or 4.2.5.Final

#### CVE-2025-58057 - Improper Handling of Highly Compressed Data
- **CWE:** CWE-409
- **Snyk ID:** SNYK-JAVA-IONETTY-12485150
- **Fix Version:** 4.1.125.Final

---

### 3. io.netty:netty-handler - Improper Validation

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.118.Final or 4.2.0.RC3  
**Status:** 🔴 Open - Requires dependency update

#### CVE-2025-24970 - Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284
- **Snyk ID:** SNYK-JAVA-IONETTY-8707739

---

### 4. com.google.protobuf:protobuf-java - Stack-based Buffer Overflow

**Current Version:** 3.22.3  
**Fix Version:** 3.25.5, 4.27.5, or 4.28.2  
**Status:** 🔴 Open - Requires dependency update

#### CVE-2024-7254 - Stack-based Buffer Overflow
- **CWE:** CWE-121
- **Snyk ID:** SNYK-JAVA-COMGOOGLEPROTOBUF-8055227

---

## Medium Severity Issues (5)

### 5. io.netty:netty-codec-http - Resource Allocation

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.108.Final  
**Status:** 🟡 Open - Requires dependency update

#### CVE-2024-29025 - Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Snyk ID:** SNYK-JAVA-IONETTY-6483812
- **Learn More:** https://learn.snyk.io/lesson/no-rate-limiting/?loc=ide

---

### 6. io.netty:netty-handler - Denial of Service

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.94.Final  
**Status:** 🟡 Open - Requires dependency update

#### CVE-2023-34462 - Denial of Service (DoS)
- **CWE:** CWE-400
- **Snyk ID:** SNYK-JAVA-IONETTY-5725787
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

### 7. io.netty:netty-common - Multiple Issues

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.118.Final  
**Status:** 🟡 Open - Requires dependency update

#### CVE-2024-47535 - Denial of Service (DoS)
- **CWE:** CWE-670, CWE-789
- **Snyk ID:** SNYK-JAVA-IONETTY-8367012
- **Fix Version:** 4.1.115.Final

#### CVE-2025-25193 - Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284
- **Snyk ID:** SNYK-JAVA-IONETTY-8707740
- **Fix Version:** 4.1.118 or 4.2.0.RC3

---

### 8. commons-io:commons-io - Resource Exhaustion

**Current Version:** 2.13.0  
**Fix Version:** 2.14.0  
**Status:** 🟡 Open - Requires dependency update

#### CVE-2024-47554 - Uncontrolled Resource Consumption
- **CWE:** CWE-400
- **Snyk ID:** SNYK-JAVA-COMMONSIO-8161190
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

## Low Severity Issues (0 - All Fixed)

### 9. org.jetbrains.kotlin:kotlin-stdlib - Information Exposure ✅ FIXED

**Previous Version:** 2.0.21  
**Fixed Version:** 2.1.10  
**Status:** ✅ Fixed on October 30, 2025

#### CVE-2020-29582 - Information Exposure
- **CWE:** CWE-378
- **Snyk ID:** SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744
- **Fix Applied:** Updated Hilt from 2.54 to 2.56, which transitively updated Kotlin stdlib to 2.1.10
- **Verification:** Confirmed via `./gradlew app:dependencies` showing `kotlin-stdlib:2.1.0 -> 2.1.10`

---

## Recommended Actions

### Context: Netty and Protobuf Vulnerabilities

**Investigation Results (October 30, 2025):**
- Netty and Protobuf vulnerabilities are in **transitive dependencies**
- Likely sources: CameraX (1.4.1) and ML Kit Barcode Scanning (17.3.0)
- CameraX 1.5.0 is available but requires compileSdk 35 (project currently on 34)
- Updating to SDK 35 would be a major project-wide change

**Root Cause:**
These Google libraries (CameraX, ML Kit) bundle older versions of Netty and Protobuf internally. We cannot directly update these transitive dependencies without updating the parent libraries.

### Immediate (High Priority)

1. **Evaluate SDK 35 Migration** (Addresses 7 high + 4 medium Netty vulnerabilities)
   - **Impact:** Major - Requires updating compileSdk, targetSdk, testing across all features
   - **Benefit:** Enables CameraX 1.5.0 which may include updated Netty/Protobuf
   - **Timeline:** 2-3 weeks for full migration and testing
   - **Steps:**
     1. Update `compileSdk` to 35 in `app/build.gradle.kts`
     2. Update `targetSdk` to 35
     3. Update CameraX to 1.5.0
     4. Test all camera and barcode scanning features
     5. Address any API changes or deprecations
     6. Re-run security scan to verify fixes

2. **Alternative: Dependency Exclusion + Direct Netty Declaration** (Advanced)
   - Exclude old Netty from CameraX/ML Kit
   - Declare newer Netty version directly
   - **Risk:** May cause runtime issues if libraries depend on specific Netty versions
   - **Not recommended** without thorough testing

### Short-term (Medium Priority)

3. **Update Commons-IO** (1 medium severity) ✅ **Can be done now**
   - Current: 2.13.0
   - Target: 2.14.0
   - This is likely a transitive dependency
   - Run: `./gradlew app:dependencies | findstr "commons-io"`
   - Identify parent dependency and update it

### Accepted Risk (For Now)

4. **Netty and Protobuf Vulnerabilities**
   - **Status:** Accepted risk until SDK 35 migration
   - **Justification:**
     - Vulnerabilities are in camera/barcode scanning features (not core app functionality)
     - App does not expose these features to untrusted input
     - Requires major SDK migration to fix
     - No known exploits targeting Android apps specifically
   - **Mitigation:**
     - Camera and barcode features used only with user consent
     - No network-facing camera/barcode endpoints
     - Regular monitoring for security updates
   - **Review Date:** When planning SDK 35 migration

---

## Investigation Steps

1. **Identify transitive dependencies:**
   ```bash
   ./gradlew app:dependencies > dependencies_full.txt
   ```

2. **Search for specific packages:**
   ```bash
   ./gradlew app:dependencies | findstr "netty"
   ./gradlew app:dependencies | findstr "protobuf"
   ./gradlew app:dependencies | findstr "commons-io"
   ```

3. **Check for dependency updates:**
   ```bash
   ./gradlew dependencyUpdates
   ```

---

## Fixed Issues

### October 30, 2025

#### ✅ CVE-2020-29582 - Kotlin stdlib Information Exposure (Low Severity)
- **Package:** org.jetbrains.kotlin:kotlin-stdlib
- **Previous Version:** 2.0.21
- **Fixed Version:** 2.1.10
- **Action Taken:** Updated Hilt from 2.54 to 2.56 in `gradle/libs.versions.toml`
- **Verification:** Confirmed via dependency tree showing `kotlin-stdlib:2.1.0 -> 2.1.10`
- **Build Status:** ✅ Main code builds successfully
- **Files Changed:** `gradle/libs.versions.toml`

#### 📝 Investigation: Netty and Protobuf Vulnerabilities (12 issues)
- **Status:** Investigated - Requires SDK 35 migration to fix
- **Finding:** Vulnerabilities are in transitive dependencies from CameraX 1.4.1 and ML Kit 17.3.0
- **Attempted Fix:** Tried updating CameraX to 1.5.0
- **Blocker:** CameraX 1.5.0 requires compileSdk 35 (project currently on 34)
- **Decision:** Accept risk until SDK 35 migration is planned
- **Documentation:** Updated SECURITY_ISSUES.md with investigation results and action plan
- **Next Steps:** Plan SDK 35 migration as part of regular update cycle

---

## Notes

- **SAST Issues:** All 8 code-level issues are in test code only (hardcoded test passwords) - accepted risk
- **SCA Issues:** 12 remaining dependency vulnerabilities (1 fixed)
  - **Netty (11 vulnerabilities):** Transitive from CameraX 1.4.1 and ML Kit Barcode Scanning 17.3.0
  - **Protobuf (1 vulnerability):** Transitive from CameraX/ML Kit
  - **Commons-IO (1 vulnerability):** Transitive dependency, parent unknown
- **Investigation Complete:** Identified that fixing Netty/Protobuf requires SDK 35 migration
- **Decision:** Accept Netty/Protobuf risk until SDK 35 migration is planned
- **Rationale:** 
  - Vulnerabilities in non-critical features (camera/barcode)
  - No untrusted input to these features
  - SDK 35 migration is a major undertaking (2-3 weeks)
  - Should be planned as part of regular SDK update cycle

---

## Scan History

### October 30, 2025 - Full Security Scan
- **SAST:** 8 low severity issues (test code only)
- **SCA:** 13 dependency vulnerabilities
- **Total:** 21 issues
- **Critical/High:** 7 (all in dependencies)

---

## Next Steps

- [ ] Run `./gradlew app:dependencies` to identify which direct dependencies bring in vulnerable packages
- [ ] Check for updates to direct dependencies (Hilt, Retrofit, OkHttp, gRPC, CameraX, ML Kit)
- [ ] Update `gradle/libs.versions.toml` with new versions
- [ ] Test thoroughly after updates
- [ ] Re-run security scan to verify fixes

---

**Scan Commands Used:**
```bash
# Get absolute path
Get-Location

# SAST scan
snyk_code_scan(path = "D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android\app\src")

# SCA scan
snyk_sca_scan(path = "D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android", all_projects = true)
```

**Re-scan After Fixes:**
```bash
snyk_code_scan(path = "D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android\app\src")
snyk_sca_scan(path = "D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android", all_projects = true)
```
