# Implementation Plan

- [x] 1. Prepare for dependency removal
  - Document current dependency state and create backups
  - _Requirements: 1.1, 3.3_

- [x] 1.1 Generate current dependency tree
  - Run `./gradlew app:dependencies` and save output to `dependencies_before.txt`
  - Document current Netty and Protobuf versions present
  - _Requirements: 3.3_

- [x] 1.2 Create backup of build configuration files
  - Copy `app/build.gradle.kts` to `app/build.gradle.kts.backup`
  - Copy `gradle/libs.versions.toml` to `gradle/libs.versions.toml.backup`
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 1.3 Verify no code references exist
  - Search codebase for `import androidx.camera` patterns
  - Search codebase for `import com.google.mlkit.vision.barcode` patterns
  - Search for `CameraX`, `barcode`, `scanner` keywords in Kotlin files
  - Document findings (should be zero matches)
  - _Requirements: 3.2_

- [x] 2. Remove dependencies from build configuration
  - Remove CameraX and ML Kit dependencies from Gradle files
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 2.1 Remove dependencies from app/build.gradle.kts
  - Remove `implementation(libs.camerax.core)` line
  - Remove `implementation(libs.camerax.camera2)` line
  - Remove `implementation(libs.camerax.lifecycle)` line
  - Remove `implementation(libs.camerax.view)` line
  - Remove `implementation(libs.mlkit.barcode.scanning)` line
  - _Requirements: 1.1, 1.2_

- [x] 2.2 Remove version references from gradle/libs.versions.toml
  - Remove `camerax = "1.4.1"` line from versions section
  - Remove `barcode-scanning = "17.3.0"` line from versions section
  - _Requirements: 1.3, 1.4_

- [x] 2.3 Remove library definitions from gradle/libs.versions.toml
  - Remove `camerax-core` library definition
  - Remove `camerax-camera2` library definition
  - Remove `camerax-lifecycle` library definition
  - Remove `camerax-view` library definition
  - Remove `mlkit-barcode-scanning` library definition
  - Remove any associated comments (e.g., "# CameraX", "# ML Kit Barcode Scanning")
  - _Requirements: 1.5, 1.6_

- [x] 2.4 Sync Gradle project
  - Run Gradle sync in IDE or execute `./gradlew --refresh-dependencies`
  - Verify sync completes without errors
  - _Requirements: 3.1_

- [x] 3. Verify build and dependencies
  - Confirm project builds successfully and dependencies are removed
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3.1 Clean and rebuild project
  - Run `./gradlew clean`
  - Run `./gradlew build`
  - Verify build completes successfully without errors
  - Document any warnings or issues
  - _Requirements: 3.1, 3.2_

- [x] 3.2 Generate new dependency tree
  - Run `./gradlew app:dependencies` and save output to `dependencies_after.txt`
  - Compare with `dependencies_before.txt`
  - _Requirements: 3.3_

- [x] 3.3 Verify dependencies removed
  - Confirm CameraX libraries are not in dependency tree
  - Confirm ML Kit Barcode Scanning is not in dependency tree
  - Confirm Netty is not present (or only from other sources, not CameraX/ML Kit)
  - Confirm Protobuf is not present (or only from other sources, not CameraX/ML Kit)
  - Document findings
  - _Requirements: 3.3, 3.4_

- [x] 4. Run security scan
  - Execute security scan and verify vulnerabilities are resolved
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 4.1 Get absolute project path
  - Run `pwd` or `Get-Location` to get absolute path
  - Document path for scan command
  - _Requirements: 4.1_

- [x] 4.2 Execute SCA security scan
  - Run `snyk_sca_scan(path = "<absolute-path>", all_projects = true)`
  - Save scan results
  - _Requirements: 4.1_

- [x] 4.3 Review scan results
  - Check for Netty vulnerabilities from CameraX/ML Kit (should be absent)
  - Check for Protobuf vulnerabilities from CameraX/ML Kit (should be absent)
  - Document any remaining vulnerabilities from other sources
  - Compare with previous scan results
  - _Requirements: 4.2, 4.3_

- [x] 4.4 Document scan results
  - Note date of scan
  - Note number of vulnerabilities resolved
  - Note any remaining vulnerabilities
  - _Requirements: 4.4_

- [x] 5. Update security documentation
  - Update SECURITY_ISSUES.md to reflect dependency removal
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 4.4_

- [x] 5.1 Update summary section
  - Remove Netty vulnerability count (11 vulnerabilities)
  - Remove Protobuf vulnerability count (1 vulnerability)
  - Update total vulnerability count
  - Update last scan date
  - _Requirements: 2.1_

- [x] 5.2 Remove accepted risks section entries
  - Remove entries about CameraX/ML Kit vulnerabilities
  - Remove entries about Netty transitive dependencies
  - Remove entries about Protobuf transitive dependencies
  - Remove entries about SDK 35 migration plans related to CameraX
  - _Requirements: 2.3_

- [x] 5.3 Add dependency removal decision record
  - Add new section documenting the removal decision
  - Include date of removal
  - Include rationale (unused dependencies, security vulnerabilities)
  - Include verification results (scan results, build success)
  - _Requirements: 2.2, 2.4_

- [x] 5.4 Update scan history
  - Add entry for post-removal security scan
  - Document vulnerabilities resolved
  - Include scan date and results summary
  - _Requirements: 2.4, 4.4_

- [x] 6. Update project documentation
  - Update product and dependency documentation
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 6.1 Review and update README.md
  - Search for any mentions of barcode scanning
  - Remove or update any barcode scanning references
  - Add note if barcode scanning was mentioned in planned features
  - _Requirements: 5.1_

- [x] 6.2 Review and update product documentation
  - Check `.kiro/steering/product.md` for barcode scanning references
  - Check `docs/README.md` for barcode scanning references
  - Remove barcode scanning from feature lists
  - Add note explaining removal decision
  - _Requirements: 5.1, 5.3_

- [x] 6.3 Update dependency documentation
  - Remove CameraX from dependency lists
  - Remove ML Kit Barcode Scanning from dependency lists
  - Update `dependencies.txt` if it's manually maintained
  - _Requirements: 5.2_

- [x] 6.4 Add decision rationale documentation
  - Create or update decision record explaining why barcode scanning was removed
  - Include context (never implemented, security issues, not in roadmap)
  - Include consequences (positive: security, size, build time; neutral: no lost functionality)
  - Document for future reference if feature is reconsidered
  - _Requirements: 5.4_

- [x] 7. Final verification and testing
  - Perform final checks to ensure everything works
  - _Requirements: 3.1, 3.2_

- [x] 7.1 Build and install debug APK
  - Run `./gradlew assembleDebug`
  - Install APK on test device or emulator
  - Verify installation succeeds
  - _Requirements: 3.1_

- [x] 7.2 Test application functionality
  - Launch application
  - Navigate through all main screens (Meals, Planner, Shopping)
  - Test core features (add meal, create meal plan, view shopping list)
  - Verify no crashes or errors
  - Confirm all existing features work as expected
  - _Requirements: 3.2_

- [x] 7.3 Compare APK sizes
  - Note size of APK before changes (from previous builds)
  - Note size of APK after changes
  - Document size reduction
  - _Requirements: 3.1_

- [x] 7.4 Measure build time improvement
  - Note build time before changes (from previous builds if available)
  - Measure clean build time after changes
  - Document any improvement
  - _Requirements: 3.1_

- [x] 8. Clean up and finalize
  - Remove backup files and finalize changes
  - _Requirements: All_

- [x] 8.1 Remove backup files
  - Delete `app/build.gradle.kts.backup`
  - Delete `gradle/libs.versions.toml.backup`
  - Keep `dependencies_before.txt` and `dependencies_after.txt` for reference
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [x] 8.2 Review all changes
  - Review all modified files
  - Verify all documentation is updated
  - Confirm all tasks completed
  - _Requirements: All_

- [x] 8.3 Prepare commit message
  - Write commit message following conventional commits format
  - Include rationale and impact in commit body
  - Reference security issues resolved
  - _Requirements: All_
