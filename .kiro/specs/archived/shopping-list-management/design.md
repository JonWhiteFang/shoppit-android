# Design Document

## Overview

The Shopping List Management feature extends the basic shopping list generation with interactive capabilities designed for real-world shopping scenarios. This design focuses on enhancing user experience during active shopping sessions through smart organization, quick interactions, and collaborative features. The architecture builds upon the existing shopping list foundation while adding new layers for user preferences, shopping history, and real-time collaboration.

## Architecture

### Enhanced Layer Structure

```
com.shoppit.app/
├── ui/
│   └── shopping/
│       ├── ShoppingListScreen.kt              # Enhanced with new features
│       ├── ShoppingListViewModel.kt           # Extended state management
│       ├── ShoppingModeScreen.kt              # Simplified shopping interface
│       ├── QuickAddSheet.kt                   # Bottom sheet for quick add
│       ├── ItemHistoryScreen.kt               # Shopping history view
│       ├── TemplateManagerScreen.kt           # Template CRUD
│       ├── StoreSectionEditor.kt              # Custom section ordering
│       ├── BarcodeScanner.kt                  # Barcode scanning UI
│       └── VoiceInputDialog.kt                # Voice command interface
├── domain/
│   ├── model/
│   │   ├── ShoppingListItem.kt                # Extended with new fields
│   │   ├── ItemHistory.kt                     # Purchase history record
│   │   ├── ShoppingTemplate.kt                # Saved list template
│   │   ├── StoreSection.kt                    # Custom section definition
│   │   ├── ItemNote.kt                        # Item-specific notes
│   │   └── PriceEstimate.kt                   # Budget tracking
│   ├── repository/
│   │   ├── ShoppingListRepository.kt          # Extended interface
│   │   ├── ItemHistoryRepository.kt           # History management
│   │   ├── TemplateRepository.kt              # Template storage
│   │   └── StoreSectionRepository.kt          # Section preferences
│   └── usecase/
│       ├── QuickAddItemUseCase.kt             # Fast item addition
│       ├── AdjustQuantityUseCase.kt           # Increment/decrement
│       ├── ReorderItemsUseCase.kt             # Drag-and-drop support
│       ├── ToggleShoppingModeUseCase.kt       # Mode switching
│       ├── MoveItemToSectionUseCase.kt        # Section reassignment
│       ├── AddItemNoteUseCase.kt              # Note management
│       ├── TogglePriorityUseCase.kt           # Priority marking
│       ├── DuplicateItemUseCase.kt            # Item duplication
│       ├── GetItemHistoryUseCase.kt           # History retrieval
│       ├── SaveTemplateUseCase.kt             # Template creation
│       ├── LoadTemplateUseCase.kt             # Template application
│       ├── UpdatePriceEstimateUseCase.kt      # Budget tracking
│       ├── ScanBarcodeUseCase.kt              # Barcode processing
│       ├── ProcessVoiceInputUseCase.kt        # Voice command parsing
│       └── GetSuggestedItemsUseCase.kt        # Smart suggestions
└── data/
    ├── local/
    │   ├── entity/
    │   │   ├── ShoppingListItemEntity.kt      # Extended schema
    │   │   ├── ItemHistoryEntity.kt           # History table
    │   │   ├── ShoppingTemplateEntity.kt      # Template table
    │   │   ├── TemplateItemEntity.kt          # Template items
    │   │   └── StoreSectionEntity.kt          # Section preferences
    │   ├── dao/
    │   │   ├── ShoppingListDao.kt             # Extended queries
    │   │   ├── ItemHistoryDao.kt              # History operations
    │   │   ├── TemplateDao.kt                 # Template CRUD
    │   │   └── StoreSectionDao.kt             # Section management
    │   └── database/
    │       └── AppDatabase.kt                 # Updated schema version
    ├── repository/
    │   ├── ShoppingListRepositoryImpl.kt      # Enhanced implementation
    │   ├── ItemHistoryRepositoryImpl.kt       # History management
    │   ├── TemplateRepositoryImpl.kt          # Template storage
    │   └── StoreSectionRepositoryImpl.kt      # Section preferences
    └── mapper/
        └── ShoppingListMapper.kt              # Extended mappings
```

## Components and Interfaces

### 1. Enhanced Domain Models

```kotlin
// Extended shopping list item with management features
data class ShoppingListItem(
    val id: Long = 0,
    val name: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val isChecked: Boolean = false,
    val isManual: Boolean = false,
    val mealIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    
    // New fields for management features
    val notes: String = "",
    val isPriority: Boolean = false,
    val customOrder: Int = 0,
    val estimatedPrice: Double? = null,
    val storeSection: String = category.name,
    val lastModifiedAt: Long = System.currentTimeMillis()
)

// Shopping history for quick re-add
data class ItemHistory(
    val id: Long = 0,
    val itemName: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val purchaseCount: Int = 1,
    val lastPurchasedAt: Long = System.currentTimeMillis(),
    val averagePrice: Double? = null
)

// Saved shopping list template
data class ShoppingTemplate(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val items: List<TemplateItem>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)

data class TemplateItem(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: ItemCategory,
    val notes: String = ""
)

// Custom store section configuration
data class StoreSection(
    val id: Long = 0,
    val name: String,
    val displayOrder: Int,
    val isCollapsed: Boolean = false,
    val color: String = "#000000"
)

// Shopping mode preferences
data class ShoppingModePreferences(
    val isEnabled: Boolean = false,
    val hideCheckedItems: Boolean = true,
    val increasedTextSize: Boolean = true,
    val showOnlyEssentials: Boolean = true
)

// Budget tracking
data class BudgetSummary(
    val totalEstimated: Double,
    val checkedTotal: Double,
    val remainingBudget: Double,
    val itemsWithPrices: Int,
    val totalItems: Int
)
```

### 2. Extended Repository Interfaces

```kotlin
// Extended shopping list repository
interface ShoppingListRepository {
    // Existing methods...
    fun getShoppingList(): Flow<Result<List<ShoppingListItem>>>
    suspend fun updateShoppingListItem(item: ShoppingListItem): Result<Unit>
    
    // New methods for management features
    suspend fun updateItemNotes(itemId: Long, notes: String): Result<Unit>
    suspend fun toggleItemPriority(itemId: Long, isPriority: Boolean): Result<Unit>
    suspend fun updateItemOrder(itemId: Long, newOrder: Int): Result<Unit>
    suspend fun updateItemPrice(itemId: Long, price: Double?): Result<Unit>
    suspend fun moveItemToSection(itemId: Long, section: String): Result<Unit>
    suspend fun duplicateItem(itemId: Long): Result<Long>
    fun getItemsBySection(section: String): Flow<Result<List<ShoppingListItem>>>
    fun getPriorityItems(): Flow<Result<List<ShoppingListItem>>>
    suspend fun getBudgetSummary(): Result<BudgetSummary>
}

// Item history repository
interface ItemHistoryRepository {
    fun getRecentHistory(limit: Int = 100): Flow<Result<List<ItemHistory>>>
    fun getFrequentItems(limit: Int = 20): Flow<Result<List<ItemHistory>>>
    suspend fun addToHistory(item: ShoppingListItem): Result<Unit>
    suspend fun incrementPurchaseCount(itemName: String): Result<Unit>
    suspend fun updateAveragePrice(itemName: String, price: Double): Result<Unit>
    suspend fun clearHistory(): Result<Unit>
}

// Template repository
interface TemplateRepository {
    fun getAllTemplates(): Flow<Result<List<ShoppingTemplate>>>
    fun getTemplate(id: Long): Flow<Result<ShoppingTemplate>>
    suspend fun saveTemplate(name: String, description: String, items: List<ShoppingListItem>): Result<Long>
    suspend fun updateTemplate(template: ShoppingTemplate): Result<Unit>
    suspend fun deleteTemplate(id: Long): Result<Unit>
    suspend fun updateLastUsed(id: Long): Result<Unit>
}

// Store section repository
interface StoreSectionRepository {
    fun getAllSections(): Flow<Result<List<StoreSection>>>
    suspend fun updateSectionOrder(sections: List<StoreSection>): Result<Unit>
    suspend fun toggleSectionCollapsed(sectionId: Long, isCollapsed: Boolean): Result<Unit>
    suspend fun createCustomSection(name: String, color: String): Result<Long>
    suspend fun deleteCustomSection(sectionId: Long): Result<Unit>
}
```

### 3. Key Use Cases

```kotlin
// Quick add from history
class QuickAddItemUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository,
    private val itemHistoryRepository: ItemHistoryRepository
) {
    suspend operator fun invoke(historyItem: ItemHistory): Result<Long> {
        val item = ShoppingListItem(
            name = historyItem.itemName,
            quantity = historyItem.quantity,
            unit = historyItem.unit,
            category = historyItem.category,
            isManual = true,
            estimatedPrice = historyItem.averagePrice
        )
        
        return shoppingListRepository.addShoppingListItem(item)
    }
}

// Adjust quantity with increment/decrement
class AdjustQuantityUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long, increment: Boolean): Result<Unit> {
        return repository.getShoppingListItem(itemId).first().flatMap { item ->
            val currentQuantity = item.quantity.toIntOrNull() ?: return@flatMap Result.failure(
                ValidationException("Cannot adjust non-numeric quantity")
            )
            
            val newQuantity = if (increment) {
                currentQuantity + 1
            } else {
                maxOf(1, currentQuantity - 1)
            }
            
            val updatedItem = item.copy(quantity = newQuantity.toString())
            repository.updateShoppingListItem(updatedItem)
        }
    }
}

// Reorder items via drag-and-drop
class ReorderItemsUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long, newPosition: Int): Result<Unit> {
        return repository.updateItemOrder(itemId, newPosition)
    }
}

// Toggle shopping mode
class ToggleShoppingModeUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean): Result<Unit> {
        return preferencesRepository.setShoppingModeEnabled(enabled)
    }
    
    fun getShoppingModeState(): Flow<ShoppingModePreferences> {
        return preferencesRepository.getShoppingModePreferences()
    }
}

// Move item to different section
class MoveItemToSectionUseCase @Inject constructor(
    private val repository: ShoppingListRepository,
    private val sectionRepository: StoreSectionRepository
) {
    suspend operator fun invoke(itemId: Long, sectionName: String): Result<Unit> {
        // Learn the section preference for this item name
        return repository.getShoppingListItem(itemId).first().flatMap { item ->
            repository.moveItemToSection(itemId, sectionName).also {
                // Store preference for future auto-categorization
                sectionRepository.learnItemSection(item.name, sectionName)
            }
        }
    }
}

// Add or update item notes
class AddItemNoteUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long, notes: String): Result<Unit> {
        return repository.updateItemNotes(itemId, notes.trim())
    }
}

// Toggle priority status
class TogglePriorityUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long, isPriority: Boolean): Result<Unit> {
        return repository.toggleItemPriority(itemId, isPriority)
    }
}

// Duplicate item
class DuplicateItemUseCase @Inject constructor(
    private val repository: ShoppingListRepository
) {
    suspend operator fun invoke(itemId: Long): Result<Long> {
        return repository.getShoppingListItem(itemId).first().flatMap { item ->
            val duplicatedItem = item.copy(
                id = 0,
                name = "${item.name} (copy)",
                isManual = true,
                createdAt = System.currentTimeMillis()
            )
            repository.addShoppingListItem(duplicatedItem)
        }
    }
}

// Get shopping history
class GetItemHistoryUseCase @Inject constructor(
    private val repository: ItemHistoryRepository
) {
    operator fun invoke(limit: Int = 100): Flow<Result<List<ItemHistory>>> {
        return repository.getRecentHistory(limit)
    }
    
    fun getFrequentItems(limit: Int = 20): Flow<Result<List<ItemHistory>>> {
        return repository.getFrequentItems(limit)
    }
}

// Save current list as template
class SaveTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    suspend operator fun invoke(name: String, description: String): Result<Long> {
        if (name.isBlank()) {
            return Result.failure(ValidationException("Template name cannot be empty"))
        }
        
        return shoppingListRepository.getShoppingList().first().flatMap { result ->
            result.flatMap { items ->
                templateRepository.saveTemplate(name, description, items)
            }
        }
    }
}

// Load template into current list
class LoadTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    suspend operator fun invoke(templateId: Long): Result<Unit> {
        return templateRepository.getTemplate(templateId).first().flatMap { result ->
            result.flatMap { template ->
                val items = template.items.map { templateItem ->
                    ShoppingListItem(
                        name = templateItem.name,
                        quantity = templateItem.quantity,
                        unit = templateItem.unit,
                        category = templateItem.category,
                        notes = templateItem.notes,
                        isManual = true
                    )
                }
                
                shoppingListRepository.addShoppingListItems(items).also {
                    templateRepository.updateLastUsed(templateId)
                }.map { Unit }
            }
        }
    }
}

// Update price estimate
class UpdatePriceEstimateUseCase @Inject constructor(
    private val repository: ShoppingListRepository,
    private val historyRepository: ItemHistoryRepository
) {
    suspend operator fun invoke(itemId: Long, price: Double?): Result<Unit> {
        return repository.getShoppingListItem(itemId).first().flatMap { item ->
            repository.updateItemPrice(itemId, price).also {
                // Update history average price
                if (price != null) {
                    historyRepository.updateAveragePrice(item.name, price)
                }
            }
        }
    }
}

// Process voice input
class ProcessVoiceInputUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {
    suspend operator fun invoke(voiceText: String): Result<Long> {
        // Parse voice input: "add 2 pounds of chicken"
        val parsed = parseVoiceInput(voiceText)
        
        if (parsed.itemName.isBlank()) {
            return Result.failure(ValidationException("Could not understand item name"))
        }
        
        val item = ShoppingListItem(
            name = parsed.itemName,
            quantity = parsed.quantity,
            unit = parsed.unit,
            category = ItemCategory.OTHER,
            isManual = true
        )
        
        return shoppingListRepository.addShoppingListItem(item)
    }
    
    private data class ParsedVoiceInput(
        val itemName: String,
        val quantity: String = "1",
        val unit: String = ""
    )
    
    private fun parseVoiceInput(text: String): ParsedVoiceInput {
        // Simple parsing logic - can be enhanced with NLP
        val words = text.lowercase().split(" ")
        val addIndex = words.indexOf("add")
        
        if (addIndex == -1) {
            return ParsedVoiceInput(itemName = text.trim())
        }
        
        val afterAdd = words.drop(addIndex + 1)
        val quantity = afterAdd.firstOrNull()?.toIntOrNull()?.toString() ?: "1"
        val unit = if (quantity != "1") afterAdd.getOrNull(1) ?: "" else ""
        val nameStart = if (quantity != "1") 2 else 0
        val itemName = afterAdd.drop(nameStart).joinToString(" ")
        
        return ParsedVoiceInput(
            itemName = itemName,
            quantity = quantity,
            unit = unit
        )
    }
}

// Get suggested items based on meal plan
class GetSuggestedItemsUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val shoppingListRepository: ShoppingListRepository
) {
    suspend operator fun invoke(): Result<List<String>> {
        // Analyze meal plans and suggest complementary items
        // For example: if pasta is in the plan, suggest parmesan cheese
        
        return mealPlanRepository.getCurrentWeekMealPlans().first().map { mealPlans ->
            val suggestions = mutableSetOf<String>()
            
            // Get current shopping list items
            val currentItems = shoppingListRepository.getShoppingList().first()
                .getOrNull()?.map { it.name.lowercase() } ?: emptyList()
            
            // Simple rule-based suggestions
            mealPlans.forEach { plan ->
                // Add complementary item suggestions based on meal types
                when {
                    plan.mealType == MealType.BREAKFAST -> {
                        if ("eggs" !in currentItems) suggestions.add("Eggs")
                        if ("bread" !in currentItems) suggestions.add("Bread")
                    }
                    plan.mealType == MealType.DINNER -> {
                        if ("salt" !in currentItems) suggestions.add("Salt")
                        if ("pepper" !in currentItems) suggestions.add("Pepper")
                    }
                }
            }
            
            suggestions.take(10).toList()
        }
    }
}
```

## Data Models

### Extended Room Entities

```kotlin
@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["is_manual"]),
        Index(value = ["is_checked"]),
        Index(value = ["is_priority"]),
        Index(value = ["store_section"])
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,
    
    @ColumnInfo(name = "is_manual")
    val isManual: Boolean = false,
    
    @ColumnInfo(name = "meal_ids")
    val mealIds: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    // New fields
    @ColumnInfo(name = "notes")
    val notes: String = "",
    
    @ColumnInfo(name = "is_priority")
    val isPriority: Boolean = false,
    
    @ColumnInfo(name = "custom_order")
    val customOrder: Int = 0,
    
    @ColumnInfo(name = "estimated_price")
    val estimatedPrice: Double? = null,
    
    @ColumnInfo(name = "store_section")
    val storeSection: String,
    
    @ColumnInfo(name = "last_modified_at")
    val lastModifiedAt: Long
)

@Entity(
    tableName = "item_history",
    indices = [
        Index(value = ["item_name"]),
        Index(value = ["last_purchased_at"])
    ]
)
data class ItemHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "item_name")
    val itemName: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "purchase_count")
    val purchaseCount: Int,
    
    @ColumnInfo(name = "last_purchased_at")
    val lastPurchasedAt: Long,
    
    @ColumnInfo(name = "average_price")
    val averagePrice: Double? = null
)

@Entity(tableName = "shopping_templates")
data class ShoppingTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    
    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long? = null
)

@Entity(
    tableName = "template_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["template_id"])]
)
data class TemplateItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "template_id")
    val templateId: Long,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "quantity")
    val quantity: String,
    
    @ColumnInfo(name = "unit")
    val unit: String,
    
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "notes")
    val notes: String
)

@Entity(tableName = "store_sections")
data class StoreSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "display_order")
    val displayOrder: Int,
    
    @ColumnInfo(name = "is_collapsed")
    val isCollapsed: Boolean = false,
    
    @ColumnInfo(name = "color")
    val color: String
)
```

## Error Handling

### Exception Types

```kotlin
sealed class ShoppingListException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationException(message: String) : ShoppingListException(message)
    class DatabaseException(message: String, cause: Throwable? = null) : ShoppingListException(message, cause)
    class PermissionException(message: String) : ShoppingListException(message)
    class NetworkException(message: String, cause: Throwable? = null) : ShoppingListException(message, cause)
    class NotFoundException(message: String) : ShoppingListException(message)
}
```

## Testing Strategy

### Unit Tests
- Test all use cases with mock repositories
- Test voice input parsing logic
- Test quantity adjustment edge cases
- Test template save/load operations
- Test budget calculation accuracy

### Integration Tests
- Test Room database migrations for new schema
- Test repository implementations with in-memory database
- Test history tracking across multiple purchases
- Test section reordering persistence

### UI Tests
- Test shopping mode toggle
- Test drag-and-drop reordering
- Test quick add from history
- Test voice input flow
- Test barcode scanning flow

## Performance Considerations

1. **Database Indexing**: Add indices on frequently queried columns (is_priority, store_section)
2. **Lazy Loading**: Load history and templates on-demand
3. **Caching**: Cache frequent items in memory for quick add
4. **Debouncing**: Debounce voice input and search queries
5. **Pagination**: Paginate history list for large datasets

## Security and Privacy

1. **Location Permission**: Request only when location features are enabled
2. **Camera Permission**: Request only for barcode scanning
3. **Microphone Permission**: Request only for voice input
4. **Data Encryption**: Encrypt sensitive data like price estimates
5. **Shared Lists**: Implement proper authentication for collaborative features

## Migration Strategy

### Database Migration

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new columns to shopping_list_items
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN is_priority INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN custom_order INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN estimated_price REAL")
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN store_section TEXT NOT NULL DEFAULT 'OTHER'")
        database.execSQL("ALTER TABLE shopping_list_items ADD COLUMN last_modified_at INTEGER NOT NULL DEFAULT 0")
        
        // Create new tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS item_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                item_name TEXT NOT NULL,
                quantity TEXT NOT NULL,
                unit TEXT NOT NULL,
                category TEXT NOT NULL,
                purchase_count INTEGER NOT NULL,
                last_purchased_at INTEGER NOT NULL,
                average_price REAL
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS shopping_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                last_used_at INTEGER
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS template_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                template_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                quantity TEXT NOT NULL,
                unit TEXT NOT NULL,
                category TEXT NOT NULL,
                notes TEXT NOT NULL,
                FOREIGN KEY(template_id) REFERENCES shopping_templates(id) ON DELETE CASCADE
            )
        """)
        
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS store_sections (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                display_order INTEGER NOT NULL,
                is_collapsed INTEGER NOT NULL DEFAULT 0,
                color TEXT NOT NULL
            )
        """)
        
        // Create indices
        database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_is_priority ON shopping_list_items(is_priority)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_shopping_list_items_store_section ON shopping_list_items(store_section)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_item_history_item_name ON item_history(item_name)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_item_history_last_purchased_at ON item_history(last_purchased_at)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_template_items_template_id ON template_items(template_id)")
    }
}
```

## Future Enhancements

1. **Machine Learning**: Use ML to improve item categorization and suggestions
2. **Store Maps**: Integrate store layout maps for optimal shopping routes
3. **Recipe Integration**: Link shopping items directly to recipe steps
4. **Multi-Store Support**: Track prices across different stores
5. **Loyalty Programs**: Integrate with store loyalty programs for automatic discounts
6. **Nutritional Info**: Display nutritional information for items
7. **Meal Prep Tracking**: Track which meals have been prepared
8. **Expiration Tracking**: Track expiration dates for purchased items
