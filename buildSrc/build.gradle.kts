plugins {
    `kotlin-dsl`
    kotlin("jvm") version "2.1.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Kotlin compiler for PSI parsing
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.0")
    
    // Gson for JSON serialization (baseline management)
    implementation("com.google.code.gson:gson:2.10.1")
}
