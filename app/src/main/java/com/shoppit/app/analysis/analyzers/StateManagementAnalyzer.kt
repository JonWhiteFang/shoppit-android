package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPrivate

/**
 * Analyzer that validates state management patterns in ViewModels and repositories.
 * 
 * Validates:
 * - Private mutable state is not exposed publicly
 * - State updates use _state.update { } pattern
 * - flowOn(Dispatchers.IO) is applied for database operations
 * - ViewModels use viewModelScope for coroutines
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6
 */
class StateManagementAnalyzer : CodeAnalyzer {
    
    override val id: String = "state-management"
    
    override val name: String = "State Management Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.STATE_MANAGEMENT
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze UI layer (ViewModels) and Data layer (Repositories)
        if (file.layer != CodeLayer.UI && file.layer != CodeLayer.DATA) {
            return findings
        }
        
        // Check if file is a ViewModel
        val isViewModel = isViewModelFile(ast)
        
        // Check if file is a Repository
        val isRepository = file.relativePath.contains("/repository/") &&
                          file.relativePath.endsWith("Impl.kt")
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                
                if (isViewModel) {
                    // Validate ViewModel state management patterns
                    findings.addAll(checkStateExposure(file, klass))
                    findings.addAll(checkViewModelScope(file, klass))
                }
            }
            
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                
                if (isViewModel) {
                    // Check for exposed MutableStateFlow
                    findings.addAll(checkMutableStateFlowExposure(file, property))
                }
            }
            
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                
                if (isViewModel) {
                    // Check for direct state mutations
                    findings.addAll(checkDirectStateMutation(file, expression))
                }
            }
            
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                if (isRepository || isViewModel) {
                    // Check for missing flowOn
                    findings.addAll(checkFlowDispatcher(file, function))
                }
            }
        })
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to ViewModels and Repositories
        return (file.layer == CodeLayer.UI && file.relativePath.endsWith("ViewModel.kt")) ||
               (file.layer == CodeLayer.DATA && file.relativePath.contains("/repository/"))
    }
    
    /**
     * Checks if file contains a ViewModel class.
     */
    private fun isViewModelFile(ast: KtFile): Boolean {
        var isViewModel = false
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                
                // Check if extends ViewModel
                klass.superTypeListEntries.forEach { entry ->
                    val typeName = entry.typeAsUserType?.referencedName
                    if (typeName == "ViewModel") {
                        isViewModel = true
                    }
                }
                
                // Check for @HiltViewModel annotation
                if (klass.annotationEntries.any { it.shortName?.asString() == "HiltViewModel" }) {
                    isViewModel = true
                }
            }
        })
        
        return isViewModel
    }
    
    /**
     * Checks if private mutable state is exposed publicly.
     */
    private fun checkStateExposure(file: FileInfo, klass: KtClass): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        klass.declarations.filterIsInstance<KtProperty>().forEach { property ->
            if (isMutableStateFlow(property) && !property.isPrivate()) {
                findings.add(createExposedMutableStateFinding(file, property))
            }
        }
        
        return findings
    }
    
    /**
     * Checks if property is a MutableStateFlow that's exposed publicly.
     */
    private fun checkMutableStateFlowExposure(file: FileInfo, property: KtProperty): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        if (isMutableStateFlow(property) && !property.isPrivate()) {
            findings.add(createExposedMutableStateFinding(file, property))
        }
        
        return findings
    }
    
    /**
     * Checks if property is a MutableStateFlow.
     */
    private fun isMutableStateFlow(property: KtProperty): Boolean {
        val typeReference = property.typeReference?.text ?: return false
        return typeReference.contains("MutableStateFlow")
    }
    
    /**
     * Checks for direct state mutations instead of update { }.
     */
    private fun checkDirectStateMutation(file: FileInfo, expression: KtBinaryExpression): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check for pattern: _state.value = ...
        if (expression.operationReference.text == "=") {
            val left = expression.left
            if (left is KtDotQualifiedExpression) {
                val receiver = left.receiverExpression.text
                val selector = left.selectorExpression?.text
                
                // Check if it's a state property assignment
                if (receiver.startsWith("_") && selector == "value") {
                    findings.add(createDirectStateMutationFinding(file, expression, receiver))
                }
            }
        }
        
        return findings
    }
    
    /**
     * Checks if ViewModels use viewModelScope for coroutines.
     */
    private fun checkViewModelScope(file: FileInfo, klass: KtClass): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        klass.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                
                // Check for launch calls
                if (expression.calleeExpression?.text == "launch") {
                    // Check if it's using viewModelScope
                    val parent = expression.parent
                    if (parent is KtDotQualifiedExpression) {
                        val receiver = parent.receiverExpression.text
                        if (receiver != "viewModelScope") {
                            findings.add(createMissingViewModelScopeFinding(file, expression))
                        }
                    } else {
                        // Direct launch without scope
                        findings.add(createMissingViewModelScopeFinding(file, expression))
                    }
                }
            }
        })
        
        return findings
    }
    
    /**
     * Checks if Flow operations use flowOn(Dispatchers.IO).
     */
    private fun checkFlowDispatcher(file: FileInfo, function: KtNamedFunction): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check if function returns Flow
        val returnType = function.typeReference?.text ?: return findings
        if (!returnType.contains("Flow<")) {
            return findings
        }
        
        // Check if function body contains database operations
        val hasDatabaseOp = containsDatabaseOperation(function)
        if (!hasDatabaseOp) {
            return findings
        }
        
        // Check if flowOn is used
        val hasFlowOn = containsFlowOn(function)
        if (!hasFlowOn) {
            findings.add(createMissingFlowOnFinding(file, function))
        }
        
        return findings
    }
    
    /**
     * Checks if function contains database operations.
     */
    private fun containsDatabaseOperation(function: KtNamedFunction): Boolean {
        var hasDatabaseOp = false
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                
                val receiver = expression.receiverExpression.text
                val selector = expression.selectorExpression?.text ?: ""
                
                // Check for DAO calls
                if (receiver.endsWith("Dao") || receiver.endsWith("dao") ||
                    selector.startsWith("insert") || selector.startsWith("update") ||
                    selector.startsWith("delete") || selector.startsWith("query") ||
                    selector.startsWith("get") || selector.startsWith("find")) {
                    hasDatabaseOp = true
                }
            }
        })
        
        return hasDatabaseOp
    }
    
    /**
     * Checks if function contains flowOn call.
     */
    private fun containsFlowOn(function: KtNamedFunction): Boolean {
        var hasFlowOn = false
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                
                if (expression.calleeExpression?.text == "flowOn") {
                    hasFlowOn = true
                }
            }
        })
        
        return hasFlowOn
    }
    
    /**
     * Creates finding for exposed mutable state.
     */
    private fun createExposedMutableStateFinding(
        file: FileInfo,
        property: KtProperty
    ): Finding {
        val propertyName = property.name ?: "state"
        
        return Finding(
            id = "state-exposed-mutable-${file.path}-${getLineNumber(property)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Public MutableStateFlow Exposed",
            description = "Property '$propertyName' exposes MutableStateFlow publicly. " +
                    "This violates encapsulation and allows external code to mutate state directly. " +
                    "ViewModels should expose immutable StateFlow and keep MutableStateFlow private.",
            file = file.path,
            lineNumber = getLineNumber(property),
            codeSnippet = property.text,
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
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow",
                "docs/compose-patterns.md"
            )
        )
    }
    
    /**
     * Creates finding for direct state mutation.
     */
    private fun createDirectStateMutationFinding(
        file: FileInfo,
        expression: KtBinaryExpression,
        propertyName: String
    ): Finding {
        return Finding(
            id = "state-direct-mutation-${file.path}-${getLineNumber(expression)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Direct State Mutation Instead of update { }",
            description = "State property '$propertyName' is being mutated directly using '.value = '. " +
                    "Direct mutations can lead to race conditions. Use '.update { }' for atomic updates.",
            file = file.path,
            lineNumber = getLineNumber(expression),
            codeSnippet = expression.text,
            recommendation = "Replace direct value assignment with the .update { } pattern. " +
                    "This ensures atomic updates and makes state transitions more explicit.",
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow#update-stateflow"
            )
        )
    }
    
    /**
     * Creates finding for missing flowOn(Dispatchers.IO).
     */
    private fun createMissingFlowOnFinding(
        file: FileInfo,
        function: KtNamedFunction
    ): Finding {
        val functionName = function.name ?: "function"
        
        return Finding(
            id = "state-missing-flowon-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Missing flowOn(Dispatchers.IO) for Database Operation",
            description = "Function '$functionName' returns a Flow and performs database operations " +
                    "but does not use flowOn(Dispatchers.IO). Database operations should not run on " +
                    "the main thread as they can block the UI and cause ANR errors.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = getCodeSnippet(function.text, maxLines = 10),
            recommendation = "Add .flowOn(Dispatchers.IO) to the Flow chain to ensure database " +
                    "operations execute on the IO dispatcher.",
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/kotlin/flow#modify-stream",
                "docs/data-layer-patterns.md"
            )
        )
    }
    
    /**
     * Creates finding for missing viewModelScope.
     */
    private fun createMissingViewModelScopeFinding(
        file: FileInfo,
        expression: KtCallExpression
    ): Finding {
        return Finding(
            id = "state-missing-viewmodelscope-${file.path}-${getLineNumber(expression)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Coroutine Launch Not Using viewModelScope",
            description = "Coroutine is launched without using viewModelScope. ViewModels should use " +
                    "viewModelScope to ensure coroutines are automatically cancelled when the ViewModel is cleared.",
            file = file.path,
            lineNumber = getLineNumber(expression),
            codeSnippet = expression.text,
            recommendation = "Replace the coroutine launch with viewModelScope.launch. This ensures " +
                    "the coroutine is tied to the ViewModel's lifecycle.",
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/topic/libraries/architecture/coroutines#viewmodelscope"
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
