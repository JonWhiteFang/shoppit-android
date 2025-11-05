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
 * Analyzer that validates error handling patterns in repositories and use cases.
 * 
 * Validates:
 * - Repository functions catch and map exceptions to AppError types
 * - Result type is used for failable operations
 * - Exceptions don't reach the UI layer
 * - No empty catch blocks or generic exception handling
 * 
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
class ErrorHandlingAnalyzer : CodeAnalyzer {
    
    override val id: String = "error-handling"
    
    override val name: String = "Error Handling Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.ERROR_HANDLING
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check if file is a Repository implementation
        val isRepository = file.layer == CodeLayer.DATA && 
                          file.relativePath.contains("/repository/") &&
                          file.relativePath.endsWith("Impl.kt")
        
        // Check if file is a Use Case
        val isUseCase = file.layer == CodeLayer.DOMAIN &&
                       file.relativePath.contains("/usecase/") &&
                       file.relativePath.endsWith("UseCase.kt")
        
        // Check if file is in UI layer
        val isUiLayer = file.layer == CodeLayer.UI
        
        if (isRepository) {
            // Validate repository error handling patterns
            findings.addAll(checkRepositoryExceptionMapping(file, fileContent))
            findings.addAll(checkResultTypeUsage(file, fileContent))
        }
        
        if (isUseCase) {
            // Validate use case error handling patterns
            findings.addAll(checkResultTypeUsage(file, fileContent))
        }
        
        if (isUiLayer) {
            // Check for exceptions reaching UI layer
            findings.addAll(checkExceptionsInUiLayer(file, fileContent))
        }
        
        // Check for empty catch blocks in all layers
        findings.addAll(checkEmptyCatchBlocks(file, fileContent))
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to repositories, use cases, and UI layer
        return (file.layer == CodeLayer.DATA && file.relativePath.contains("/repository/")) ||
               (file.layer == CodeLayer.DOMAIN && file.relativePath.contains("/usecase/")) ||
               file.layer == CodeLayer.UI
    }
    
    /**
     * Checks if repository functions catch and map exceptions.
     * Requirement 6.1
     */
    private fun checkRepositoryExceptionMapping(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for repository functions (override or suspend functions)
            if (isRepositoryFunction(line)) {
                val functionName = extractFunctionName(line)
                val functionBody = extractFunctionBody(lines, i)
                
                // Check if function has database/network operations
                if (containsRiskyOperations(functionBody)) {
                    // Check if function has try-catch with exception mapping
                    if (!hasTryCatchWithMapping(functionBody)) {
                        findings.add(
                            createMissingExceptionMappingFinding(
                                file,
                                i + 1,
                                line,
                                functionName
                            )
                        )
                    }
                }
                
                // Skip to end of function
                i += functionBody.lines().size
            }
            i++
        }
        
        return findings
    }
    
    /**
     * Checks if functions that can fail use Result<T> return type.
     * Requirement 6.2
     */
    private fun checkResultTypeUsage(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for functions that can fail
            if (isFunctionDeclaration(line)) {
                val functionName = extractFunctionName(line)
                val functionBody = extractFunctionBody(lines, i)
                
                // Check if function has operations that can fail
                if (containsFailableOperations(functionBody)) {
                    // Check if return type is Result<T>
                    if (!hasResultReturnType(line)) {
                        findings.add(
                            createMissingResultTypeFinding(
                                file,
                                i + 1,
                                line,
                                functionName
                            )
                        )
                    }
                }
                
                // Skip to end of function
                i += functionBody.lines().size
            }
            i++
        }
        
        return findings
    }
    
    /**
     * Checks if exceptions reach the UI layer.
     * Requirement 6.3
     */
    private fun checkExceptionsInUiLayer(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Look for throw statements in UI layer
            if (trimmed.startsWith("throw ")) {
                findings.add(
                    createExceptionInUiLayerFinding(
                        file,
                        index + 1,
                        trimmed
                    )
                )
            }
        }
        
        return findings
    }
    
    /**
     * Checks for empty catch blocks or generic exception handling.
     * Requirement 6.4
     */
    private fun checkEmptyCatchBlocks(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for catch blocks
            if (line.startsWith("catch") || line.contains("} catch")) {
                val catchBlock = extractCatchBlock(lines, i)
                val exceptionType = extractExceptionType(line)
                
                // Check if catch block is empty or only contains logging
                if (isEmptyOrOnlyLogging(catchBlock)) {
                    findings.add(
                        createEmptyCatchBlockFinding(
                            file,
                            i + 1,
                            line,
                            exceptionType
                        )
                    )
                }
                
                // Check for generic Exception catching
                if (isGenericExceptionCatch(line)) {
                    findings.add(
                        createGenericExceptionCatchFinding(
                            file,
                            i + 1,
                            line
                        )
                    )
                }
                
                // Skip to end of catch block
                i += catchBlock.lines().size
            }
            i++
        }
        
        return findings
    }
    
    // Helper methods for pattern detection
    
    private fun isRepositoryFunction(line: String): Boolean {
        return (line.contains("override fun ") || line.contains("suspend fun ")) &&
               !line.contains("private fun")
    }
    
    private fun isFunctionDeclaration(line: String): Boolean {
        return line.contains("fun ") && line.contains("(")
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
    
    private fun containsRiskyOperations(functionBody: String): Boolean {
        // Check for database, network, or file operations
        return functionBody.contains("Dao.") ||
               functionBody.contains("dao.") ||
               functionBody.contains("api.") ||
               functionBody.contains("Api.") ||
               functionBody.contains("database.") ||
               functionBody.contains("retrofit") ||
               functionBody.contains("okhttp") ||
               functionBody.contains("File(") ||
               functionBody.contains("FileInputStream") ||
               functionBody.contains("FileOutputStream")
    }
    
    private fun hasTryCatchWithMapping(functionBody: String): Boolean {
        // Check if function has try-catch and maps exceptions
        return functionBody.contains("try {") &&
               functionBody.contains("catch") &&
               (functionBody.contains("AppError") ||
                functionBody.contains("mapException") ||
                functionBody.contains("Result.failure"))
    }
    
    private fun containsFailableOperations(functionBody: String): Boolean {
        // Check for operations that can fail
        return containsRiskyOperations(functionBody) ||
               functionBody.contains("throw ") ||
               functionBody.contains("require(") ||
               functionBody.contains("check(") ||
               functionBody.contains("error(")
    }
    
    private fun hasResultReturnType(line: String): Boolean {
        return line.contains(": Result<") || line.contains(":Result<")
    }
    
    private fun extractCatchBlock(lines: List<String>, startIndex: Int): String {
        val block = StringBuilder()
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
                block.append(line).append("\n")
            }
            
            if (braceCount == 0 && foundStart) {
                break
            }
            
            currentIndex++
        }
        
        return block.toString()
    }
    
    private fun extractExceptionType(line: String): String {
        val match = Regex("catch\\s*\\(\\s*([a-zA-Z]+)").find(line)
        return match?.groupValues?.get(1) ?: "Exception"
    }
    
    private fun isEmptyOrOnlyLogging(catchBlock: String): Boolean {
        val contentLines = catchBlock.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "{" && it != "}" }
        
        // Empty if no content
        if (contentLines.isEmpty()) {
            return true
        }
        
        // Only logging if all lines are logging statements
        return contentLines.all { line ->
            line.startsWith("Log.") ||
            line.startsWith("Timber.") ||
            line.startsWith("println(") ||
            line.startsWith("print(") ||
            line.contains("logger.") ||
            line.contains("log.")
        }
    }
    
    private fun isGenericExceptionCatch(line: String): Boolean {
        return line.contains("catch (e: Exception)") ||
               line.contains("catch (ex: Exception)") ||
               line.contains("catch (exception: Exception)")
    }
    
    // Finding creation methods
    
    /**
     * Creates finding for missing exception mapping in repository.
     */
    private fun createMissingExceptionMappingFinding(
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
            title = "Missing Exception Mapping in Repository",
            description = "Repository function '$functionName' performs risky operations (database/network) " +
                    "but does not catch and map exceptions to AppError types. Exceptions should be caught " +
                    "at repository boundaries and mapped to domain-specific error types to prevent " +
                    "implementation details from leaking to upper layers.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Wrap risky operations in try-catch blocks and map exceptions to AppError types. " +
                    "Use a mapException helper function to convert framework exceptions (SQLiteException, " +
                    "IOException, etc.) to domain errors. Return Result<T> to indicate success or failure.",
            beforeExample = """
                override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
                    val id = mealDao.insertMeal(meal.toEntity())
                    Result.success(id)
                }
            """.trimIndent(),
            afterExample = """
                override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        Result.success(id)
                    } catch (e: Exception) {
                        Result.failure(mapException(e))
                    }
                }
                
                private fun mapException(e: Exception): AppError = when (e) {
                    is SQLiteException -> AppError.DatabaseError("Database error: ${'$'}{e.message}", e)
                    is IOException -> AppError.NetworkError("Network error: ${'$'}{e.message}", e)
                    else -> AppError.UnknownError(e.message ?: "Unknown error", e)
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#handle-exceptions",
                "https://kotlinlang.org/docs/exception-handling.html"
            )
        )
    }
    
    /**
     * Creates finding for missing Result type.
     */
    private fun createMissingResultTypeFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String,
        functionName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Missing Result Type for Failable Operation",
            description = "Function '$functionName' performs operations that can fail but does not use " +
                    "Result<T> as the return type. Functions that can throw exceptions or fail should " +
                    "return Result<T> to make error handling explicit and force callers to handle both " +
                    "success and failure cases.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Change the return type to Result<T> and wrap the result in Result.success() " +
                    "or Result.failure(). This makes error handling explicit and prevents uncaught exceptions " +
                    "from propagating to callers.",
            beforeExample = """
                suspend fun addMeal(meal: Meal): Long {
                    return repository.addMeal(meal)
                }
            """.trimIndent(),
            afterExample = """
                suspend fun addMeal(meal: Meal): Result<Long> {
                    return repository.addMeal(meal)
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-result/",
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#handle-exceptions"
            )
        )
    }
    
    /**
     * Creates finding for exceptions in UI layer.
     */
    private fun createExceptionInUiLayerFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Exception Thrown in UI Layer",
            description = "UI layer code throws an exception directly. Exceptions should not reach the UI layer. " +
                    "All error handling should be done in the repository or use case layer, and errors should " +
                    "be communicated to the UI through Result types or sealed error states. Throwing exceptions " +
                    "in the UI layer can crash the app and provides poor user experience.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Remove the throw statement and handle errors through Result types or UI state. " +
                    "ViewModels should catch errors from use cases and update the UI state to show error messages " +
                    "to users instead of crashing the app.",
            beforeExample = """
                fun loadMeals() {
                    viewModelScope.launch {
                        if (meals.isEmpty()) {
                            throw IllegalStateException("No meals found")
                        }
                        _uiState.update { MealUiState.Success(meals) }
                    }
                }
            """.trimIndent(),
            afterExample = """
                fun loadMeals() {
                    viewModelScope.launch {
                        getMealsUseCase().collect { result ->
                            result.fold(
                                onSuccess = { meals ->
                                    if (meals.isEmpty()) {
                                        _uiState.update { MealUiState.Empty }
                                    } else {
                                        _uiState.update { MealUiState.Success(meals) }
                                    }
                                },
                                onFailure = { error ->
                                    _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                                }
                            )
                        }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/topic/architecture/ui-layer#handle-errors",
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#handle-exceptions"
            )
        )
    }
    
    /**
     * Creates finding for empty catch block.
     */
    private fun createEmptyCatchBlockFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String,
        exceptionType: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Empty or Logging-Only Catch Block",
            description = "Catch block for '$exceptionType' is empty or only contains logging statements. " +
                    "Empty catch blocks silently swallow exceptions, making debugging difficult and hiding " +
                    "potential issues. Exceptions should be properly handled, logged with context, and " +
                    "communicated to callers through Result types or error states.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Handle the exception appropriately: log it with context, map it to a domain error, " +
                    "and return a Result.failure() or update error state. If the exception is truly ignorable, " +
                    "add a comment explaining why.",
            beforeExample = """
                try {
                    val meals = mealDao.getAllMeals()
                    return Result.success(meals)
                } catch (e: SQLiteException) {
                    // Empty catch block - BAD
                }
            """.trimIndent(),
            afterExample = """
                try {
                    val meals = mealDao.getAllMeals()
                    return Result.success(meals)
                } catch (e: SQLiteException) {
                    Timber.e(e, "Failed to load meals from database")
                    return Result.failure(AppError.DatabaseError("Failed to load meals", e))
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/exception-handling.html",
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#handle-exceptions"
            )
        )
    }
    
    /**
     * Creates finding for generic Exception catch.
     */
    private fun createGenericExceptionCatchFinding(
        file: FileInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Generic Exception Catch",
            description = "Catch block catches generic 'Exception' type. While this is sometimes necessary, " +
                    "it's better to catch specific exception types when possible. This allows for more " +
                    "precise error handling and makes it clear what types of errors are expected. " +
                    "Generic catches can hide unexpected errors and make debugging harder.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Consider catching specific exception types (SQLiteException, IOException, etc.) " +
                    "when you know what exceptions can be thrown. Use generic Exception catch only as a last " +
                    "resort for truly unexpected errors, and log them appropriately.",
            beforeExample = """
                try {
                    val meals = mealDao.getAllMeals()
                    return Result.success(meals)
                } catch (e: Exception) {  // Too generic
                    return Result.failure(mapException(e))
                }
            """.trimIndent(),
            afterExample = """
                try {
                    val meals = mealDao.getAllMeals()
                    return Result.success(meals)
                } catch (e: SQLiteException) {
                    return Result.failure(AppError.DatabaseError("Database error", e))
                } catch (e: Exception) {
                    // Catch unexpected errors
                    Timber.e(e, "Unexpected error loading meals")
                    return Result.failure(AppError.UnknownError("Unexpected error", e))
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://kotlinlang.org/docs/exception-handling.html",
                "https://developer.android.com/kotlin/coroutines/coroutines-best-practices#handle-exceptions"
            )
        )
    }
}
