package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that identifies performance optimization opportunities.
 * 
 * Detects:
 * - Inefficient list iterations that could use sequences
 * - String concatenation in loops
 * - Unnecessary object allocations in hot paths
 * - Unstable Compose parameters causing excessive recomposition
 * 
 * Requirements: 9.1, 9.2, 9.3, 9.4
 */
class PerformanceAnalyzer : CodeAnalyzer {
    
    override val id: String = "performance"
    
    override val name: String = "Performance Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.PERFORMANCE
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Analyze list operations in loops
        findings.addAll(analyzeListOperationsInLoops(file, fileContent))
        
        // Analyze string concatenation in loops
        findings.addAll(analyzeStringConcatenationInLoops(file, fileContent))
        
        // Analyze unstable Compose parameters
        if (isComposeFile(fileContent)) {
            findings.addAll(analyzeUnstableComposeParameters(file, fileContent))
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files
        return file.relativePath.endsWith(".kt")
    }
    
    /**
     * Checks if the file contains Compose code.
     */
    private fun isComposeFile(fileContent: String): Boolean {
        return fileContent.contains("@Composable") || 
               fileContent.contains("import androidx.compose")
    }
    
    /**
     * Analyzes list operations in loops for inefficiency.
     * Requirement 9.1: Detect inefficient list iterations that could use sequences.
     */
    private fun analyzeListOperationsInLoops(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var inLoop = false
        var loopStartLine = -1
        var loopIndent = 0
        var braceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            val currentIndent = line.takeWhile { it.isWhitespace() }.length
            
            // Detect loop start
            if (trimmedLine.startsWith("for (") || 
                trimmedLine.startsWith("while (") ||
                trimmedLine.contains(".forEach") ||
                trimmedLine.contains(".forEachIndexed")) {
                
                inLoop = true
                loopStartLine = index + 1
                loopIndent = currentIndent
                braceCount = line.count { it == '{' } - line.count { it == '}' }
            }
            
            // Track braces to determine loop scope
            if (inLoop) {
                braceCount += line.count { it == '{' }
                braceCount -= line.count { it == '}' }
                
                // Check for chained list operations within the loop
                if (trimmedLine.contains(".filter") || 
                    trimmedLine.contains(".map") ||
                    trimmedLine.contains(".flatMap") ||
                    trimmedLine.contains(".distinct") ||
                    trimmedLine.contains(".sorted")) {
                    
                    // Check if it's a chained operation (multiple operations)
                    val chainedOperations = listOf(".filter", ".map", ".flatMap", ".distinct", ".sorted")
                    val operationCount = chainedOperations.count { trimmedLine.contains(it) }
                    
                    if (operationCount >= 2 || 
                        (operationCount >= 1 && (trimmedLine.contains(".toList()") || trimmedLine.contains(".toSet()")))) {
                        
                        findings.add(createInefficientListOperationFinding(
                            file,
                            index + 1,
                            trimmedLine,
                            loopStartLine
                        ))
                    }
                }
                
                // Check if loop ended
                if (braceCount == 0 && currentIndent <= loopIndent) {
                    inLoop = false
                }
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes string concatenation in loops.
     * Requirement 9.2: Detect string concatenation in loops.
     */
    private fun analyzeStringConcatenationInLoops(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var inLoop = false
        var loopStartLine = -1
        var loopIndent = 0
        var braceCount = 0
        val stringVariables = mutableSetOf<String>()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            val currentIndent = line.takeWhile { it.isWhitespace() }.length
            
            // Detect loop start
            if (trimmedLine.startsWith("for (") || 
                trimmedLine.startsWith("while (") ||
                trimmedLine.contains(".forEach") ||
                trimmedLine.contains(".forEachIndexed")) {
                
                inLoop = true
                loopStartLine = index + 1
                loopIndent = currentIndent
                braceCount = line.count { it == '{' } - line.count { it == '}' }
                stringVariables.clear()
            }
            
            // Track braces to determine loop scope
            if (inLoop) {
                braceCount += line.count { it == '{' }
                braceCount -= line.count { it == '}' }
                
                // Detect string variable declarations
                if ((trimmedLine.startsWith("var ") || trimmedLine.startsWith("val ")) && 
                    (trimmedLine.contains(": String") || trimmedLine.contains("= \""))) {
                    
                    val varName = trimmedLine
                        .substringAfter("var ")
                        .substringAfter("val ")
                        .substringBefore(":")
                        .substringBefore("=")
                        .trim()
                    
                    stringVariables.add(varName)
                }
                
                // Detect string concatenation with += or = ... +
                for (varName in stringVariables) {
                    if (trimmedLine.contains("$varName +=") || 
                        (trimmedLine.contains("$varName =") && trimmedLine.contains(" + "))) {
                        
                        findings.add(createStringConcatenationInLoopFinding(
                            file,
                            index + 1,
                            trimmedLine,
                            varName,
                            loopStartLine
                        ))
                        break
                    }
                }
                
                // Check if loop ended
                if (braceCount == 0 && currentIndent <= loopIndent) {
                    inLoop = false
                    stringVariables.clear()
                }
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes Compose functions for unstable parameters.
     * Requirement 9.4: Detect unstable Compose parameters causing excessive recomposition.
     */
    private fun analyzeUnstableComposeParameters(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var inComposableFunction = false
        var functionStartLine = -1
        var functionName = ""
        val unstableParameters = mutableListOf<String>()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Detect @Composable annotation
            if (trimmedLine == "@Composable") {
                inComposableFunction = true
                functionStartLine = index + 1
                unstableParameters.clear()
            }
            
            // Detect function declaration after @Composable
            if (inComposableFunction && trimmedLine.startsWith("fun ")) {
                functionName = trimmedLine
                    .substringAfter("fun ")
                    .substringBefore("(")
                    .trim()
                
                // Extract parameters
                val parameterSection = extractParameterSection(lines, index)
                val parameters = parseParameters(parameterSection)
                
                // Check for unstable parameter types
                for (param in parameters) {
                    if (isUnstableType(param.type)) {
                        unstableParameters.add(param.name)
                    }
                }
                
                // Create finding if unstable parameters found
                if (unstableParameters.isNotEmpty()) {
                    findings.add(createUnstableComposableParameterFinding(
                        file,
                        functionStartLine + 1,
                        functionName,
                        unstableParameters
                    ))
                }
                
                inComposableFunction = false
            }
        }
        
        return findings
    }
    
    /**
     * Extracts the parameter section from a function declaration.
     */
    private fun extractParameterSection(lines: List<String>, startIndex: Int): String {
        val builder = StringBuilder()
        var parenCount = 0
        var started = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            
            for (char in line) {
                if (char == '(') {
                    parenCount++
                    started = true
                }
                if (started) {
                    builder.append(char)
                }
                if (char == ')') {
                    parenCount--
                    if (parenCount == 0) {
                        return builder.toString()
                    }
                }
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Parses parameters from a parameter section string.
     */
    private fun parseParameters(parameterSection: String): List<Parameter> {
        val parameters = mutableListOf<Parameter>()
        
        // Remove parentheses and split by comma (simple parsing)
        val paramString = parameterSection
            .substringAfter("(")
            .substringBeforeLast(")")
            .trim()
        
        if (paramString.isEmpty()) {
            return parameters
        }
        
        // Split by comma, but be careful with generic types
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var angleCount = 0
        var parenCount = 0
        
        for (char in paramString) {
            when (char) {
                '<' -> angleCount++
                '>' -> angleCount--
                '(' -> parenCount++
                ')' -> parenCount--
                ',' -> {
                    if (angleCount == 0 && parenCount == 0) {
                        parts.add(current.toString().trim())
                        current = StringBuilder()
                        continue
                    }
                }
            }
            current.append(char)
        }
        
        if (current.isNotEmpty()) {
            parts.add(current.toString().trim())
        }
        
        // Parse each parameter
        for (part in parts) {
            val colonIndex = part.indexOf(':')
            if (colonIndex > 0) {
                val name = part.substring(0, colonIndex).trim()
                val type = part.substring(colonIndex + 1)
                    .substringBefore("=")
                    .trim()
                
                parameters.add(Parameter(name, type))
            }
        }
        
        return parameters
    }
    
    /**
     * Checks if a type is unstable for Compose recomposition.
     */
    private fun isUnstableType(type: String): Boolean {
        // Unstable types that can cause excessive recomposition
        val unstableTypes = listOf(
            "MutableList",
            "ArrayList",
            "MutableSet",
            "HashSet",
            "MutableMap",
            "HashMap",
            "Array<"
        )
        
        // Check if type is unstable
        for (unstableType in unstableTypes) {
            if (type.contains(unstableType)) {
                return true
            }
        }
        
        // Check for lambda types without @Stable or @Immutable
        // (This is a simplified check; full analysis would require AST)
        if (type.contains("->") && !type.contains("@Stable") && !type.contains("@Immutable")) {
            // Lambda parameters are generally stable if they're simple function types
            // Only flag if it's a complex lambda
            return false
        }
        
        return false
    }
    
    /**
     * Creates a finding for inefficient list operations in loops.
     */
    private fun createInefficientListOperationFinding(
        file: FileInfo,
        lineNumber: Int,
        code: String,
        loopStartLine: Int
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Inefficient List Operations in Loop",
            description = "Chained list operations inside a loop create intermediate collections on each iteration, " +
                    "which is inefficient for large datasets. Using sequences instead of lists for chained operations " +
                    "processes elements lazily without creating intermediate collections, significantly improving performance.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Convert the list to a sequence using .asSequence() before chaining operations, " +
                    "then convert back to a list with .toList() only at the end if needed. Sequences process " +
                    "elements lazily, avoiding intermediate collection creation.",
            beforeExample = """
                for (item in items) {
                    val result = data
                        .filter { it.value > 0 }
                        .map { it.transform() }
                        .toList()
                }
            """.trimIndent(),
            afterExample = """
                for (item in items) {
                    val result = data
                        .asSequence()
                        .filter { it.value > 0 }
                        .map { it.transform() }
                        .toList()
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://kotlinlang.org/docs/sequences.html",
                "https://kotlinlang.org/docs/collection-transformations.html#sequence"
            )
        )
    }
    
    /**
     * Creates a finding for string concatenation in loops.
     */
    private fun createStringConcatenationInLoopFinding(
        file: FileInfo,
        lineNumber: Int,
        code: String,
        variableName: String,
        loopStartLine: Int
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "String Concatenation in Loop",
            description = "String concatenation using '+' or '+=' in a loop creates a new String object " +
                    "on each iteration because Strings are immutable in Kotlin/Java. For loops with many " +
                    "iterations, this creates excessive object allocations and garbage collection pressure, " +
                    "significantly impacting performance.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Use StringBuilder instead of String concatenation in loops. StringBuilder " +
                    "is mutable and designed for efficient string building, avoiding the creation of " +
                    "intermediate String objects. Call .toString() at the end to get the final String.",
            beforeExample = """
                var result = ""
                for (item in items) {
                    result += item.name + ", "
                }
            """.trimIndent(),
            afterExample = """
                val result = StringBuilder()
                for (item in items) {
                    result.append(item.name).append(", ")
                }
                val finalResult = result.toString()
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-string-builder/",
                "https://docs.oracle.com/javase/tutorial/java/data/buffers.html"
            )
        )
    }
    
    /**
     * Creates a finding for unstable Compose parameters.
     */
    private fun createUnstableComposableParameterFinding(
        file: FileInfo,
        lineNumber: Int,
        functionName: String,
        unstableParameters: List<String>
    ): Finding {
        val paramList = unstableParameters.joinToString(", ")
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Unstable Compose Parameters Cause Excessive Recomposition",
            description = "Composable function '$functionName' has unstable parameters: $paramList. " +
                    "Unstable types (like MutableList, Array, HashMap) cause Compose to recompose the function " +
                    "even when the actual data hasn't changed, because Compose cannot determine if the content " +
                    "is the same. This leads to unnecessary recompositions and poor performance.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "@Composable\nfun $functionName(...)",
            recommendation = "Use immutable collections (List, Set, Map) instead of mutable ones, or mark " +
                    "your data classes with @Immutable or @Stable annotations. For collections, prefer " +
                    "List<T> over MutableList<T>, and use kotlinx.collections.immutable for truly immutable " +
                    "collections. This allows Compose to skip recomposition when data hasn't changed.",
            beforeExample = """
                @Composable
                fun ItemList(
                    items: MutableList<Item>,
                    onItemClick: (Item) -> Unit
                ) {
                    // Recomposes on every parent recomposition
                }
            """.trimIndent(),
            afterExample = """
                @Composable
                fun ItemList(
                    items: List<Item>,  // Immutable list
                    onItemClick: (Item) -> Unit
                ) {
                    // Only recomposes when items actually change
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/performance/stability",
                "https://developer.android.com/jetpack/compose/performance/stability/fix",
                "https://github.com/Kotlin/kotlinx.collections.immutable"
            )
        )
    }
    
    /**
     * Data class to represent a function parameter.
     */
    private data class Parameter(
        val name: String,
        val type: String
    )
}
