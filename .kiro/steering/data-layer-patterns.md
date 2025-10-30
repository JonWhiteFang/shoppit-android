# Data Layer Patterns

## Room Database Setup

### Database Class

```kotlin
@Database(
    entities = [
        MealEntity::class,
        IngredientEntity::class,
        MealPlanEntity::class,
        ShoppingListItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class ShoppitDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
}
```

### Database Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ShoppitDatabase {
        return Room.databaseBuilder(
            context,
            ShoppitDatabase::class.java,
            "shoppit_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration() // Only for development
            .build()
    }
    
    @Provides
    fun provideMealDao(database: ShoppitDatabase): MealDao {
        return database.mealDao()
    }
    
    @Provides
    fun provideIngredientDao(database: ShoppitDatabase): IngredientDao {
        return database.ingredientDao()
    }
}
```

## Entity Design

### Simple Entity

```kotlin
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "notes")
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)
```

### Entity with Foreign Key

```kotlin
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["meal_id"])]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String
)
```

### Embedded Objects

```kotlin
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Long,
    
    val name: String,
    
    @Embedded(prefix = "addr_")
    val address: Address
)
// Creates columns: id, name, addr_street, addr_city, addr_zipCode
```

## DAO Patterns

### Basic CRUD Operations

```kotlin
@Dao
interface MealDao {
    
    // Query - returns Flow for reactive updates
    @Query("SELECT * FROM meals ORDER BY created_at DESC")
    fun getAllMeals(): Flow<List<MealEntity>>
    
    @Query("SELECT * FROM meals WHERE id = :mealId")
    fun getMealById(mealId: Long): Flow<MealEntity?>
    
    @Query("SELECT * FROM meals WHERE name LIKE '%' || :query || '%'")
    fun searchMeals(query: String): Flow<List<MealEntity>>
    
    // Insert - returns generated ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealEntity>)
    
    // Update - returns number of rows updated
    @Update
    suspend fun updateMeal(meal: MealEntity): Int
    
    // Delete - returns number of rows deleted
    @Delete
    suspend fun deleteMeal(meal: MealEntity): Int
    
    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long): Int
    
    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()
}
```

### Complex Queries

```kotlin
@Dao
interface MealDao {
    
    // Join query with relation
    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithIngredients(mealId: Long): MealWithIngredients?
    
    // Aggregation
    @Query("SELECT COUNT(*) FROM meals")
    fun getMealCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM meals WHERE is_synced = 0")
    suspend fun getUnsyncedMealCount(): Int
    
    // Conditional query
    @Query("""
        SELECT * FROM meals 
        WHERE (:query IS NULL OR name LIKE '%' || :query || '%')
        AND (:showSynced OR is_synced = 0)
        ORDER BY 
            CASE WHEN :sortBy = 'name' THEN name END ASC,
            CASE WHEN :sortBy = 'date' THEN created_at END DESC
    """)
    fun getFilteredMeals(
        query: String?,
        showSynced: Boolean,
        sortBy: String
    ): Flow<List<MealEntity>>
    
    // Batch operations
    @Query("UPDATE meals SET is_synced = 1 WHERE id IN (:mealIds)")
    suspend fun markAsSynced(mealIds: List<Long>)
}
```

### Relations

```kotlin
// One-to-Many
data class MealWithIngredients(
    @Embedded val meal: MealEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "meal_id"
    )
    val ingredients: List<IngredientEntity>
)

// Many-to-Many with junction table
@Entity(
    tableName = "meal_tag_cross_ref",
    primaryKeys = ["meal_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MealTagCrossRef(
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    
    @ColumnInfo(name = "tag_id")
    val tagId: Long
)

data class MealWithTags(
    @Embedded val meal: MealEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealTagCrossRef::class,
            parentColumn = "meal_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
```

## Type Converters

### Basic Converters

```kotlin
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    
    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }
    
    @TypeConverter
    fun fromJson(value: String?): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, type) ?: emptyMap()
    }
    
    @TypeConverter
    fun toJson(map: Map<String, String>?): String {
        return Gson().toJson(map)
    }
}
```

## Repository Implementation Patterns

### Basic Repository

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val ingredientDao: IngredientDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        mealDao.getAllMeals()
            .map { entities -> entities.map { it.toMeal() } }
            .collect { meals ->
                emit(Result.success(meals))
            }
    }.catch { e ->
        emit(Result.failure(mapException(e)))
    }.flowOn(dispatcher)
    
    override suspend fun getMealById(id: Long): Result<Meal> = withContext(dispatcher) {
        try {
            val mealWithIngredients = mealDao.getMealWithIngredients(id)
            if (mealWithIngredients != null) {
                Result.success(mealWithIngredients.toMeal())
            } else {
                Result.failure(AppError.DataNotFoundError("Meal not found"))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(dispatcher) {
        try {
            val mealId = mealDao.insertMeal(meal.toEntity())
            
            // Insert ingredients
            val ingredientEntities = meal.ingredients.map { ingredient ->
                ingredient.toEntity(mealId)
            }
            ingredientDao.insertAll(ingredientEntities)
            
            Result.success(mealId)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    override suspend fun updateMeal(meal: Meal): Result<Unit> = withContext(dispatcher) {
        try {
            mealDao.updateMeal(meal.toEntity())
            
            // Delete old ingredients and insert new ones
            ingredientDao.deleteByMealId(meal.id)
            val ingredientEntities = meal.ingredients.map { it.toEntity(meal.id) }
            ingredientDao.insertAll(ingredientEntities)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    override suspend fun deleteMeal(mealId: Long): Result<Unit> = withContext(dispatcher) {
        try {
            mealDao.deleteMealById(mealId)
            // Ingredients deleted automatically via CASCADE
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
}
```

### Repository with Caching

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val mealApi: MealApi,
    private val networkMonitor: NetworkMonitor
) : MealRepository {
    
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    init {
        // Trigger initial refresh
        refreshTrigger.tryEmit(Unit)
    }
    
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        // Emit cached data first
        mealDao.getAllMeals()
            .map { entities -> entities.map { it.toMeal() } }
            .collect { meals ->
                emit(Result.success(meals))
            }
    }.catch { e ->
        emit(Result.failure(mapException(e)))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun refreshMeals(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline()) {
            return@withContext Result.failure(AppError.NoInternetError())
        }
        
        try {
            val remoteMeals = mealApi.getMeals()
            
            // Update local database
            val entities = remoteMeals.map { it.toEntity() }
            mealDao.insertAll(entities)
            
            refreshTrigger.emit(Unit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
}
```

### Repository with Offline-First Pattern

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val mealApi: MealApi,
    private val syncQueue: SyncQueue
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        // Always return local data
        mealDao.getAllMeals()
            .map { entities -> entities.map { it.toMeal() } }
            .collect { meals ->
                emit(Result.success(meals))
            }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Save locally first
            val mealId = mealDao.insertMeal(
                meal.toEntity().copy(isSynced = false)
            )
            
            // Queue for sync
            syncQueue.enqueueMealSync(mealId)
            
            Result.success(mealId)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    override suspend fun syncMeals(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get unsynced meals
            val unsyncedMeals = mealDao.getUnsyncedMeals()
            
            // Upload to server
            unsyncedMeals.forEach { meal ->
                try {
                    mealApi.uploadMeal(meal.toDto())
                    mealDao.updateMeal(meal.copy(isSynced = true))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync meal ${meal.id}")
                }
            }
            
            // Download from server
            val remoteMeals = mealApi.getMeals()
            mealDao.insertAll(remoteMeals.map { it.toEntity().copy(isSynced = true) })
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
}
```

## Mapper Patterns

### Extension Functions

```kotlin
// Entity to Domain Model
fun MealEntity.toMeal(): Meal = Meal(
    id = id,
    name = name,
    ingredients = emptyList(), // Loaded separately
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MealWithIngredients.toMeal(): Meal = Meal(
    id = meal.id,
    name = meal.name,
    ingredients = ingredients.map { it.toIngredient() },
    notes = meal.notes,
    createdAt = meal.createdAt,
    updatedAt = meal.updatedAt
)

// Domain Model to Entity
fun Meal.toEntity(): MealEntity = MealEntity(
    id = id,
    name = name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = false
)

fun Ingredient.toEntity(mealId: Long): IngredientEntity = IngredientEntity(
    id = 0, // Auto-generated
    mealId = mealId,
    name = name,
    quantity = quantity,
    unit = unit
)

// DTO to Entity
fun MealDto.toEntity(): MealEntity = MealEntity(
    id = id,
    name = name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = true
)

// Entity to DTO
fun MealEntity.toDto(): MealDto = MealDto(
    id = id,
    name = name,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)
```

### Mapper Classes

```kotlin
class MealMapper @Inject constructor() {
    
    fun toMeal(entity: MealEntity, ingredients: List<IngredientEntity>): Meal {
        return Meal(
            id = entity.id,
            name = entity.name,
            ingredients = ingredients.map { toIngredient(it) },
            notes = entity.notes
        )
    }
    
    fun toEntity(meal: Meal): MealEntity {
        return MealEntity(
            id = meal.id,
            name = meal.name,
            notes = meal.notes,
            createdAt = meal.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    private fun toIngredient(entity: IngredientEntity): Ingredient {
        return Ingredient(
            name = entity.name,
            quantity = entity.quantity,
            unit = entity.unit
        )
    }
}
```

## Database Migrations

### Simple Migration

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL(
            "ALTER TABLE meals ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0"
        )
    }
}
```

### Complex Migration

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                color TEXT NOT NULL
            )
        """)
        
        // Create junction table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS meal_tag_cross_ref (
                meal_id INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                PRIMARY KEY(meal_id, tag_id),
                FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE,
                FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
        """)
        
        // Create indices
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_meal_tag_cross_ref_meal_id ON meal_tag_cross_ref(meal_id)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_meal_tag_cross_ref_tag_id ON meal_tag_cross_ref(tag_id)"
        )
    }
}
```

### Migration with Data Transformation

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create temporary table with new schema
        database.execSQL("""
            CREATE TABLE meals_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                is_synced INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // Copy data with transformation
        database.execSQL("""
            INSERT INTO meals_new (id, name, notes, created_at, updated_at, is_synced)
            SELECT id, name, notes, created_at, updated_at, 0
            FROM meals
        """)
        
        // Drop old table
        database.execSQL("DROP TABLE meals")
        
        // Rename new table
        database.execSQL("ALTER TABLE meals_new RENAME TO meals")
        
        // Recreate indices
        database.execSQL("CREATE INDEX IF NOT EXISTS index_meals_name ON meals(name)")
    }
}
```

## Testing Data Layer

### DAO Testing

```kotlin
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    
    private lateinit var database: ShoppitDatabase
    private lateinit var mealDao: MealDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            ShoppitDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        mealDao = database.mealDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveMeal() = runTest {
        // Given
        val meal = MealEntity(
            id = 1,
            name = "Pasta",
            createdAt = System.currentTimeMillis()
        )
        
        // When
        mealDao.insertMeal(meal)
        val meals = mealDao.getAllMeals().first()
        
        // Then
        assertEquals(1, meals.size)
        assertEquals("Pasta", meals[0].name)
    }
    
    @Test
    fun deleteMealCascadesIngredients() = runTest {
        // Given
        val mealId = mealDao.insertMeal(MealEntity(name = "Pasta"))
        ingredientDao.insertIngredient(
            IngredientEntity(mealId = mealId, name = "Tomato", quantity = "2", unit = "pcs")
        )
        
        // When
        mealDao.deleteMealById(mealId)
        
        // Then
        val ingredients = ingredientDao.getIngredientsByMealId(mealId).first()
        assertTrue(ingredients.isEmpty())
    }
}
```

### Repository Testing

```kotlin
class MealRepositoryImplTest {
    
    private lateinit var mealDao: MealDao
    private lateinit var ingredientDao: IngredientDao
    private lateinit var repository: MealRepositoryImpl
    
    @Before
    fun setup() {
        mealDao = mockk()
        ingredientDao = mockk()
        repository = MealRepositoryImpl(mealDao, ingredientDao)
    }
    
    @Test
    fun `getMeals returns flow of meals`() = runTest {
        // Given
        val entities = listOf(
            MealEntity(id = 1, name = "Pasta", createdAt = 0L)
        )
        every { mealDao.getAllMeals() } returns flowOf(entities)
        
        // When
        val result = repository.getMeals().first()
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals("Pasta", result.getOrNull()?.first()?.name)
    }
    
    @Test
    fun `addMeal inserts meal and ingredients`() = runTest {
        // Given
        val meal = Meal(
            id = 0,
            name = "Pasta",
            ingredients = listOf(Ingredient("Tomato", "2", "pcs"))
        )
        coEvery { mealDao.insertMeal(any()) } returns 1L
        coEvery { ingredientDao.insertAll(any()) } just Runs
        
        // When
        val result = repository.addMeal(meal)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1L, result.getOrNull())
        coVerify { mealDao.insertMeal(any()) }
        coVerify { ingredientDao.insertAll(any()) }
    }
}
```

## Best Practices

### Do
- ✅ Use Flow for reactive queries
- ✅ Use suspend functions for one-shot operations
- ✅ Apply `flowOn(Dispatchers.IO)` for database operations
- ✅ Use transactions for multi-step operations
- ✅ Create indices for frequently queried columns
- ✅ Use foreign keys with CASCADE for related data
- ✅ Export schema for migration tracking
- ✅ Test migrations thoroughly
- ✅ Use type converters for complex types
- ✅ Map exceptions at repository boundaries

### Don't
- ❌ Perform database operations on main thread
- ❌ Return mutable state from repositories
- ❌ Use fallbackToDestructiveMigration in production
- ❌ Store large blobs in database (use file storage)
- ❌ Create circular dependencies between entities
- ❌ Forget to close database in tests
- ❌ Use raw SQL without parameterization
- ❌ Ignore migration testing

## Quick Reference

### Flow vs Suspend
```kotlin
// Use Flow for reactive data (observes changes)
fun getMeals(): Flow<List<Meal>>

// Use suspend for one-shot operations
suspend fun addMeal(meal: Meal): Result<Long>
```

### Transaction Pattern
```kotlin
@Transaction
suspend fun updateMealWithIngredients(meal: Meal) {
    mealDao.updateMeal(meal.toEntity())
    ingredientDao.deleteByMealId(meal.id)
    ingredientDao.insertAll(meal.ingredients.map { it.toEntity(meal.id) })
}
```
