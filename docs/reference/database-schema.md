# Database Schema Reference

## Overview

Shoppit uses Room Persistence Library for local data storage with an offline-first architecture. The database is implemented using SQLite and provides reactive data access through Kotlin Flow.

**Database Name**: `shoppit_database`  
**Current Version**: 2  
**Package**: `com.shoppit.app.data.local`

## Entities

### MealEntity

Represents a meal with its ingredients, notes, and metadata.

**Table Name**: `meals`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY, AUTO INCREMENT | Unique identifier for the meal |
| `name` | TEXT | NOT NULL | Name of the meal |
| `ingredients` | TEXT | NOT NULL | JSON array of ingredients (see IngredientEntity) |
| `notes` | TEXT | NOT NULL | Optional notes or cooking instructions |
| `created_at` | INTEGER | NOT NULL | Unix timestamp (seconds) when meal was created |
| `updated_at` | INTEGER | NOT NULL | Unix timestamp (seconds) when meal was last updated |

**Kotlin Definition**:
```kotlin
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "ingredients")
    val ingredients: List<IngredientEntity>,
    
    @ColumnInfo(name = "notes")
    val notes: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
```

**Indexes**: None (default primary key index on `id`)

**Example Data**:
```json
{
  "id": 1,
  "name": "Spaghetti Carbonara",
  "ingredients": "[{\"name\":\"Pasta\",\"quantity\":\"400\",\"unit\":\"g\"},{\"name\":\"Eggs\",\"quantity\":\"4\",\"unit\":\"pcs\"}]",
  "notes": "Cook pasta al dente. Mix eggs with cheese before adding to hot pasta.",
  "created_at": 1698765432,
  "updated_at": 1698765432
}
```

### IngredientEntity

Embedded data class representing an ingredient within a meal. Not a separate table - stored as JSON within MealEntity.

**Storage**: Embedded in `meals.ingredients` column as JSON array

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Name of the ingredient (e.g., "Flour", "Eggs") |
| `quantity` | String | Amount of ingredient (e.g., "2", "1.5") |
| `unit` | String | Unit of measurement (e.g., "cups", "grams", "pieces") |

**Kotlin Definition**:
```kotlin
data class IngredientEntity(
    val name: String,
    val quantity: String,
    val unit: String
)
```

**JSON Example**:
```json
[
  {
    "name": "Flour",
    "quantity": "2",
    "unit": "cups"
  },
  {
    "name": "Eggs",
    "quantity": "3",
    "unit": "pcs"
  }
]
```

### PlaceholderEntity

Legacy entity from initial database setup. Currently unused but maintained for migration compatibility.

**Table Name**: `placeholder_table`

## Relationships

### Current Schema (v2)
- **MealEntity** has embedded **IngredientEntity** list (one-to-many, embedded)
- No foreign key relationships (single entity design)

### Future Schema Considerations
When implementing meal planning and shopping list features:

```
MealEntity (1) ──< (N) MealPlanEntity
MealPlanEntity (N) >── (1) MealEntity
ShoppingListItemEntity (derived from MealPlanEntity meals)
```

## Data Access Objects (DAOs)

### MealDao

Provides CRUD operations for meals with reactive Flow-based queries.

**Package**: `com.shoppit.app.data.local.dao`

#### Query Methods

##### getAllMeals()
```kotlin
@Query("SELECT * FROM meals ORDER BY name ASC")
fun getAllMeals(): Flow<List<MealEntity>>
```
- **Returns**: Flow emitting all meals sorted alphabetically by name
- **Use Case**: Display meal list in UI
- **Reactive**: Emits new list whenever meals table changes

##### getMealById()
```kotlin
@Query("SELECT * FROM meals WHERE id = :mealId")
fun getMealById(mealId: Long): Flow<MealEntity?>
```
- **Parameters**: `mealId` - Unique identifier of the meal
- **Returns**: Flow emitting the meal or null if not found
- **Use Case**: Display meal details, edit meal
- **Reactive**: Emits updated meal when it changes

#### Mutation Methods

##### insertMeal()
```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertMeal(meal: MealEntity): Long
```
- **Parameters**: `meal` - MealEntity to insert
- **Returns**: Row ID of inserted meal
- **Conflict Strategy**: REPLACE - Updates existing meal if ID matches
- **Use Case**: Add new meal or update existing meal

##### updateMeal()
```kotlin
@Update
suspend fun updateMeal(meal: MealEntity)
```
- **Parameters**: `meal` - MealEntity with updated values
- **Use Case**: Update existing meal details
- **Note**: Meal must exist (ID must match existing record)

##### deleteMealById()
```kotlin
@Query("DELETE FROM meals WHERE id = :mealId")
suspend fun deleteMealById(mealId: Long)
```
- **Parameters**: `mealId` - ID of meal to delete
- **Use Case**: Remove meal from database
- **Note**: Silently succeeds if meal doesn't exist

## Type Converters

Room requires type converters for complex data types that aren't natively supported by SQLite.

### MealConverters

Handles serialization of ingredient lists using Gson.

```kotlin
@TypeConverter
fun fromIngredientList(value: String?): List<IngredientEntity>

@TypeConverter
fun toIngredientList(list: List<IngredientEntity>): String
```

**Storage Format**: JSON array string  
**Example**: `"[{\"name\":\"Flour\",\"quantity\":\"2\",\"unit\":\"cups\"}]"`

### Converters

Handles date/time conversions for future use.

```kotlin
@TypeConverter
fun fromTimestamp(value: Long?): LocalDateTime?

@TypeConverter
fun dateToTimestamp(date: LocalDateTime?): Long?

@TypeConverter
fun fromLocalDate(value: Long?): LocalDate?

@TypeConverter
fun localDateToLong(date: LocalDate?): Long?
```

**Date Storage**: Unix timestamps (seconds since epoch, UTC)  
**LocalDate Storage**: Days since epoch

## Migration Strategies

### Version 1 → Version 2

Added `MealEntity` table and `MealDao` for meal management feature.

**Migration Code** (if needed):
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS meals (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                ingredients TEXT NOT NULL,
                notes TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
    }
}
```

**Note**: Current implementation uses `fallbackToDestructiveMigration()` which drops and recreates tables on version changes. This is acceptable during development but should be replaced with proper migrations before production release.

### Future Migrations

When adding meal planning and shopping list features:

**Version 2 → 3**: Add `meal_plans` table
```sql
CREATE TABLE meal_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    meal_id INTEGER NOT NULL,
    date INTEGER NOT NULL,
    meal_type TEXT NOT NULL,
    FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
)
```

**Version 3 → 4**: Add `shopping_list_items` table
```sql
CREATE TABLE shopping_list_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ingredient_name TEXT NOT NULL,
    quantity TEXT NOT NULL,
    unit TEXT NOT NULL,
    is_checked INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
)
```

## Indexes

### Current Indexes

- **meals.id**: Automatic primary key index (B-tree)

### Recommended Future Indexes

When implementing meal planning:

```kotlin
@Entity(
    tableName = "meal_plans",
    indices = [
        Index(value = ["date"]),
        Index(value = ["meal_id"]),
        Index(value = ["date", "meal_type"], unique = true)
    ]
)
```

When implementing shopping lists:

```kotlin
@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["ingredient_name"]),
        Index(value = ["is_checked"])
    ]
)
```

## Database Configuration

### Database Builder

Located in `di/DatabaseModule.kt`:

```kotlin
@Provides
@Singleton
fun provideAppDatabase(
    @ApplicationContext context: Context
): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "shoppit_database"
    )
        .fallbackToDestructiveMigration() // TODO: Replace with proper migrations
        .build()
}
```

### Type Converter Registration

```kotlin
@Database(
    entities = [PlaceholderEntity::class, MealEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class, MealConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
}
```

## Query Performance Tips

### Use Flow for Reactive Queries
```kotlin
// Good - Reactive, updates automatically
mealDao.getAllMeals().collect { meals ->
    // UI updates automatically when data changes
}

// Avoid - Requires manual refresh
val meals = mealDao.getAllMealsSync() // Not implemented
```

### Use Suspend Functions for Mutations
```kotlin
// Good - Non-blocking
viewModelScope.launch {
    mealDao.insertMeal(meal)
}

// Avoid - Blocks main thread
mealDao.insertMealBlocking(meal) // Not implemented
```

### Limit Query Results for Large Datasets
```kotlin
// Future optimization for large meal libraries
@Query("SELECT * FROM meals ORDER BY name ASC LIMIT :limit OFFSET :offset")
fun getMealsPaged(limit: Int, offset: Int): Flow<List<MealEntity>>
```

## Testing Database

### In-Memory Database for Tests

```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    )
        .allowMainThreadQueries() // Only for tests
        .build()
    
    mealDao = database.mealDao()
}

@After
fun teardown() {
    database.close()
}
```

### Example DAO Test

```kotlin
@Test
fun insertAndRetrieveMeal() = runTest {
    // Given
    val meal = MealEntity(
        name = "Pasta",
        ingredients = listOf(
            IngredientEntity("Pasta", "400", "g")
        ),
        notes = "Cook al dente",
        createdAt = System.currentTimeMillis() / 1000,
        updatedAt = System.currentTimeMillis() / 1000
    )
    
    // When
    val id = mealDao.insertMeal(meal)
    val retrieved = mealDao.getMealById(id).first()
    
    // Then
    assertEquals("Pasta", retrieved?.name)
    assertEquals(1, retrieved?.ingredients?.size)
}
```

## Common Patterns

### Creating a New Meal

```kotlin
val meal = MealEntity(
    name = "Spaghetti Carbonara",
    ingredients = listOf(
        IngredientEntity("Spaghetti", "400", "g"),
        IngredientEntity("Eggs", "4", "pcs"),
        IngredientEntity("Parmesan", "100", "g")
    ),
    notes = "Traditional Roman recipe",
    createdAt = System.currentTimeMillis() / 1000,
    updatedAt = System.currentTimeMillis() / 1000
)

val mealId = mealDao.insertMeal(meal)
```

### Updating a Meal

```kotlin
mealDao.getMealById(mealId).first()?.let { existingMeal ->
    val updatedMeal = existingMeal.copy(
        name = "Spaghetti Carbonara (Updated)",
        updatedAt = System.currentTimeMillis() / 1000
    )
    mealDao.updateMeal(updatedMeal)
}
```

### Deleting a Meal

```kotlin
mealDao.deleteMealById(mealId)
```

### Observing All Meals

```kotlin
mealDao.getAllMeals().collect { meals ->
    // Update UI with meals list
    _uiState.update { MealUiState.Success(meals.map { it.toDomainModel() }) }
}
```

## Troubleshooting

### Common Issues

**Issue**: "Cannot find getter for field" error  
**Solution**: Ensure all entity fields have public visibility and are immutable (`val`)

**Issue**: Type converter not working  
**Solution**: Verify `@TypeConverters` annotation is on the `@Database` class

**Issue**: Flow not emitting updates  
**Solution**: Ensure you're using `@Query` methods that return `Flow<T>`, not suspend functions

**Issue**: Database locked error  
**Solution**: Don't perform database operations on main thread; use `suspend` functions in coroutines

**Issue**: Migration failed  
**Solution**: During development, use `.fallbackToDestructiveMigration()` or implement proper `Migration` objects

## See Also

- [Architecture Overview](../architecture/overview.md) - Clean Architecture implementation
- [Data Flow](../architecture/data-flow.md) - How data moves through layers
- [Repository Pattern](../guides/getting-started.md#repository-pattern) - Repository implementation guide
- [Testing Guide](../guides/testing.md) - Testing Room DAOs
