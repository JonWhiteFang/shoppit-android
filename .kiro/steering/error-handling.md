# Error Handling

## Error Hierarchy

### Sealed AppError Class

Define a sealed class hierarchy for all application errors:

```kotlin
sealed class AppError : Exception() {
    abstract val message: String
    abstract val cause: Throwable?
    
    // Network errors
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    data class NoInternetError(
        override val message: String = "No internet connection",
        override val cause: Throwable? = null
    ) : AppError()
    
    data class TimeoutError(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null
    ) : AppError()
    
    // Database errors
    data class DatabaseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    data class DataNotFoundError(
        override val message: String = "Data not found",
        override val cause: Throwable? = null
    ) : AppError()
    
    // Validation errors
    data class ValidationError(
        override val message: String,
        val field: String? = null,
        override val cause: Throwable? = null
    ) : AppError()
    
    // Business logic errors
    data class BusinessLogicError(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError()
    
    // Unknown errors
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError()
}
```

## Repository Layer Error Handling

### Exception Mapping Pattern

Catch and map exceptions at repository boundaries:

```kotlin
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao,
    private val mealApi: MealApi
) : MealRepository {
    
    override fun getMeals(): Flow<Result<List<Meal>>> = flow {
        try {
            mealDao.getAllMeals()
                .map { entities -> entities.map { it.toMeal() } }
                .collect { meals ->
                    emit(Result.success(meals))
                }
        } catch (e: Exception) {
            emit(Result.failure(mapException(e)))
        }
    }.flowOn(Dispatchers.IO)
    
    override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = mealDao.insertMeal(meal.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    override suspend fun syncMeals(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteMeals = mealApi.getMeals()
            mealDao.insertAll(remoteMeals.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }
    
    private fun mapException(e: Exception): AppError = when (e) {
        is SQLiteConstraintException -> AppError.DatabaseError(
            message = "Failed to save meal: duplicate entry",
            cause = e
        )
        is SQLiteException -> AppError.DatabaseError(
            message = "Database error: ${e.message}",
            cause = e
        )
        is IOException -> AppError.NetworkError(
            message = "Network error: ${e.message}",
            cause = e
        )
        is SocketTimeoutException -> AppError.TimeoutError(cause = e)
        is UnknownHostException -> AppError.NoInternetError(cause = e)
        is HttpException -> when (e.code()) {
            404 -> AppError.DataNotFoundError("Resource not found", e)
            500, 502, 503 -> AppError.NetworkError("Server error", e)
            else -> AppError.NetworkError("HTTP ${e.code()}: ${e.message()}", e)
        }
        else -> AppError.UnknownError(
            message = e.message ?: "An unexpected error occurred",
            cause = e
        )
    }
}
```

### Flow Error Handling

```kotlin
override fun getMealById(id: Long): Flow<Result<Meal>> = flow {
    mealDao.getMealById(id)
        .catch { e ->
            emit(null) // Emit null on error, handle below
        }
        .collect { entity ->
            if (entity != null) {
                emit(Result.success(entity.toMeal()))
            } else {
                emit(Result.failure(AppError.DataNotFoundError("Meal not found")))
            }
        }
}.catch { e ->
    emit(Result.failure(mapException(e)))
}.flowOn(Dispatchers.IO)
```

## Use Case Layer Error Handling

### Validation Before Repository Calls

```kotlin
class AddMealUseCase @Inject constructor(
    private val repository: MealRepository,
    private val validator: MealValidator
) {
    suspend operator fun invoke(meal: Meal): Result<Long> {
        // Validate before calling repository
        val validationResult = validator.validate(meal)
        if (validationResult.isFailure) {
            return validationResult.map { 0L } // Convert Result<Unit> to Result<Long>
        }
        
        // Repository handles its own errors
        return repository.addMeal(meal)
    }
}
```

### Validation Pattern

```kotlin
class MealValidator @Inject constructor() {
    
    fun validate(meal: Meal): Result<Unit> {
        return when {
            meal.name.isBlank() -> Result.failure(
                AppError.ValidationError(
                    message = "Meal name cannot be empty",
                    field = "name"
                )
            )
            meal.name.length > 100 -> Result.failure(
                AppError.ValidationError(
                    message = "Meal name must be less than 100 characters",
                    field = "name"
                )
            )
            meal.ingredients.isEmpty() -> Result.failure(
                AppError.ValidationError(
                    message = "Meal must have at least one ingredient",
                    field = "ingredients"
                )
            )
            else -> Result.success(Unit)
        }
    }
    
    fun validateIngredient(ingredient: Ingredient): Result<Unit> {
        return when {
            ingredient.name.isBlank() -> Result.failure(
                AppError.ValidationError(
                    message = "Ingredient name cannot be empty",
                    field = "ingredient.name"
                )
            )
            ingredient.quantity.isBlank() -> Result.failure(
                AppError.ValidationError(
                    message = "Quantity cannot be empty",
                    field = "ingredient.quantity"
                )
            )
            ingredient.quantity.toDoubleOrNull() == null -> Result.failure(
                AppError.ValidationError(
                    message = "Quantity must be a valid number",
                    field = "ingredient.quantity"
                )
            )
            ingredient.quantity.toDouble() <= 0 -> Result.failure(
                AppError.ValidationError(
                    message = "Quantity must be greater than zero",
                    field = "ingredient.quantity"
                )
            )
            else -> Result.success(Unit)
        }
    }
}
```

## ViewModel Layer Error Handling

### Error State Management

```kotlin
@HiltViewModel
class MealViewModel @Inject constructor(
    private val getMealsUseCase: GetMealsUseCase,
    private val addMealUseCase: AddMealUseCase,
    private val deleteMealUseCase: DeleteMealUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
    
    private val _errorEvent = MutableSharedFlow<ErrorEvent>()
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent.asSharedFlow()
    
    init {
        loadMeals()
    }
    
    fun loadMeals() {
        viewModelScope.launch {
            _uiState.update { MealUiState.Loading }
            
            getMealsUseCase()
                .catch { error ->
                    handleError(error)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { meals ->
                            _uiState.update { MealUiState.Success(meals) }
                        },
                        onFailure = { error ->
                            handleError(error)
                        }
                    )
                }
        }
    }
    
    fun addMeal(meal: Meal) {
        viewModelScope.launch {
            _uiState.update { 
                if (it is MealUiState.Success) {
                    it.copy(isSaving = true)
                } else {
                    it
                }
            }
            
            addMealUseCase(meal).fold(
                onSuccess = { id ->
                    _uiState.update {
                        if (it is MealUiState.Success) {
                            it.copy(isSaving = false)
                        } else {
                            it
                        }
                    }
                    _errorEvent.emit(ErrorEvent.Success("Meal added successfully"))
                },
                onFailure = { error ->
                    _uiState.update {
                        if (it is MealUiState.Success) {
                            it.copy(isSaving = false)
                        } else {
                            it
                        }
                    }
                    handleError(error)
                }
            )
        }
    }
    
    private suspend fun handleError(error: Throwable) {
        val errorMessage = when (error) {
            is AppError.ValidationError -> error.message
            is AppError.NetworkError -> "Network error: ${error.message}"
            is AppError.NoInternetError -> "No internet connection. Please check your network."
            is AppError.TimeoutError -> "Request timed out. Please try again."
            is AppError.DatabaseError -> "Failed to save data. Please try again."
            is AppError.DataNotFoundError -> "Data not found."
            else -> "An unexpected error occurred. Please try again."
        }
        
        _uiState.update { MealUiState.Error(errorMessage) }
        _errorEvent.emit(ErrorEvent.Error(errorMessage))
        
        // Log error for debugging
        Timber.e(error, "Error in MealViewModel")
    }
}

sealed interface ErrorEvent {
    data class Error(val message: String) : ErrorEvent
    data class Success(val message: String) : ErrorEvent
}
```

### UI State with Error Handling

```kotlin
sealed interface MealUiState {
    data object Loading : MealUiState
    
    data class Success(
        val meals: List<Meal>,
        val isSaving: Boolean = false
    ) : MealUiState
    
    data class Error(val message: String) : MealUiState
}

// Alternative: Multiple independent states
data class MealDetailUiState(
    val meal: Meal? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
```

## UI Layer Error Display

### Error Screen Component

```kotlin
@Composable
fun ErrorScreen(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            onRetry?.let {
                Button(
                    onClick = it,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}
```

### Snackbar Error Display

```kotlin
@Composable
fun MealListScreen(
    viewModel: MealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Collect error events
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { event ->
            when (event) {
                is ErrorEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
                is ErrorEvent.Success -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        MealListContent(
            uiState = uiState,
            onRetry = viewModel::loadMeals,
            modifier = Modifier.padding(padding)
        )
    }
}
```

### Inline Error Display

```kotlin
@Composable
fun MealForm(
    meal: Meal,
    onMealChange: (Meal) -> Unit,
    validationErrors: Map<String, String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = meal.name,
            onValueChange = { onMealChange(meal.copy(name = it)) },
            label = { Text("Meal Name") },
            isError = validationErrors.containsKey("name"),
            supportingText = {
                validationErrors["name"]?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // More fields...
    }
}
```

## Retry Strategies

### Exponential Backoff

```kotlin
class RetryStrategy {
    suspend fun <T> retryWithExponentialBackoff(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    return Result.failure(e)
                }
                
                // Only retry on network errors
                if (e !is IOException && e !is SocketTimeoutException) {
                    return Result.failure(e)
                }
                
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return Result.failure(Exception("Max retries exceeded"))
    }
}

// Usage in repository
override suspend fun syncMeals(): Result<Unit> = withContext(Dispatchers.IO) {
    retryStrategy.retryWithExponentialBackoff {
        val remoteMeals = mealApi.getMeals()
        mealDao.insertAll(remoteMeals.map { it.toEntity() })
    }
}
```

### Conditional Retry

```kotlin
fun shouldRetry(error: Throwable): Boolean = when (error) {
    is AppError.NetworkError -> true
    is AppError.TimeoutError -> true
    is AppError.NoInternetError -> false // Don't retry if no internet
    is AppError.ValidationError -> false // Don't retry validation errors
    is AppError.DataNotFoundError -> false // Don't retry 404s
    else -> false
}
```

## Logging

### Structured Error Logging

```kotlin
object ErrorLogger {
    fun log(error: Throwable, context: String = "") {
        when (error) {
            is AppError.ValidationError -> {
                Timber.w(error, "Validation error in $context: ${error.message}")
            }
            is AppError.NetworkError -> {
                Timber.e(error, "Network error in $context: ${error.message}")
            }
            is AppError.DatabaseError -> {
                Timber.e(error, "Database error in $context: ${error.message}")
            }
            else -> {
                Timber.e(error, "Unexpected error in $context: ${error.message}")
            }
        }
        
        // Send to crash reporting service in production
        if (BuildConfig.DEBUG.not()) {
            // Firebase Crashlytics, Sentry, etc.
            // crashlytics.recordException(error)
        }
    }
}

// Usage in ViewModel
private suspend fun handleError(error: Throwable) {
    ErrorLogger.log(error, "MealViewModel")
    // ... rest of error handling
}
```

## Testing Error Handling

### Repository Error Tests

```kotlin
class MealRepositoryImplTest {
    
    @Test
    fun `addMeal returns DatabaseError on SQLiteException`() = runTest {
        // Given
        val meal = Meal(name = "Pasta", ingredients = listOf())
        coEvery { dao.insertMeal(any()) } throws SQLiteException("Constraint violation")
        
        // When
        val result = repository.addMeal(meal)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.DatabaseError)
    }
    
    @Test
    fun `syncMeals returns NetworkError on IOException`() = runTest {
        // Given
        coEvery { api.getMeals() } throws IOException("Connection failed")
        
        // When
        val result = repository.syncMeals()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.NetworkError)
    }
}
```

### ViewModel Error Tests

```kotlin
class MealViewModelTest {
    
    @Test
    fun `displays error state when loading fails`() = runTest {
        // Given
        val error = AppError.NetworkError("Connection failed")
        coEvery { getMealsUseCase() } returns flowOf(Result.failure(error))
        
        // When
        viewModel.loadMeals()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.value
        assertTrue(state is MealUiState.Error)
        assertTrue((state as MealUiState.Error).message.contains("Network error"))
    }
    
    @Test
    fun `emits error event on validation failure`() = runTest {
        // Given
        val meal = Meal(name = "", ingredients = listOf())
        val error = AppError.ValidationError("Name cannot be empty", "name")
        coEvery { addMealUseCase(meal) } returns Result.failure(error)
        
        val events = mutableListOf<ErrorEvent>()
        val job = launch {
            viewModel.errorEvent.collect { events.add(it) }
        }
        
        // When
        viewModel.addMeal(meal)
        advanceUntilIdle()
        
        // Then
        assertTrue(events.any { it is ErrorEvent.Error })
        job.cancel()
    }
}
```

## Best Practices

### Do
- ✅ Catch exceptions at repository boundaries
- ✅ Map exceptions to domain errors
- ✅ Use sealed classes for type-safe error handling
- ✅ Validate input before repository calls
- ✅ Provide user-friendly error messages
- ✅ Log errors with context
- ✅ Use Result<T> for failable operations
- ✅ Handle errors at appropriate layer
- ✅ Test error scenarios thoroughly

### Don't
- ❌ Let exceptions reach the UI layer
- ❌ Use generic Exception types
- ❌ Swallow errors silently
- ❌ Show technical error messages to users
- ❌ Retry non-retryable errors
- ❌ Mix error handling with business logic
- ❌ Use exceptions for control flow
- ❌ Forget to log errors

## Quick Reference

### Error Mapping
```kotlin
Repository: Exception → AppError
Use Case: Validation → AppError.ValidationError
ViewModel: AppError → User-friendly message
UI: Display error message
```

### Error Flow
```kotlin
DataSource (throws Exception)
    ↓
Repository (catches, maps to AppError, returns Result<T>)
    ↓
Use Case (validates, returns Result<T>)
    ↓
ViewModel (handles Result, updates UI state)
    ↓
UI (displays error to user)
```
