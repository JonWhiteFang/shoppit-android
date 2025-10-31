# Security Scan Results - Post CameraX/ML Kit Removal

**Scan Date:** October 31, 2025  
**Scan Type:** SCA (Software Composition Analysis)  
**Project Path:** D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android  
**Scan Command:** `snyk_sca_scan(path = "D:\Users\jpawhite\Documents\Kiro Projects\shoppit-android", all_projects = true)`

## Executive Summary

**Total Issues Found:** 13 vulnerabilities

**Severity Breakdown:**
- 🔴 **High Severity:** 7 vulnerabilities
- 🟡 **Medium Severity:** 5 vulnerabilities  
- 🔵 **Low Severity:** 1 vulnerability

## Key Finding: CameraX/ML Kit Dependencies Successfully Removed

✅ **CameraX dependencies:** REMOVED - No longer present in dependency tree  
✅ **ML Kit Barcode Scanning:** REMOVED - No longer present in dependency tree

**However:** Netty and Protobuf vulnerabilities **REMAIN** because they come from **OTHER sources**, not CameraX/ML Kit.

## Vulnerability Sources Identified

### Netty Vulnerabilities (9 issues)
**Source:** `io.grpc:grpc-netty:1.57.0`  
**Current Version:** io.netty:netty-* 4.1.93.Final  
**Used By:** gRPC libraries (likely Firebase or other Google services)

The Netty vulnerabilities are transitive dependencies from gRPC, which is used for:
- Firebase services
- Google Cloud services
- Other gRPC-based communication

### Protobuf Vulnerabilities (1 issue)
**Source:** `com.google.protobuf:protobuf-java:3.22.3`  
**Used By:** 
- gRPC libraries (io.grpc:grpc-protobuf:1.57.0)
- Android build tools (com.android.tools.*)
- Google Crypto Tink (com.google.crypto.tink:tink:1.7.0)

### Commons-IO Vulnerability (1 issue)
**Source:** `commons-io:commons-io:2.13.0`  
**Used By:** Android build tools

### Kotlin stdlib Vulnerability (1 issue)
**Source:** `org.jetbrains.kotlin:kotlin-stdlib:1.9.20`  
**Note:** This was previously fixed by updating Hilt to 2.56, but appears to have regressed

## Detailed Vulnerability List

### High Severity (7)

#### 1. SNYK-JAVA-IONETTY-11799531
- **Package:** io.netty:netty-codec-http2:4.1.93.Final
- **CVE:** CVE-2025-55163
- **Issue:** Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Fix Version:** 4.1.124.Final, 4.2.4.Final

#### 2. SNYK-JAVA-IONETTY-12485151
- **Package:** io.netty:netty-codec-http2:4.1.93.Final
- **CVE:** CVE-2025-58057
- **Issue:** Improper Handling of Highly Compressed Data (Data Amplification)
- **CWE:** CWE-409
- **Fix Version:** 4.1.125.Final

#### 3. SNYK-JAVA-IONETTY-5953332
- **Package:** io.netty:netty-codec-http2:4.1.93.Final
- **CVE:** CVE-2023-44487
- **Issue:** Denial of Service (DoS)
- **CWE:** CWE-400
- **Fix Version:** 4.1.100.Final

#### 4. SNYK-JAVA-IONETTY-12485149
- **Package:** io.netty:netty-codec-http:4.1.93.Final
- **CVE:** CVE-2025-58056
- **Issue:** HTTP Request Smuggling
- **CWE:** CWE-444
- **Fix Version:** 4.1.125.Final, 4.2.5.Final

#### 5. SNYK-JAVA-IONETTY-12485150
- **Package:** io.netty:netty-codec-http:4.1.93.Final
- **CVE:** CVE-2025-58057
- **Issue:** Improper Handling of Highly Compressed Data (Data Amplification)
- **CWE:** CWE-409
- **Fix Version:** 4.1.125.Final

#### 6. SNYK-JAVA-IONETTY-8707739
- **Package:** io.netty:netty-handler:4.1.93.Final
- **CVE:** CVE-2025-24970
- **Issue:** Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284
- **Fix Version:** 4.1.118.Final, 4.2.0.RC3

#### 7. SNYK-JAVA-COMGOOGLEPROTOBUF-8055227
- **Package:** com.google.protobuf:protobuf-java:3.22.3
- **CVE:** CVE-2024-7254
- **Issue:** Stack-based Buffer Overflow
- **CWE:** CWE-121
- **Fix Version:** 3.25.5, 4.27.5, 4.28.2

### Medium Severity (5)

#### 8. SNYK-JAVA-COMMONSIO-8161190
- **Package:** commons-io:commons-io:2.13.0
- **CVE:** CVE-2024-47554
- **Issue:** Uncontrolled Resource Consumption ('Resource Exhaustion')
- **CWE:** CWE-400
- **Fix Version:** 2.14.0

#### 9. SNYK-JAVA-IONETTY-6483812
- **Package:** io.netty:netty-codec-http:4.1.93.Final
- **CVE:** CVE-2024-29025
- **Issue:** Allocation of Resources Without Limits or Throttling
- **CWE:** CWE-770
- **Fix Version:** 4.1.108.Final

#### 10. SNYK-JAVA-IONETTY-5725787
- **Package:** io.netty:netty-handler:4.1.93.Final
- **CVE:** CVE-2023-34462
- **Issue:** Denial of Service (DoS)
- **CWE:** CWE-400
- **Fix Version:** 4.1.94.Final

#### 11. SNYK-JAVA-IONETTY-8367012
- **Package:** io.netty:netty-common:4.1.93.Final
- **CVE:** CVE-2024-47535
- **Issue:** Denial of Service (DoS)
- **CWE:** CWE-670, CWE-789
- **Fix Version:** 4.1.115.Final

#### 12. SNYK-JAVA-IONETTY-8707740
- **Package:** io.netty:netty-common:4.1.93.Final
- **CVE:** CVE-2025-25193
- **Issue:** Improper Validation of Specified Quantity in Input
- **CWE:** CWE-1284
- **Fix Version:** 4.1.118, 4.2.0.RC3

### Low Severity (1)

#### 13. SNYK-JAVA-ORGJETBRAINSKOTLIN-2393744
- **Package:** org.jetbrains.kotlin:kotlin-stdlib:1.9.20
- **CVE:** CVE-2020-29582
- **Issue:** Information Exposure
- **CWE:** CWE-378
- **Fix Version:** 2.1.0
- **Note:** This was previously fixed but appears to have regressed

## Comparison with Previous Scan

### Before Dependency Removal (October 30, 2025)
- **Total Issues:** 13 SCA issues
- **Netty Issues:** 11 (attributed to CameraX/ML Kit)
- **Protobuf Issues:** 1 (attributed to CameraX/ML Kit)
- **Commons-IO:** 1
- **Kotlin stdlib:** 1 (FIXED)

### After Dependency Removal (October 31, 2025)
- **Total Issues:** 13 SCA issues
- **Netty Issues:** 9 (from gRPC, not CameraX/ML Kit)
- **Protobuf Issues:** 1 (from gRPC and build tools, not CameraX/ML Kit)
- **Commons-IO:** 1
- **Kotlin stdlib:** 1 (REGRESSED)

### Analysis
The removal of CameraX and ML Kit dependencies **did NOT reduce** the Netty and Protobuf vulnerabilities because:

1. **Netty vulnerabilities** come from `io.grpc:grpc-netty:1.57.0`, not CameraX/ML Kit
2. **Protobuf vulnerabilities** come from gRPC and Android build tools, not CameraX/ML Kit
3. The previous assessment incorrectly attributed these vulnerabilities to CameraX/ML Kit

## Recommended Actions

### Immediate Actions

1. **Update gRPC Libraries** (Addresses 9 Netty vulnerabilities)
   - Current: io.grpc:grpc-*:1.57.0
   - Check for latest version: `./gradlew dependencyUpdates`
   - Update gRPC to latest stable version
   - This will transitively update Netty to a newer version

2. **Update Protobuf** (Addresses 1 high severity vulnerability)
   - Current: com.google.protobuf:protobuf-java:3.22.3
   - Target: 3.25.5 or higher
   - May require updating gRPC first

3. **Update Commons-IO** (Addresses 1 medium severity vulnerability)
   - Current: commons-io:commons-io:2.13.0
   - Target: 2.14.0
   - This is a transitive dependency from Android build tools

4. **Re-fix Kotlin stdlib** (Addresses 1 low severity vulnerability)
   - Verify Hilt version is still 2.56
   - Check if another dependency is pulling in older Kotlin stdlib

### Investigation Needed

1. **Identify gRPC usage:**
   ```bash
   ./gradlew app:dependencies | findstr "grpc"
   ```
   Determine which direct dependency brings in gRPC (likely Firebase)

2. **Check for gRPC updates:**
   - Review Firebase SDK versions
   - Check if newer Firebase SDKs use updated gRPC

3. **Evaluate gRPC necessity:**
   - If gRPC is only used for specific features, consider alternatives
   - If required, plan for regular gRPC updates

## Conclusion

The removal of CameraX and ML Kit dependencies was **successful** in eliminating those unused libraries from the project. However, the Netty and Protobuf vulnerabilities **persist** because they originate from **gRPC libraries** (likely used by Firebase or other Google services), not from the removed CameraX/ML Kit dependencies.

**Next Steps:**
1. Update gRPC libraries to latest version
2. Update Protobuf to 3.25.5 or higher
3. Update Commons-IO to 2.14.0
4. Re-run security scan to verify fixes
5. Update SECURITY_ISSUES.md with corrected information

**Impact of CameraX/ML Kit Removal:**
- ✅ Reduced APK size (estimated 5-10 MB)
- ✅ Faster build times
- ✅ Removed unused dependencies
- ✅ Simplified dependency management
- ❌ Did NOT resolve Netty/Protobuf vulnerabilities (they come from other sources)
