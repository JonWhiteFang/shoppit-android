package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import org.jetbrains.kotlin.psi.*

/**
 * Analyzer for detecting code smells and anti-patterns.
 * 
 * Detects:
 * - Long functions (> 50 lines)
 * - Large classes (> 300 lines)
 * - High cyclomatic complexity (> 15)
 * - Deep nesting (> 4 levels)
 * - Too many parameters (> 5)
 */
class CodeSmellAnalyzer : CodeAnalyzer {
    
    override val id = "code-smell"
    override val name = "Code Smell Analyzer"
    override val category = AnalysisCategory.CODE_SMELL
    
    companion object {
        private const val MAX_FUNCTION_LINES = 50
        private const val MAX_CLASS_LINES = 300
        private const val MAX_COMPLEXITY = 15
        private const val MAX_NESTING_DEPTH = 4
        private const val MAX_PARAMETERS = 5
    }
    
    override suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        ast.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                // Check function length
                val lineCount = countLines(function.text)
                if (lineCount > MAX_FUNCTION_LINES) {
                    findings.add(createLongFunctionFinding(file, function, lineCount))
                }
                
                // Check parameter count
                val paramCount = function.valueParameters.size
                if (paramCount > MAX_PARAMETERS) {
                    findings.add(createTooManyParametersFinding(file, function, paramCount))
                }
                
                // Check cyclomatic complexity
                val complexity = calculateComplexity(function)
                if (complexity > MAX_COMPLEXITY) {
                    findings.add(createHighComplexityFinding(file, function, complexity))
                }
                
                // Check nesting depth
                val maxDepth = calculateMaxNestingDepth(function)
                if (maxDepth > MAX_NESTING_DEPTH) {
                    findings.add(createDeepNestingFinding(file, function, maxDepth))
                }
            }
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                
                val lineCount = countLines(klass.text)
                if (lineCount > MAX_CLASS_LINES) {
                    findings.add(createLargeClassFinding(file, klass, lineCount))
                }
            }
        })
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean = true
    
    /**
     * Counts non-empty lines in text.
     */
    private fun countLines(text: String): Int {
        return text.lines().count { it.trim().isNotEmpty() }
    }
    
    /**
     * Calculates cyclomatic complexity of a function.
     * Complexity = 1 + number of decision points
     */
    private fun calculateComplexity(function: KtNamedFunction): Int {
        var complexity = 1
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)
                complexity++
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                super.visitWhenExpression(expression)
                // Each when entry adds to complexity
                complexity += expression.entries.size
            }
            
            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)
                complexity++
            }
            
            override fun visitWhileExpression(expression: KtWhileExpression) {
                super.visitWhileExpression(expression)
                complexity++
            }
            
            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                super.visitDoWhileExpression(expression)
                complexity++
            }
            
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                // Count && and || operators
                val op = expression.operationReference.text
                if (op == "&&" || op == "||") {
                    complexity++
                }
            }
        })
        
        return complexity
    }
    
    /**
     * Calculates maximum nesting depth in a function.
     */
    private fun calculateMaxNestingDepth(function: KtNamedFunction): Int {
        var maxDepth = 0
        var currentDepth = 0
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitIfExpression(expression)
                currentDepth--
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitWhenExpression(expression)
                currentDepth--
            }
            
            override fun visitForExpression(expression: KtForExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitForExpression(expression)
                currentDepth--
            }
            
            override fun visitWhileExpression(expression: KtWhileExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitWhileExpression(expression)
                currentDepth--
            }
            
            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitDoWhileExpression(expression)
                currentDepth--
            }
        })
        
        return maxDepth
    }
    
    /**
     * Creates a finding for a long function.
     */
    private fun createLongFunctionFinding(
        file: FileInfo,
        function: KtNamedFunction,
        lineCount: Int
    ): Finding {
        val functionName = function.name ?: "anonymous"
        val snippet = getCodeSnippet(function.text, maxLines = 10)
        
        return Finding(
            id = "code-smell-long-function-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Long Function: $functionName",
            description = "Function '$functionName' has $lineCount lines, exceeding the recommended maximum of $MAX_FUNCTION_LINES lines. " +
                    "Long functions are harder to understand, test, and maintain.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = snippet,
            recommendation = "Break this function into smaller, focused functions. Each function should do one thing well. " +
                    "Consider extracting logical blocks into separate functions with descriptive names.",
            beforeExample = """
                fun processOrder(order: Order) {
                    // 50+ lines of code doing multiple things
                    validateOrder(order)
                    calculateTotals(order)
                    applyDiscounts(order)
                    processPayment(order)
                    updateInventory(order)
                    sendConfirmation(order)
                }
            """.trimIndent(),
            afterExample = """
                fun processOrder(order: Order) {
                    validateOrder(order)
                    val totals = calculateOrderTotals(order)
                    val finalAmount = applyDiscounts(totals)
                    processPayment(order, finalAmount)
                    updateInventory(order)
                    sendOrderConfirmation(order)
                }
                
                private fun calculateOrderTotals(order: Order): OrderTotals {
                    // Focused calculation logic
                }
                
                private fun applyDiscounts(totals: OrderTotals): Money {
                    // Focused discount logic
                }
            """.trimIndent(),
            effort = Effort.MEDIUM,
            references = listOf(
                "https://refactoring.guru/smells/long-method",
                "Clean Code by Robert C. Martin - Functions chapter"
            )
        )
    }
    
    /**
     * Creates a finding for a large class.
     */
    private fun createLargeClassFinding(
        file: FileInfo,
        klass: KtClass,
        lineCount: Int
    ): Finding {
        val className = klass.name ?: "anonymous"
        val snippet = getCodeSnippet(klass.text, maxLines = 10)
        
        return Finding(
            id = "code-smell-large-class-${file.path}-${getLineNumber(klass)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Large Class: $className",
            description = "Class '$className' has $lineCount lines, exceeding the recommended maximum of $MAX_CLASS_LINES lines. " +
                    "Large classes often violate the Single Responsibility Principle and are difficult to maintain.",
            file = file.path,
            lineNumber = getLineNumber(klass),
            codeSnippet = snippet,
            recommendation = "Break this class into smaller, focused classes. Each class should have a single, well-defined responsibility. " +
                    "Consider extracting related functionality into separate classes or using composition.",
            beforeExample = """
                class OrderManager {
                    // 300+ lines handling orders, payments, inventory, notifications, etc.
                }
            """.trimIndent(),
            afterExample = """
                class OrderManager(
                    private val paymentProcessor: PaymentProcessor,
                    private val inventoryManager: InventoryManager,
                    private val notificationService: NotificationService
                ) {
                    // Focused on coordinating order processing
                }
                
                class PaymentProcessor {
                    // Focused on payment processing
                }
                
                class InventoryManager {
                    // Focused on inventory management
                }
            """.trimIndent(),
            effort = Effort.LARGE,
            references = listOf(
                "https://refactoring.guru/smells/large-class",
                "SOLID Principles - Single Responsibility Principle"
            )
        )
    }
    
    /**
     * Creates a finding for too many parameters.
     */
    private fun createTooManyParametersFinding(
        file: FileInfo,
        function: KtNamedFunction,
        paramCount: Int
    ): Finding {
        val functionName = function.name ?: "anonymous"
        val snippet = getCodeSnippet(function.text, maxLines = 5)
        
        return Finding(
            id = "code-smell-too-many-params-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Too Many Parameters: $functionName",
            description = "Function '$functionName' has $paramCount parameters, exceeding the recommended maximum of $MAX_PARAMETERS. " +
                    "Functions with many parameters are harder to understand and use.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = snippet,
            recommendation = "Reduce the number of parameters by grouping related parameters into a data class or configuration object. " +
                    "This makes the function signature cleaner and easier to evolve.",
            beforeExample = """
                fun createUser(
                    name: String,
                    email: String,
                    phone: String,
                    address: String,
                    city: String,
                    zipCode: String
                ) {
                    // Implementation
                }
            """.trimIndent(),
            afterExample = """
                data class UserInfo(
                    val name: String,
                    val email: String,
                    val phone: String,
                    val address: Address
                )
                
                data class Address(
                    val street: String,
                    val city: String,
                    val zipCode: String
                )
                
                fun createUser(userInfo: UserInfo) {
                    // Implementation
                }
            """.trimIndent(),
            effort = Effort.SMALL,
            references = listOf(
                "https://refactoring.guru/smells/long-parameter-list",
                "Clean Code by Robert C. Martin - Function Arguments"
            )
        )
    }
    
    /**
     * Creates a finding for high cyclomatic complexity.
     */
    private fun createHighComplexityFinding(
        file: FileInfo,
        function: KtNamedFunction,
        complexity: Int
    ): Finding {
        val functionName = function.name ?: "anonymous"
        val snippet = getCodeSnippet(function.text, maxLines = 10)
        
        return Finding(
            id = "code-smell-high-complexity-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "High Cyclomatic Complexity: $functionName",
            description = "Function '$functionName' has cyclomatic complexity of $complexity, exceeding the recommended maximum of $MAX_COMPLEXITY. " +
                    "High complexity indicates too many decision points, making the code difficult to test and understand.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = snippet,
            recommendation = "Simplify this function by extracting complex conditions into separate functions with descriptive names. " +
                    "Use early returns to reduce nesting. Consider using polymorphism or strategy pattern for complex branching logic.",
            beforeExample = """
                fun calculatePrice(item: Item, user: User): Money {
                    if (item.isOnSale) {
                        if (user.isPremium) {
                            if (item.category == "electronics") {
                                // Complex calculation
                            } else if (item.category == "clothing") {
                                // Another calculation
                            }
                        } else {
                            // More branching
                        }
                    } else {
                        // Even more branching
                    }
                }
            """.trimIndent(),
            afterExample = """
                fun calculatePrice(item: Item, user: User): Money {
                    return when {
                        item.isOnSale && user.isPremium -> calculatePremiumSalePrice(item)
                        item.isOnSale -> calculateSalePrice(item)
                        user.isPremium -> calculatePremiumPrice(item)
                        else -> item.basePrice
                    }
                }
                
                private fun calculatePremiumSalePrice(item: Item): Money {
                    // Focused calculation
                }
            """.trimIndent(),
            effort = Effort.MEDIUM,
            references = listOf(
                "https://en.wikipedia.org/wiki/Cyclomatic_complexity",
                "Refactoring: Improving the Design of Existing Code"
            )
        )
    }
    
    /**
     * Creates a finding for deep nesting.
     */
    private fun createDeepNestingFinding(
        file: FileInfo,
        function: KtNamedFunction,
        depth: Int
    ): Finding {
        val functionName = function.name ?: "anonymous"
        val snippet = getCodeSnippet(function.text, maxLines = 10)
        
        return Finding(
            id = "code-smell-deep-nesting-${file.path}-${getLineNumber(function)}",
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Deep Nesting: $functionName",
            description = "Function '$functionName' has nesting depth of $depth, exceeding the recommended maximum of $MAX_NESTING_DEPTH. " +
                    "Deep nesting makes code harder to read and understand.",
            file = file.path,
            lineNumber = getLineNumber(function),
            codeSnippet = snippet,
            recommendation = "Reduce nesting by using early returns (guard clauses), extracting nested blocks into separate functions, " +
                    "or inverting conditions. Prefer flat code structure over deeply nested code.",
            beforeExample = """
                fun processData(data: Data?) {
                    if (data != null) {
                        if (data.isValid) {
                            if (data.hasPermission) {
                                if (data.isActive) {
                                    // Process data
                                }
                            }
                        }
                    }
                }
            """.trimIndent(),
            afterExample = """
                fun processData(data: Data?) {
                    if (data == null) return
                    if (!data.isValid) return
                    if (!data.hasPermission) return
                    if (!data.isActive) return
                    
                    // Process data
                }
            """.trimIndent(),
            effort = Effort.SMALL,
            references = listOf(
                "https://refactoring.guru/replace-nested-conditional-with-guard-clauses",
                "Clean Code by Robert C. Martin - Error Handling"
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
