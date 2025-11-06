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
 * Analyzer that validates Jetpack Compose best practices.
 * 
 * Validates:
 * - Composable functions have Modifier parameter with default value
 * - Expensive computations are wrapped in remember or derivedStateOf
 * - LazyColumn items have stable keys
 * - No nested LazyColumns
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
class ComposeAnalyzer : CodeAnalyzer {
    
    override val id: String = "compose"
    
    override val name: String = "Compose Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.COMPOSE
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze UI layer files
        if (file.layer != CodeLayer.UI) {
            return findings
        }
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                // Check if function is annotated with @Composable
                if (isComposable(function)) {
                    // Check for Modifier parameter
                    findings.addAll(checkModifierParameter(file, function))
                    
                    // Check for LazyColumn usage
                    findings.addAll(checkLazyColumnUsage(file, function))
                    
                    // Check for expensive computations not in remember
                    findings.addAll(checkRememberUsage(file, function))
                }
            }
        })
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to UI layer files that likely contain Compose code
        return file.layer == CodeLayer.UI && 
               (file.relativePath.contains("/ui/") || 
                file.relativePath.endsWith("Screen.kt"))
    }
    
    /**
     * Checks if a function is annotated with @Composable.
     */
    private fun isComposable(function: KtNamedFunction): Boolean {
        return function.annotationEntries.any { 
            it.shortName?.asString() == "Composable" 
        }
    }
    
    /**
     * Checks if Composable function has Modifier parameter with default value.
     */
    private fun checkModifierParameter(file: FileInfo, function: KtNamedFunction): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Skip private composables
        if (function.isPrivate()) {
            return findings
        }
        
        val functionName = function.name ?: "anonymous"
        val parameters = function.valueParameters
        
        // Check if there's a Modifier parameter
        val modifierParam = parameters.find { param ->
            param.typeReference?.text?.contains("Modifier") == true
        }
        
        if (modifierParam == null) {
            // Missing Modifier parameter
            findings.add(createMissingModifierFinding(file, function, functionName))
        } else if (!hasDefaultValue(modifierParam)) {
            // Modifier parameter without default value
            findings.add(createModifierNoDefaultFinding(file, function, functionName))
        }
        
        return findings
    }
    
    /**
     * Checks if parameter has a default value.
     */
    private fun hasDefaultValue(parameter: KtParameter): Boolean {
        return parameter.hasDefaultValue()
    }
    
    /**
     * Checks for LazyColumn usage issues.
     */
    private fun checkLazyColumnUsage(file: FileInfo, function: KtNamedFunction): List<Finding> {
        val findings = mutableListOf<Finding>()
        var lazyColumnDepth = 0
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitCallExpression(expression: KtCallExpression) {
                super.visitCallExpression(expression)
                
                val callName = expression.calleeExpression?.text
                
                // Check for LazyColumn
                if (callName == "LazyColumn") {
                    lazyColumnDepth++
                    
                    // Check for nested LazyColumn
                    if (lazyColumnDepth > 1) {
                        findings.add(createNestedLazyColumnFinding(file, function, expression))
                    }
                }
                
                // Check for items() call without key parameter
                if (callName == "items" && lazyColumnDepth > 0) {
                    val hasKeyParam = expression.valueArguments.any { arg ->
                        arg.getArgumentName()?.asName?.asString() == "key"
                    }
                    
                    if (!hasKeyParam) {
                        findings.add(createMissingKeyParameterFinding(file, function, expression))
                    }
                }
            }
        })
        
        return findings
    }
    
    /**
     * Checks for expensive computations not wrapped in remember.
     */
    private fun checkRememberUsage(file: FileInfo, function: KtNamedFunction): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                
                // Check if property initialization contains expensive operations
                val initializer = property.initializer
                if (initializer != null && isExpensiveOperation(initializer)) {
                    // Check if wrapped in remember
                    if (!isInRememberBlock(initializer)) {
                        findings.add(createMissingRememberFinding(file, function, property))
                    }
                }
            }
        })
        
        return findings
    }
    
    /**
     * Checks if expression contains expensive operations.
     */
    private fun isExpensiveOperation(expression: KtExpression): Boolean {
        var hasExpensiveOp = false
        
        expression.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                
                val selectorName = expression.selectorExpression?.text
                
                // Check for expensive list operations
                if (selectorName != null && listOf(
                    "filter", "map", "flatMap", "sorted", "sortedBy",
                    "groupBy", "partition", "associate", "distinct"
                ).any { selectorName.startsWith(it) }) {
                    hasExpensiveOp = true
                }
            }
        })
        
        return hasExpensiveOp
    }
    
    /**
     * Checks if expression is inside a remember block.
     */
    private fun isInRememberBlock(expression: KtExpression): Boolean {
        var parent = expression.parent
        
        while (parent != null) {
            if (parent is KtCallExpression) {
                val callName = parent.calleeExpression?.text
                if (callName == "remember" || callName == "derivedStateOf") {
                    return true
                }
            }
            parent = parent.parent
        }
        
        return false
    }
    
    /**
     * Creates finding for missing Modifier parameter.
     */
    private fun createMissingModifierFinding(
        file: FileInfo,
        function: KtNamedFunction,
        functionName: String
    ): Finding {
        return Finding(
            id = "compose-missing-modifier-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Composable Missing Modifier Parameter",
            description = "Composable function '$functionName' does not have a Modifier parameter. " +
                    "All public Composable functions should accept a Modifier parameter with a default value " +
                    "to allow callers to customize layout, padding, size, and other visual properties.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = getCodeSnippet(function.text, maxLines = 5),
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/modifiers",
                "docs/compose-patterns.md"
            )
        )
    }
    
    /**
     * Creates finding for Modifier parameter without default value.
     */
    private fun createModifierNoDefaultFinding(
        file: FileInfo,
        function: KtNamedFunction,
        functionName: String
    ): Finding {
        return Finding(
            id = "compose-modifier-no-default-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Modifier Parameter Missing Default Value",
            description = "Composable function '$functionName' has a Modifier parameter " +
                    "but it doesn't have a default value. Modifier parameters should default to " +
                    "'Modifier' to make them optional for callers.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = getCodeSnippet(function.text, maxLines = 5),
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/modifiers"
            )
        )
    }
    
    /**
     * Creates finding for nested LazyColumn.
     */
    private fun createNestedLazyColumnFinding(
        file: FileInfo,
        function: KtNamedFunction,
        expression: KtCallExpression
    ): Finding {
        return Finding(
            id = "compose-nested-lazycolumn-${file.path}-${getLineNumber(expression)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Nested LazyColumn Detected",
            description = "Nested LazyColumn found in '${function.name}'. Nesting scrollable containers " +
                    "is an anti-pattern that causes performance issues and unexpected scrolling behavior.",
            file = file.path,
            lineNumber = getLineNumber(expression),
            codeSnippet = expression.text,
            recommendation = "Refactor to use a single LazyColumn with different item types. " +
                    "Use items() with different composables for different sections.",
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
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/jetpack/compose/lists#avoid-nesting"
            )
        )
    }
    
    /**
     * Creates finding for LazyColumn items without key parameter.
     */
    private fun createMissingKeyParameterFinding(
        file: FileInfo,
        function: KtNamedFunction,
        expression: KtCallExpression
    ): Finding {
        return Finding(
            id = "compose-missing-key-${file.path}-${getLineNumber(expression)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "LazyColumn items() Missing key Parameter",
            description = "LazyColumn items() call in '${function.name}' does not specify a key parameter. " +
                    "Without stable keys, Compose cannot efficiently track items when the list changes.",
            file = file.path,
            lineNumber = getLineNumber(expression),
            codeSnippet = expression.text,
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/lists#item-keys"
            )
        )
    }
    
    /**
     * Creates finding for expensive computation not wrapped in remember.
     */
    private fun createMissingRememberFinding(
        file: FileInfo,
        function: KtNamedFunction,
        property: KtProperty
    ): Finding {
        val propertyName = property.name ?: "value"
        
        return Finding(
            id = "compose-missing-remember-${file.path}-${getLineNumber(property)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Expensive Computation Not Wrapped in remember",
            description = "Expensive operation in property '$propertyName' is not wrapped in remember. " +
                    "This computation will be re-executed on every recomposition, causing performance issues.",
            file = file.path,
            lineNumber = getLineNumber(property),
            codeSnippet = property.text,
            recommendation = "Wrap the expensive computation in remember { } to cache the result " +
                    "and only recompute when dependencies change.",
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
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/jetpack/compose/state#remember"
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
