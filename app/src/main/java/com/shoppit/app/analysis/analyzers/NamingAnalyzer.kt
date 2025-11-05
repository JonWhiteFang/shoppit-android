package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates naming conventions throughout the codebase.
 * 
 * Validates:
 * - File naming conventions (PascalCase for classes, camelCase for functions)
 * - Class names (PascalCase)
 * - Function names (camelCase)
 * - Constants (UPPER_SNAKE_CASE)
 * - Private mutable state (underscore prefix)
 * 
 * Requirements: 10.1, 10.2, 10.3, 10.4, 10.5
 */
class NamingAnalyzer : CodeAnalyzer {
    
    override val id: String = "naming"
    
    override val name: String = "Naming Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.NAMING
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Validate file naming conventions
        findings.addAll(validateFileNaming(file))
        
        // Validate class names
        findings.addAll(validateClassNames(file, fileContent))
        
        // Validate function names
        findings.addAll(validateFunctionNames(file, fileContent))
        
        // Validate constants
        findings.addAll(validateConstants(file, fileContent))
        
        // Validate private mutable state
        findings.addAll(validatePrivateMutableState(file, fileContent))
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files
        return file.relativePath.endsWith(".kt")
    }
    
    /**
     * Validates file naming conventions.
     * Requirement 10.1: Verify files follow the project naming conventions.
     */
    private fun validateFileNaming(file: FileInfo): List<Finding> {
        val findings = mutableListOf<Finding>()
        val fileName = file.relativePath.substringAfterLast("/").substringBeforeLast(".kt")
        
        // Expected patterns based on project conventions:
        // - Screens: [Feature]Screen.kt (e.g., MealListScreen.kt)
        // - ViewModels: [Feature]ViewModel.kt (e.g., MealViewModel.kt)
        // - Use Cases: [Action][Entity]UseCase.kt (e.g., AddMealUseCase.kt)
        // - Repositories: [Entity]Repository.kt or [Entity]RepositoryImpl.kt
        // - DAOs: [Entity]Dao.kt (e.g., MealDao.kt)
        // - Entities: [Entity]Entity.kt (e.g., MealEntity.kt)
        // - Domain Models: PascalCase (e.g., Meal.kt, Ingredient.kt)
        // - Modules: [Purpose]Module.kt (e.g., DatabaseModule.kt)
        
        // Check if file name is PascalCase
        if (!isPascalCase(fileName)) {
            findings.add(createFileNamingViolationFinding(file, fileName))
        }
        
        return findings
    }
    
    /**
     * Validates class names follow PascalCase convention.
     * Requirement 10.2: Verify classes use PascalCase naming.
     */
    private fun validateClassNames(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Match class, data class, sealed class, interface, object declarations
            val classPatterns = listOf(
                Regex("""^(data\s+)?class\s+(\w+)"""),
                Regex("""^sealed\s+class\s+(\w+)"""),
                Regex("""^interface\s+(\w+)"""),
                Regex("""^object\s+(\w+)"""),
                Regex("""^enum\s+class\s+(\w+)""")
            )
            
            for (pattern in classPatterns) {
                val match = pattern.find(trimmedLine)
                if (match != null) {
                    val className = match.groupValues.last()
                    
                    if (!isPascalCase(className)) {
                        findings.add(createClassNamingViolationFinding(
                            file,
                            index + 1,
                            className,
                            trimmedLine
                        ))
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Validates function names follow camelCase convention.
     * Requirement 10.3: Verify functions use camelCase naming.
     */
    private fun validateFunctionNames(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Match function declarations
            val functionPattern = Regex("""^(private\s+|public\s+|protected\s+|internal\s+)?(suspend\s+|inline\s+|override\s+)*(fun)\s+(\w+)""")
            val match = functionPattern.find(trimmedLine)
            
            if (match != null) {
                val functionName = match.groupValues[4]
                
                // Skip special cases: operator functions, test functions
                if (functionName.startsWith("test") || 
                    isOperatorFunction(functionName) ||
                    functionName == "invoke") {
                    return@forEachIndexed
                }
                
                if (!isCamelCase(functionName)) {
                    findings.add(createFunctionNamingViolationFinding(
                        file,
                        index + 1,
                        functionName,
                        trimmedLine
                    ))
                }
            }
        }
        
        return findings
    }
    
    /**
     * Validates constants follow UPPER_SNAKE_CASE convention.
     * Requirement 10.4: Verify constants use UPPER_SNAKE_CASE naming.
     */
    private fun validateConstants(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var inCompanionObject = false
        var companionObjectBraceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track companion object scope
            if (trimmedLine.startsWith("companion object")) {
                inCompanionObject = true
                companionObjectBraceCount = 0
            }
            
            if (inCompanionObject) {
                companionObjectBraceCount += line.count { it == '{' }
                companionObjectBraceCount -= line.count { it == '}' }
                
                if (companionObjectBraceCount == 0 && trimmedLine.contains("}")) {
                    inCompanionObject = false
                }
            }
            
            // Match const val declarations or val in companion object
            val isConstant = trimmedLine.startsWith("const val") || 
                            (inCompanionObject && trimmedLine.startsWith("val"))
            
            if (isConstant) {
                val constantPattern = Regex("""(const\s+)?val\s+(\w+)""")
                val match = constantPattern.find(trimmedLine)
                
                if (match != null) {
                    val constantName = match.groupValues[2]
                    
                    // Skip if it's a property with custom getter (not a true constant)
                    if (trimmedLine.contains("get()")) {
                        return@forEachIndexed
                    }
                    
                    if (!isUpperSnakeCase(constantName)) {
                        findings.add(createConstantNamingViolationFinding(
                            file,
                            index + 1,
                            constantName,
                            trimmedLine
                        ))
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Validates private mutable state uses underscore prefix.
     * Requirement 10.5: Verify private mutable state uses underscore prefix.
     */
    private fun validatePrivateMutableState(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Match private mutable state declarations (MutableStateFlow, MutableLiveData, etc.)
            if (trimmedLine.startsWith("private val") || trimmedLine.startsWith("private var")) {
                val mutableTypes = listOf(
                    "MutableStateFlow",
                    "MutableSharedFlow",
                    "MutableLiveData",
                    "mutableStateOf",
                    "mutableStateListOf",
                    "mutableStateMapOf"
                )
                
                val hasMutableType = mutableTypes.any { trimmedLine.contains(it) }
                
                if (hasMutableType) {
                    val propertyPattern = Regex("""private\s+(val|var)\s+(\w+)""")
                    val match = propertyPattern.find(trimmedLine)
                    
                    if (match != null) {
                        val propertyName = match.groupValues[2]
                        
                        // Check if it starts with underscore
                        if (!propertyName.startsWith("_")) {
                            findings.add(createPrivateMutableStateNamingViolationFinding(
                                file,
                                index + 1,
                                propertyName,
                                trimmedLine
                            ))
                        }
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Checks if a name follows PascalCase convention.
     */
    private fun isPascalCase(name: String): Boolean {
        if (name.isEmpty()) return false
        
        // First character must be uppercase
        if (!name[0].isUpperCase()) return false
        
        // Must not contain underscores (except for special cases like test names)
        if (name.contains("_") && !name.endsWith("Test")) return false
        
        // Must not be all uppercase (that's UPPER_SNAKE_CASE)
        if (name.all { it.isUpperCase() || it == '_' }) return false
        
        return true
    }
    
    /**
     * Checks if a name follows camelCase convention.
     */
    private fun isCamelCase(name: String): Boolean {
        if (name.isEmpty()) return false
        
        // First character must be lowercase
        if (!name[0].isLowerCase()) return false
        
        // Must not contain underscores
        if (name.contains("_")) return false
        
        // Must not be all lowercase (should have at least one uppercase for compound words)
        // Exception: single word functions can be all lowercase
        return true
    }
    
    /**
     * Checks if a name follows UPPER_SNAKE_CASE convention.
     */
    private fun isUpperSnakeCase(name: String): Boolean {
        if (name.isEmpty()) return false
        
        // All characters must be uppercase, digits, or underscores
        return name.all { it.isUpperCase() || it.isDigit() || it == '_' }
    }
    
    /**
     * Checks if a function name is an operator function.
     */
    private fun isOperatorFunction(name: String): Boolean {
        val operatorFunctions = setOf(
            "plus", "minus", "times", "div", "rem", "mod",
            "rangeTo", "contains", "get", "set",
            "plusAssign", "minusAssign", "timesAssign", "divAssign", "remAssign",
            "inc", "dec", "unaryPlus", "unaryMinus",
            "not", "equals", "compareTo",
            "iterator", "next", "hasNext",
            "component1", "component2", "component3", "component4", "component5"
        )
        
        return operatorFunctions.contains(name)
    }
    
    /**
     * Creates a finding for file naming violations.
     */
    private fun createFileNamingViolationFinding(
        file: FileInfo,
        fileName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "File Name Does Not Follow PascalCase Convention",
            description = "File '$fileName.kt' does not follow the project's PascalCase naming convention. " +
                    "Kotlin files should be named using PascalCase to match the primary class they contain " +
                    "and to maintain consistency across the codebase.",
            file = file.relativePath,
            lineNumber = 1,
            codeSnippet = "File: $fileName.kt",
            recommendation = "Rename the file to use PascalCase. For example, if the file contains a class " +
                    "named 'MealList', the file should be named 'MealList.kt'. Follow project conventions: " +
                    "Screens end with 'Screen', ViewModels with 'ViewModel', etc.",
            beforeExample = "File: ${fileName}.kt",
            afterExample = "File: ${toPascalCase(fileName)}.kt",
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://kotlinlang.org/docs/coding-conventions.html#naming-rules",
                "Project structure.md: File Naming Conventions"
            )
        )
    }
    
    /**
     * Creates a finding for class naming violations.
     */
    private fun createClassNamingViolationFinding(
        file: FileInfo,
        lineNumber: Int,
        className: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Class Name Does Not Follow PascalCase Convention",
            description = "Class '$className' does not follow PascalCase naming convention. Class names " +
                    "should start with an uppercase letter and use PascalCase for compound words (e.g., " +
                    "MealViewModel, not mealViewModel or meal_view_model).",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Rename the class to use PascalCase. Each word should start with an uppercase " +
                    "letter, with no underscores or other separators.",
            beforeExample = "class $className { }",
            afterExample = "class ${toPascalCase(className)} { }",
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/coding-conventions.html#naming-rules",
                "https://developer.android.com/kotlin/style-guide#naming"
            )
        )
    }
    
    /**
     * Creates a finding for function naming violations.
     */
    private fun createFunctionNamingViolationFinding(
        file: FileInfo,
        lineNumber: Int,
        functionName: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Function Name Does Not Follow camelCase Convention",
            description = "Function '$functionName' does not follow camelCase naming convention. Function " +
                    "names should start with a lowercase letter and use camelCase for compound words (e.g., " +
                    "loadMeals, not LoadMeals or load_meals).",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Rename the function to use camelCase. The first letter should be lowercase, " +
                    "and subsequent words should start with uppercase letters, with no underscores.",
            beforeExample = "fun $functionName() { }",
            afterExample = "fun ${toCamelCase(functionName)}() { }",
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/coding-conventions.html#naming-rules",
                "https://developer.android.com/kotlin/style-guide#naming"
            )
        )
    }
    
    /**
     * Creates a finding for constant naming violations.
     */
    private fun createConstantNamingViolationFinding(
        file: FileInfo,
        lineNumber: Int,
        constantName: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Constant Does Not Follow UPPER_SNAKE_CASE Convention",
            description = "Constant '$constantName' does not follow UPPER_SNAKE_CASE naming convention. " +
                    "Constants should use all uppercase letters with underscores separating words (e.g., " +
                    "MAX_COUNT, not maxCount or MaxCount).",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Rename the constant to use UPPER_SNAKE_CASE. All letters should be uppercase, " +
                    "with underscores separating words.",
            beforeExample = "const val $constantName = ...",
            afterExample = "const val ${toUpperSnakeCase(constantName)} = ...",
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://kotlinlang.org/docs/coding-conventions.html#naming-rules",
                "https://developer.android.com/kotlin/style-guide#constant-names"
            )
        )
    }
    
    /**
     * Creates a finding for private mutable state naming violations.
     */
    private fun createPrivateMutableStateNamingViolationFinding(
        file: FileInfo,
        lineNumber: Int,
        propertyName: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Private Mutable State Missing Underscore Prefix",
            description = "Private mutable state property '$propertyName' should use an underscore prefix. " +
                    "This is a project convention to distinguish private mutable state from public immutable " +
                    "state, especially for StateFlow/LiveData patterns where you have both _state (private " +
                    "mutable) and state (public immutable).",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Add an underscore prefix to the property name. This makes it clear that this " +
                    "is internal mutable state that should not be exposed directly. Typically, you'll have a " +
                    "corresponding public property without the underscore that exposes an immutable view.",
            beforeExample = "private val $propertyName = MutableStateFlow(...)",
            afterExample = "private val _$propertyName = MutableStateFlow(...)\nval $propertyName = _$propertyName.asStateFlow()",
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "Project compose-patterns.md: State Management Patterns",
                "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow"
            )
        )
    }
    
    /**
     * Converts a name to PascalCase (best effort).
     */
    private fun toPascalCase(name: String): String {
        return name.split("_", "-")
            .joinToString("") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
    
    /**
     * Converts a name to camelCase (best effort).
     */
    private fun toCamelCase(name: String): String {
        val parts = name.split("_", "-")
        if (parts.isEmpty()) return name
        
        return parts.first().replaceFirstChar { it.lowercase() } +
                parts.drop(1).joinToString("") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
    }
    
    /**
     * Converts a name to UPPER_SNAKE_CASE (best effort).
     */
    private fun toUpperSnakeCase(name: String): String {
        // Insert underscores before uppercase letters (for camelCase/PascalCase)
        val withUnderscores = name.replace(Regex("([a-z])([A-Z])"), "$1_$2")
        return withUnderscores.uppercase()
    }
}
