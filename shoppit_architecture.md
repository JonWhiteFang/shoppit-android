# 🛒 Shoppit - Android Architecture Design

## 1. High-Level Overview

Shoppit should let users:
- Save **meals** (with their ingredients)
- Plan **weekly meals**
- Automatically generate a **shopping list** of ingredients

This involves three domains:
- **Meal management**
- **Meal planning**
- **Shopping list generation**

Recommended architecture: **Clean Architecture + MVVM + Offline-First**

### Core Principles
- **Offline-first**: App works without internet, syncs when available
- **Reactive**: UI updates automatically when data changes
- **Testable**: Clear separation of concerns for easy testing
- **Scalable**: Modular design for future enhancements

---

## 2. Architecture Layers

### 1. Presentation Layer (UI + ViewModel)
**Responsibility:** Display data and handle user interaction.

**Tech stack:**
- Jetpack Compose (UI)
- ViewModel (Android Jetpack)
- StateFlow (reactive state management)
- Compose Navigation

**Structure:**
```
ui/
 ├─ meal/
 │   ├─ MealListScreen.kt
 │   ├─ MealDetailScreen.kt
 │   ├─ MealViewModel.kt
 │   └─ MealUiState.kt
 ├─ planner/
 │   ├─ PlannerScreen.kt
 │   ├─ PlannerViewModel.kt
 │   └─ PlannerUiState.kt
 ├─ shopping/
 │   ├─ ShoppingListScreen.kt
 │   ├─ ShoppingListViewModel.kt
 │   └─ ShoppingUiState.kt
 └─ common/
     ├─ ErrorScreen.kt
     ├─ LoadingScreen.kt
     └─ SyncStatusIndicator.kt
```

**UI State Management:**
```kotlin
data class MealUiState(
    val meals: List<Meal> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

enum class SyncStatus { SYNCING, SYNCED, ERROR, OFFLINE }
```

---

### 2. Domain Layer
**Responsibility:** Business logic and rules (pure Kotlin).

**Components:**
- Entities (data classes): `Meal`, `Ingredient`, `ShoppingItem`, `MealPlan`
- Use Cases (Interactors): Business operations with validation
- Repository Interfaces: Data access contracts
- Error Handling: Result pattern for operations

**Enhanced Entities:**
```kotlin
data class Ingredient(
    val name: String, 
    val quantity: Double, 
    val unit: String,
    val category: IngredientCategory = IngredientCategory.OTHER
)

data class Meal(
    val id: Int, 
    val name: String, 
    val ingredients: List<Ingredient>,
    val mealType: MealType = MealType.DINNER,
    val imageUrl: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

data class MealPlan(
    val id: Int,
    val mealId: Int,
    val date: LocalDate,
    val mealType: MealType
)

data class ShoppingItem(
    val name: String, 
    val totalQuantity: Double, 
    val unit: String,
    val category: IngredientCategory,
    val isCompleted: Boolean = false
)

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class IngredientCategory { PRODUCE, DAIRY, MEAT, PANTRY, FROZEN, OTHER }
```

**Error Handling:**
```kotlin
sealed class AppError {
    object NetworkError : AppError()
    object DatabaseError : AppError()
    data class ValidationError(val message: String) : AppError()
    data class UnknownError(val throwable: Throwable) : AppError()
}

typealias AppResult<T> = Result<T>
```

---

### 3. Data Layer
**Responsibility:** Handle local and remote data with offline-first approach.

**Tech stack:**
- Room (local storage with full-text search)
- Retrofit + OkHttp (network layer)
- DataStore (preferences and settings)
- WorkManager (background sync)

**Structure:**
```
data/
 ├─ local/
 │   ├─ dao/
 │   │   ├─ MealDao.kt
 │   │   ├─ IngredientDao.kt
 │   │   ├─ MealPlanDao.kt
 │   │   └─ ShoppingListDao.kt
 │   ├─ database/
 │   │   ├─ AppDatabase.kt
 │   │   └─ DatabaseMigrations.kt
 │   └─ entity/
 │       ├─ MealEntity.kt
 │       ├─ IngredientEntity.kt
 │       ├─ MealPlanEntity.kt
 │       └─ ShoppingListEntity.kt
 ├─ remote/
 │   ├─ api/
 │   │   ├─ MealApiService.kt
 │   │   └─ SyncApiService.kt
 │   ├─ dto/
 │   │   ├─ MealDto.kt
 │   │   └─ SyncResponseDto.kt
 │   └─ interceptor/
 │       ├─ AuthInterceptor.kt
 │       └─ NetworkInterceptor.kt
 ├─ repository/
 │   ├─ MealRepositoryImpl.kt
 │   ├─ PlannerRepositoryImpl.kt
 │   ├─ ShoppingRepositoryImpl.kt
 │   └─ SyncRepositoryImpl.kt
 ├─ sync/
 │   ├─ SyncWorker.kt
 │   └─ ConflictResolver.kt
 └─ mapper/
     ├─ MealMapper.kt
     ├─ IngredientMapper.kt
     └─ DtoMapper.kt
```

**Enhanced Repository Interface:**
```kotlin
interface MealRepository {
    fun getMealsFlow(): Flow<AppResult<List<Meal>>>
    suspend fun getMealById(id: Int): AppResult<Meal?>
    suspend fun addMeal(meal: Meal): AppResult<Unit>
    suspend fun updateMeal(meal: Meal): AppResult<Unit>
    suspend fun deleteMeal(id: Int): AppResult<Unit>
    suspend fun searchMeals(query: String): AppResult<List<Meal>>
    suspend fun syncWithRemote(): AppResult<Unit>
    suspend fun clearCache(): AppResult<Unit>
}
```

**Caching Strategy:**
```kotlin
class MealRepositoryImpl @Inject constructor(
    private val localDataSource: MealDao,
    private val remoteDataSource: MealApiService,
    private val syncManager: SyncManager
) : MealRepository {
    
    override fun getMealsFlow(): Flow<AppResult<List<Meal>>> = 
        localDataSource.getAllMealsFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .map { AppResult.success(it) }
            .catch { emit(AppResult.failure(it)) }
}
```

---

## 3. Data Flow Example

Example: Generating the shopping list

1. `PlannerViewModel` calls `GenerateShoppingListUseCase`
2. Use case fetches planned meals from `PlannerRepository`
3. Aggregates all ingredients (e.g., sums "Tomato" quantities)
4. Returns list of `ShoppingItem` to ViewModel
5. ViewModel exposes the list via `StateFlow`
6. `ShoppingListScreen` displays items in UI

---

## 4. Key Use Cases

**Use Case Implementation Pattern:**
```kotlin
abstract class UseCase<in P, R> {
    suspend operator fun invoke(parameters: P): AppResult<R> {
        return try {
            execute(parameters)
        } catch (e: Exception) {
            AppResult.failure(e)
        }
    }
    
    @Throws(RuntimeException::class)
    protected abstract suspend fun execute(parameters: P): AppResult<R>
}
```

| Use Case | Description | Validation Rules |
|-----------|--------------|------------------|
| `AddMealUseCase` | Add or edit a saved meal and its ingredients | Name not empty, at least one ingredient |
| `PlanWeekUseCase` | Assign meals to days of the week | Date not in past, meal exists |
| `GenerateShoppingListUseCase` | Merge ingredients from all planned meals | At least one planned meal |
| `MarkItemAsBoughtUseCase` | Mark items as completed in the shopping list | Item exists in current list |
| `GetMealSuggestionsUseCase` | Suggest meals based on past selections | Return max 10 suggestions |
| `SyncDataUseCase` | Synchronize local data with remote server | Network available, user authenticated |
| `SearchMealsUseCase` | Search meals by name or ingredient | Query length >= 2 characters |
| `ValidateIngredientUseCase` | Validate ingredient data before saving | Quantity > 0, valid unit |

**Example Use Case Implementation:**
```kotlin
class AddMealUseCase @Inject constructor(
    private val mealRepository: MealRepository,
    private val validator: MealValidator
) : UseCase<AddMealUseCase.Params, Unit>() {
    
    override suspend fun execute(parameters: Params): AppResult<Unit> {
        val validationResult = validator.validate(parameters.meal)
        if (validationResult.isFailure) {
            return AppResult.failure(ValidationError(validationResult.message))
        }
        
        return mealRepository.addMeal(parameters.meal)
    }
    
    data class Params(val meal: Meal)
}
```

---

## 5. Database Model (Room)

### Enhanced Database Schema

```kotlin
@Database(
    entities = [
        MealEntity::class,
        IngredientEntity::class,
        MealPlanEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
}
```

### MealEntity
```kotlin
@Entity(
    tableName = "meals",
    indices = [Index(value = ["name"], unique = true)]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(name = "meal_type") val mealType: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "sync_status") val syncStatus: String = "SYNCED"
)
```

### IngredientEntity
```kotlin
@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["mealId"]), Index(value = ["name"])]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "meal_id") val mealId: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String
)
```

### MealPlanEntity
```kotlin
@Entity(
    tableName = "meal_plans",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["date", "mealType"], unique = true)]
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "meal_id") val mealId: Int,
    val date: Long, // LocalDate as timestamp
    @ColumnInfo(name = "meal_type") val mealType: String
)
```

### ShoppingListEntity
```kotlin
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "week_start_date") val weekStartDate: Long,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
```

### ShoppingItemEntity
```kotlin
@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingListEntity::class,
        parentColumns = ["id"],
        childColumns = ["shoppingListId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["shoppingListId"]), Index(value = ["category"])]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "shopping_list_id") val shoppingListId: Int,
    val name: String,
    @ColumnInfo(name = "total_quantity") val totalQuantity: Double,
    val unit: String,
    val category: String,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false
)
```

### Enhanced DAO with Full-Text Search
```kotlin
@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMealsFlow(): Flow<List<MealEntity>>
    
    @Query("SELECT * FROM meals WHERE name LIKE '%' || :query || '%'")
    suspend fun searchMeals(query: String): List<MealEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealWithIngredients(id: Int): MealWithIngredients?
    
    @Update
    suspend fun updateMeal(meal: MealEntity)
    
    @Delete
    suspend fun deleteMeal(meal: MealEntity)
}
```

---

## 6. UI Design Concept (Jetpack Compose)

- **HomeScreen:** Tabs for “Meals”, “Planner”, “Shopping List”
- **MealScreen:** Add/edit meals and ingredients
- **PlannerScreen:** Drag meals into weekly grid
- **ShoppingListScreen:** Grouped list with checkboxes for bought items

Use **Material 3 Components** for consistent theming.

---

## 7. Dependency Injection (Hilt)

### Module Structure
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "shoppit_database")
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideMealRepository(
        mealDao: MealDao,
        apiService: MealApiService,
        syncManager: SyncManager
    ): MealRepository = MealRepositoryImpl(mealDao, apiService, syncManager)
}

@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    @Provides
    fun provideAddMealUseCase(
        repository: MealRepository,
        validator: MealValidator
    ): AddMealUseCase = AddMealUseCase(repository, validator)
}
```

## 8. Recommended Libraries

| Purpose | Library | Version |
|----------|----------|---------|
| UI | Jetpack Compose + Material3 | Latest stable |
| Architecture | ViewModel + Kotlin Coroutines + Flow | Latest stable |
| Database | Room + SQLite FTS | 2.5.0+ |
| Dependency Injection | Hilt | 2.48+ |
| Navigation | Jetpack Navigation (Compose) | Latest stable |
| Network | Retrofit + OkHttp + Moshi | Latest stable |
| Image Loading | Coil (Compose) | 2.4.0+ |
| Background Work | WorkManager | 2.8.0+ |
| Preferences | DataStore | 1.0.0+ |
| Testing | JUnit + MockK + Turbine (Flow testing) | Latest stable |
| UI Testing | Compose Testing + Espresso | Latest stable |
| Cloud Sync | Firebase Firestore / Supabase | Latest stable |
| Security | EncryptedSharedPreferences | Latest stable |

---

## 9. Testing Strategy

### Testing Pyramid
```
ui/
 ├─ MealViewModelTest.kt (Unit)
 ├─ MealScreenTest.kt (UI/Integration)
 └─ MealE2ETest.kt (End-to-End)

domain/
 ├─ AddMealUseCaseTest.kt (Unit)
 ├─ MealValidatorTest.kt (Unit)
 └─ GenerateShoppingListUseCaseTest.kt (Unit)

data/
 ├─ MealRepositoryTest.kt (Integration)
 ├─ MealDaoTest.kt (Integration)
 └─ FakeMealRepository.kt (Test Double)
```

### Test Configuration
```kotlin
// Test Application
@HiltAndroidApp
class ShoppitTestApplication : Application()

// Test Database
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

## 10. Security Considerations

### Data Protection
- **Local Storage**: Use Room with SQLCipher for database encryption
- **Network**: Implement certificate pinning and request/response encryption
- **Authentication**: OAuth 2.0 with PKCE for secure cloud sync
- **API Keys**: Store in BuildConfig, never in source code

### Privacy Compliance
- **Data Minimization**: Only collect necessary user data
- **Consent Management**: Clear opt-in for cloud sync and analytics
- **Data Retention**: Implement automatic data cleanup policies
- **Export/Delete**: Provide user data export and account deletion

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {
    @Provides
    @Singleton
    fun provideEncryptedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            "shoppit_secure_prefs",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
```

## 11. Implementation Timeline

### Phase 1: Core Foundation (Weeks 1-3)
- Set up project structure and dependencies
- Implement basic entities and database schema
- Create meal management (CRUD operations)
- Basic UI with Compose and Material3

### Phase 2: Planning & Shopping (Weeks 4-6)
- Implement meal planning functionality
- Shopping list generation and management
- Enhanced UI with navigation
- Local data persistence

### Phase 3: Polish & Testing (Weeks 7-8)
- Comprehensive testing suite
- Error handling and edge cases
- Performance optimization
- UI/UX improvements

### Phase 4: Cloud Sync (Weeks 9-10)
- User authentication
- Remote API integration
- Conflict resolution
- Background synchronization

### Phase 5: Advanced Features (Weeks 11-12)
- Search and filtering
- Data export/import
- Accessibility improvements
- Analytics and crash reporting

## 12. Performance Optimization

### Database Optimization
- **Indexing**: Add indexes on frequently queried columns
- **Pagination**: Implement paging for large datasets
- **Lazy Loading**: Load ingredients only when needed
- **Query Optimization**: Use @Transaction for complex queries

### UI Performance
- **Compose**: Use `remember` and `derivedStateOf` appropriately
- **Image Loading**: Implement proper caching with Coil
- **List Performance**: Use `LazyColumn` with proper keys
- **State Management**: Minimize recomposition with stable classes

### Memory Management
- **Leak Prevention**: Use lifecycle-aware components
- **Cache Management**: Implement LRU cache for images
- **Background Processing**: Use WorkManager for heavy operations

## 13. Future Enhancements

### Short-term (Next 6 months)
- **Barcode Scanning**: ML Kit for ingredient recognition
- **Recipe Import**: Parse recipes from URLs
- **Nutritional Info**: Integration with nutrition APIs
- **Smart Suggestions**: ML-based meal recommendations

### Medium-term (6-12 months)
- **Pantry Tracking**: Track what you already have at home
- **Price Tracking**: Monitor grocery prices and suggest savings
- **Social Features**: Share meal plans with family/friends

### Long-term (12+ months)
- **AI Meal Planning**: Generate meal plans based on preferences
- **Dietary Restrictions**: Advanced filtering for allergies/diets
- **Sustainability Tracking**: Carbon footprint of meals

---
