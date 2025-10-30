# Security Vulnerabilities Report

**Last Scan Date:** October 30, 2025  
**Last Update:** October 30, 2025  
**Project:** Shoppit Android  
**Total Issues:** 14 vulnerabilities (mitigation in progress)

## Summary

- ✅ **Code Security (SAST):** No issues found
- ⚠️ **Dependency Vulnerabilities (SCA):** 14 issues found
  - 🔴 High Severity: 7
  - 🟡 Medium Severity: 5
  - 🟢 Low Severity: 2

---

## High Severity Issues (7)

### 1. io.netty:netty-codec-http2 - Multiple Vulnerabilities

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.125.Final or 4.2.5.Final

#### CVE-2025-55163 - Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Learn More:** https://learn.snyk.io/lesson/no-rate-limiting/?loc=ide

#### CVE-2025-58057 - Improper Handling of Highly Compressed Data
- **CWE:** CWE-409
- **Fix Version:** 4.1.125.Final

#### CVE-2023-44487 - Denial of Service (DoS)
- **CWE:** CWE-400
- **Fix Version:** 4.1.100.Final
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

### 2. io.netty:netty-codec-http - Multiple Vulnerabilities

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.125.Final or 4.2.5.Final

#### CVE-2025-58056 - HTTP Request Smuggling
- **CWE:** CWE-444
- **Fix Version:** 4.1.125.Final or 4.2.5.Final

#### CVE-2025-58057 - Improper Handling of Highly Compressed Data
- **CWE:** CWE-409
- **Fix Version:** 4.1.125.Final

---

### 3. io.netty:netty-handler - Improper Validation

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.118.Final or 4.2.0.RC3

#### CVE-2025-24970 - Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284

---

### 4. com.google.protobuf:protobuf-java - Stack-based Buffer Overflow

**Current Version:** 3.22.3  
**Fix Version:** 3.25.5, 4.27.5, or 4.28.2

#### CVE-2024-7254 - Stack-based Buffer Overflow
- **CWE:** CWE-121
- **Snyk ID:** SNYK-JAVA-COMGOOGLEPROTOBUF-8055227

---

## Medium Severity Issues (5)

### 5. io.netty:netty-codec-http - Resource Allocation

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.108.Final

#### CVE-2024-29025 - Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Learn More:** https://learn.snyk.io/lesson/no-rate-limiting/?loc=ide

---

### 6. io.netty:netty-handler - Denial of Service

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.94.Final

#### CVE-2023-34462 - Denial of Service (DoS)
- **CWE:** CWE-400
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

### 7. io.netty:netty-common - Multiple Issues

**Current Version:** 4.1.93.Final  
**Fix Version:** 4.1.118.Final

#### CVE-2024-47535 - Denial of Service (DoS)
- **CWE:** CWE-670, CWE-789
- **Fix Version:** 4.1.115.Final

#### CVE-2025-25193 - Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284
- **Fix Version:** 4.1.118 or 4.2.0.RC3

---

### 8. commons-io:commons-io - Resource Exhaustion

**Current Version:** 2.13.0  
**Fix Version:** 2.14.0

#### CVE-2024-47554 - Uncontrolled Resource Consumption
- **CWE:** CWE-400
- **Snyk ID:** SNYK-JAVA-COMMONSIO-8161190
- **Learn More:** https://learn.snyk.io/lesson/unrestricted-resource-consumption/?loc=ide

---

## Low Severity Issues (2)

### 9. com.google.guava:guava - Insecure Temp Files

**Current Version:** 31.0.1-jre (transitive via Hilt)  
**Fix Version:** 32.0.0-android or 32.0.0-jre

#### CVE-2023-2976 - Creation of Temporary File in Directory with Insecure Permissions
- **CWE:** CWE-379
- **Snyk ID:** SNYK-JAVA-COMGOOGLEGUAVA-5710356
- **Remediation:** Upgrade com.google.dagger:hilt-compiler to 2.51

---

### 10. org.jetbrains.kotlin:kotlin-stdlib - Information Exposure

**Current Version:** 2.0.21  
**Fix Version:** 2.1.0

#### CVE-2020-29582 - Information Exposure
- **CWE:** CWE-378
- **Snyk ID:** SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744
- **Remediation:** Upgrade to org.jetbrains.kotlin:kotlin-stdlib@2.1.0

---

## Actions Taken (October 30, 2025)

### ✅ Completed Updates

1. **Updated Kotlin** (2.0.21 → 2.1.0)
   - Fixes CVE-2020-29582 (Information Exposure)
   - Status: ✅ Complete

2. **Updated Hilt** (2.48 → 2.52)
   - Transitively updates Guava to fix CVE-2023-2976
   - Status: ✅ Complete

3. **Updated KSP** (2.0.21-1.0.28 → 2.1.0-1.0.29)
   - Required for Kotlin 2.1.0 compatibility
   - Status: ✅ Complete

4. **Updated CameraX** (1.3.0 → 1.4.1)
   - May reduce transitive Netty/Protobuf vulnerabilities
   - Status: ✅ Complete

5. **Updated ML Kit Barcode Scanning** (17.2.0 → 17.3.0)
   - May reduce transitive vulnerabilities
   - Status: ✅ Complete

### 🔄 Next Steps Required

**Note:** Full verification scan blocked by JAVA_HOME environment issue with Snyk CLI.
Manual verification needed after resolving environment configuration.

## Recommended Actions

### Immediate (High Priority)

1. **Update Netty dependencies** (9 vulnerabilities)
   - These are likely transitive dependencies from gRPC, OkHttp, or other networking libraries
   - Check which direct dependencies bring in Netty
   - Update those direct dependencies to versions that use Netty 4.1.125.Final+

2. **Update Protobuf** (1 high severity)
   - Current: 3.22.3
   - Target: 3.25.5+ or 4.27.5+
   - Check if this is from gRPC or another dependency

### Short-term (Medium Priority)

3. **Update Commons-IO** (1 medium severity)
   - Current: 2.13.0
   - Target: 2.14.0
   - Check which dependency brings this in

### Low Priority

4. **Update Hilt** (fixes Guava)
   - Current Hilt: Check version
   - Target: 2.51+
   - This will update Guava transitively

5. **Update Kotlin**
   - Current: 2.0.21
   - Target: 2.1.0
   - Update in `gradle/libs.versions.toml`

---

## Investigation Steps

1. **Identify transitive dependencies:**
   ```bash
   ./gradlew app:dependencies > dependencies.txt
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

## Notes

- Most issues are in **transitive dependencies** (Netty, Protobuf, Commons-IO)
- These are likely brought in by:
  - OkHttp/Retrofit (networking)
  - gRPC (if used)
  - Firebase (if used)
  - Other Google libraries

- **No code-level security issues** were found in the Kotlin source code ✅

---

## Next Steps

- [ ] Run `./gradlew app:dependencies` to identify which direct dependencies bring in vulnerable packages
- [ ] Check for updates to direct dependencies (Hilt, Retrofit, OkHttp, etc.)
- [ ] Update `gradle/libs.versions.toml` with new versions
- [ ] Test thoroughly after updates
- [ ] Re-run security scan to verify fixes

---

**Scan Command Used:**
```bash
snyk code test app/src
snyk test --all-projects
```

**Re-scan After Fixes:**
```bash
snyk code test app/src
snyk test --all-projects
```
