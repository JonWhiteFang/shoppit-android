# Design Document

## Overview

The Meal Planning feature enables users to organize meals across a weekly calendar by assigning meals from their library to specific dates and meal types. The design follows Clean Architecture principles with an offline-first approach using Room database, reactive data flow with Kotlin Flow, and clear separation between domain logic and UI concerns. This feature bridges the meal library and shopping list generation, providing the foundation for automated ingredient aggregation.

## Architecture

### Layer Structure

```
com.shoppit.app/
├── ui/
│   └── planner/
│       ├── MealPlannerScreen.kt           # Weekly calendar view
│       ├── MealSelectionDialog.kt         # Meal picker dialog
│       ├── MealPlannerViewModel.kt        # State management
│       └── MealPlannerUiState.kt          # UI state definitions
├── domain/
│   ├── model/
│   │   ├── MealPlan.kt                    # Domain meal plan entity
│   │   └── MealType.kt                    # Meal type enumeration
│   ├── repository/
│   │   └── MealPlanRepository.kt          # Repository interface
│   └── usecase/
│       ├── GetMealPlansForWeekUseCase.kt  # Retrieve week's plans
│       ├── GetMealPlansForDateUseCase.kt  # Retrieve day's plans
│       ├── AssignMealToPlanUseCase.kt     # Create meal assignment
│       ├── UpdateMealPlanUseCase.kt       # Replace meal assignment
│       ├── DeleteMealPlanUseCase.kt       # Remove assignment
│       ├── CopyDayPlansUseCase.kt         # Copy day to another
│       └── ClearDayPlansUseCase.kt        # Clear all day plans
└── data/
    ├── local/
    │   ├── entity/
    │   │   └── MealPlanEntity.kt          # Room entity
    │   ├── dao/
    │   │   └── MealPlanDao.kt             # Data access object
    │   └── database/
    │       └── AppDatabase.kt             # Updated with MealPlanDao
    ├── repository/
    │   └── MealPlanRepositoryImpl.kt      # Repository implementation
    └── mapper/
        └── MealPlanMapper.kt              # Entity/model conversions
```

## Components and Interfaces

### 1. Domain Models

```kotlin
// Meal plan assignment linking meal to date and type
data class MealPlan(
    val id: Long = 0,
    val mealId: Long,
    val date: LocalDate,
    val mealType: MealType,
    val createdAt: Long = System.currentTimeMillis()
)

// Meal type enumeration
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;
    
    fun displayName(): String = when (this) {
        BREAKFAST -> "Breakfast"
        LUNCH -> "Lunch"
        DINNER -> "Dinner"
        SNACK -> "Snack"
    }
}

// Week view data combining plans with meal details
data class WeekPlanData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val plansByDate: Map<LocalDate, List<MealPlanWithMeal>>
)

// Meal plan with associated meal details
data class MealPlanWithMeal(
    val mealPlan: MealPlan,
    val meal: Meal
)
```

### 2. Repository Interface

```kotlin
interface MealPlanRepository {
    // Reactive queries return Flow for real-time updates
    fun getMealPlansForWeek(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<MealPlan>>>
    fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>>
    fun getMealPlanById(id: Long): Flow<Result<MealPlan>>
    
    // Mutations return Result for error handling
    suspend fun addMealPlan(mealPlan: MealPlan): Result<Long>
    suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit>
    suspend fun deleteMealPlan(mealPlanId: Long): Result<Unit>
    suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit>
    
    // Batch operations
    suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<List<Long>>
}
```

### 3. Use Cases

```kotlin
// Get all meal plans for a week with meal details
class GetMealPlansForWeekUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository
) {
    operator fun invoke(startDate: LocalDate): Flow<Result<WeekPlanData>> {
        val endDate = startDate.plusDays(6)
        
        return mealPlanRepository.getMealPlansForWeek(startDate, endDate)
            .combine(mealRepository.getMeals()) { plansResult, mealsResult ->
                plansResult.flatMap { plans ->
                    mealsResult.map { meals ->
                        val mealMap = meals.associateBy { it.id }
                        val plansByDate = plans
                            .mapNotNull { plan ->
                                mealMap[plan.mealId]?.let { meal ->
                                    MealPlanWithMeal(plan, meal)
                                }
                            }
                            .groupBy { it.mealPlan.date }
                        
                        WeekPlanData(startDate, endDate, plansByDate)
                    }
                }
            }
    }
}

// Get meal plans for a specific date
class GetMealPlansForDateUseCase @Inject constructor(
    private val mealPlanRepository: MealPlanRepository,
    private val mealRepository: MealRepository
) {
    operator fun invoke(date: LocalDate): Flow<Result<List<MealPlanWithMeal>>> {
        return mealPlanRepository.getMealPlansForDate(date)
            .combine(mealRepository.getMeals()) { plansResult, mealsResult ->
                plansResult.flatMap { plans ->
                    mealsResult.map { meals ->
                        val mealMap = meals.associateBy { it.id }
                        plans.mapNotNull { plan ->
                            mealMap[plan.mealId]?.let { meal ->
                                MealPlanWithMeal(plan, meal)
                            }
                        }
                    }
                }
            }
    }
}

// Assign a meal to a specific date and meal type
class AssignMealToPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(
        mealId: Long,
        date: LocalDate,
        mealType: MealType
    ): Result<Long> {
        val mealPlan = MealPlan(
            mealId = mealId,
            date = date,
            mealType = mealType
        )
        return repository.addMealPlan(mealPlan)
    }
}

// Update an existing meal plan (replace meal)
class UpdateMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(
        mealPlanId: Long,
        newMealId: Long
    ): Result<Unit> {
        return repository.getMealPlanById(mealPlanId).first().flatMap { mealPlan ->
            val updatedPlan = mealPlan.copy(mealId = newMealId)
            repository.updateMealPlan(updatedPlan)
        }
    }
}

// Delete a meal plan assignment
class DeleteMealPlanUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(mealPlanId: Long): Result<Unit> {
        return repository.deleteMealPlan(mealPlanId)
    }
}

// Copy all meal plans from one day to another
class CopyDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(
        sourceDate: LocalDate,
        targetDate: LocalDate,
        replaceExisting: Boolean = false
    ): Result<Unit> {
        return repository.getMealPlansForDate(sourceDate).first().flatMap { sourcePlans ->
            if (replaceExisting) {
                repository.deleteMealPlansForDate(targetDate).flatMap {
                    copyPlans(sourcePlans, targetDate)
                }
            } else {
                copyPlans(sourcePlans, targetDate)
            }
        }
    }
    
    private suspend fun copyPlans(
        sourcePlans: List<MealPlan>,
        targetDate: LocalDate
    ): Result<Unit> {
        val newPlans = sourcePlans.map { plan ->
            plan.copy(id = 0, date = targetDate)
        }
        return repository.addMealPlans(newPlans).map { Unit }
    }
}

// Clear all meal plans for a specific date
class ClearDayPlansUseCase @Inject constructor(
    private val repository: MealPlanRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<Unit> {
        return repository.deleteMealPlansForDate(date)
    }
}
```

## Data Models

### Room Entities

```kotlin
@Entity(
    tableName = "meal_plans",
    indices = [
        Index(value = ["date"]),
        Index(value = ["meal_id"]),
        Index(value = ["date", "meal_type"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["meal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MealPlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    
    @ColumnInfo(name = "date")
    val date: Long, // Days since epoch
    
    @ColumnInfo(name = "meal_type")
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
```

### Type Converters

```kotlin
class Converters {
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

### Data Access Object

```kotlin
@Dao
interface MealPlanDao {
    @Query("""
        SELECT * FROM meal_plans 
        WHERE date >= :startDate AND date <= :endDate 
        ORDER BY date ASC, meal_type ASC
    """)
    fun getMealPlansForWeek(startDate: Long, endDate: Long): Flow<List<MealPlanEntity>>
    
    @Query("SELECT * FROM meal_plans WHERE date = :date ORDER BY meal_type ASC")
    fun getMealPlansForDate(date: Long): Flow<List<MealPlanEntity>>
    
    @Query("SELECT * FROM meal_plans WHERE id = :mealPlanId")
    fun getMealPlanById(mealPlanId: Long): Flow<MealPlanEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: MealPlanEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlans(mealPlans: List<MealPlanEntity>): List<Long>
    
    @Update
    suspend fun updateMealPlan(mealPlan: MealPlanEntity)
    
    @Query("DELETE FROM meal_plans WHERE id = :mealPlanId")
    suspend fun deleteMealPlanById(mealPlanId: Long)
    
    @Query("DELETE FROM meal_plans WHERE date = :date")
    suspend fun deleteMealPlansForDate(date: Long)
    
    @Query("""
        SELECT meal_id, COUNT(*) as count 
        FROM meal_plans 
        GROUP BY meal_id 
        ORDER BY count DESC
    """)
    fun getMealFrequency(): Flow<List<MealFrequency>>
}

data class MealFrequency(
    @ColumnInfo(name = "meal_id")
    val mealId: Long,
    val count: Int
)
```

### Repository Implementation

```kotlin
class MealPlanRepositoryImpl @Inject constructor(
    private val mealPlanDao: MealPlanDao
) : MealPlanRepository {
    
    override fun getMealPlansForWeek(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<Result<List<MealPlan>>> {
        return mealPlanDao.getMealPlansForWeek(
            startDate.toEpochDay(),
            endDate.toEpochDay()
        )
            .map { entities -> 
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> 
                emit(Result.failure(DatabaseException("Failed to load meal plans", e)))
            }
    }
    
    override fun getMealPlansForDate(date: LocalDate): Flow<Result<List<MealPlan>>> {
        return mealPlanDao.getMealPlansForDate(date.toEpochDay())
            .map { entities -> 
                Result.success(entities.map { it.toDomainModel() })
            }
            .catch { e -> 
                emit(Result.failure(DatabaseException("Failed to load meal plans", e)))
            }
    }
    
    override fun getMealPlanById(id: Long): Flow<Result<MealPlan>> {
        return mealPlanDao.getMealPlanById(id)
            .map { entity ->
                entity?.let { Result.success(it.toDomainModel()) }
                    ?: Result.failure(NotFoundException("Meal plan not found"))
            }
            .catch { e ->
                emit(Result.failure(DatabaseException("Failed to load meal plan", e)))
            }
    }
    
    override suspend fun addMealPlan(mealPlan: MealPlan): Result<Long> {
        return try {
            val id = mealPlanDao.insertMealPlan(mealPlan.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to add meal plan", e))
        }
    }
    
    override suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit> {
        return try {
            mealPlanDao.updateMealPlan(mealPlan.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to update meal plan", e))
        }
    }
    
    override suspend fun deleteMealPlan(mealPlanId: Long): Result<Unit> {
        return try {
            mealPlanDao.deleteMealPlanById(mealPlanId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to delete meal plan", e))
        }
    }
    
    override suspend fun deleteMealPlansForDate(date: LocalDate): Result<Unit> {
        return try {
            mealPlanDao.deleteMealPlansForDate(date.toEpochDay())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to clear day plans", e))
        }
    }
    
    override suspend fun addMealPlans(mealPlans: List<MealPlan>): Result<List<Long>> {
        return try {
            val ids = mealPlanDao.insertMealPlans(mealPlans.map { it.toEntity() })
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(DatabaseException("Failed to add meal plans", e))
        }
    }
}
```

### Mappers

```kotlin
fun MealPlanEntity.toDomainModel(): MealPlan {
    return MealPlan(
        id = id,
        mealId = mealId,
        date = LocalDate.ofEpochDay(date),
        mealType = MealType.valueOf(mealType),
        createdAt = createdAt
    )
}

fun MealPlan.toEntity(): MealPlanEntity {
    return MealPlanEntity(
        id = id,
        mealId = mealId,
        date = date.toEpochDay(),
        mealType = mealType.name,
        createdAt = createdAt
    )
}
```

## UI Layer Design

### UI State Models

```kotlin
// Meal planner screen state
data class MealPlannerUiState(
    val weekData: WeekPlanData? = null,
    val currentWeekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showMealSelection: Boolean = false,
    val selectedSlot: MealSlot? = null,
    val availableMeals: List<Meal> = emptyList()
)

// Represents a slot in the calendar
data class MealSlot(
    val date: LocalDate,
    val mealType: MealType,
    val existingPlan: MealPlan? = null
)
```

### ViewModel

```kotlin
@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val getMealPlansForWeekUseCase: GetMealPlansForWeekUseCase,
    private val getMealsUseCase: GetMealsUseCase,
    private val assignMealToPlanUseCase: AssignMealToPlanUseCase,
    private val updateMealPlanUseCase: UpdateMealPlanUseCase,
    private val deleteMealPlanUseCase: DeleteMealPlanUseCase,
    private val copyDayPlansUseCase: CopyDayPlansUseCase,
    private val clearDayPlansUseCase: ClearDayPlansUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MealPlannerUiState())
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()
    
    init {
        loadWeekData()
        loadAvailableMeals()
    }
    
    private fun loadWeekData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getMealPlansForWeekUseCase(uiState.value.currentWeekStart)
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load meal plans"
                        )
                    }
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { weekData ->
                            _uiState.update { 
                                it.copy(
                                    weekData = weekData,
                                    isLoading = false,
                                    error = null
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = error.message ?: "Failed to load meal plans"
                                )
                            }
                        }
                    )
                }
        }
    }
    
    private fun loadAvailableMeals() {
        viewModelScope.launch {
            getMealsUseCase()
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            _uiState.update { it.copy(availableMeals = meals) }
                        },
                        onFailure = { /* Handle silently, show in meal selection */ }
                    )
                }
        }
    }
    
    fun navigateToNextWeek() {
        _uiState.update { 
            it.copy(currentWeekStart = it.currentWeekStart.plusWeeks(1))
        }
        loadWeekData()
    }
    
    fun navigateToPreviousWeek() {
        _uiState.update { 
            it.copy(currentWeekStart = it.currentWeekStart.minusWeeks(1))
        }
        loadWeekData()
    }
    
    fun navigateToToday() {
        _uiState.update { 
            it.copy(currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY))
        }
        loadWeekData()
    }
    
    fun onSlotClick(date: LocalDate, mealType: MealType) {
        val existingPlan = uiState.value.weekData?.plansByDate?.get(date)
            ?.find { it.mealPlan.mealType == mealType }?.mealPlan
        
        _uiState.update {
            it.copy(
                showMealSelection = true,
                selectedSlot = MealSlot(date, mealType, existingPlan)
            )
        }
    }
    
    fun onMealSelected(mealId: Long) {
        viewModelScope.launch {
            val slot = uiState.value.selectedSlot ?: return@launch
            
            val result = if (slot.existingPlan != null) {
                updateMealPlanUseCase(slot.existingPlan.id, mealId)
            } else {
                assignMealToPlanUseCase(mealId, slot.date, slot.mealType).map { Unit }
            }
            
            result.fold(
                onSuccess = {
                    _uiState.update { 
                        it.copy(
                            showMealSelection = false,
                            selectedSlot = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to assign meal")
                    }
                }
            )
        }
    }
    
    fun dismissMealSelection() {
        _uiState.update { 
            it.copy(
                showMealSelection = false,
                selectedSlot = null
            )
        }
    }
    
    fun deleteMealPlan(mealPlanId: Long) {
        viewModelScope.launch {
            deleteMealPlanUseCase(mealPlanId).fold(
                onSuccess = { /* List updates automatically */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to delete meal plan")
                    }
                }
            )
        }
    }
    
    fun copyDay(sourceDate: LocalDate, targetDate: LocalDate, replace: Boolean) {
        viewModelScope.launch {
            copyDayPlansUseCase(sourceDate, targetDate, replace).fold(
                onSuccess = { /* List updates automatically */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to copy day")
                    }
                }
            )
        }
    }
    
    fun clearDay(date: LocalDate) {
        viewModelScope.launch {
            clearDayPlansUseCase(date).fold(
                onSuccess = { /* List updates automatically */ },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(error = error.message ?: "Failed to clear day")
                    }
                }
            )
        }
    }
}
```


### Screen Composables

```kotlin
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel = hiltViewModel(),
    onMealDetailClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    MealPlannerContent(
        uiState = uiState,
        onSlotClick = viewModel::onSlotClick,
        onMealSelected = viewModel::onMealSelected,
        onDismissMealSelection = viewModel::dismissMealSelection,
        onDeleteMealPlan = viewModel::deleteMealPlan,
        onNextWeek = viewModel::navigateToNextWeek,
        onPreviousWeek = viewModel::navigateToPreviousWeek,
        onToday = viewModel::navigateToToday,
        onCopyDay = viewModel::copyDay,
        onClearDay = viewModel::clearDay,
        onMealDetailClick = onMealDetailClick
    )
}

@Composable
fun MealPlannerContent(
    uiState: MealPlannerUiState,
    onSlotClick: (LocalDate, MealType) -> Unit,
    onMealSelected: (Long) -> Unit,
    onDismissMealSelection: () -> Unit,
    onDeleteMealPlan: (Long) -> Unit,
    onNextWeek: () -> Unit,
    onPreviousWeek: () -> Unit,
    onToday: () -> Unit,
    onCopyDay: (LocalDate, LocalDate, Boolean) -> Unit,
    onClearDay: (LocalDate) -> Unit,
    onMealDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            WeekNavigationBar(
                weekStart = uiState.currentWeekStart,
                weekEnd = uiState.currentWeekStart.plusDays(6),
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
                onToday = onToday
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingScreen()
            uiState.error != null -> ErrorScreen(
                message = uiState.error,
                onRetry = { /* Handled by ViewModel */ }
            )
            uiState.weekData != null -> {
                WeekCalendarView(
                    weekData = uiState.weekData,
                    currentDate = LocalDate.now(),
                    onSlotClick = onSlotClick,
                    onDeleteMealPlan = onDeleteMealPlan,
                    onCopyDay = onCopyDay,
                    onClearDay = onClearDay,
                    onMealDetailClick = onMealDetailClick,
                    modifier = Modifier.padding(padding)
                )
            }
        }
        
        if (uiState.showMealSelection) {
            MealSelectionDialog(
                meals = uiState.availableMeals,
                selectedSlot = uiState.selectedSlot,
                onMealSelected = onMealSelected,
                onDismiss = onDismissMealSelection
            )
        }
    }
}

@Composable
fun WeekCalendarView(
    weekData: WeekPlanData,
    currentDate: LocalDate,
    onSlotClick: (LocalDate, MealType) -> Unit,
    onDeleteMealPlan: (Long) -> Unit,
    onCopyDay: (LocalDate, LocalDate, Boolean) -> Unit,
    onClearDay: (LocalDate) -> Unit,
    onMealDetailClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = (0..6).map { weekData.startDate.plusDays(it.toLong()) }
    
    LazyColumn(modifier = modifier) {
        // Header row with dates
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dates.forEach { date ->
                    DayHeader(
                        date = date,
                        isToday = date == currentDate,
                        planCount = weekData.plansByDate[date]?.size ?: 0,
                        onCopyDay = { targetDate -> onCopyDay(date, targetDate, false) },
                        onClearDay = { onClearDay(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        // Meal type rows
        items(MealType.values()) { mealType ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dates.forEach { date ->
                    val mealPlan = weekData.plansByDate[date]
                        ?.find { it.mealPlan.mealType == mealType }
                    
                    MealSlotCard(
                        date = date,
                        mealType = mealType,
                        mealPlanWithMeal = mealPlan,
                        onClick = { onSlotClick(date, mealType) },
                        onDelete = mealPlan?.let { { onDeleteMealPlan(it.mealPlan.id) } },
                        onViewDetails = mealPlan?.let { { onMealDetailClick(it.meal.id) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MealSlotCard(
    date: LocalDate,
    mealType: MealType,
    mealPlanWithMeal: MealPlanWithMeal?,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onViewDetails: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (mealPlanWithMeal != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mealPlanWithMeal.meal.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        onViewDetails?.let {
                            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "View details",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        onDelete?.let {
                            IconButton(onClick = it, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add meal",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MealSelectionDialog(
    meals: List<Meal>,
    selectedSlot: MealSlot?,
    onMealSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredMeals = remember(meals, searchQuery) {
        if (searchQuery.isBlank()) {
            meals
        } else {
            meals.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedSlot?.existingPlan != null) {
                    "Replace Meal"
                } else {
                    "Select Meal for ${selectedSlot?.mealType?.displayName()}"
                }
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search meals") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (filteredMeals.isEmpty()) {
                    Text(
                        text = if (meals.isEmpty()) {
                            "No meals available. Create meals first."
                        } else {
                            "No meals match your search."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(filteredMeals, key = { it.id }) { meal ->
                            MealSelectionItem(
                                meal = meal,
                                onClick = { onMealSelected(meal.id) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MealSelectionItem(
    meal: Meal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = meal.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${meal.ingredients.size} ingredients",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

## Error Handling

### Exception Types

```kotlin
sealed class MealPlanException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class DatabaseException(message: String, cause: Throwable? = null) : MealPlanException(message, cause)
    class NotFoundException(message: String) : MealPlanException(message)
    class ConflictException(message: String) : MealPlanException(message)
}
```

### Error Mapping

- **DatabaseException** → Display generic "Something went wrong" message, log details
- **NotFoundException** → Display "Meal plan not found" message
- **ConflictException** → Display "A meal is already planned for this slot" message
- **Unknown exceptions** → Display generic error, log full stack trace

## Testing Strategy

### Unit Tests

```kotlin
// Use case tests with fake repositories
class AssignMealToPlanUseCaseTest {
    private lateinit var repository: FakeMealPlanRepository
    private lateinit var useCase: AssignMealToPlanUseCase
    
    @Test
    fun `assigns meal to plan successfully`() = runTest {
        val result = useCase(
            mealId = 1L,
            date = LocalDate.now(),
            mealType = MealType.LUNCH
        )
        assertTrue(result.isSuccess)
    }
}

// ViewModel tests with fake use cases
class MealPlannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var getMealPlansUseCase: FakeGetMealPlansForWeekUseCase
    private lateinit var viewModel: MealPlannerViewModel
    
    @Test
    fun `loads week data successfully`() = runTest {
        val weekData = WeekPlanData(
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(6),
            plansByDate = emptyMap()
        )
        getMealPlansUseCase.setWeekData(weekData)
        
        viewModel = MealPlannerViewModel(/* ... */)
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(weekData, state.weekData)
    }
}
```

### Instrumented Tests

```kotlin
// DAO tests with in-memory database
@RunWith(AndroidJUnit4::class)
class MealPlanDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var mealPlanDao: MealPlanDao
    
    @Test
    fun insertAndRetrieveMealPlan() = runTest {
        val mealPlan = MealPlanEntity(
            mealId = 1L,
            date = LocalDate.now().toEpochDay(),
            mealType = "LUNCH",
            createdAt = System.currentTimeMillis()
        )
        mealPlanDao.insertMealPlan(mealPlan)
        
        val plans = mealPlanDao.getMealPlansForDate(LocalDate.now().toEpochDay()).first()
        assertEquals(1, plans.size)
        assertEquals("LUNCH", plans[0].mealType)
    }
    
    @Test
    fun uniqueConstraintPreventsDoubleBooking() = runTest {
        val date = LocalDate.now().toEpochDay()
        val plan1 = MealPlanEntity(mealId = 1L, date = date, mealType = "LUNCH", createdAt = 0L)
        val plan2 = MealPlanEntity(mealId = 2L, date = date, mealType = "LUNCH", createdAt = 0L)
        
        mealPlanDao.insertMealPlan(plan1)
        mealPlanDao.insertMealPlan(plan2) // Should replace plan1
        
        val plans = mealPlanDao.getMealPlansForDate(date).first()
        assertEquals(1, plans.size)
        assertEquals(2L, plans[0].mealId)
    }
}
```

## Performance Considerations

### Database Optimization
- Index on date for fast week queries
- Index on meal_id for cascade delete performance
- Unique index on (date, meal_type) prevents double booking
- Foreign key cascade delete automatically removes plans when meal is deleted

### UI Performance
- LazyColumn for efficient scrolling
- Stable keys for meal items
- Remember expensive date calculations
- Debounce search input

### Memory Management
- Flow-based reactive queries prevent memory leaks
- Proper lifecycle management in ViewModels
- Cancel coroutines on ViewModel clear
- Limit week range to prevent excessive data loading

## Navigation Integration

```kotlin
sealed class Screen(val route: String) {
    object MealPlanner : Screen("meal_planner")
    object MealDetail : Screen("meal_detail/{mealId}") {
        fun createRoute(mealId: Long) = "meal_detail/$mealId"
    }
}

// In NavHost
composable(Screen.MealPlanner.route) {
    MealPlannerScreen(
        onMealDetailClick = { mealId ->
            navController.navigate(Screen.MealDetail.createRoute(mealId))
        }
    )
}
```

## Dependency Injection

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class MealPlanRepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMealPlanRepository(
        impl: MealPlanRepositoryImpl
    ): MealPlanRepository
}

// Database migration
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS meal_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                meal_id INTEGER NOT NULL,
                date INTEGER NOT NULL,
                meal_type TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(meal_id) REFERENCES meals(id) ON DELETE CASCADE
            )
        """)
        
        database.execSQL("""
            CREATE INDEX index_meal_plans_date ON meal_plans(date)
        """)
        
        database.execSQL("""
            CREATE INDEX index_meal_plans_meal_id ON meal_plans(meal_id)
        """)
        
        database.execSQL("""
            CREATE UNIQUE INDEX index_meal_plans_date_meal_type 
            ON meal_plans(date, meal_type)
        """)
    }
}
```

## Future Enhancements

### Meal Frequency Tracking
- Track how often meals are planned
- Sort meal selection by frequency
- Show "favorite" badge on frequently planned meals

### Meal Suggestions
- Suggest meals based on planning history
- Recommend meals for empty slots
- Balance meal variety across the week

### Bulk Operations
- Copy entire week to another week
- Template weeks for recurring patterns
- Quick-fill week with random meals

### Calendar Integration
- Export meal plans to device calendar
- Import events from calendar
- Sync with external calendar apps
