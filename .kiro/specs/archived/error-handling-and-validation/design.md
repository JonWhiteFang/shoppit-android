# Design Document: Error Handling and Validation

## Overview

This design document outlines the comprehensive error handling and validation system for the Shoppit Android application. The system builds upon existing error handling infrastructure (AppError, ErrorMapper, validators) and extends it to provide consistent, user-friendly error management across all layers of the application.

### Goals

1. **Consistency**: Uniform error handling patterns across all features
2. **User-Friendliness**: Clear, actionable error messages for users
3. **Robustness**: Graceful degradation and recovery from errors
4. **Developer Experience**: Easy-to-use validation and error handling APIs
5. **Observability**: Comprehensive error logging for debugging

### Current State Analysis

The application already has:
- ✅ `AppError` sealed class hierarchy for typed errors
- ✅ `ErrorMapper` for converting errors to user messages
- ✅ `MealValidator` with `ValidationResult` pattern
- ✅ `ErrorScreen` and `ErrorDialog` composables
- ✅ Basic error handling in ViewModels

**Gaps to Address:**
- ❌ Inconsistent validation across different features
- ❌ Missing inline validation error display in forms
- ❌ No centralized error logging
- ❌ Incomplete error recovery strategies
- ❌ Missing success message feedback
- ❌ No ingredient-level validation

## Architecture

### Layer Responsibilities

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  - Display error messages (inline, snackbar, dialog)        │
│  - Show loading/saving states                               │
│  - Provide retry actions                                    │
│  - Collect validation errors from ViewModel                 │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    ViewModel Layer                           │
│  - Manage UI state (loading, error, success)                │
│  - Handle Result<T> from use cases                          │
│  - Map errors to UI-friendly messages                       │
│  - Emit error events for snackbars                          │
│  - Track validation errors per field                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer                             │
│  - Validate input before repository calls                   │
│  - Return ValidationResult or Result<T>                     │
│  - Define business rules                                    │
│  - No exception handling (uses Result)                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Repository Layer                           │
│  - Catch all exceptions from data sources                   │
│  - Map exceptions to AppError types                         │
│  - Return Result<T> (never throw)                           │
│  - Log errors with context                                  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Data Sources                              │
│  - Room DAOs (can throw SQLiteException)                    │
│  - Retrofit APIs (can throw IOException)                    │
│  - May throw any exception                                  │
└─────────────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. Enhanced AppError (Existing - Minor Updates)

**Location**: `domain/error/AppError.kt`

**Updates Needed**:
- Add `field` parameter to `ValidationError` for field-specific errors
- Add `cause` parameter to all error types for exception chaining

```kotlin
sealed class AppError : Exception() {
    abstract val message: String
    abstract val cause: Throwable?
    
    data class NetworkError(
        override val message: String = "Network connection failed",
        override val cause: Throwable? = null
    ) : AppError()
    
    data class DatabaseError(
        override val message: String = "Database error occurred",
        override val cause: Throwable? = null
    ) : AppError()
    
    data class ValidationError(
        override val message: String,
        val field: String? = null,  // NEW: Field identifier
        override val cause: Throwable? = null
    ) : AppError()
    
    // ... other error types with cause parameter
}
```

### 2. Enhanced Validators

**Location**: `domain/validator/`

**New Components**:

#### IngredientValidator
```kotlin
class IngredientValidator @Inject constructor() : DataValidator<Ingredient> {
    override fun validate(data: Ingredient): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Name validation
        if (data.name.isBlank()) {
            errors.add(ValidationError(
                field = "name",
                message = "Ingredient name cannot be empty",
                code = ValidationError.CODE_REQUIRED
            ))
        }
        
        // Quantity validation
        if (data.quantity.isBlank()) {
            errors.add(ValidationError(
                field = "quantity",
                message = "Quantity cannot be empty",
                code = ValidationError.CODE_REQUIRED
            ))
        } else {
            val quantityValue = data.quantity.toDoubleOrNull()
            if (quantityValue == null) {
                errors.add(ValidationError(
                    field = "quantity",
                    message = "Quantity must be a valid number",
                    code = ValidationError.CODE_INVALID_FORMAT
                ))
            } else if (quantityValue <= 0) {
                errors.add(ValidationError(
                    field = "quantity",
                    message = "Quantity must be greater than zero",
                    code = ValidationError.CODE_OUT_OF_RANGE
                ))
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
```

#### MealPlanValidator
```kotlin
class MealPlanValidator @Inject constructor() : DataValidator<MealPlan> {
    override fun validate(data: MealPlan): ValidationResult {
        val errors = mutableListOf<ValidationError>()
        
        // Date validation
        if (data.date < 0) {
            errors.add(ValidationError(
                field = "date",
                message = "Invalid date",
                code = ValidationError.CODE_INVALID_VALUE
            ))
        }
        
        // Meal type validation
        if (data.mealType.isBlank()) {
            errors.add(ValidationError(
                field = "mealType",
                message = "Meal type is required",
                code = ValidationError.CODE_REQUIRED
            ))
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}
```

### 3. Error Logger

**Location**: `domain/error/ErrorLogger.kt`

**Purpose**: Centralized error logging with context

```kotlin
interface ErrorLogger {
    fun logError(error: Throwable, context: String, additionalData: Map<String, Any> = emptyMap())
    fun logWarning(message: String, context: String)
    fun logInfo(message: String, context: String)
}

class ErrorLoggerImpl @Inject constructor() : ErrorLogger {
    override fun logError(error: Throwable, context: String, additionalData: Map<String, Any>) {
        when (error) {
            is AppError.ValidationError -> {
                Timber.w(error, "[$context] Validation error: ${error.message}")
            }
            is AppError.NetworkError -> {
                Timber.e(error, "[$context] Network error: ${error.message}")
            }
            is AppError.DatabaseError -> {
                Timber.e(error, "[$context] Database error: ${error.message}")
            }
            else -> {
                Timber.e(error, "[$context] Unexpected error: ${error.message}")
            }
        }
        
        // In production, send to crash reporting
        if (!BuildConfig.DEBUG) {
            // Firebase Crashlytics or similar
            // crashlytics.recordException(error)
            // crashlytics.setCustomKeys(additionalData)
        }
    }
    
    override fun logWarning(message: String, context: String) {
        Timber.w("[$context] $message")
    }
    
    override fun logInfo(message: String, context: String) {
        Timber.i("[$context] $message")
    }
}
```

### 4. Enhanced ViewModel Error Handling

**Pattern**: All ViewModels should follow this pattern

```kotlin
@HiltViewModel
class ExampleViewModel @Inject constructor(
    private val useCase: ExampleUseCase,
    private val errorLogger: ErrorLogger
) : ViewModel() {
    
    // UI State with error tracking
    private val _uiState = MutableStateFlow(ExampleUiState())
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()
    
    // Error events for snackbars (one-time events)
    private val _errorEvent = MutableSharedFlow<ErrorEvent>()
    val errorEvent: SharedFlow<ErrorEvent> = _errorEvent.asSharedFlow()
    
    fun performAction() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            useCase().fold(
                onSuccess = { data ->
                    _uiState.update { it.copy(isLoading = false, data = data) }
                    _errorEvent.emit(ErrorEvent.Success("Action completed"))
                },
                onFailure = { error ->
                    handleError(error, "performAction")
                }
            )
        }
    }
    
    private suspend fun handleError(error: Throwable, context: String) {
        errorLogger.logError(error, "ExampleViewModel.$context")
        
        val userMessage = ErrorMapper.toUserMessage(error)
        
        _uiState.update { it.copy(isLoading = false, error = userMessage) }
        _errorEvent.emit(ErrorEvent.Error(userMessage))
    }
}

sealed interface ErrorEvent {
    data class Error(val message: String) : ErrorEvent
    data class Success(val message: String) : ErrorEvent
}
```

### 5. UI Components

#### Enhanced Form Field with Inline Errors

**Location**: `presentation/ui/common/FormField.kt`

```kotlin
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let {
            { Text(text = it, color = MaterialTheme.colorScheme.error) }
        },
        enabled = enabled,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth()
    )
}
```

#### Snackbar Handler

**Location**: `presentation/ui/common/SnackbarHandler.kt`

```kotlin
@Composable
fun ErrorSnackbarHandler(
    errorEventFlow: SharedFlow<ErrorEvent>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(Unit) {
        errorEventFlow.collect { event ->
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
}
```

#### Loading Overlay

**Location**: `presentation/ui/common/LoadingOverlay.kt`

```kotlin
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .clickable(enabled = false) { },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
```

## Data Models

### UI State Pattern

```kotlin
// For screens with mutually exclusive states
sealed interface ScreenUiState {
    data object Loading : ScreenUiState
    data class Success(val data: Data) : ScreenUiState
    data class Error(val message: String) : ScreenUiState
}

// For forms with multiple independent states
data class FormUiState(
    val data: FormData = FormData(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val validationErrors: Map<String, String> = emptyMap()
)
```

### Validation Error Mapping

```kotlin
// Extension to convert ValidationResult to field-error map
fun ValidationResult.toFieldErrorMap(): Map<String, String> {
    return when (this) {
        is ValidationResult.Valid -> emptyMap()
        is ValidationResult.Invalid -> {
            validationErrors.associate { it.field to it.message }
        }
    }
}
```

## Error Handling Strategy

### Repository Layer Pattern

```kotlin
class ExampleRepositoryImpl @Inject constructor(
    private val dao: ExampleDao,
    private val errorLogger: ErrorLogger
) : ExampleRepository {
    
    override fun getData(): Flow<Result<List<Data>>> = flow {
        dao.getAll()
            .map { entities -> entities.map { it.toDomain() } }
            .collect { data ->
                emit(Result.success(data))
            }
    }.catch { e ->
        errorLogger.logError(e, "ExampleRepository.getData")
        emit(Result.failure(mapException(e)))
    }.flowOn(Dispatchers.IO)
    
    override suspend fun saveData(data: Data): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val id = dao.insert(data.toEntity())
            Result.success(id)
        } catch (e: Exception) {
            errorLogger.logError(e, "ExampleRepository.saveData")
            Result.failure(mapException(e))
        }
    }
    
    private fun mapException(e: Throwable): AppError {
        return when (e) {
            is SQLiteConstraintException -> AppError.DatabaseError(
                message = "Failed to save: duplicate entry",
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
            is SocketTimeoutException -> AppError.NetworkError(
                message = "Request timed out",
                cause = e
            )
            else -> AppError.UnknownError(
                message = e.message ?: "An unexpected error occurred"
            )
        }
    }
}
```

### Use Case Layer Pattern

```kotlin
class ExampleUseCase @Inject constructor(
    private val repository: ExampleRepository,
    private val validator: ExampleValidator
) {
    suspend operator fun invoke(data: Data): Result<Long> {
        // Validate first
        val validationResult = validator.validate(data)
        if (validationResult.isInvalid()) {
            val errors = validationResult.getErrors()
            val message = errors.joinToString("; ") { "${it.field}: ${it.message}" }
            return Result.failure(AppError.ValidationError(message))
        }
        
        // Proceed with repository call
        return repository.saveData(data)
    }
}
```

## Testing Strategy

### Unit Tests

#### Validator Tests
```kotlin
class MealValidatorTest {
    private lateinit var validator: MealValidator
    
    @Before
    fun setup() {
        validator = MealValidator()
    }
    
    @Test
    fun `validate returns Invalid when meal name is empty`() {
        val meal = Meal(name = "", ingredients = listOf())
        val result = validator.validate(meal)
        
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertTrue(errors.any { it.field == "name" && it.code == ValidationError.CODE_REQUIRED })
    }
    
    @Test
    fun `validate returns Invalid when meal has no ingredients`() {
        val meal = Meal(name = "Pasta", ingredients = emptyList())
        val result = validator.validate(meal)
        
        assertTrue(result.isInvalid())
        val errors = result.getErrors()
        assertTrue(errors.any { it.field == "ingredients" })
    }
    
    @Test
    fun `validate returns Valid for valid meal`() {
        val meal = Meal(
            name = "Pasta",
            ingredients = listOf(Ingredient("Tomato", "2", "pcs"))
        )
        val result = validator.validate(meal)
        
        assertTrue(result.isValid())
    }
}
```

#### ViewModel Error Handling Tests
```kotlin
class ExampleViewModelTest {
    @Test
    fun `displays error state when operation fails`() = runTest {
        val error = AppError.NetworkError("Connection failed")
        coEvery { useCase() } returns Result.failure(error)
        
        viewModel.performAction()
        advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Network"))
    }
    
    @Test
    fun `emits error event on failure`() = runTest {
        val error = AppError.ValidationError("Invalid data")
        coEvery { useCase() } returns Result.failure(error)
        
        val events = mutableListOf<ErrorEvent>()
        val job = launch {
            viewModel.errorEvent.collect { events.add(it) }
        }
        
        viewModel.performAction()
        advanceUntilIdle()
        
        assertTrue(events.any { it is ErrorEvent.Error })
        job.cancel()
    }
}
```

### Integration Tests

#### Repository Error Mapping Tests
```kotlin
class ExampleRepositoryImplTest {
    @Test
    fun `maps SQLiteException to DatabaseError`() = runTest {
        coEvery { dao.insert(any()) } throws SQLiteException("Constraint violation")
        
        val result = repository.saveData(testData)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.DatabaseError)
    }
}
```

### UI Tests

#### Form Validation Tests
```kotlin
@Test
fun `displays inline error when meal name is empty`() {
    composeTestRule.setContent {
        AddEditMealScreen(viewModel = viewModel)
    }
    
    // Clear name field
    composeTestRule.onNodeWithTag("meal_name_field").performTextClearance()
    
    // Try to save
    composeTestRule.onNodeWithTag("save_button").performClick()
    
    // Verify error is displayed
    composeTestRule.onNodeWithText("Meal name cannot be empty").assertIsDisplayed()
}
```

## Implementation Phases

### Phase 1: Core Infrastructure (Priority: High)
1. Update AppError with field and cause parameters
2. Create ErrorLogger interface and implementation
3. Create IngredientValidator
4. Update MealValidator to use IngredientValidator for ingredient validation
5. Create ValidationResult extension functions

### Phase 2: UI Components (Priority: High)
1. Create ValidatedTextField component
2. Create ErrorSnackbarHandler component
3. Create LoadingOverlay component
4. Update ErrorScreen with better retry handling

### Phase 3: ViewModel Updates (Priority: High)
1. Update AddEditMealViewModel with field-specific validation errors
2. Add error event flow to all ViewModels
3. Integrate ErrorLogger in all ViewModels
4. Add success message events

### Phase 4: Repository Updates (Priority: Medium)
1. Add ErrorLogger to all repositories
2. Ensure all exceptions are caught and mapped
3. Add cause parameter to all AppError instances
4. Verify Result<T> is used consistently

### Phase 5: Screen Updates (Priority: Medium)
1. Update AddEditMealScreen with inline validation errors
2. Add snackbar handlers to all screens
3. Add loading overlays where needed
4. Implement retry actions on error screens

### Phase 6: Testing (Priority: High)
1. Write unit tests for all validators
2. Write ViewModel error handling tests
3. Write repository error mapping tests
4. Write UI tests for form validation

## Success Metrics

1. **Error Coverage**: 100% of exceptions caught at repository boundaries
2. **Validation Coverage**: All user input validated before persistence
3. **User Feedback**: All operations provide success/error feedback
4. **Error Logging**: All errors logged with context
5. **Test Coverage**: 80%+ coverage for error handling code

## Dependencies

- Existing: Timber for logging
- Existing: Hilt for dependency injection
- Existing: Kotlin Coroutines and Flow
- Existing: Jetpack Compose
- Future: Firebase Crashlytics (for production error reporting)

## Security Considerations

1. **No Sensitive Data in Logs**: Ensure error logs don't contain user passwords, tokens, or PII
2. **User-Friendly Messages**: Never expose technical details (SQL queries, stack traces) to users
3. **Rate Limiting**: Consider rate limiting retry attempts to prevent abuse
4. **Input Sanitization**: Validate and sanitize all user input before processing

## Performance Considerations

1. **Validation Performance**: Validators should execute in < 10ms
2. **Error Logging**: Asynchronous logging to avoid blocking UI thread
3. **Snackbar Queuing**: Queue multiple snackbar messages to avoid overlap
4. **Memory**: Limit error event buffer size to prevent memory leaks

## Accessibility

1. **Screen Reader Support**: Error messages announced by TalkBack
2. **Error Color Contrast**: Ensure error colors meet WCAG AA standards
3. **Focus Management**: Move focus to first error field on validation failure
4. **Error Descriptions**: Provide clear, actionable error descriptions
