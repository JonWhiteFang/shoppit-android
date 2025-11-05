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
 * Analyzer that validates adherence to Clean Architecture principles.
 * 
 * Validates:
 * - Domain layer has no Android framework imports
 * - Repository implementations are in the data layer
 * - ViewModels expose StateFlow, not MutableStateFlow
 * - Use cases have a single public operator function
 * - Proper layer separation and dependency flow
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
class ArchitectureAnalyzer : CodeAnalyzer {
    
    override val id: String = "architecture"
    
    override val name: String = "Architecture Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.ARCHITECTURE
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        when (file.layer) {
            CodeLayer.DOMAIN -> {
                findings.addAll(analyzeDomainLayer(file, fileContent))
            }
            CodeLayer.UI -> {
                findings.addAll(analyzeUiLayer(file, fileContent))
            }
            CodeLayer.DATA -> {
                findings.addAll(analyzeDataLayer(file, fileContent))
            }
            else -> {
                // No specific architecture checks for DI or TEST layers
            }
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all layers except null (unknown layer)
        return file.layer != null
    }
    
    /**
     * Analyzes domain layer files for architecture violations.
     * Checks for Android framework imports and use case structure.
     */
    private fun analyzeDomainLayer(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check for Android imports
        val lines = fileContent.lines()
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("import android.")) {
                findings.add(createAndroidImportInDomainFinding(file, index + 1, trimmedLine))
            }
        }
        
        // Check use case structure if this is a use case file
        if (file.relativePath.contains("/usecase/") || file.relativePath.contains("UseCase.kt")) {
            findings.addAll(analyzeUseCase(file, fileContent))
        }
        
        return findings
    }
    
    /**
     * Analyzes UI layer files for architecture violations.
     * Checks ViewModels for exposed MutableStateFlow.
     */
    private fun analyzeUiLayer(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check if this is a ViewModel file
        if (file.relativePath.contains("ViewModel.kt")) {
            findings.addAll(analyzeViewModel(file, fileContent))
        }
        
        return findings
    }
    
    /**
     * Analyzes data layer files for architecture violations.
     * Currently placeholder for future checks.
     */
    private fun analyzeDataLayer(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Future: Check repository implementations are in data layer
        // This would require cross-file analysis
        
        return findings
    }
    
    /**
     * Analyzes ViewModel for exposed MutableStateFlow.
     */
    private fun analyzeViewModel(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inClass = false
        var braceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track if we're inside a class
            if (trimmedLine.contains("class ") && trimmedLine.contains("ViewModel")) {
                inClass = true
            }
            
            // Count braces to track class scope
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            
            if (braceCount == 0 && inClass) {
                inClass = false
            }
            
            // Check for exposed MutableStateFlow
            if (inClass && 
                (trimmedLine.startsWith("val ") || trimmedLine.startsWith("var ")) &&
                trimmedLine.contains("MutableStateFlow") &&
                !trimmedLine.startsWith("private")) {
                
                findings.add(createMutableStateFlowExposedFinding(file, index + 1, trimmedLine))
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes use case for single operator function pattern.
     */
    private fun analyzeUseCase(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inClass = false
        var braceCount = 0
        var publicFunctionCount = 0
        var hasOperatorFunction = false
        val publicFunctions = mutableListOf<Pair<Int, String>>()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track if we're inside a class
            if (trimmedLine.contains("class ") && trimmedLine.contains("UseCase")) {
                inClass = true
            }
            
            // Count braces to track class scope
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            
            if (braceCount == 0 && inClass) {
                inClass = false
            }
            
            // Check for public functions
            if (inClass && trimmedLine.startsWith("fun ") || 
                (inClass && trimmedLine.startsWith("suspend fun "))) {
                
                // Skip private, protected, and internal functions
                val previousLine = if (index > 0) lines[index - 1].trim() else ""
                if (!previousLine.contains("private") && 
                    !previousLine.contains("protected") &&
                    !previousLine.contains("internal")) {
                    
                    publicFunctionCount++
                    publicFunctions.add(Pair(index + 1, trimmedLine))
                    
                    // Check if it's an operator function
                    if (trimmedLine.contains("operator fun invoke") || 
                        trimmedLine.contains("suspend operator fun invoke")) {
                        hasOperatorFunction = true
                    }
                }
            }
        }
        
        // Use case should have exactly one public function (the operator function)
        if (inClass && publicFunctionCount > 1) {
            findings.add(createMultiplePublicFunctionsFinding(file, publicFunctions))
        } else if (inClass && publicFunctionCount == 1 && !hasOperatorFunction) {
            findings.add(createMissingOperatorFunctionFinding(file, publicFunctions.firstOrNull()))
        }
        
        return findings
    }
    
    /**
     * Creates a finding for Android import in domain layer.
     */
    private fun createAndroidImportInDomainFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        val importPath = codeLine.substringAfter("import ").substringBefore(";").trim()
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Android Framework Import in Domain Layer",
            description = "Domain layer should not depend on Android framework. " +
                    "Import '$importPath' violates Clean Architecture principles. " +
                    "The domain layer must remain pure Kotlin with no Android dependencies.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Move this code to the appropriate layer (Data or UI) or " +
                    "create an abstraction in the domain layer that can be implemented " +
                    "in the Data or UI layer.",
            beforeExample = """
                // Domain layer file
                import android.content.Context
                
                class MyUseCase(private val context: Context) {
                    // ...
                }
            """.trimIndent(),
            afterExample = """
                // Domain layer - interface only
                interface ResourceProvider {
                    fun getString(id: Int): String
                }
                
                class MyUseCase(private val resourceProvider: ResourceProvider) {
                    // ...
                }
                
                // Data/UI layer - implementation
                class AndroidResourceProvider(
                    private val context: Context
                ) : ResourceProvider {
                    override fun getString(id: Int) = context.getString(id)
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/topic/architecture",
                "https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html"
            )
        )
    }
    
    /**
     * Creates a finding for exposed MutableStateFlow in ViewModel.
     */
    private fun createMutableStateFlowExposedFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        val propertyName = codeLine.substringAfter("val ")
            .substringAfter("var ")
            .substringBefore(":")
            .substringBefore("=")
            .trim()
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Exposed MutableStateFlow in ViewModel",
            description = "ViewModel exposes MutableStateFlow '$propertyName' publicly. " +
                    "This violates the principle of encapsulation and allows external code " +
                    "to modify the state directly. ViewModels should expose immutable StateFlow " +
                    "and keep MutableStateFlow private.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Make the MutableStateFlow private with an underscore prefix " +
                    "and expose an immutable StateFlow using asStateFlow().",
            beforeExample = """
                class MealViewModel : ViewModel() {
                    val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                }
            """.trimIndent(),
            afterExample = """
                class MealViewModel : ViewModel() {
                    private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                    val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
                    
                    fun updateState(newState: MealUiState) {
                        _uiState.update { newState }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow",
                "https://developer.android.com/topic/architecture/ui-layer/state-production"
            )
        )
    }
    
    /**
     * Creates a finding for use case with multiple public functions.
     */
    private fun createMultiplePublicFunctionsFinding(
        file: FileInfo,
        publicFunctions: List<Pair<Int, String>>
    ): Finding {
        val functionNames = publicFunctions.map { (_, line) ->
            line.substringAfter("fun ")
                .substringBefore("(")
                .trim()
        }
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Use Case Has Multiple Public Functions",
            description = "Use case has ${publicFunctions.size} public functions: ${functionNames.joinToString(", ")}. " +
                    "Use cases should follow the Single Responsibility Principle and have " +
                    "exactly one public operator function (invoke). This makes the use case " +
                    "callable like a function and clearly defines its single purpose.",
            file = file.relativePath,
            lineNumber = publicFunctions.firstOrNull()?.first ?: 1,
            codeSnippet = publicFunctions.joinToString("\n") { it.second },
            recommendation = "Refactor the use case to have a single public operator function. " +
                    "If you need multiple operations, consider creating separate use cases " +
                    "or making the additional functions private helper methods.",
            beforeExample = """
                class GetMealsUseCase @Inject constructor(
                    private val repository: MealRepository
                ) {
                    fun getMeals(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                    
                    fun getMealById(id: Long): Flow<Result<Meal>> {
                        return repository.getMealById(id)
                    }
                }
            """.trimIndent(),
            afterExample = """
                // Separate use cases for different operations
                class GetMealsUseCase @Inject constructor(
                    private val repository: MealRepository
                ) {
                    operator fun invoke(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                }
                
                class GetMealByIdUseCase @Inject constructor(
                    private val repository: MealRepository
                ) {
                    operator fun invoke(id: Long): Flow<Result<Meal>> {
                        return repository.getMealById(id)
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/topic/architecture/domain-layer",
                "https://kotlinlang.org/docs/operator-overloading.html#invoke-operator"
            )
        )
    }
    
    /**
     * Creates a finding for use case missing operator function.
     */
    private fun createMissingOperatorFunctionFinding(
        file: FileInfo,
        publicFunction: Pair<Int, String>?
    ): Finding {
        val lineNumber = publicFunction?.first ?: 1
        val codeLine = publicFunction?.second ?: ""
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Use Case Missing Operator Function",
            description = "Use case has a public function but it's not an operator function. " +
                    "Use cases should use the operator fun invoke() pattern to make them " +
                    "callable like a function. This is a convention that clearly identifies " +
                    "the primary operation of the use case.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Rename the public function to 'operator fun invoke()' to follow " +
                    "the use case pattern. This allows the use case to be called directly " +
                    "like: useCase() instead of useCase.execute().",
            beforeExample = """
                class AddMealUseCase @Inject constructor(
                    private val repository: MealRepository
                ) {
                    suspend fun execute(meal: Meal): Result<Long> {
                        return repository.addMeal(meal)
                    }
                }
                
                // Usage
                val result = addMealUseCase.execute(meal)
            """.trimIndent(),
            afterExample = """
                class AddMealUseCase @Inject constructor(
                    private val repository: MealRepository
                ) {
                    suspend operator fun invoke(meal: Meal): Result<Long> {
                        return repository.addMeal(meal)
                    }
                }
                
                // Usage - cleaner and more idiomatic
                val result = addMealUseCase(meal)
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/topic/architecture/domain-layer",
                "https://kotlinlang.org/docs/operator-overloading.html#invoke-operator"
            )
        )
    }
}
