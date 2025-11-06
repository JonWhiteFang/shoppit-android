package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import org.jetbrains.kotlin.psi.*

/**
 * Analyzer for validating Clean Architecture principles.
 * 
 * Validates:
 * - Domain layer has no Android framework imports
 * - ViewModels expose StateFlow, not MutableStateFlow
 * - Use cases have single operator function
 * - Proper layer separation
 */
class ArchitectureAnalyzer : CodeAnalyzer {
    
    override val id = "architecture"
    override val name = "Architecture Analyzer"
    override val category = AnalysisCategory.ARCHITECTURE
    
    companion object {
        private val ANDROID_FRAMEWORK_PACKAGES = listOf(
            "android.",
            "androidx."
        )
    }
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        when (file.layer) {
            CodeLayer.DOMAIN -> {
                findings.addAll(validateDomainLayer(file, ast))
            }
            CodeLayer.UI -> {
                findings.addAll(validateUILayer(file, ast))
            }
            else -> {
                // No specific validation for other layers yet
            }
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to domain and UI layers
        return file.layer == CodeLayer.DOMAIN || file.layer == CodeLayer.UI
    }
    
    /**
     * Validates domain layer files.
     */
    private fun validateDomainLayer(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check for Android framework imports
        ast.importDirectives.forEach { import ->
            val importPath = import.importPath?.pathStr ?: return@forEach
            
            if (ANDROID_FRAMEWORK_PACKAGES.any { importPath.startsWith(it) }) {
                findings.add(createAndroidImportInDomainFinding(file, import, importPath))
            }
        }
        
        // Check use case structure
        if (file.relativePath.contains("/domain/usecase/")) {
            ast.declarations.filterIsInstance<KtClass>().forEach { klass ->
                findings.addAll(validateUseCase(file, klass))
            }
        }
        
        return findings
    }
    
    /**
     * Validates UI layer files.
     */
    private fun validateUILayer(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check ViewModels
        ast.declarations.filterIsInstance<KtClass>().forEach { klass ->
            if (isViewModel(klass)) {
                findings.addAll(validateViewModel(file, klass))
            }
        }
        
        return findings
    }
    
    /**
     * Checks if a class is a ViewModel.
     */
    private fun isViewModel(klass: KtClass): Boolean {
        val className = klass.name ?: return false
        
        // Check if class name ends with ViewModel
        if (className.endsWith("ViewModel")) {
            return true
        }
        
        // Check if extends ViewModel
        klass.superTypeListEntries.forEach { entry ->
            val typeName = entry.typeAsUserType?.referencedName
            if (typeName == "ViewModel") {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Validates a ViewModel class.
     */
    private fun validateViewModel(file: FileInfo, klass: KtClass): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check for exposed MutableStateFlow
        klass.declarations.filterIsInstance<KtProperty>().forEach { property ->
            if (isPublicProperty(property) && isMutableStateFlow(property)) {
                findings.add(createMutableStateFlowExposedFinding(file, klass, property))
            }
        }
        
        return findings
    }
    
    /**
     * Checks if a property is public.
     */
    private fun isPublicProperty(property: KtProperty): Boolean {
        // If no visibility modifier, it's public by default
        val modifiers = property.modifierList?.text ?: return true
        return !modifiers.contains("private") && !modifiers.contains("protected") && !modifiers.contains("internal")
    }
    
    /**
     * Checks if a property is a MutableStateFlow.
     */
    private fun isMutableStateFlow(property: KtProperty): Boolean {
        val typeReference = property.typeReference?.text ?: return false
        return typeReference.contains("MutableStateFlow")
    }
    
    /**
     * Validates a use case class.
     */
    private fun validateUseCase(file: FileInfo, klass: KtClass): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Count public functions
        val publicFunctions = klass.declarations
            .filterIsInstance<KtNamedFunction>()
            .filter { isPublicFunction(it) }
        
        // Check for operator function
        val hasOperatorFunction = publicFunctions.any { it.name == "invoke" }
        
        if (!hasOperatorFunction && publicFunctions.isNotEmpty()) {
            findings.add(createMissingOperatorFunctionFinding(file, klass))
        }
        
        if (publicFunctions.size > 1) {
            findings.add(createMultiplePublicFunctionsFinding(file, klass, publicFunctions.size))
        }
        
        return findings
    }
    
    /**
     * Checks if a function is public.
     */
    private fun isPublicFunction(function: KtNamedFunction): Boolean {
        val modifiers = function.modifierList?.text ?: return true
        return !modifiers.contains("private") && !modifiers.contains("protected") && !modifiers.contains("internal")
    }
    
    /**
     * Creates a finding for Android import in domain layer.
     */
    private fun createAndroidImportInDomainFinding(
        file: FileInfo,
        import: KtImportDirective,
        importPath: String
    ): Finding {
        return Finding(
            id = "architecture-android-import-domain-${file.path}-${getLineNumber(import)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Android Framework Import in Domain Layer",
            description = "Domain layer file imports Android framework package '$importPath'. " +
                    "The domain layer should be pure Kotlin with no Android dependencies to maintain testability and portability.",
            file = file.path,
            lineNumber = getLineNumber(import),
            codeSnippet = import.text,
            recommendation = "Remove Android framework imports from domain layer. Move Android-specific code to the data or UI layer. " +
                    "Use interfaces in the domain layer and implement them in the data/UI layer if needed.",
            beforeExample = """
                // domain/usecase/GetMealsUseCase.kt
                import android.content.Context
                import androidx.lifecycle.LiveData
                
                class GetMealsUseCase(private val context: Context) {
                    fun execute(): LiveData<List<Meal>> {
                        // Implementation
                    }
                }
            """.trimIndent(),
            afterExample = """
                // domain/usecase/GetMealsUseCase.kt
                import kotlinx.coroutines.flow.Flow
                
                class GetMealsUseCase(private val repository: MealRepository) {
                    operator fun invoke(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                }
                
                // domain/repository/MealRepository.kt
                interface MealRepository {
                    fun getMeals(): Flow<Result<List<Meal>>>
                }
            """.trimIndent(),
            effort = Effort.MEDIUM,
            references = listOf(
                "Clean Architecture by Robert C. Martin",
                "https://developer.android.com/topic/architecture"
            )
        )
    }
    
    /**
     * Creates a finding for exposed MutableStateFlow.
     */
    private fun createMutableStateFlowExposedFinding(
        file: FileInfo,
        klass: KtClass,
        property: KtProperty
    ): Finding {
        val className = klass.name ?: "ViewModel"
        val propertyName = property.name ?: "state"
        
        return Finding(
            id = "architecture-mutable-stateflow-exposed-${file.path}-${getLineNumber(property)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Exposed MutableStateFlow in $className",
            description = "ViewModel '$className' exposes MutableStateFlow property '$propertyName' publicly. " +
                    "This allows external code to modify the state directly, breaking encapsulation.",
            file = file.path,
            lineNumber = getLineNumber(property),
            codeSnippet = property.text,
            recommendation = "Make the MutableStateFlow private and expose an immutable StateFlow using asStateFlow(). " +
                    "This ensures state can only be modified within the ViewModel.",
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow",
                "docs/compose-patterns.md - State Management"
            )
        )
    }
    
    /**
     * Creates a finding for missing operator function in use case.
     */
    private fun createMissingOperatorFunctionFinding(
        file: FileInfo,
        klass: KtClass
    ): Finding {
        val className = klass.name ?: "UseCase"
        
        return Finding(
            id = "architecture-missing-operator-${file.path}-${getLineNumber(klass)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Missing Operator Function in $className",
            description = "Use case '$className' does not have an operator invoke() function. " +
                    "Use cases should have a single operator function for clean, consistent usage.",
            file = file.path,
            lineNumber = getLineNumber(klass),
            codeSnippet = getCodeSnippet(klass.text, maxLines = 10),
            recommendation = "Add an operator invoke() function as the single entry point for the use case. " +
                    "This allows the use case to be called like a function: useCase() instead of useCase.execute().",
            beforeExample = """
                class GetMealsUseCase(
                    private val repository: MealRepository
                ) {
                    fun execute(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                }
                
                // Usage
                val meals = getMealsUseCase.execute()
            """.trimIndent(),
            afterExample = """
                class GetMealsUseCase(
                    private val repository: MealRepository
                ) {
                    operator fun invoke(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                }
                
                // Usage
                val meals = getMealsUseCase()
            """.trimIndent(),
            effort = Effort.TRIVIAL,
            references = listOf(
                "docs/structure.md - Use Cases",
                "Clean Architecture - Use Case Pattern"
            )
        )
    }
    
    /**
     * Creates a finding for multiple public functions in use case.
     */
    private fun createMultiplePublicFunctionsFinding(
        file: FileInfo,
        klass: KtClass,
        functionCount: Int
    ): Finding {
        val className = klass.name ?: "UseCase"
        
        return Finding(
            id = "architecture-multiple-functions-${file.path}-${getLineNumber(klass)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Multiple Public Functions in $className",
            description = "Use case '$className' has $functionCount public functions. " +
                    "Use cases should have a single responsibility with one public operator function.",
            file = file.path,
            lineNumber = getLineNumber(klass),
            codeSnippet = getCodeSnippet(klass.text, maxLines = 10),
            recommendation = "Split this use case into multiple use cases, each with a single operator function. " +
                    "Each use case should do one thing well. Make helper functions private.",
            beforeExample = """
                class MealUseCase(private val repository: MealRepository) {
                    fun getMeals(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                    
                    fun addMeal(meal: Meal): Result<Long> {
                        return repository.addMeal(meal)
                    }
                    
                    fun deleteMeal(id: Long): Result<Unit> {
                        return repository.deleteMeal(id)
                    }
                }
            """.trimIndent(),
            afterExample = """
                class GetMealsUseCase(private val repository: MealRepository) {
                    operator fun invoke(): Flow<Result<List<Meal>>> {
                        return repository.getMeals()
                    }
                }
                
                class AddMealUseCase(private val repository: MealRepository) {
                    operator suspend fun invoke(meal: Meal): Result<Long> {
                        return repository.addMeal(meal)
                    }
                }
                
                class DeleteMealUseCase(private val repository: MealRepository) {
                    operator suspend fun invoke(id: Long): Result<Unit> {
                        return repository.deleteMeal(id)
                    }
                }
            """.trimIndent(),
            effort = Effort.MEDIUM,
            references = listOf(
                "docs/structure.md - Use Cases",
                "SOLID Principles - Single Responsibility Principle"
            )
        )
    }
    
    /**
     * Gets a code snippet from text, limiting to max lines.
     */
    private fun getCodeSnippet(text: String, maxLines: Int = 10): String {
        val lines = text.lines()
        return if (lines.size <= maxLines) {
            text
        } else {
            lines.take(maxLines).joinToString("\n") + "\n// ... (${lines.size - maxLines} more lines)"
        }
    }
    
    /**
     * Gets the line number of a PSI element.
     */
    private fun getLineNumber(element: KtElement): Int {
        val document = element.containingKtFile.viewProvider.document
        return (document?.getLineNumber(element.textOffset) ?: 0) + 1
    }
}
