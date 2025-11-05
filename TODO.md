# Shoppit Android - TODO

## High Priority

### 1. Fix Hilt/Dagger Dependency Injection Issues
**Status:** Blocking full app build  
**Priority:** High

#### Issues:
- [ ] **Duplicate PerformanceMonitor binding**
  - `PerformanceMonitor` is bound multiple times in the DI graph
  - Need to review and consolidate DI modules
  - Check `PerformanceModule.kt` and any other modules providing `PerformanceMonitor`
  
- [ ] **Missing CoroutineDispatcher provider**
  - `CoroutineDispatcher` cannot be provided without an `@Provides`-annotated method
  - Need to add a Hilt module with `@Provides` methods for:
    - `@IoDispatcher CoroutineDispatcher`
    - `@MainDispatcher CoroutineDispatcher`
    - `@DefaultDispatcher CoroutineDispatcher`

#### Action Items:
1. Review all Hilt modules in `app/src/main/java/com/shoppit/app/di/`
2. Identify duplicate `PerformanceMonitor` bindings
3. Create or update `DispatcherModule.kt` with proper dispatcher providers
4. Run `./gradlew.bat :app:assembleDebug` to verify fixes
5. Run tests to ensure DI graph is correct

#### Example Fix:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

// Qualifier annotations
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
```

---

## Medium Priority

### 2. Implement Missing Repository Methods
**Status:** Commented out with TODO markers  
**Priority:** Medium

#### Missing Methods:
- [ ] **MealPlanRepository.getMealPlansForDateRange()**
  - Required by: `CacheWarmer.warmMealPlanCache()`
  - Location: `app/src/main/java/com/shoppit/app/data/cache/CacheWarmer.kt:135`
  - Signature: `fun getMealPlansForDateRange(startDate: Long, endDate: Long): Flow<Result<List<MealPlan>>>`

- [ ] **ShoppingListRepository.getShoppingListItems()**
  - Required by: `CacheWarmer.warmShoppingListCache()`
  - Location: `app/src/main/java/com/shoppit/app/data/cache/CacheWarmer.kt:163`
  - Signature: `fun getShoppingListItems(): Flow<Result<List<ShoppingListItem>>>`

#### Action Items:
1. Implement `getMealPlansForDateRange()` in `MealPlanRepository` interface
2. Implement `getMealPlansForDateRange()` in `MealPlanRepositoryImpl`
3. Add corresponding DAO method in `MealPlanDao`
4. Implement `getShoppingListItems()` in `ShoppingListRepository` interface
5. Implement `getShoppingListItems()` in `ShoppingListRepositoryImpl`
6. Add corresponding DAO method in `ShoppingListDao`
7. Uncomment cache warming methods in `CacheWarmer.kt`
8. Test cache warming functionality

---

### 3. Implement Navigation Analytics Metrics Collection
**Status:** Using placeholder data classes  
**Priority:** Medium

#### Placeholder Classes:
- [ ] **FrameMetrics**
  - Currently: Empty placeholder with default values
  - Location: `app/src/main/java/com/shoppit/app/presentation/ui/navigation/NavigationAnalyticsDashboard.kt:887`
  - Needs: Actual frame rate monitoring implementation

- [ ] **MemoryMetrics**
  - Currently: Empty placeholder with default values
  - Location: `app/src/main/java/com/shoppit/app/presentation/ui/navigation/NavigationAnalyticsDashboard.kt:893`
  - Needs: Actual memory usage monitoring implementation

#### Action Items:
1. Create `NavigationFrameRateMonitor` class with actual FPS tracking
2. Create `NavigationMemoryMonitor` class with actual memory tracking
3. Integrate monitors with `NavigationPerformanceAnalytics`
4. Update `NavigationAnalyticsDashboard` to use real metrics
5. Remove placeholder data classes
6. Test analytics dashboard with real data

---

## Low Priority

### 4. Code Quality Improvements
**Status:** Ongoing  
**Priority:** Low

- [ ] Review and refactor code analysis features (removed files may need replacement)
- [ ] Add unit tests for newly fixed components
- [ ] Update documentation for cache warming feature
- [ ] Review and optimize performance monitoring overhead

---

## Completed ✓

- ✅ Fixed compilation errors in main codebase (2025-01-XX)
  - Fixed MealListScreen preview functions
  - Fixed NavigationPerformanceMonitor const placement
  - Fixed NavigationAnalyticsDashboard property delegates
  - Removed kotlin-compiler-embeddable dependency
  - Fixed StateBatchingOptimizations coroutine syntax
  - Removed duplicate NavigationLoadingState file

---

## Notes

- All Kotlin code compiles successfully as of commit `7345eef`
- Hilt DI issues are the only blocker for full app build
- Cache warming feature is partially implemented but functional
- Navigation analytics dashboard displays placeholder data until monitors are implemented

---

**Last Updated:** 2025-01-05
