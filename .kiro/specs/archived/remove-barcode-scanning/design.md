# Design Document

## Overview

This design outlines the approach for removing barcode scanning dependencies from the Shoppit Android application. Since the barcode scanning feature was never implemented in code, this is purely a dependency cleanup task that will:

1. Remove unused dependencies from build configuration
2. Eliminate security vulnerabilities from transitive dependencies
3. Reduce application size and build complexity
4. Update documentation to reflect the change

The removal is low-risk as no source code depends on these libraries.

## Architecture

### Current State

The project currently includes:

**Direct Dependencies:**
- `androidx.camera:camera-core:1.4.1`
- `androidx.camera:camera-camera2:1.4.1`
- `androidx.camera:camera-lifecycle:1.4.1`
- `androidx.camera:camera-view:1.4.1`
- `com.google.mlkit:barcode-scanning:17.3.0`

**Problematic Transitive Dependencies:**
- Netty (11 known vulnerabilities) - from CameraX and ML Kit
- Protobuf (1 known vulnerability) - from CameraX and ML Kit

**Impact:**
- No Kotlin/Java source files reference these libraries
- No UI components use camera or barcode scanning
- Dependencies only add security risk and build overhead

### Target State

After removal:
- All CameraX dependencies removed from build configuration
- ML Kit Barcode Scanning dependency removed
- Netty and Protobuf transitive dependencies eliminated
- Security vulnerabilities resolved
- Smaller APK size
- Faster build times

## Components and Interfaces

### Build Configuration Files

#### 1. `app/build.gradle.kts`

**Current Dependencies to Remove:**
```kotlin
// CameraX
implementation(libs.camerax.core)
implementation(libs.camerax.camera2)
implementation(libs.camerax.lifecycle)
implementation(libs.camerax.view)

// ML Kit Barcode Scanning
implementation(libs.mlkit.barcode.scanning)
```

**Action:** Delete these 5 dependency declarations

#### 2. `gradle/libs.versions.toml`

**Current Version References to Remove:**
```toml
camerax = "1.4.1"
barcode-scanning = "17.3.0"
```

**Current Library Definitions to Remove:**
```toml
# CameraX
camerax-core = { group = "androidx.camera", name = "camera-core", version.ref = "camerax" }
camerax-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "camerax" }
camerax-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "camerax" }
camerax-view = { group = "androidx.camera", name = "camera-view", version.ref = "camerax" }

# ML Kit Barcode Scanning
mlkit-barcode-scanning = { group = "com.google.mlkit", name = "barcode-scanning", version.ref = "barcode-scanning" }
```

**Action:** Delete version references and library definitions

### Documentation Files

#### 1. `SECURITY_ISSUES.md`

**Sections to Update:**

1. **Summary Section** - Update vulnerability counts
   - Remove Netty vulnerabilities (11)
   - Remove Protobuf vulnerability (1)
   - Update total count

2. **Accepted Risks Section** - Remove entries about:
   - CameraX/ML Kit vulnerabilities
   - Netty transitive dependencies
   - Protobuf transitive dependencies
   - SDK 35 migration plans

3. **Recent Scans Section** - Add new entry:
   - Date of dependency removal
   - Scan results after removal
   - Verification that vulnerabilities are resolved

4. **Scan History** - Add entry documenting the change

#### 2. Product Documentation

**Files to Review and Update:**
- `README.md` - Ensure no mention of barcode scanning
- `docs/README.md` - Remove any barcode scanning references
- `.kiro/steering/product.md` - Verify feature list accuracy

**Action:** Add note explaining barcode scanning was planned but not implemented, and dependencies have been removed

## Data Models

No data models are affected as the feature was never implemented.

## Error Handling

### Build Errors

**Potential Issue:** Missing dependency references if code exists that we haven't found

**Detection:**
- Gradle sync will fail with "Unresolved reference" errors
- Compilation will fail with import errors

**Resolution:**
- Review error messages to identify any missed code
- Search codebase for camera/barcode imports
- Remove or refactor any found code

**Likelihood:** Very low - grep search found no references

### Dependency Resolution Errors

**Potential Issue:** Other dependencies might transitively depend on CameraX or ML Kit

**Detection:**
- Gradle sync will fail with dependency resolution errors
- Build will report missing transitive dependencies

**Resolution:**
- Review dependency tree to identify source
- Evaluate if the dependent library is necessary
- Find alternative library or add back minimal dependencies

**Likelihood:** Very low - these are leaf dependencies

## Testing Strategy

### Pre-Removal Verification

1. **Codebase Search**
   - Search for `import androidx.camera`
   - Search for `import com.google.mlkit.vision.barcode`
   - Search for `CameraX`, `barcode`, `scanner` in Kotlin files
   - Verify no matches found

2. **Current Dependency Tree**
   - Run `./gradlew app:dependencies > dependencies_before.txt`
   - Document current Netty and Protobuf versions
   - Save for comparison

### Post-Removal Verification

1. **Build Verification**
   - Run `./gradlew clean`
   - Run `./gradlew build`
   - Verify successful compilation
   - Verify no warnings about missing dependencies

2. **Dependency Tree Verification**
   - Run `./gradlew app:dependencies > dependencies_after.txt`
   - Compare with before state
   - Verify CameraX libraries absent
   - Verify ML Kit Barcode Scanning absent
   - Verify Netty absent (or only from other sources)
   - Verify Protobuf absent (or only from other sources)

3. **Security Scan**
   - Run SCA scan: `snyk_sca_scan(path = "<absolute-path>", all_projects = true)`
   - Verify Netty vulnerabilities from CameraX/ML Kit are gone
   - Verify Protobuf vulnerability from CameraX/ML Kit is gone
   - Document remaining vulnerabilities (if any)

4. **Application Testing**
   - Install debug APK on device
   - Verify app launches successfully
   - Navigate through all screens
   - Verify no crashes or missing functionality
   - Confirm all existing features work

### Test Execution Order

1. Pre-removal verification (codebase search, dependency tree)
2. Remove dependencies from build files
3. Gradle sync
4. Build verification
5. Dependency tree comparison
6. Security scan
7. Application testing
8. Documentation updates

## Implementation Approach

### Phase 1: Preparation
1. Document current state (dependency tree, security scan results)
2. Create backup of build configuration files
3. Verify no code references exist

### Phase 2: Dependency Removal
1. Remove dependencies from `app/build.gradle.kts`
2. Remove version references from `gradle/libs.versions.toml`
3. Remove library definitions from `gradle/libs.versions.toml`
4. Sync Gradle project

### Phase 3: Verification
1. Clean and rebuild project
2. Generate new dependency tree
3. Compare dependency trees
4. Run security scan
5. Test application functionality

### Phase 4: Documentation
1. Update `SECURITY_ISSUES.md`
2. Update product documentation
3. Add decision record explaining removal
4. Update any affected guides or references

## Rollback Plan

If issues are discovered after removal:

1. **Immediate Rollback**
   - Restore `app/build.gradle.kts` from backup
   - Restore `gradle/libs.versions.toml` from backup
   - Sync Gradle project
   - Rebuild application

2. **Investigation**
   - Review error messages
   - Identify unexpected dependencies
   - Search for missed code references

3. **Alternative Approach**
   - If code exists, remove code first
   - If transitive dependency needed, find alternative
   - Document findings and update plan

## Security Considerations

### Vulnerability Resolution

**Current Vulnerabilities (from CameraX/ML Kit):**
- Netty: 11 vulnerabilities (various severities)
- Protobuf: 1 vulnerability

**Expected Outcome:**
- All CameraX/ML Kit-sourced vulnerabilities eliminated
- Significant reduction in security risk
- Cleaner security scan results

**Verification:**
- Run SCA scan before and after
- Compare vulnerability counts
- Confirm specific CVEs are resolved
- Document any remaining vulnerabilities from other sources

### Future Considerations

If barcode scanning is needed in the future:
1. Evaluate alternative libraries with better security posture
2. Consider web-based barcode scanning (camera API in browser)
3. Wait for CameraX updates that resolve transitive vulnerabilities
4. Plan for SDK 35 migration if CameraX is required

## Performance Impact

### Build Performance
- **Expected Improvement:** Faster Gradle sync and build times
- **Reason:** Fewer dependencies to resolve and compile
- **Measurement:** Compare build times before and after

### Application Performance
- **Expected Improvement:** Smaller APK size
- **Reason:** CameraX and ML Kit libraries removed
- **Measurement:** Compare APK sizes before and after
- **Estimate:** 5-10 MB reduction in APK size

### Runtime Performance
- **Expected Impact:** None (feature was never used)
- **Verification:** Test app performance on device

## Documentation Updates

### Files to Update

1. **SECURITY_ISSUES.md**
   - Remove CameraX/ML Kit vulnerability entries
   - Update summary counts
   - Add removal decision record
   - Update scan history

2. **README.md**
   - Verify no barcode scanning mentioned
   - Add note if needed

3. **Product Documentation**
   - Remove any barcode scanning references
   - Update feature lists
   - Add decision rationale

4. **Dependency Documentation**
   - Update dependency lists
   - Remove CameraX/ML Kit references

### Decision Record Template

```markdown
## Barcode Scanning Feature Removal

**Date:** [Date of removal]
**Decision:** Remove barcode scanning dependencies (CameraX and ML Kit)

**Context:**
- Barcode scanning dependencies were added but never implemented
- Dependencies introduce 12 security vulnerabilities via transitive dependencies
- No code in the project uses these libraries
- Feature is not part of current product roadmap

**Decision:**
- Remove all CameraX dependencies
- Remove ML Kit Barcode Scanning dependency
- Update security documentation
- Document decision for future reference

**Consequences:**
- Positive: Eliminates 12 security vulnerabilities
- Positive: Reduces APK size by ~5-10 MB
- Positive: Faster build times
- Positive: Simpler dependency management
- Neutral: Feature was never available, so no functionality lost
- Future: If barcode scanning needed, evaluate alternatives with better security

**Verification:**
- Security scan shows vulnerabilities resolved
- Application builds and runs successfully
- All existing features continue to work
```

## Success Criteria

The removal is considered successful when:

1. ✅ All CameraX and ML Kit dependencies removed from build files
2. ✅ Project builds successfully without errors
3. ✅ Dependency tree shows no CameraX or ML Kit libraries
4. ✅ Security scan shows Netty/Protobuf vulnerabilities from CameraX/ML Kit resolved
5. ✅ Application installs and runs without issues
6. ✅ All existing features function correctly
7. ✅ Documentation updated to reflect changes
8. ✅ APK size reduced
9. ✅ Build times improved (measurable)

## Timeline Estimate

- **Preparation:** 15 minutes
- **Dependency Removal:** 10 minutes
- **Verification:** 30 minutes
- **Documentation:** 20 minutes
- **Total:** ~75 minutes (1.25 hours)

This is a low-risk, straightforward task with clear success criteria and rollback plan.
