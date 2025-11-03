# Implementation Plan

- [x] 1. Database Performance Optimization
  - Add database indices for frequently queried columns to improve query performance
  - Optimize existing queries with proper JOIN operations and compiled queries
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 1.1 Add database indices to Room entities
  - Add index on meals table for name and created_at columns
  - Add composite index on meal_plans table for date and meal_type
  - Add index on shopping_list_items table for is_checked and ingredient_name
  - Update Room database version and create migration
  - _Requirements: 3.2_

- [x] 1.2 Optimize MealDao queries
  - Review and optimize getAllMeals query with proper ordering
  - Add compiled query for frequently used searchMeals operation
  - Optimize getMealWithIngredients with proper JOIN
  - Wrap queries with PerformanceMonitor tracking
  - _Requirements: 3.1, 3.5_

- [x] 1.3 Optimize ShoppingListDao queries
  - Optimize aggregation query for shopping list generation
  - Use transaction for batch insert/update operations
  - Add compiled query for getItemsBySection
  - Wrap queries with PerformanceMonitor tracking
  - _Requirements: 3.3, 3.4, 3.5_

- [x] 1.4 Optimize MealPlanDao queries
  - Add index for date-based queries
  - Optimize getPlansForWeek with date range query
  - Use batch operations for multiple plan updates
  - Wrap queries with PerformanceMonitor tracking
  - _Requirements: 3.1, 3.4, 3.5_

- [ ] 2. Compose UI Performance Optimization
  - Optimize LazyColumn rendering with stable keys and immutable data
  - Minimize recomposition scope with proper state management
  - _Requirements: 2.1, 2.2, 2.4, 5.1, 5.2, 5.3, 5.4_

- [ ] 2.1 Add stable keys to all LazyColumn implementations
  - Update MealListScreen LazyColumn with meal.id keys
  - Update ShoppingListScreen LazyColumn with item.id keys
  - Update MealPlannerScreen LazyColumn with plan.id keys
  - Verify key stability with Compose Layout Inspector
  - _Requirements: 2.4, 5.1, 5.2_

- [ ] 2.2 Mark domain models as @Immutable or @Stable
  - Add @Immutable annotation to Meal, Ingredient, MealPlan models
  - Add @Stable annotation to UI state classes
  - Ensure all data classes use immutable collections
  - Verify with Compose compiler metrics
  - _Requirements: 6.3, 5.3_

- [ ] 2.3 Optimize expensive computations with remember
  - Add remember for sorted/filtered lists in MealListScreen
  - Use derivedStateOf for computed values in ViewModels
  - Add remember for lambda callbacks in list items
  - Profile recomposition with Layout Inspector
  - _Requirements: 5.4, 6.2_

- [ ] 2.4 Minimize recomposition scope in composables
  - Extract frequently changing state to child composables
  - Use Modifier.clickable with remember for callbacks
  - Avoid passing entire state objects when only subset is needed
  - Add recomposition highlighting in debug builds
  - _Requirements: 2.4, 6.1_

- [ ] 3. Startup Performance Optimization
  - Implement deferred initialization for non-critical components
  - Track startup phases with performance monitoring
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [ ] 3.1 Create StartupOptimizer component
  - Define StartupOptimizer interface with initializeCritical and initializeDeferred
  - Implement StartupOptimizerImpl with phase tracking
  - Add StartupPhase enum for different initialization stages
  - Integrate with PerformanceMonitor for duration tracking
  - _Requirements: 1.4, 1.5_

- [ ] 3.2 Optimize ShoppitApplication initialization
  - Move WorkManager initialization to deferred phase
  - Defer Timber initialization for non-critical loggers
  - Use lazy initialization for Hilt modules where possible
  - Track each initialization phase duration
  - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [ ] 3.3 Implement startup tracing
  - Add startup trace markers for each phase
  - Log startup durations with Timber
  - Track cold/warm/hot start times separately
  - Create startup metrics data class
  - _Requirements: 1.5_

- [ ] 3.4 Optimize MainActivity initialization
  - Defer non-critical UI setup to LaunchedEffect
  - Preload first screen data during splash
  - Use rememberSaveable for state restoration
  - Measure time to first frame
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 4. Memory Management Optimization
  - Implement memory monitoring and pressure handling
  - Optimize cache sizes and eviction policies
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [ ] 4.1 Create MemoryManager component
  - Define MemoryManager interface with memory tracking methods
  - Implement MemoryManagerImpl with ComponentCallbacks2
  - Add MemoryPressureLevel enum (LOW, MODERATE, CRITICAL)
  - Track current and available memory
  - _Requirements: 4.1, 4.5_

- [ ] 4.2 Implement memory pressure handling
  - Register MemoryManager in Application class
  - Implement onTrimMemory callback for different levels
  - Clear caches on TRIM_MEMORY_RUNNING_LOW
  - Reduce cache sizes on memory pressure
  - _Requirements: 4.4, 4.5_

- [ ] 4.3 Optimize cache configuration
  - Increase meal cache size to 200 entries with 10min TTL
  - Set shopping list cache to 50 entries with 2min TTL
  - Implement cache warming on app startup
  - Add cache metrics tracking with PerformanceMonitor
  - _Requirements: 4.2, 8.1, 8.2, 8.3, 8.4_

- [ ] 4.4 Add memory monitoring to PerformanceMonitor
  - Extend PerformanceMonitor with trackMemoryUsage method
  - Log memory metrics when usage exceeds thresholds
  - Create MemoryMetrics data class
  - Track memory pressure events
  - _Requirements: 4.5, 10.3_

- [ ] 5. Navigation Performance Optimization
  - Optimize screen transitions and data preloading
  - Improve back stack management
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 5.1 Implement navigation preloading
  - Add NavigationPreloader component (already exists, enhance it)
  - Preload meal detail data during list navigation
  - Preload shopping list data during planner navigation
  - Use LaunchedEffect for predictive preloading
  - _Requirements: 9.2_

- [ ] 5.2 Optimize navigation transitions
  - Ensure all navigation completes within 100ms
  - Add navigation duration tracking to PerformanceMonitor
  - Use type-safe navigation routes consistently
  - Profile navigation with NavigationPerformanceMonitor
  - _Requirements: 9.1, 9.4, 10.2_

- [ ] 5.3 Optimize back stack management
  - Clear unnecessary back stack entries on main screen navigation
  - Use saveState and restoreState for bottom navigation
  - Implement proper launchSingleTop for main destinations
  - Test back navigation performance
  - _Requirements: 9.3_

- [ ] 5.4 Add loading states during navigation
  - Show loading indicators during data-heavy transitions
  - Use Skeleton screens for predictable layouts
  - Implement progressive loading for large lists
  - Test with slow network conditions
  - _Requirements: 9.5_

- [ ] 6. Background Task Optimization
  - Ensure all background operations use proper dispatchers
  - Optimize coroutine usage for non-blocking operations
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 6.1 Create dispatcher qualifier annotations
  - Define @IoDispatcher, @DefaultDispatcher, @MainDispatcher annotations
  - Create DispatcherModule for Hilt injection
  - Update all repositories to inject dispatchers
  - Verify dispatcher usage with code review
  - _Requirements: 7.1, 7.4_

- [ ] 6.2 Optimize repository coroutine usage
  - Ensure all Flow operations use flowOn(ioDispatcher)
  - Use withContext for suspend functions
  - Verify no blocking operations on main thread
  - Add dispatcher tests
  - _Requirements: 7.1, 7.2_

- [ ] 6.3 Add progress indicators for long operations
  - Show loading states during sync operations
  - Display progress for batch operations
  - Use SnackBar for background task completion
  - Test with simulated slow operations
  - _Requirements: 7.3_

- [ ] 6.4 Implement error handling for background tasks
  - Catch exceptions in coroutine scopes
  - Update UI on main thread after errors
  - Log background task failures
  - Test error scenarios
  - _Requirements: 7.5_

- [ ] 7. State Management Optimization
  - Optimize ViewModel state updates and Flow collection
  - Implement efficient derived state computation
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 7.1 Optimize ViewModel state exposure
  - Ensure all ViewModels expose StateFlow (not MutableStateFlow)
  - Use _state.update { } for state modifications
  - Verify immutable state exposure
  - Add state management tests
  - _Requirements: 6.1_

- [ ] 7.2 Implement derivedStateOf for computed values
  - Use derivedStateOf for filtered/sorted lists
  - Add derivedStateOf for count calculations
  - Optimize expensive computations with remember
  - Profile recomposition reduction
  - _Requirements: 6.2, 6.5_

- [ ] 7.3 Optimize Flow collection in composables
  - Use collectAsState with proper lifecycle
  - Avoid collecting same Flow multiple times
  - Use SharedFlow for one-time events
  - Test state collection behavior
  - _Requirements: 6.4_

- [ ] 7.4 Implement state batching for frequent updates
  - Batch rapid state updates in ViewModels
  - Use debounce for search input
  - Throttle scroll position updates
  - Test batching effectiveness
  - _Requirements: 6.5_

- [ ] 8. Performance Monitoring Enhancement
  - Extend PerformanceMonitor with additional metrics
  - Implement comprehensive performance reporting
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 8.1 Extend PerformanceMonitor interface
  - Add trackFrameTime method for UI rendering
  - Add trackNavigation method for screen transitions
  - Add trackMemoryUsage method for memory monitoring
  - Create EnhancedPerformanceMonitor interface
  - _Requirements: 10.1, 10.2, 10.3_

- [ ] 8.2 Implement frame time tracking
  - Track frame rendering duration per screen
  - Calculate frame drop statistics
  - Log slow frames exceeding 16.67ms
  - Create FrameDropStats data class
  - _Requirements: 2.5, 10.4_

- [ ] 8.3 Implement performance summary reporting
  - Create comprehensive performance summary
  - Include startup, query, UI, and memory metrics
  - Add slow query reporting with threshold
  - Implement metrics export functionality
  - _Requirements: 10.4, 10.5_

- [ ] 8.4 Add performance logging
  - Log performance metrics with Timber
  - Use structured logging for metrics
  - Add performance tags for filtering
  - Implement debug-only detailed logging
  - _Requirements: 10.3_

- [ ] 9. List Rendering Optimization
  - Optimize LazyColumn performance for large lists
  - Implement efficient item rendering
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ] 9.1 Implement lazy loading for list items
  - Add pagination for meal lists
  - Implement infinite scroll for shopping lists
  - Use placeholder items during loading
  - Test with large datasets (1000+ items)
  - _Requirements: 5.5_

- [ ] 9.2 Optimize list item composables
  - Minimize composable complexity in list items
  - Extract reusable components
  - Use Modifier.drawBehind for custom drawing
  - Profile item rendering performance
  - _Requirements: 5.3_

- [ ] 9.3 Implement item prefetching
  - Prefetch next page of items before scroll end
  - Preload images for upcoming items
  - Use remember for item state
  - Test prefetching effectiveness
  - _Requirements: 5.5_

- [ ] 9.4 Add list performance monitoring
  - Track scroll performance metrics
  - Monitor item recomposition frequency
  - Log slow list rendering
  - Create list performance dashboard
  - _Requirements: 2.1, 2.2_

- [ ] 10. Cache Effectiveness Optimization
  - Improve cache hit rates and effectiveness
  - Implement intelligent cache warming
  - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [ ] 10.1 Implement cache warming strategy
  - Preload frequently accessed meals on startup
  - Warm shopping list cache on planner view
  - Prefetch meal plans for current week
  - Track cache warming effectiveness
  - _Requirements: 8.5_

- [ ] 10.2 Optimize cache eviction policy
  - Implement LRU + TTL eviction
  - Add access frequency tracking
  - Prioritize frequently accessed items
  - Test eviction under memory pressure
  - _Requirements: 8.1, 8.2_

- [ ] 10.3 Add cache metrics tracking
  - Track cache hits with PerformanceMonitor
  - Track cache misses with PerformanceMonitor
  - Calculate and log cache hit rate
  - Create cache effectiveness report
  - _Requirements: 8.3, 8.4_

- [ ] 10.4 Implement cache refresh strategy
  - Refresh stale cache in background
  - Use cache-aside pattern with auto-refresh
  - Implement optimistic cache updates
  - Test cache consistency
  - _Requirements: 8.5_

- [ ] 11. Performance Testing and Validation
  - Create comprehensive performance test suite
  - Validate performance targets are met
  - _Requirements: All requirements_

- [ ] 11.1 Create startup performance tests
  - Test cold start time < 2000ms
  - Test warm start time < 1000ms
  - Test hot start time < 500ms
  - Measure startup phase durations
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 11.2 Create database performance tests
  - Test query execution time < 50ms
  - Test transaction performance
  - Benchmark with large datasets
  - Verify index usage with EXPLAIN
  - _Requirements: 3.1, 3.2, 3.3_

- [ ] 11.3 Create UI performance tests
  - Test LazyColumn scroll at 60 FPS
  - Test navigation transition < 100ms
  - Measure frame drop rate
  - Test with Compose Layout Inspector
  - _Requirements: 2.1, 2.2, 2.3, 9.1_

- [ ] 11.4 Create memory performance tests
  - Test memory usage < 100MB
  - Test cache effectiveness > 80%
  - Verify no memory leaks with LeakCanary
  - Test memory pressure handling
  - _Requirements: 4.1, 4.2, 8.1, 8.2_

- [ ] 11.5 Create Macrobenchmark tests
  - Set up Macrobenchmark module
  - Create startup benchmark
  - Create scroll benchmark
  - Create navigation benchmark
  - _Requirements: All requirements_

- [ ] 11.6 Validate performance targets
  - Run all performance tests
  - Compare results against targets
  - Document performance improvements
  - Create performance report
  - _Requirements: All requirements_
