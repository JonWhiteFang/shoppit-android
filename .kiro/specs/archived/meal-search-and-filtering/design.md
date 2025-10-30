# Design Document

## Overview

The Meal Search and Filtering feature enhances the existing meal list functionality by adding real-time search capabilities and tag-based filtering. The design follows Shoppit's Clean Architecture pattern with MVVM, maintaining offline-first principles and ensuring all operations are performed on locally cached data.

This feature extends the existing `MealListScreen` and `MealViewModel` to include search and filter UI components, while adding new domain logic for filtering meals based on search queries and tags.

## Architecture

### Layer Responsibilities

**Domain Layer:**
- Add `tags` property to `Meal` model
- Create `SearchMealsUseCase` for filtering meals by search query
- Create `FilterMealsByTagsUseCase` for filtering meals by tags
- Define `MealTag` enum for predefined meal categories

**Data Layer:**
- Update `MealEntity` to include tags
- Update `MealDao` to support tag queries (optional optimization)
- Update repository to handle tag persistence

**UI Layer:**
- Extend `MealViewModel` to manage search and filter state
- Update `MealListScreen` to include search bar and filter chips
- Create `SearchBar` composable component
- Create `FilterChipRow` composable component
- Update `MealListUiState` to include search and filter information

### Data Flow

```
User Input (Search/Filter) 
  → ViewModel updates search/filter state
  → Use cases filter meal list
  → Repository provides all meals via Flow
  → Use cases apply filters in memory
  → ViewModel updates UI state
  → UI recomposes with filtered results
```

## Components and Interfaces

### Domain Models

#### Updated Meal Model

```kotlin
data class Meal(
    val id: Long = 0,
    val name: String,
    val ingredients: List<Ingredient>,
    val notes: String = "",
    val tags: Set<MealTag> = emptySet(), // NEW
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### MealTag Enum

```kotlin
enum class MealTag(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACK("Snack"),
    VEGETARIAN("Vegetarian"),
    VEGAN("Vegan"),
    GLUTEN_FREE("Gluten Free"),
    DAIRY_FREE("Dairy Free"),
    QUICK("Quick (<30 min)"),
    HEALTHY("Healthy"),
    COMFORT_FOOD("Comfort Food"),
    DESSERT("Dessert")
}
```

### Use Cases

#### SearchMealsUseCase

```kotlin
class SearchMealsUseCase @Inject constructor() {
    operator fun invoke(meals: List<Meal>, query: String): List<Meal> {
        if (query.isBlank()) return meals
        
        val normalizedQuery = query.trim().lowercase()
        
        return meals.filter { meal ->
            // Search by meal name
            meal.name.lowercase().contains(normalizedQuery) ||
            // Search by ingredient names
            meal.ingredients.any { ingredient ->
                ingredient.name.lowercase().contains(normalizedQuery)
            }
        }
    }
}
```

#### FilterMealsByTagsUseCase

```kotlin
class FilterMealsByTagsUseCase @Inject constructor() {
    operator fun invoke(meals: List<Meal>, tags: Set<MealTag>): List<Meal> {
        if (tags.isEmpty()) return meals
        
        return meals.filter { meal ->
            // Meal must have ALL selected tags
            tags.all { tag -> meal.tags.contains(tag) }
        }
    }
}
```

### ViewModel Updates

#### Extended MealViewModel

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val deleteMealUseCase: DeleteMealUseCase,
    private val searchMealsUseCase: SearchMealsUseCase, // NEW
    private val filterMealsByTagsUseCase: FilterMealsByTagsUseCase, // NEW
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<MealListUiState>(MealListUiState.Loading)
    val uiState: StateFlow<MealListUiState> = _uiState.asStateFlow()
    
    // Search query state
    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY) ?: ""
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Selected tags state
    private val _selectedTags = MutableStateFlow<Set<MealTag>>(
        savedStateHandle.get<Set<MealTag>>(KEY_SELECTED_TAGS) ?: emptySet()
    )
    val selectedTags: StateFlow<Set<MealTag>> = _selectedTags.asStateFlow()
    
    // All meals from repository (unfiltered)
    private var allMeals: List<Meal> = emptyList()
    
    init {
        loadMeals()
    }
    
    private fun loadMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .catch { error ->
                    _uiState.update { 
                        MealListUiState.Error(error.message ?: "Unknown error")
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            allMeals = meals
                            applyFilters()
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                MealListUiState.Error(error.message ?: "Failed to load meals")
                            }
                        }
                    )
                }
        }
    }
    
    private fun applyFilters() {
        val query = _searchQuery.value
        val tags = _selectedTags.value
        
        // Apply search filter
        val searchFiltered = searchMealsUseCase(allMeals, query)
        
        // Apply tag filter
        val tagFiltered = filterMealsByTagsUseCase(searchFiltered, tags)
        
        _uiState.update { 
            MealListUiState.Success(
                meals = tagFiltered,
                totalCount = allMeals.size,
                filteredCount = tagFiltered.size,
                isFiltered = query.isNotBlank() || tags.isNotEmpty()
            )
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
        applyFilters()
    }
    
    fun toggleTag(tag: MealTag) {
        val currentTags = _selectedTags.value.toMutableSet()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _selectedTags.value = currentTags
        savedStateHandle[KEY_SELECTED_TAGS] = currentTags
        applyFilters()
    }
    
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptySet()
        savedStateHandle[KEY_SEARCH_QUERY] = ""
        savedStateHandle[KEY_SELECTED_TAGS] = emptySet<MealTag>()
        applyFilters()
    }
}
```

#### Updated MealListUiState

```kotlin
sealed interface MealListUiState {
    data object Loading : MealListUiState
    
    data class Success(
        val meals: List<Meal>,
        val totalCount: Int = meals.size, // NEW
        val filteredCount: Int = meals.size, // NEW
        val isFiltered: Boolean = false // NEW
    ) : MealListUiState
    
    data class Error(val message: String) : MealListUiState
}
```

### UI Components

#### SearchBar Component

```kotlin
@Composable
fun MealSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search meals or ingredients...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}
```

#### FilterChipRow Component

```kotlin
@Composable
fun FilterChipRow(
    selectedTags: Set<MealTag>,
    onTagToggle: (MealTag) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(MealTag.values()) { tag ->
            FilterChip(
                selected = selectedTags.contains(tag),
                onClick = { onTagToggle(tag) },
                label = { Text(tag.displayName) },
                leadingIcon = if (selectedTags.contains(tag)) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}
```

#### ResultsHeader Component

```kotlin
@Composable
fun ResultsHeader(
    totalCount: Int,
    filteredCount: Int,
    isFiltered: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isFiltered) {
                "$filteredCount of $totalCount meals"
            } else {
                "$totalCount meal${if (totalCount != 1) "s" else ""}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (isFiltered) {
            TextButton(onClick = onClearFilters) {
                Text("Clear filters")
            }
        }
    }
}
```

#### Updated MealListScreen Layout

```kotlin
@Composable
fun MealListContent(
    uiState: MealListUiState,
    searchQuery: String,
    selectedTags: Set<MealTag>,
    onSearchQueryChange: (String) -> Unit,
    onTagToggle: (MealTag) -> Unit,
    onClearFilters: () -> Unit,
    onMealClick: (Long) -> Unit,
    onAddMealClick: () -> Unit,
    onDeleteMeal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMealClick) {
                Icon(Icons.Default.Add, contentDescription = "Add meal")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search bar
            MealSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(16.dp)
            )
            
            // Filter chips
            FilterChipRow(
                selectedTags = selectedTags,
                onTagToggle = onTagToggle,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Content based on state
            when (uiState) {
                is MealListUiState.Loading -> LoadingScreen()
                is MealListUiState.Success -> {
                    if (uiState.meals.isEmpty() && !uiState.isFiltered) {
                        EmptyState(
                            message = "No meals yet. Add your first meal!",
                            actionLabel = "Add Meal",
                            onActionClick = onAddMealClick
                        )
                    } else if (uiState.meals.isEmpty() && uiState.isFiltered) {
                        EmptyState(
                            message = "No meals match your search or filters",
                            actionLabel = "Clear Filters",
                            onActionClick = onClearFilters
                        )
                    } else {
                        Column {
                            ResultsHeader(
                                totalCount = uiState.totalCount,
                                filteredCount = uiState.filteredCount,
                                isFiltered = uiState.isFiltered,
                                onClearFilters = onClearFilters
                            )
                            MealList(
                                meals = uiState.meals,
                                onMealClick = onMealClick,
                                onDeleteMeal = onDeleteMeal
                            )
                        }
                    }
                }
                is MealListUiState.Error -> ErrorScreen(message = uiState.message)
            }
        }
    }
}
```

## Data Models

### Updated Room Entity

```kotlin
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val notes: String,
    val tags: String, // Comma-separated tag names: "BREAKFAST,VEGETARIAN"
    val createdAt: Long,
    val updatedAt: Long
)
```

### Type Converters

```kotlin
class MealTagConverter {
    @TypeConverter
    fun fromTagSet(tags: Set<MealTag>): String {
        return tags.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toTagSet(tagsString: String): Set<MealTag> {
        if (tagsString.isBlank()) return emptySet()
        return tagsString.split(",")
            .mapNotNull { tagName ->
                try {
                    MealTag.valueOf(tagName)
                } catch (e: IllegalArgumentException) {
                    null // Ignore invalid tags
                }
            }
            .toSet()
    }
}
```

### Mapper Updates

```kotlin
// Entity to Model
fun MealEntity.toMeal(ingredients: List<Ingredient>): Meal {
    return Meal(
        id = id,
        name = name,
        ingredients = ingredients,
        notes = notes,
        tags = MealTagConverter().toTagSet(tags), // NEW
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// Model to Entity
fun Meal.toEntity(): MealEntity {
    return MealEntity(
        id = id,
        name = name,
        notes = notes,
        tags = MealTagConverter().fromTagSet(tags), // NEW
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
```

## Error Handling

### Search and Filter Errors

Since search and filtering are performed in-memory on already-loaded data, errors are minimal:

1. **Empty Results**: Display appropriate empty state message
2. **Invalid Search Query**: Accept all input, no validation needed
3. **Database Load Failure**: Handled by existing error state in ViewModel

### Performance Considerations

- Search and filter operations run on UI thread (fast for <1000 meals)
- If performance becomes an issue, move filtering to background thread
- Consider debouncing search input if typing causes lag

## Testing Strategy

### Unit Tests

**Domain Layer:**
- `SearchMealsUseCaseTest`: Test search by name and ingredients
- `FilterMealsByTagsUseCaseTest`: Test single and multiple tag filtering
- `MealTagConverterTest`: Test tag serialization/deserialization

**ViewModel:**
- `MealViewModelTest`: Test search query updates
- `MealViewModelTest`: Test tag selection/deselection
- `MealViewModelTest`: Test combined search and filter
- `MealViewModelTest`: Test clear filters functionality
- `MealViewModelTest`: Test state preservation with SavedStateHandle

### UI Tests

**Composable Tests:**
- `MealSearchBarTest`: Test input and clear button
- `FilterChipRowTest`: Test chip selection
- `ResultsHeaderTest`: Test count display and clear button
- `MealListScreenTest`: Test search and filter integration

### Integration Tests

- Test full flow: load meals → search → filter → clear
- Test state preservation across configuration changes
- Test empty states for no meals vs no results

## Accessibility

### Content Descriptions

- Search bar: "Search meals or ingredients"
- Clear search button: "Clear search"
- Filter chips: "[Tag name] filter, [selected/not selected]"
- Clear filters button: "Clear all filters"
- Results count: Announced via live region

### Keyboard Navigation

- Tab through search bar → filter chips → meal list
- Enter to select/deselect filter chips
- Escape to clear search

### Screen Reader Announcements

- Announce result count changes when filters applied
- Announce "No results found" when search/filter yields empty list
- Announce "Filters cleared" when clear button pressed

## Migration Strategy

### Database Migration

Add migration to update `meals` table with `tags` column:

```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE meals ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}
```

### Backward Compatibility

- Existing meals will have empty tag set
- Users can add tags when editing meals
- Search and filter work immediately with existing data

## Performance Optimization

### Search Optimization

- Use `contains()` for simple substring matching (fast enough for <1000 meals)
- Consider indexing if meal library grows beyond 1000 items
- Debounce search input with 300ms delay to reduce recompositions

### Filter Optimization

- Filter operations are O(n) where n = number of meals
- Tag filtering uses Set operations (O(1) lookup)
- Combined search + filter: search first (more selective), then filter

### Memory Optimization

- Keep single copy of all meals in ViewModel
- Filtered results are new list (acceptable for <1000 items)
- Use `key` parameter in LazyColumn for efficient recomposition

## Future Enhancements

1. **Custom Tags**: Allow users to create custom tags
2. **Tag Management**: UI for managing available tags
3. **Search History**: Remember recent searches
4. **Advanced Filters**: Filter by ingredient count, creation date
5. **Sort Options**: Sort by name, date, ingredient count
6. **Fuzzy Search**: Tolerate typos in search queries
7. **Search Suggestions**: Auto-complete based on meal/ingredient names
