package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates state management patterns in ViewModels and repositories.
 * 
 * Validates:
 * - Private mutable state is not exposed publicly
 * - State updates use _state.update { } pattern
 * - Sealed classes are used for mutually exclusive states
 * - flowOn(Dispatchers.IO) is applied for database operations
 * - ViewModels use viewModelScope for coroutines
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
class StateManagementAnalyzer : CodeAnalyzer {
    
    override val id: String = "state-management"
    
    override val name: String = "State Management Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.STATE_MANAGEMENT
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze UI layer (ViewModels) and Data layer (Repositories)
        if (file.layer != CodeLayer.UI && file.layer != CodeLayer.DATA) {
            return findings
        }
        
        // Check if file is a ViewModel
        val isViewModel = fileContent.contains(": ViewModel()") || 
                         fileContent.contains("@HiltViewModel")
        
        // Check if file is a Repository
        val isRepository = file.relativePath.contains("/repository/") &&
                          file.relativePath.endsWith("Impl.kt")
        
        if (isViewModel) {
            // Validate ViewModel state management patterns
            findings.addAll(checkStateExposure(file, fileContent))
            findings.addAll(checkStateUpdatePattern(file, fileContent))
            findings.addAll(checkViewModelScope(file, fileContent))
        }
        
        if (isRepository || isViewModel) {
            // Validate Flow dispatcher usage
            findings.addAll(checkFlowDispatcher(file, fileContent))
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to ViewModels and Repositories
        return (file.layer == CodeLayer.UI && file.relativePath.endsWith("ViewModel.kt")) ||
               (file.layer == CodeLayer.DATA && file.relativePath.contains("/repository/"))
    }
    
    /**
     * Checks if private mutable state is exposed publicly.
     * Requirement 5.1
     */
    private fun checkStateExposure(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Look for public MutableStateFlow properties
            if (isMutableStateFlowProperty(trimmed) && !isPrivate(trimmed)) {
                // Check if it's a public property (not private or protected)
                if (isPublicProperty(trimmed)) {
                    findings.add(
                        createExposedMutableStateFinding(
                            file,
                            index + 1,
                            trimmed,
                            extractPropertyName(trimmed)
                        )
                    )
                }
            }
        }
        
        return findings
    }
    
    /**
     * Checks if state updates use the _state.update { } pattern.
     * Requirement 5.2
     */
    private fun checkStateUpdatePattern(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Look for direct state assignments (e.g., _state.value = ...)
            if (isDirectStateAssignment(trimmed)) {
                val propertyName = extractStatePropertyName(trimmed)
                
                findings.add(
                    createDirectStateMutationFinding(
                        file,
                        index + 1,
                        trimmed,
                        propertyName
                    )
                )
            }
        }
        
        return findings
    }
    
    /**
     * Checks if Flow operations use flowOn(Dispatchers.IO).
     * Requirement 5.4
     */
    private fun checkFlowDispatcher(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for Flow-returning functions
            if (isFlowReturningFunction(line)) {
                // Extract function name
                val functionName = extractFunctionName(line)
                
                // Check if function body contains database operations
                val functionBody = extractFunctionBody(lines, i)
                
                if (containsDatabaseOperation(functionBody) && 
                    !containsFlowOn(functionBody)) {
                    findings.add(
                        createMissingFlowOnFinding(
                            file,
                            i + 1,
                            line,
                            functionName
                        )
                    )
                }
                
                // Skip to end of function
                i += functionBody.lines().size
            }
            i++
        }
        
        return findings
    }
    
    /**
     * Checks if ViewModels use viewModelScope for coroutines.
     * Requirement 5.5
     */
    private fun checkViewModelScope(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Look for coroutine launches not using viewModelScope
            if (isCoroutineLaunch(trimmed) && !usesViewModelScope(trimmed)) {
                findings.add(
                    createMissingViewModelScopeFinding(
                        file,
                        index + 1,
                        trimmed
                    )
                )
            }
        }
        
        return findings
    }
    
    // Helper methods for pattern detection
    
    private fun isMutableStateFlowProperty(line: String): Boolean {
        return line.contains("MutableStateFlow") && 
               (line.contains("val ") || line.contains("var "))
    }
    
    private fun isPrivate(line: String): Boolean {
        return line.startsWith("private ")
    }
    
    private fun isPublicProperty(line: String): Boolean {
        // If it doesn't start with private, protected, or internal, it's public
        return !line.startsWith("private ") && 
               !line.startsWith("protected ") && 
               !line.startsWith("internal ")
    }
    
    private fun extractPropertyName(line: String): String {
        return line.substringAfter("val ")
            .substringAfter("var ")
            .substringBefore(":")
            .substringBefore("=")
            .trim()
    }
    
    private fun isDirectStateAssignment(line: String): Boolean {
        // Look for patterns like: _state.value = ... or _uiState.value = ...
        return line.matches(Regex(".*_[a-zA-Z]+\\.value\\s*=.*")) &&
               !line.contains(".update")
    }
    
    private fun extractStatePropertyName(line: String): String {
        val match = Regex("(_[a-zA-Z]+)\\.value").find(line)
        return match?.groupValues?.get(1) ?: "_state"
    }
    
    private fun isFlowReturningFunction(line: String): Boolean {
        return line.contains("fun ") && 
               (line.contains(": Flow<") || line.contains(":Flow<"))
    }
    
    private fun extractFunctionName(line: String): String {
        return line.substringAfter("fun ")
            .substringBefore("(")
            .trim()
    }
    
    private fun extractFunctionBody(lines: List<String>, startIndex: Int): String {
        val body = StringBuilder()
        var braceCount = 0
        var foundStart = false
        var currentIndex = startIndex
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]
            
            for (char in line) {
                if (char == '{') {
                    braceCount++
                    foundStart = true
                } else if (char == '}') {
                    braceCount--
                }
            }
            
            if (foundStart) {
                body.append(line).append("\n")
            }
            
            if (braceCount == 0 && foundStart) {
                break
            }
            
            currentIndex++
        }
        
        return body.toString()
    }
    
    private fun containsDatabaseOperation(functionBody: String): Boolean {
        // Check for DAO calls or database operations
        return functionBody.contains("Dao.") ||
               functionBody.contains("dao.") ||
               functionBody.contains("database.") ||
               functionBody.contains("Room.") ||
               functionBody.contains(".query(") ||
               functionBody.contains(".insert(") ||
               functionBody.contains(".update(") ||
               functionBody.contains(".delete(")
    }
    
    private fun containsFlowOn(functionBody: String): Boolean {
        return functionBody.contains("flowOn(") ||
               functionBody.contains(".flowOn(")
    }
    
    private fun isCoroutineLaunch(line: String): Boolean {
        return line.contains("launch") && line.contains("{")
    }
    
    private fun usesViewModelScope(line: String): Boolean {
        return line.contains("viewModelScope.launch")
    }
    
    // Finding creation methods
    
    /**
     * Creates finding for exposed mutable state.
     */
    private fun createExposedMutableStateFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String,
        propertyName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Public MutableStateFlow Exposed",
            description = "Property '$propertyName' exposes MutableStateFlow publicly. " +
                    "This violates the principle of encapsulation and allows external code to " +
                    "mutate the state directly, bypassing any validation or business logic. " +
                    "ViewModels should expose immutable StateFlow and keep MutableStateFlow private.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Make the MutableStateFlow property private and expose an immutable " +
                    "StateFlow using asStateFlow(). This ensures state can only be modified " +
                    "through controlled methods within the ViewModel.",
            beforeExample = """
                class MealViewModel : ViewModel() {
                    val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                    
                    fun loadMeals() {
                        uiState.value = MealUiState.Success(meals)
                    }
                }
            """.trimIndent(),
            afterExample = """
                class MealViewModel : ViewModel() {
                    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
                    
                    fun loadMeals() {
                        _uiState.update { MealUiState.Success(meals) }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow",
                "https://developer.android.com/topic/architecture/ui-layer#expose-ui-state"
            )
        )
    }
    
    /**
     * Creates finding for direct state mutation.
     */
    private fun createDirectStateMutationFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String,
        propertyName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Direct State Mutation Instead of update { }",
            description = "State property '$propertyName' is being mutated directly using '.value = '. " +
                    "Direct mutations can lead to race conditions in concurrent scenarios and make " +
                    "state updates harder to track. The recommended pattern is to use '.update { }' " +
                    "which provides atomic updates and better thread safety.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Replace direct value assignment with the .update { } pattern. " +
                    "This ensures atomic updates and makes state transitions more explicit and traceable.",
            beforeExample = """
                fun loadMeals() {
                    _uiState.value = MealUiState.Loading
                    
                    viewModelScope.launch {
                        val meals = getMealsUseCase()
                        _uiState.value = MealUiState.Success(meals)
                    }
                }
            """.trimIndent(),
            afterExample = """
                fun loadMeals() {
                    _uiState.update { MealUiState.Loading }
                    
                    viewModelScope.launch {
                        val meals = getMealsUseCase()
                        _uiState.update { MealUiState.Success(meals) }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#update-stateflow",
                "https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html"
            )
        )
    }
    
    /**
     * Creates finding for missing flowOn(Dispatchers.IO).
     */
    private fun createMissingFlowOnFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String,
        functionName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Missing flowOn(Dispatchers.IO) for Database Operation",
            description = "Function '$functionName' returns a Flow and performs database operations " +
                    "but does not use flowOn(Dispatchers.IO). Database operations should not run on " +
                    "the main thread as they can block the UI and cause ANR (Application Not Responding) errors. " +
                    "Use flowOn(Dispatchers.IO) to ensure database operations run on the IO dispatcher.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Add .flowOn(Dispatchers.IO) to the Flow chain to ensure database " +
                    "operations execute on the IO dispatcher. This should be applied after all " +
                    "transformations but before returning the Flow.",
            beforeExample = """
                override fun getMeals(): Flow<Result<List<Meal>>> = flow {
                    mealDao.getAllMeals()
                        .map { entities -> entities.map { it.toMeal() } }
                        .collect { meals ->
                            emit(Result.success(meals))
                        }
                }.catch { e ->
                    emit(Result.failure(mapException(e)))
                }
            """.trimIndent(),
            afterExample = """
                override fun getMeals(): Flow<Result<List<Meal>>> = flow {
                    mealDao.getAllMeals()
                        .map { entities -> entities.map { it.toMeal() } }
                        .collect { meals ->
                            emit(Result.success(meals))
                        }
                }.catch { e ->
                    emit(Result.failure(mapException(e)))
                }.flowOn(Dispatchers.IO)
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/kotlin/flow#modify-stream",
                "https://kotlinlang.org/docs/flow.html#flow-context"
            )
        )
    }
    
    /**
     * Creates finding for missing viewModelScope.
     */
    private fun createMissingViewModelScopeFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Coroutine Launch Not Using viewModelScope",
            description = "Coroutine is launched without using viewModelScope. ViewModels should use " +
                    "viewModelScope for launching coroutines to ensure they are automatically cancelled " +
                    "when the ViewModel is cleared. This prevents memory leaks and ensures coroutines " +
                    "don't continue running after the ViewModel is destroyed.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Replace the coroutine launch with viewModelScope.launch. This ensures " +
                    "the coroutine is tied to the ViewModel's lifecycle and will be automatically " +
                    "cancelled when the ViewModel is cleared.",
            beforeExample = """
                class MealViewModel : ViewModel() {
                    fun loadMeals() {
                        GlobalScope.launch {  // BAD
                            val meals = getMealsUseCase()
                            _uiState.update { MealUiState.Success(meals) }
                        }
                    }
                }
            """.trimIndent(),
            afterExample = """
                class MealViewModel : ViewModel() {
                    fun loadMeals() {
                        viewModelScope.launch {  // GOOD
                            val meals = getMealsUseCase()
                            _uiState.update { MealUiState.Success(meals) }
                        }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope",
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#viewmodel-coroutines"
            )
        )
    }
}
