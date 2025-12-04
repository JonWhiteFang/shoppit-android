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
        
        // Execute analysis using the AnalysisRunner
        try {
            // Use reflection to load and run the AnalysisRunner
            // This avoids compile-time dependencies on the analysis code
            val runnerClass = Class.forName("com.shoppit.app.analysis.AnalysisRunner")
            val constructor = runnerClass.getConstructor(
                String::class.java,  // projectRoot
                String::class.java,  // outputPath
                List::class.java,    // analyzers
                Boolean::class.java, // generateBaseline
                Boolean::class.java  // enableDetekt
            )
            
            val runner = constructor.newInstance(
                path,
                output,
                if (analyzersList.isEmpty()) null else analyzersList,
                baseline,
                true // enableDetekt
            )
            
            val runAnalysisMethod = runnerClass.getMethod("runAnalysis")
            runAnalysisMethod.invoke(runner)
            
        } catch (e: ClassNotFoundException) {
            throw GradleException(
                "Analysis runner not found. Make sure the analysis code is compiled.\n" +
                "Run: .\\gradlew.bat compileDebugKotlin"
            )
        } catch (e: Exception) {
            throw GradleException("Error running analysis: ${e.message}", e)
        }
    }
}

tasks.register<AnalyzeCodeQualityTask>("analyzeCodeQuality") {
    group = "verification"
    description = "Runs comprehensive code quality analysis on the codebase"
    
    // Ensure analysis code is compiled before running
    dependsOn("compileDebugKotlin")
    
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
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose)
}