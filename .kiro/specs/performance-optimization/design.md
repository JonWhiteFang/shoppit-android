# Design Document

## Overview

This design document outlines the technical approach for implementing performance optimizations across the Shoppit Android application. The design leverages existing infrastructure (PerformanceMonitor, CacheManager, Room database) while introducing new optimizations for startup, UI rendering, database queries, memory management, and navigation.

The optimization strategy follows a layered approach:
- **Application Layer**: Startup optimization and initialization
- **UI Layer**: Compose performance and rendering optimization
- **Data Layer**: Database query optimization and caching
- **Infrastructure Layer**: Memory management and monitoring

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Startup    │  │  App Init    │  │   Tracing    │     │
│  │ Optimizer    │  │  Manager     │  │   Manager    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Compose    │  │    State     │  │  Navigation  │     │
│  │ Optimizer    │  │  Optimizer   │  │  Optimizer   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Database   │  │    Cache     │  │   Query      │     │
│  │  Optimizer   │  │  Manager     │  │  Optimizer   │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│                Infrastructure Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Memory     │  │ Performance  │  │   Metrics    │     │
│  │  Manager     │  │   Monitor    │  │  Reporter    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Startup Optimization

#### StartupOptimizer
**Purpose**: Manages app initialization to minimize cold start time.

**Interface**:
```kotlin
interface StartupOptimizer {
    /**
     * Initializes critical components synchronously on main thread.
     * Should complete in <100ms.
     */
    suspend fun initializeCritical()
    
    /**
     * Initializes non-critical components asynchronously.
     * Can take longer, runs in background.
     */
    suspend fun initializeDeferred()
    
    /**
     * Tracks startup phase durations.
     */
    fun trackStartupPhase(phase: StartupPhase, duration: Long)
}

enum class StartupPhase {
    APP_CREATION,
    HILT_INITIALIZATION,
    DATABASE_INITIALIZATION,
    CRITICAL_SERVICES,
    DEFERRED_SERVICES,
    FIRST_FRAME
}
```

**Implementation Strategy**:
- Move non-critical initialization (analytics, crash reporting) to background
- Use lazy initialization for Hilt modules where possible
- Defer WorkManager initialization
- Preload first screen data during splash

### 2. Database Query Optimization

#### QueryOptimizer
**Purpose**: Optimizes Room database queries for performance.

**Interface**:
```kotlin
interface QueryOptimizer {
    /**
     * Wraps query execution with performance tracking.
     */
    suspend fun <T> executeOptimized(
        queryName: String,
        query: suspend () -> T
    ): T
    
    /**
     * Analyzes query plan and suggests optimizations.
     */
    fun analyzeQuery(sql: String): QueryAnalysis
}

data class QueryAnalysis(
    val usesIndex: Boolean,
    val estimatedRows: Int,
    val suggestions: List<String>
)
```

**Optimization Techniques**:
- Add missing database indices on frequently queried columns
- Use EXPLAIN QUERY PLAN to identify full table scans
- Optimize JOIN operations with proper foreign keys
- Use compiled queries for frequently executed operations
- Batch INSERT/UPDATE operations in transactions

**Key Indices to Add**:
```sql
-- Meal queries
CREATE INDEX idx_meals_name ON meals(name)
CREATE INDEX idx_meals_created_at ON meals(created_at DESC)

-- Meal plan queries
CREATE INDEX idx_meal_plans_date ON meal_plans(date)
CREATE INDEX idx_meal_plans_meal_type ON meal_plans(meal_type)
CREATE INDEX idx_meal_plans_date_type ON meal_plans(date, meal_type)

-- Shopping list queries
CREATE INDEX idx_shopping_items_checked ON shopping_list_items(is_checked)
CREATE INDEX idx_shopping_items_name ON shopping_list_items(ingredient_name)
```

### 3. Compose Performance Optimization

#### ComposeOptimizer
**Purpose**: Ensures efficient Compose recomposition and rendering.

**Optimization Strategies**:

**a) Stable Keys for LazyColumn**:
```kotlin
// Before (inefficient)
LazyColumn {
    items(meals) { meal ->
        MealCard(meal)
    }
}

// After (optimized)
LazyColumn {
    items(
        items = meals,
        key = { meal -> meal.id } // Stable key
    ) { meal ->
        MealCard(meal)
    }
}
```

**b) Immutable Data Classes**:
```kotlin
@Immutable
data class Meal(
    val id: Long,
    val name: String,
    val ingredients: List<Ingredient>
)

@Stable
data class MealUiState(
    val meals: List<Meal> = emptyList(),
    val isLoading: Boolean = false
)
```

**c) Remember Expensive Computations**:
```kotlin
@Composable
fun MealList(meals: List<Meal>) {
    val sortedMeals = remember(meals) {
        meals.sortedBy { it.name }
    }
    
    val filteredCount = remember(sortedMeals) {
        derivedStateOf {
            sortedMeals.count { it.isVisible }
        }
    }
}
```

**d) Minimize Recomposition Scope**:
```kotlin
@Composable
fun MealCard(meal: Meal, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        // Isolate frequently changing state
        val isExpanded = remember { mutableStateOf(false) }
        
        MealCardContent(meal, isExpanded)
    }
}
```

### 4. Memory Management

#### MemoryManager
**Purpose**: Monitors and optimizes memory usage.

**Interface**:
```kotlin
interface MemoryManager {
    /**
     * Gets current memory usage in bytes.
     */
    fun getCurrentMemoryUsage(): Long
    
    /**
     * Gets available memory in bytes.
     */
    fun getAvailableMemory(): Long
    
    /**
     * Triggers cache cleanup when memory is low.
     */
    fun onLowMemory()
    
    /**
     * Registers a component for memory pressure callbacks.
     */
    fun registerMemoryPressureListener(listener: MemoryPressureListener)
}

interface MemoryPressureListener {
    fun onMemoryPressure(level: MemoryPressureLevel)
}

enum class MemoryPressureLevel {
    LOW,      // < 25% available
    MODERATE, // < 15% available
    CRITICAL  // < 10% available
}
```

**Memory Optimization Strategies**:
- Implement ComponentCallbacks2 in Application class
- Clear image caches on TRIM_MEMORY_RUNNING_LOW
- Reduce cache sizes on memory pressure
- Use WeakReference for non-critical cached data
- Profile memory usage with LeakCanary in debug builds

### 5. Cache Optimization

**Leverage Existing CacheManager**:
```kotlin
// Current implementation in data/cache/CacheManager.kt
interface CacheManager {
    suspend fun <T> get(key: String): T?
    suspend fun <T> put(key: String, value: T, ttl: Long)
    suspend fun invalidate(key: String)
    suspend fun clear()
    fun getHitRate(): Double
}
```

**Cache Strategy Improvements**:
- Increase cache size for frequently accessed data (meals, meal plans)
- Implement cache warming on app startup
- Use cache-aside pattern with automatic refresh
- Track cache metrics with PerformanceMonitor
- Implement cache eviction based on LRU + TTL

**Cache Configuration**:
```kotlin
data class CacheConfig(
    val maxSize: Int = 100,           // Max entries
    val defaultTtl: Long = 5 * 60 * 1000, // 5 minutes
    val enableMetrics: Boolean = true
)

// Specific cache configs
val mealCacheConfig = CacheConfig(
    maxSize = 200,
    defaultTtl = 10 * 60 * 1000 // 10 minutes
)

val shoppingListCacheConfig = CacheConfig(
    maxSize = 50,
    defaultTtl = 2 * 60 * 1000 // 2 minutes (more dynamic)
)
```

### 6. Navigation Optimization

#### NavigationOptimizer
**Purpose**: Optimizes screen transitions and navigation performance.

**Optimization Strategies**:

**a) Preload Destination Data**:
```kotlin
@Composable
fun ShoppitNavHost(navController: NavHostController) {
    val currentRoute by navController.currentBackStackEntryAsState()
    
    // Preload next likely screen
    LaunchedEffect(currentRoute) {
        when (currentRoute?.destination?.route) {
            Screen.MealList.route -> {
                // Preload meal detail data for first meal
                preloadMealDetails()
            }
        }
    }
}
```

**b) Optimize Back Stack**:
```kotlin
// Clear back stack when navigating to main screens
navController.navigate(Screen.MealList.route) {
    popUpTo(navController.graph.startDestinationId) {
        saveState = true
    }
    launchSingleTop = true
    restoreState = true
}
```

**c) Lazy ViewModel Initialization**:
```kotlin
@Composable
fun MealDetailScreen(
    mealId: Long,
    viewModel: MealDetailViewModel = hiltViewModel()
) {
    // Load data only when screen is visible
    LaunchedEffect(mealId) {
        viewModel.loadMeal(mealId)
    }
}
```

### 7. Background Task Optimization

**Coroutine Dispatcher Strategy**:
```kotlin
// Use appropriate dispatchers
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        // Database operations on IO dispatcher
        mealDao.getAllMeals()
            .map { entities -> entities.map { it.toMeal() } }
            .collect { meals ->
                emit(Result.success(meals))
            }
    }.flowOn(ioDispatcher) // Explicit dispatcher
}
```

**Dispatcher Configuration**:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
    
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

### 8. Performance Monitoring Enhancement

**Extend Existing PerformanceMonitor**:
```kotlin
interface EnhancedPerformanceMonitor : PerformanceMonitor {
    /**
     * Tracks UI frame rendering time.
     */
    fun trackFrameTime(screenName: String, frameTime: Long)
    
    /**
     * Tracks navigation transition time.
     */
    fun trackNavigation(from: String, to: String, duration: Long)
    
    /**
     * Tracks memory usage snapshot.
     */
    fun trackMemoryUsage(usedMemory: Long, availableMemory: Long)
    
    /**
     * Gets frame drop statistics.
     */
    fun getFrameDropStats(): FrameDropStats
}

data class FrameDropStats(
    val totalFrames: Int,
    val droppedFrames: Int,
    val avgFrameTime: Double,
    val maxFrameTime: Long
)
```

## Data Models

### Performance Metrics Models

```kotlin
/**
 * Startup performance metrics.
 */
data class StartupMetrics(
    val coldStartTime: Long,
    val warmStartTime: Long,
    val hotStartTime: Long,
    val phases: Map<StartupPhase, Long>
)

/**
 * UI performance metrics.
 */
data class UiPerformanceMetrics(
    val avgFrameTime: Double,
    val frameDropRate: Double,
    val slowFrames: List<SlowFrame>
)

data class SlowFrame(
    val screenName: String,
    val frameTime: Long,
    val timestamp: Long
)

/**
 * Memory metrics.
 */
data class MemoryMetrics(
    val currentUsage: Long,
    val maxUsage: Long,
    val availableMemory: Long,
    val pressureEvents: Int
)
```

## Error Handling

### Performance Degradation Handling

```kotlin
sealed class PerformanceIssue {
    data class SlowQuery(val query: String, val duration: Long) : PerformanceIssue()
    data class FrameDrop(val screen: String, val frameTime: Long) : PerformanceIssue()
    data class MemoryPressure(val level: MemoryPressureLevel) : PerformanceIssue()
    data class SlowStartup(val phase: StartupPhase, val duration: Long) : PerformanceIssue()
}

interface PerformanceIssueHandler {
    fun handleIssue(issue: PerformanceIssue)
}
```

**Handling Strategy**:
- Log performance issues with Timber
- Track issues with PerformanceMonitor
- Trigger automatic optimizations (cache cleanup, etc.)
- Report critical issues to crash reporting (if enabled)

## Testing Strategy

### Performance Testing Approach

**1. Startup Performance Tests**:
```kotlin
@Test
fun testColdStartTime() {
    val startTime = System.currentTimeMillis()
    
    // Launch activity
    activityScenario.launch(MainActivity::class.java)
    
    // Wait for first frame
    onView(isRoot()).perform(waitForView())
    
    val duration = System.currentTimeMillis() - startTime
    assertThat(duration).isLessThan(2000) // 2 seconds
}
```

**2. Database Performance Tests**:
```kotlin
@Test
fun testMealQueryPerformance() = runTest {
    // Insert test data
    repeat(1000) { i ->
        mealDao.insertMeal(createTestMeal(i))
    }
    
    // Measure query time
    val duration = measureTimeMillis {
        mealDao.getAllMeals().first()
    }
    
    assertThat(duration).isLessThan(50) // 50ms
}
```

**3. UI Performance Tests**:
```kotlin
@Test
fun testLazyColumnScrollPerformance() {
    composeTestRule.setContent {
        MealListScreen(
            uiState = MealUiState.Success(
                meals = List(100) { createTestMeal(it) }
            )
        )
    }
    
    // Measure scroll performance
    composeTestRule.onNodeWithTag("meal_list")
        .performScrollToIndex(50)
    
    // Verify no frame drops (would need custom test rule)
}
```

**4. Memory Leak Tests**:
```kotlin
@Test
fun testNoMemoryLeaksInMealList() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    
    // Navigate to meal list multiple times
    repeat(10) {
        // Navigate away and back
        scenario.onActivity { activity ->
            // Trigger navigation
        }
    }
    
    // Force GC and check for leaks
    Runtime.getRuntime().gc()
    
    // Use LeakCanary assertions
}
```

### Benchmarking

**Use Jetpack Macrobenchmark**:
```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()
    
    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = "com.shoppit.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
}
```

## Implementation Phases

### Phase 1: Foundation (Week 1)
- Add database indices
- Implement StartupOptimizer
- Add memory monitoring
- Enhance PerformanceMonitor

### Phase 2: Database Optimization (Week 1-2)
- Optimize slow queries
- Add query analysis
- Implement batch operations
- Add transaction optimization

### Phase 3: UI Optimization (Week 2)
- Add stable keys to LazyColumns
- Mark data classes as @Immutable
- Optimize state management
- Add frame time tracking

### Phase 4: Memory & Cache (Week 2-3)
- Implement MemoryManager
- Optimize cache configuration
- Add memory pressure handling
- Implement cache warming

### Phase 5: Navigation & Polish (Week 3)
- Optimize navigation transitions
- Add data preloading
- Optimize back stack management
- Final performance testing

## Performance Targets

| Metric | Target | Current | Improvement |
|--------|--------|---------|-------------|
| Cold Start | < 2000ms | TBD | TBD |
| Warm Start | < 1000ms | TBD | TBD |
| Hot Start | < 500ms | TBD | TBD |
| Query Time (avg) | < 50ms | TBD | TBD |
| Frame Rate | 60 FPS | TBD | TBD |
| Memory Usage | < 100MB | TBD | TBD |
| Cache Hit Rate | > 80% | TBD | TBD |
| Navigation Time | < 100ms | TBD | TBD |

## Monitoring and Metrics

### Key Performance Indicators (KPIs)

1. **Startup Time**: Track cold/warm/hot start durations
2. **Query Performance**: Track average and P95 query times
3. **Frame Rate**: Track frame drops and slow frames
4. **Memory Usage**: Track peak and average memory usage
5. **Cache Effectiveness**: Track hit rate and miss rate
6. **Navigation Performance**: Track transition times

### Logging Strategy

```kotlin
// Performance logging
Timber.tag("Performance").d(
    "Query: $queryName, Duration: ${duration}ms, " +
    "Threshold: ${if (duration > 50) "EXCEEDED" else "OK"}"
)

// Startup logging
Timber.tag("Startup").i(
    "Phase: $phase, Duration: ${duration}ms, " +
    "Total: ${totalStartupTime}ms"
)

// Memory logging
Timber.tag("Memory").w(
    "Memory pressure: $level, " +
    "Used: ${usedMemory}MB, Available: ${availableMemory}MB"
)
```

## Dependencies

### New Dependencies Required

```kotlin
// Macrobenchmark for performance testing
androidTestImplementation("androidx.benchmark:benchmark-macro-junit4:1.2.0")

// LeakCanary for memory leak detection (debug only)
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")

// Profiler for performance analysis (debug only)
debugImplementation("androidx.profileinstaller:profileinstaller:1.3.1")
```

### Existing Dependencies to Leverage

- Room (database optimization)
- Hilt (lazy initialization)
- Coroutines (background tasks)
- Compose (UI optimization)
- Timber (logging)
- Existing PerformanceMonitor
- Existing CacheManager

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Over-optimization complexity | High | Focus on measurable improvements, avoid premature optimization |
| Breaking existing functionality | High | Comprehensive testing, gradual rollout |
| Performance regression | Medium | Continuous monitoring, automated performance tests |
| Memory leaks from caching | Medium | Use WeakReference, implement proper cleanup |
| Database migration issues | Medium | Test migrations thoroughly, provide rollback |

## Success Criteria

1. Cold start time reduced to < 2 seconds
2. All database queries complete in < 50ms (average)
3. UI maintains 60 FPS during scrolling
4. Memory usage stays below 100MB
5. Cache hit rate exceeds 80%
6. Navigation transitions complete in < 100ms
7. No ANRs or crashes introduced
8. All existing tests pass
9. Performance metrics tracked and logged
10. User-perceived performance improvement validated
