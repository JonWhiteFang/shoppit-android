# Migration Quick Start Guide

**Goal:** Move code quality analysis from Android app to Gradle plugin  
**Time:** 8-12 hours  
**Difficulty:** Medium  

---

## Why Migrate?

The analysis code currently lives in `app/src/main/java/com/shoppit/app/analysis/` which causes:
- ❌ Build failures (DEX errors with kotlin-compiler)
- ❌ 50MB+ added to APK
- ❌ minSdk conflicts (requires API 26, app targets 24)
- ❌ Runtime overhead for development-only tool

**Solution:** Move to `buildSrc/` as a Gradle plugin.

---

## Quick Migration Steps

### 1. Create buildSrc Structure (5 minutes)

```bash
# Create directories
mkdir -p buildSrc/src/main/kotlin/com/shoppit/analysis

# Create build file
cat > buildSrc/build.gradle.kts << 'EOF'
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    implementation("io.gitlab.arturbosch.detekt:detekt-api:1.23.7")
    implementation("com.google.code.gson:gson:2.10.1")
}
EOF

# Create settings file
cat > buildSrc/settings.gradle.kts << 'EOF'
rootProject.name = "buildSrc"
EOF
```

### 2. Move Analysis Code (10 minutes)

```bash
# Move entire analysis package
mv app/src/main/java/com/shoppit/app/analysis/* \
   buildSrc/src/main/kotlin/com/shoppit/analysis/

# Remove old directory
rm -rf app/src/main/java/com/shoppit/app/analysis
```

### 3. Update Package Declarations (15 minutes)

Find and replace in all moved files:
```kotlin
// Old
package com.shoppit.app.analysis

// New
package com.shoppit.analysis
```

**Files to update:**
- All files in `buildSrc/src/main/kotlin/com/shoppit/analysis/`
- Update imports in files that reference other analysis classes

**Quick command:**
```bash
cd buildSrc/src/main/kotlin/com/shoppit/analysis
find . -name "*.kt" -exec sed -i 's/com\.shoppit\.app\.analysis/com.shoppit.analysis/g' {} +
```

### 4. Create Plugin Class (30 minutes)

Create `buildSrc/src/main/kotlin/com/shoppit/analysis/AnalysisPlugin.kt`:

```kotlin
package com.shoppit.analysis

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import java.io.File

abstract class CodeQualityAnalysisPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "codeQualysis",
            AnalysisExtension::class.java
        )
        
        project.tasks.register("analyzeCodeQuality", AnalysisTask::class.java) {
            group = "verification"
            description = "Analyzes code quality and generates report"
            
            sourceDir.set(extension.sourceDir)
            outputDir.set(extension.outputDir)
            baselinePath.set(extension.baselinePath)
        }
    }
}

abstract class AnalysisExtension {
    abstract val sourceDir: DirectoryProperty
    abstract val outputDir: DirectoryProperty
    abstract val baselinePath: Property<String>
}

abstract class AnalysisTask : DefaultTask() {
    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:Input
    abstract val baselinePath: Property<String>
    
    @TaskAction
    fun analyze() {
        val sourcePath = sourceDir.get().asFile.absolutePath
        val outputPath = outputDir.get().asFile.absolutePath
        
        println("Analyzing code in: $sourcePath")
        println("Output directory: $outputPath")
        
        // Create orchestrator and run analysis
        val orchestrator = com.shoppit.analysis.core.AnalysisOrchestratorImpl(
            analyzers = listOf(
                com.shoppit.analysis.analyzers.ArchitectureAnalyzer(),
                com.shoppit.analysis.analyzers.CodeSmellAnalyzer(),
                com.shoppit.analysis.analyzers.ComposeAnalyzer(),
                com.shoppit.analysis.analyzers.StateManagementAnalyzer(),
                // Add other analyzers
            ),
            fileScanner = com.shoppit.analysis.core.FileScannerImpl(),
            resultAggregator = com.shoppit.analysis.core.ResultAggregatorImpl(),
            reportGenerator = com.shoppit.analysis.reporting.ReportGeneratorImpl(),
            baselineManager = com.shoppit.analysis.baseline.BaselineManagerImpl(
                baselinePath = baselinePath.getOrElse("build/reports/code-quality/baseline.json")
            )
        )
        
        // Run analysis
        val result = orchestrator.analyze(
            paths = listOf(sourcePath),
            outputPath = outputPath
        )
        
        result.fold(
            onSuccess = { report ->
                println("Analysis complete!")
                println("Report: $outputPath/code-quality-report.md")
                println("Findings: ${report.totalFindings}")
            },
            onFailure = { error ->
                throw error
            }
        )
    }
}
```

### 5. Register Plugin (5 minutes)

Create `buildSrc/src/main/resources/META-INF/gradle-plugins/com.shoppit.analysis.code-quality.properties`:

```bash
mkdir -p buildSrc/src/main/resources/META-INF/gradle-plugins

cat > buildSrc/src/main/resources/META-INF/gradle-plugins/com.shoppit.analysis.code-quality.properties << 'EOF'
implementation-class=com.shoppit.analysis.CodeQualityAnalysisPlugin
EOF
```

### 6. Apply Plugin in App (5 minutes)

Update `app/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    id("com.shoppit.analysis.code-quality")
}

codeQualysis {
    sourceDir.set(file("src/main/java"))
    outputDir.set(file("build/reports/code-quality"))
    baselinePath.set("build/reports/code-quality/baseline.json")
}
```

### 7. Test Plugin (10 minutes)

```bash
# Rebuild buildSrc
./gradlew --stop
./gradlew tasks --all | grep analyzeCodeQuality

# Should see:
# analyzeCodeQuality - Analyzes code quality and generates report

# Run analysis
./gradlew analyzeCodeQuality

# Check output
cat build/reports/code-quality/code-quality-report.md
```

### 8. Verify App Builds (5 minutes)

```bash
# Clean build
./gradlew clean

# Build app (should succeed now)
./gradlew assembleDebug

# Verify APK size reduced
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

## Troubleshooting

### Issue: "Plugin not found"

**Solution:** Rebuild buildSrc
```bash
./gradlew --stop
rm -rf buildSrc/build
./gradlew tasks
```

### Issue: "Class not found" errors

**Solution:** Check package names
```bash
# Verify all files use correct package
grep -r "package com.shoppit.app.analysis" buildSrc/
# Should return nothing

grep -r "package com.shoppit.analysis" buildSrc/
# Should return all files
```

### Issue: Analysis task fails

**Solution:** Check orchestrator initialization
- Verify all analyzers are instantiated
- Check file paths are correct
- Ensure output directory exists

### Issue: Import errors in moved files

**Solution:** Update imports
```bash
# Find files with old imports
grep -r "import com.shoppit.app.analysis" buildSrc/

# Replace with new imports
find buildSrc/ -name "*.kt" -exec sed -i 's/import com\.shoppit\.app\.analysis/import com.shoppit.analysis/g' {} +
```

---

## Verification Checklist

After migration, verify:

- [ ] `./gradlew tasks` shows `analyzeCodeQuality` task
- [ ] `./gradlew analyzeCodeQuality` runs without errors
- [ ] Report is generated in `build/reports/code-quality/`
- [ ] `./gradlew assembleDebug` succeeds
- [ ] APK size is reduced by ~50MB
- [ ] No `app/src/main/java/com/shoppit/app/analysis/` directory exists
- [ ] All analysis code is in `buildSrc/src/main/kotlin/com/shoppit/analysis/`

---

## CI/CD Integration

Add to `.github/workflows/ci.yml`:

```yaml
- name: Run Code Quality Analysis
  run: ./gradlew analyzeCodeQuality

- name: Upload Analysis Report
  uses: actions/upload-artifact@v3
  with:
    name: code-quality-report
    path: build/reports/code-quality/

- name: Check for Critical Issues
  run: |
    if grep -q "Priority: CRITICAL" build/reports/code-quality/code-quality-report.md; then
      echo "Critical issues found!"
      exit 1
    fi
```

---

## Benefits After Migration

### Before:
- ❌ App won't build
- ❌ 50MB+ APK overhead
- ❌ minSdk conflicts
- ❌ Analysis code in production

### After:
- ✅ App builds successfully
- ✅ Normal APK size
- ✅ No minSdk conflicts
- ✅ Analysis runs as Gradle task
- ✅ Easy CI/CD integration
- ✅ Fast iteration on analysis code

---

## Time Breakdown

| Step | Time | Difficulty |
|------|------|------------|
| 1. Create buildSrc | 5 min | Easy |
| 2. Move code | 10 min | Easy |
| 3. Update packages | 15 min | Easy |
| 4. Create plugin | 30 min | Medium |
| 5. Register plugin | 5 min | Easy |
| 6. Apply plugin | 5 min | Easy |
| 7. Test plugin | 10 min | Easy |
| 8. Verify build | 5 min | Easy |
| **Total** | **85 min** | **Medium** |

**Note:** First-time migration may take 2-3 hours. Subsequent similar migrations will be faster.

---

## Need Help?

### Documentation:
- `ARCHITECTURAL_ISSUE.md` - Detailed problem analysis
- `COMPILATION_FIXES_SUMMARY.md` - What was fixed
- [Gradle Plugin Development](https://docs.gradle.org/current/userguide/custom_plugins.html)

### Common Issues:
- Package name mismatches → Search and replace
- Plugin not found → Rebuild buildSrc
- Task not showing → Check plugin registration
- Analysis fails → Check orchestrator initialization

---

**Ready to migrate?** Follow the steps above and you'll have a working Gradle plugin in under 2 hours!
