# Design Document

## Overview

The Project Setup design establishes a robust foundation for the Shoppit Android application using Clean Architecture principles. The design focuses on creating a scalable, testable, and maintainable codebase with proper separation of concerns, dependency injection, and offline-first data persistence capabilities.

## Architecture

### Clean Architecture Layers

```
app/
├── presentation/           # UI Layer (Activities, Composables, ViewModels)
│   ├── ui/
│   │   ├── theme/         # Material3 theme configuration
│   │   ├── common/        # Shared UI components
│   │   └── navigation/    # Navigation setup
│   └── viewmodel/         # ViewModels for state management
├── domain/                # Business Logic Layer (Use Cases, Entities)
│   ├── entity/           # Domain entities (Meal, Ingredient, etc.)
│   ├── usecase/          # Business operations
│   ├── repository/       # Repository interfaces
│   └── error/            # Error handling types
└── data/                 # Data Layer (Repositories, Data Sources)
    ├── local/            # Room database implementation
    │   ├── dao/          # Data Access Objects
    │   ├── entity/       # Database entities
    │   └── database/     # Database configuration
    ├── remote/           # Network data sources (future)
    ├── repository/       # Repository implementations
    └── mapper/           # Data mapping utilities
```

### Dependency Flow
- **Presentation** depends on **Domain**
- **Data** depends on **Domain**
- **Domain** has no dependencies (pure Kotlin)

## Components and Interfaces

### 1. Application Class
```kotlin
@HiltAndroidApp
class ShoppitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize logging, crash reporting, etc.
    }
}
```

### 2. Database Configuration
```kotlin
@Database(
    entities = [], // Will be populated in future specs
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // DAOs will be added in future specs
}
```

### 3. Hilt Modules Structure
```kotlin
// DatabaseModule - Provides database and DAOs
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule

// RepositoryModule - Binds repository implementations
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule

// UseCaseModule - Provides use cases
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule
```

### 4. Error Handling Framework
```kotlin
sealed class AppError {
    object NetworkError : AppError()
    object DatabaseError : AppError()
    data class ValidationError(val message: String) : AppError()
    data class UnknownError(val throwable: Throwable) : AppError()
}

typealias AppResult<T> = Result<T>

// Extension functions for Result handling
fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T>
fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T>
```

### 5. Base Use Case Pattern
```kotlin
abstract class UseCase<in P, R> {
    suspend operator fun invoke(parameters: P): AppResult<R> {
        return try {
            execute(parameters)
        } catch (e: Exception) {
            AppResult.failure(mapException(e))
        }
    }
    
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): AppResult<R>
    
    private fun mapException(exception: Exception): AppError {
        return when (exception) {
            is SQLiteException -> AppError.DatabaseError
            is IOException -> AppError.NetworkError
            is IllegalArgumentException -> AppError.ValidationError(exception.message ?: "Validation failed")
            else -> AppError.UnknownError(exception)
        }
    }
}
```

## Data Models

### Type Converters for Room
```kotlin
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { LocalDateTime.ofEpochSecond(it, 0, ZoneOffset.UTC) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toEpochSecond(ZoneOffset.UTC)
    }

    @TypeConverter
    fun fromLocalDate(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun localDateToLong(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}
```

## Error Handling

### Exception Mapping Strategy
1. **Database Exceptions** → `AppError.DatabaseError`
2. **Network Exceptions** → `AppError.NetworkError`
3. **Validation Exceptions** → `AppError.ValidationError`
4. **Unknown Exceptions** → `AppError.UnknownError`

### Error Propagation
- Use `AppResult<T>` wrapper for all operations that can fail
- Provide extension functions for convenient error handling
- Log errors appropriately based on severity

## Testing Strategy

### Test Configuration
```kotlin
// Test Application
@HiltAndroidApp
class ShoppitTestApplication : Application()

// Test Database Module
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideTestDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}
```

### Testing Utilities
```kotlin
// Base test class for ViewModels
abstract class ViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
}

// Base test class for Repository tests
abstract class RepositoryTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    protected lateinit var database: AppDatabase
}
```

## Build Configuration

### Version Catalog (libs.versions.toml)
```toml
[versions]
kotlin = "1.9.20"
compose-bom = "2023.10.01"
hilt = "2.48"
room = "2.6.0"
retrofit = "2.9.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version = "8.1.2" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "1.9.20-1.0.14" }
```

### Module build.gradle.kts
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.shoppit.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.hilt.android.testing)
}
```

## Security Considerations

### Build Security
- Use dependency verification for supply chain security
- Configure ProGuard/R8 for code obfuscation in release builds
- Implement certificate pinning for network security (future)

### Data Security
- Configure Room database encryption (future requirement)
- Use encrypted SharedPreferences for sensitive data
- Implement proper key management strategies

## Performance Considerations

### Build Performance
- Enable Gradle build cache
- Use KSP instead of KAPT for annotation processing
- Configure parallel builds and daemon optimization

### Runtime Performance
- Lazy initialization of heavy dependencies
- Proper lifecycle management for database connections
- Memory leak prevention with lifecycle-aware components