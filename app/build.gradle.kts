plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.shoppit.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shoppit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.shoppit.app.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    lint {
        disable += "AutoboxingStateCreation"
    }
    
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

// KSP optimization configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// Detekt configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt-config.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
    parallel = true
    ignoreFailures = false
}

// Code Quality Analysis Task
abstract class AnalyzeCodeQualityTask : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val analysisPath: Property<String>
    
    @get:Input
    @get:Optional
    abstract val analyzers: Property<String>
    
    @get:Input
    @get:Optional
    abstract val generateBaseline: Property<Boolean>
    
    @get:Input
    @get:Optional
    abstract val outputPath: Property<String>
    
    @TaskAction
    fun analyze() {
        println("=".repeat(80))
        println("Code Quality Analysis")
        println("=".repeat(80))
        
        val path = analysisPath.getOrElse("app/src/main/java")
        val analyzersStr = analyzers.orNull
        val analyzersList = analyzersStr?.split(",")?.map { it.trim() } ?: emptyList()
        val baseline = generateBaseline.getOrElse(false)
        val output = outputPath.getOrElse(".kiro/specs/code-quality-analysis")
        
        // Validate path exists
        val pathFile = File(path)
        if (!pathFile.exists()) {
            throw GradleException("Analysis path does not exist: $path")
        }
        
        // Validate analyzers if specified
        val validAnalyzers = listOf(
            "architecture", "compose", "state-management", "error-handling",
            "dependency-injection", "database", "performance", "naming",
            "test-coverage", "documentation", "security", "detekt"
        )
        
        if (analyzersList.isNotEmpty()) {
            val invalidAnalyzers = analyzersList.filter { it !in validAnalyzers }
            if (invalidAnalyzers.isNotEmpty()) {
                throw GradleException(
                    "Invalid analyzer(s): ${invalidAnalyzers.joinToString(", ")}\n" +
                    "Valid analyzers: ${validAnalyzers.joinToString(", ")}"
                )
            }
        }
        
        println("\nConfiguration:")
        println("-".repeat(80))
        println("  Path:              $path")
        println("  Analyzers:         ${if (analyzersList.isEmpty()) "all" else analyzersList.joinToString(", ")}")
        println("  Generate Baseline: $baseline")
        println("  Output Path:       $output")
        println("-".repeat(80))
        
        // Execute analysis using the orchestrator
        println("\n[INFO] Starting analysis...")
        println("[INFO] Progress reporting will show:")
        println("       - Files being analyzed with progress bar")
        println("       - Analyzer execution status")
        println("       - Completion statistics")
        println("\n[INFO] This is a placeholder. Full implementation will execute AnalysisOrchestrator.")
        println("[INFO] The orchestrator will:")
        println("       1. Scan Kotlin files in the specified path")
        println("       2. Run selected analyzers (or all if none specified)")
        println("       3. Report progress for each file and analyzer")
        println("       4. Aggregate and deduplicate findings")
        println("       5. Generate markdown report")
        if (baseline) {
            println("       6. Generate baseline snapshot")
        }
        println("       ${if (baseline) "7" else "6"}. Save report to $output/analysis-report.md")
        
        println("\n" + "=".repeat(80))
        println("Usage Examples:")
        println("=".repeat(80))
        println("\n# Analyze entire codebase:")
        println("  .\\gradlew.bat analyzeCodeQuality")
        println("\n# Analyze specific directory:")
        println("  .\\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src/main/java/com/shoppit/app/ui")
        println("\n# Run specific analyzers:")
        println("  .\\gradlew.bat analyzeCodeQuality -Panalysis.analyzers=security,architecture")
        println("\n# Generate baseline:")
        println("  .\\gradlew.bat analyzeCodeQuality -Panalysis.baseline=true")
        println("\n# Custom output path:")
        println("  .\\gradlew.bat analyzeCodeQuality -Panalysis.output=custom/output/path")
        println("\n# Combine options:")
        println("  .\\gradlew.bat analyzeCodeQuality -Panalysis.path=app/src -Panalysis.analyzers=security -Panalysis.baseline=true")
        println("\n" + "=".repeat(80))
    }
}

tasks.register<AnalyzeCodeQualityTask>("analyzeCodeQuality") {
    group = "verification"
    description = "Runs comprehensive code quality analysis on the codebase"
    
    analysisPath.set(project.providers.gradleProperty("analysis.path"))
    analyzers.set(project.providers.gradleProperty("analysis.analyzers"))
    generateBaseline.set(project.providers.gradleProperty("analysis.baseline").map { it.toBoolean() })
    outputPath.set(project.providers.gradleProperty("analysis.output"))
}

dependencies {
    // Core Android
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.activity)
    implementation(libs.compose.navigation)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    
    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.compose)
    
    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    
    // Logging
    implementation(libs.timber)
    
    // Security
    implementation(libs.security.crypto)
    implementation(libs.work.runtime)
    
    // DataStore
    implementation(libs.datastore.preferences)
    
    // Profile installer for benchmarking
    implementation(libs.profileinstaller)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    
    // Android Testing
    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.room.testing)
    
    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    
    // Detekt
    implementation(libs.detekt.api)
    implementation(libs.detekt.cli)
    implementation(libs.detekt.core)
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose)
    
    // Kotlin Compiler for PSI parsing
    implementation(libs.kotlin.compiler.embeddable)
}