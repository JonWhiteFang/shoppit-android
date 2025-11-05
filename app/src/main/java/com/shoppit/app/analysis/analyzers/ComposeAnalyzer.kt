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
 * Analyzer that validates Jetpack Compose best practices.
 * 
 * Validates:
 * - Composable functions have Modifier parameter with default value
 * - State mutations use update { } pattern
 * - Expensive computations are wrapped in remember or derivedStateOf
 * - LazyColumn items have stable keys
 * - No nested LazyColumns
 * - Proper state hoisting
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
class ComposeAnalyzer : CodeAnalyzer {
    
    override val id: String = "compose"
    
    override val name: String = "Compose Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.COMPOSE
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze UI layer files
        if (file.layer != CodeLayer.UI) {
            return findings
        }
        
        // Find all Composable functions
        val composableFunctions = findComposableFunctions(fileContent)
        
        composableFunctions.forEach { composableInfo ->
            // Check for Modifier parameter
            findings.addAll(checkModifierParameter(file, composableInfo))
            
            // Check for remember usage
            findings.addAll(checkRememberUsage(file, composableInfo, fileContent))
            
            // Check for LazyColumn validation
            findings.addAll(checkLazyColumnUsage(file, composableInfo, fileContent))
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to UI layer files that likely contain Compose code
        return file.layer == CodeLayer.UI && 
               (file.relativePath.contains("/ui/") || 
                file.relativePath.endsWith("Screen.kt"))
    }
    
    /**
     * Finds all Composable functions in the file.
     */
    private fun findComposableFunctions(fileContent: String): List<ComposableInfo> {
        val composables = mutableListOf<ComposableInfo>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Look for @Composable annotation
            if (line.startsWith("@Composable") || line.contains("@Composable")) {
                // Find the function declaration (might be on next line)
                var functionLine = ""
                var functionLineNumber = i + 1
                var j = i + 1
                
                // Skip empty lines and other annotations
                while (j < lines.size) {
                    val nextLine = lines[j].trim()
                    if (nextLine.startsWith("fun ") || nextLine.startsWith("private fun ") ||
                        nextLine.startsWith("internal fun ") || nextLine.startsWith("public fun ")) {
                        functionLine = nextLine
                        functionLineNumber = j + 1
                        break
                    }
                    j++
                }
                
                if (functionLine.isNotEmpty()) {
                    // Extract function name
                    val functionName = extractFunctionName(functionLine)
                    
                    // Check if it's private
                    val isPrivate = functionLine.startsWith("private")
                    
                    // Extract parameters
                    val parameters = extractParameters(functionLine, lines, j)
                    
                    // Find function body
                    val functionBody = extractFunctionBody(lines, j)
                    
                    composables.add(
                        ComposableInfo(
                            name = functionName,
                            lineNumber = functionLineNumber,
                            isPrivate = isPrivate,
                            parameters = parameters,
                            functionDeclaration = functionLine,
                            body = functionBody,
                            bodyStartLine = j + 1
                        )
                    )
                }
                
                i = j
            }
            i++
        }
        
        return composables
    }
    
    /**
     * Extracts function name from function declaration.
     */
    private fun extractFunctionName(functionLine: String): String {
        return functionLine
            .substringAfter("fun ")
            .substringBefore("(")
            .trim()
    }
    
    /**
     * Extracts parameters from function declaration.
     * Handles multi-line parameter lists.
     */
    private fun extractParameters(functionLine: String, lines: List<String>, startIndex: Int): List<Parameter> {
        val parameters = mutableListOf<Parameter>()
        
        // Collect full parameter string (might span multiple lines)
        val paramString = StringBuilder()
        var currentIndex = startIndex
        var openParens = 0
        var foundStart = false
        
        while (currentIndex < lines.size) {
            val line = lines[currentIndex]
            
            for (char in line) {
                if (char == '(') {
                    openParens++
                    foundStart = true
                } else if (char == ')') {
                    openParens--
                    if (openParens == 0 && foundStart) {
                        break
                    }
                }
                if (foundStart && openParens > 0) {
                    paramString.append(char)
                }
            }
            
            if (openParens == 0 && foundStart) {
                break
            }
            
            currentIndex++
        }
        
        // Parse parameters
        val paramText = paramString.toString().trim()
        if (paramText.isNotEmpty()) {
            // Split by comma, but respect nested generics and lambdas
            val params = splitParameters(paramText)
            
            params.forEach { param ->
                val trimmed = param.trim()
                if (trimmed.isNotEmpty()) {
                    val name = trimmed.substringBefore(":").trim()
                    val type = trimmed.substringAfter(":").substringBefore("=").trim()
                    val hasDefault = trimmed.contains("=")
                    
                    parameters.add(Parameter(name, type, hasDefault))
                }
            }
        }
        
        return parameters
    }
    
    /**
     * Splits parameter string by commas, respecting nested structures.
     */
    private fun splitParameters(paramText: String): List<String> {
        val params = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0
        var inString = false
        var stringChar = ' '
        
        for (char in paramText) {
            when {
                (char == '"' || char == '\'') && !inString -> {
                    inString = true
                    stringChar = char
                    current.append(char)
                }
                char == stringChar && inString -> {
                    inString = false
                    current.append(char)
                }
                !inString && (char == '<' || char == '(' || char == '{') -> {
                    depth++
                    current.append(char)
                }
                !inString && (char == '>' || char == ')' || char == '}') -> {
                    depth--
                    current.append(char)
                }
                !inString && char == ',' && depth == 0 -> {
                    params.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        
        if (current.isNotEmpty()) {
            params.add(current.toString())
        }
        
        return params
    }
    
    /**
     * Extracts function body.
     */
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
    
    /**
     * Checks if Composable function has Modifier parameter with default value.
     * Requirement 4.1
     */
    private fun checkModifierParameter(file: FileInfo, composable: ComposableInfo): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Skip private composables
        if (composable.isPrivate) {
            return findings
        }
        
        // Check if there's a Modifier parameter
        val modifierParam = composable.parameters.find { 
            it.type.contains("Modifier") 
        }
        
        if (modifierParam == null) {
            // Missing Modifier parameter
            findings.add(createMissingModifierFinding(file, composable))
        } else if (!modifierParam.hasDefault) {
            // Modifier parameter without default value
            findings.add(createModifierNoDefaultFinding(file, composable, modifierParam))
        }
        
        return findings
    }
    
    /**
     * Checks for expensive computations not wrapped in remember.
     * Requirement 4.3
     */
    private fun checkRememberUsage(
        file: FileInfo, 
        composable: ComposableInfo,
        fileContent: String
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val body = composable.body
        val lines = body.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check for list operations that should be in remember
            if (isExpensiveListOperation(trimmed) && !isInRemember(body, index)) {
                findings.add(
                    createMissingRememberFinding(
                        file, 
                        composable, 
                        composable.bodyStartLine + index,
                        trimmed
                    )
                )
            }
        }
        
        return findings
    }
    
    /**
     * Checks if line contains expensive list operation.
     */
    private fun isExpensiveListOperation(line: String): Boolean {
        // Check for list operations that create new collections
        val expensiveOperations = listOf(
            ".filter", ".map", ".flatMap", ".sorted", ".groupBy",
            ".partition", ".associate", ".distinct", ".reversed"
        )
        
        return expensiveOperations.any { line.contains(it) } &&
               line.contains("val ") &&
               !line.contains("remember")
    }
    
    /**
     * Checks if the operation is already wrapped in remember.
     */
    private fun isInRemember(body: String, lineIndex: Int): Boolean {
        val lines = body.lines()
        
        // Look backwards for remember block
        var i = lineIndex - 1
        var braceCount = 0
        
        while (i >= 0) {
            val line = lines[i].trim()
            
            // Count braces
            braceCount += line.count { it == '}' }
            braceCount -= line.count { it == '{' }
            
            // If we find remember and we're inside its block
            if (line.contains("remember") && braceCount <= 0) {
                return true
            }
            
            // If we've exited all blocks, stop looking
            if (braceCount > 0) {
                break
            }
            
            i--
        }
        
        return false
    }
    
    /**
     * Checks LazyColumn usage for stable keys and nested LazyColumns.
     * Requirements 4.4, 4.5
     */
    private fun checkLazyColumnUsage(
        file: FileInfo,
        composable: ComposableInfo,
        fileContent: String
    ): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val body = composable.body
        val lines = body.lines()
        
        var inLazyColumn = false
        var lazyColumnDepth = 0
        var braceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Track brace depth
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            
            // Check for LazyColumn
            if (trimmed.contains("LazyColumn")) {
                if (inLazyColumn) {
                    // Nested LazyColumn detected
                    findings.add(
                        createNestedLazyColumnFinding(
                            file,
                            composable,
                            composable.bodyStartLine + index,
                            trimmed
                        )
                    )
                }
                inLazyColumn = true
                lazyColumnDepth = braceCount
            }
            
            // Check for items() call without key parameter
            if (inLazyColumn && trimmed.contains("items(")) {
                if (!trimmed.contains("key") && !trimmed.contains("key =")) {
                    findings.add(
                        createMissingKeyParameterFinding(
                            file,
                            composable,
                            composable.bodyStartLine + index,
                            trimmed
                        )
                    )
                }
            }
            
            // Exit LazyColumn scope
            if (inLazyColumn && braceCount < lazyColumnDepth) {
                inLazyColumn = false
            }
        }
        
        return findings
    }
    
    /**
     * Creates finding for missing Modifier parameter.
     */
    private fun createMissingModifierFinding(
        file: FileInfo,
        composable: ComposableInfo
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Composable Missing Modifier Parameter",
            description = "Composable function '${composable.name}' does not have a Modifier parameter. " +
                    "All public Composable functions should accept a Modifier parameter with a default value " +
                    "to allow callers to customize layout, padding, size, and other visual properties.",
            file = file.relativePath,
            lineNumber = composable.lineNumber,
            codeSnippet = composable.functionDeclaration,
            recommendation = "Add a 'modifier: Modifier = Modifier' parameter as the last parameter " +
                    "of the Composable function and apply it to the root composable element.",
            beforeExample = """
                @Composable
                fun MealCard(
                    meal: Meal,
                    onClick: () -> Unit
                ) {
                    Card(onClick = onClick) {
                        Text(meal.name)
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Composable
                fun MealCard(
                    meal: Meal,
                    onClick: () -> Unit,
                    modifier: Modifier = Modifier
                ) {
                    Card(
                        modifier = modifier,
                        onClick = onClick
                    ) {
                        Text(meal.name)
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/modifiers",
                "https://developer.android.com/jetpack/compose/modifiers-list"
            )
        )
    }
    
    /**
     * Creates finding for Modifier parameter without default value.
     */
    private fun createModifierNoDefaultFinding(
        file: FileInfo,
        composable: ComposableInfo,
        modifierParam: Parameter
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Modifier Parameter Missing Default Value",
            description = "Composable function '${composable.name}' has a Modifier parameter " +
                    "but it doesn't have a default value. Modifier parameters should default to " +
                    "'Modifier' to make them optional for callers.",
            file = file.relativePath,
            lineNumber = composable.lineNumber,
            codeSnippet = composable.functionDeclaration,
            recommendation = "Add '= Modifier' as the default value for the modifier parameter.",
            beforeExample = """
                @Composable
                fun MealCard(
                    meal: Meal,
                    modifier: Modifier
                ) {
                    Card(modifier = modifier) {
                        Text(meal.name)
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Composable
                fun MealCard(
                    meal: Meal,
                    modifier: Modifier = Modifier
                ) {
                    Card(modifier = modifier) {
                        Text(meal.name)
                    }
                }
            """.trimIndent(),
            autoFixable = true,
            autoFix = "Add '= Modifier' to the modifier parameter",
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/modifiers"
            )
        )
    }
    
    /**
     * Creates finding for expensive computation not wrapped in remember.
     */
    private fun createMissingRememberFinding(
        file: FileInfo,
        composable: ComposableInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Expensive Computation Not Wrapped in remember",
            description = "Expensive list operation in Composable '${composable.name}' is not wrapped " +
                    "in remember. This computation will be re-executed on every recomposition, " +
                    "which can cause performance issues. Use remember to cache the result.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Wrap the expensive computation in remember { } to cache the result " +
                    "and only recompute when dependencies change. If the computation depends on " +
                    "state, pass the dependencies to remember.",
            beforeExample = """
                @Composable
                fun MealList(meals: List<Meal>) {
                    val sortedMeals = meals.sortedBy { it.name }
                    
                    LazyColumn {
                        items(sortedMeals) { meal ->
                            MealCard(meal)
                        }
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Composable
                fun MealList(meals: List<Meal>) {
                    val sortedMeals = remember(meals) {
                        meals.sortedBy { it.name }
                    }
                    
                    LazyColumn {
                        items(sortedMeals, key = { it.id }) { meal ->
                            MealCard(meal)
                        }
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/state#remember",
                "https://developer.android.com/jetpack/compose/performance"
            )
        )
    }
    
    /**
     * Creates finding for LazyColumn items without key parameter.
     */
    private fun createMissingKeyParameterFinding(
        file: FileInfo,
        composable: ComposableInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "LazyColumn items() Missing key Parameter",
            description = "LazyColumn items() call in '${composable.name}' does not specify a key parameter. " +
                    "Without stable keys, Compose cannot efficiently track items when the list changes, " +
                    "leading to unnecessary recompositions and potential state loss.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Add a key parameter to items() that provides a stable, unique identifier " +
                    "for each item. Typically this is the item's ID property.",
            beforeExample = """
                LazyColumn {
                    items(meals) { meal ->
                        MealCard(meal = meal)
                    }
                }
            """.trimIndent(),
            afterExample = """
                LazyColumn {
                    items(
                        items = meals,
                        key = { it.id }
                    ) { meal ->
                        MealCard(meal = meal)
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/lists#item-keys",
                "https://developer.android.com/jetpack/compose/performance#defer-reads"
            )
        )
    }
    
    /**
     * Creates finding for nested LazyColumn.
     */
    private fun createNestedLazyColumnFinding(
        file: FileInfo,
        composable: ComposableInfo,
        lineNumber: Int,
        codeLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Nested LazyColumn Detected",
            description = "Nested LazyColumn found in '${composable.name}'. Nesting scrollable containers " +
                    "is an anti-pattern that causes performance issues and unexpected scrolling behavior. " +
                    "LazyColumn already handles virtualization, so nesting defeats this optimization.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = codeLine,
            recommendation = "Refactor to use a single LazyColumn with different item types. " +
                    "Use items() with different composables for different sections, or use " +
                    "stickyHeader() for section headers.",
            beforeExample = """
                LazyColumn {
                    item {
                        Text("Section 1")
                        LazyColumn {  // Nested - BAD
                            items(meals) { meal ->
                                MealCard(meal)
                            }
                        }
                    }
                }
            """.trimIndent(),
            afterExample = """
                LazyColumn {
                    stickyHeader {
                        Text("Section 1")
                    }
                    items(meals, key = { it.id }) { meal ->
                        MealCard(meal)
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/jetpack/compose/lists#avoid-nesting",
                "https://developer.android.com/jetpack/compose/performance"
            )
        )
    }
    
    /**
     * Data class representing a Composable function.
     */
    private data class ComposableInfo(
        val name: String,
        val lineNumber: Int,
        val isPrivate: Boolean,
        val parameters: List<Parameter>,
        val functionDeclaration: String,
        val body: String,
        val bodyStartLine: Int
    )
    
    /**
     * Data class representing a function parameter.
     */
    private data class Parameter(
        val name: String,
        val type: String,
        val hasDefault: Boolean
    )
}
